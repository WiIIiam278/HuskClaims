/*
 * This file is part of HuskClaims, licensed under the Apache License 2.0.
 *
 *  Copyright (c) William278 <will27528@gmail.com>
 *  Copyright (c) contributors
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package net.william278.huskclaims.command;

import net.william278.huskclaims.HuskClaims;
import net.william278.huskclaims.claim.Claim;
import net.william278.huskclaims.claim.ClaimWorld;
import net.william278.huskclaims.claim.TrustLevel;
import net.william278.huskclaims.claim.Trustable;
import net.william278.huskclaims.config.Settings;
import net.william278.huskclaims.user.OnlineUser;
import net.william278.huskclaims.user.SavedUser;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.UUID;

//todo
public class TrustCommand extends InClaimCommand implements TrustableTabCompletable {

    private final TrustLevel level;

    protected TrustCommand(@NotNull TrustLevel level, @NotNull HuskClaims plugin) {
        super(level.getCommandAliases(), "<player>", plugin);
        this.level = level;
    }

    @Override
    public void execute(@NotNull OnlineUser executor, @NotNull ClaimWorld world, @NotNull Claim claim,
                        @NotNull String[] args) {
        final Optional<Trustable> optionalTrustable = parseStringArg(args, 0)
                .flatMap(n -> resolveTrustable(claim.getOwner().orElse(null), n));
        if (optionalTrustable.isEmpty()) {
            plugin.getLocales().getLocale("error_invalid_syntax")
                    .ifPresent(executor::sendMessage);
            return;
        }

        setTrustLevel(optionalTrustable.get(), executor, world, claim);
    }

    private void setTrustLevel(@NotNull Trustable trustable, @NotNull OnlineUser executor,
                               @NotNull ClaimWorld world, @NotNull Claim claim) {
        if (!claim.isPrivilegeAllowed(TrustLevel.Privilege.MANAGE_TRUSTEES, executor, world, plugin)) {
            plugin.getLocales().getLocale("no_managing_permission")
                    .ifPresent(executor::sendMessage);
            return;
        }

        // Check if the executor is allowed to set the trust level of the trustable
        final Optional<Integer> trustableWeight = claim.getEffectiveTrustLevel(trustable, world, plugin)
                .map(TrustLevel::getWeight);
        final int executorWeight = claim.getEffectiveTrustLevel(executor, world, plugin)
                .map(TrustLevel::getWeight).orElse(Integer.MAX_VALUE);
        if (trustableWeight.isPresent() && executorWeight <= trustableWeight.get()) {
            plugin.getLocales().getLocale("error_trust_level_rank")
                    .ifPresent(executor::sendMessage);
            return;
        }

        claim.setTrustLevel(trustable, level);
        plugin.getDatabase().updateClaimWorld(world);
        plugin.getLocales().getLocale("trust_level_set", trustable.getTrustIdentifier(plugin), level.getDisplayName());
    }

    private Optional<? extends Trustable> resolveTrustable(@Nullable UUID claimOwner, @NotNull String name) {
        // Resolve group
        final Settings.UserGroupSettings groups = plugin.getSettings().getUserGroups();
        if (groups.isEnabled() && claimOwner != null && name.startsWith(groups.getGroupSpecifierPrefix())) {
            return plugin.getUserGroup(claimOwner, name.substring(groups.getGroupSpecifierPrefix().length()));
        }

        // Resolve username
        return plugin.getDatabase().getUser(name).map(SavedUser::user);
    }

    @Nullable
    @Override
    public UUID getGroupOwner(@NotNull OnlineUser user) {
        return plugin.getClaimAt(user.getPosition()).flatMap(Claim::getOwner).orElse(null);
    }

}
