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
import net.william278.huskclaims.user.CommandUser;
import net.william278.huskclaims.user.OnlineUser;
import net.william278.huskclaims.user.SavedUser;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.UUID;

//todo
public class TrustLevelCommand extends Command implements TrustableTabCompletable {

    private final TrustLevel level;

    protected TrustLevelCommand(@NotNull TrustLevel level, @NotNull HuskClaims plugin) {
        super(level.getCommandAliases(), "<player>", plugin);
        this.level = level;
    }

    @Override
    public void execute(@NotNull CommandUser executor, @NotNull String[] args) {
        final OnlineUser trustee = (OnlineUser) executor;
        final Optional<ClaimWorld> claimWorld = plugin.getClaimWorld(trustee.getWorld());
        final Optional<Claim> claim = claimWorld.flatMap(w -> w.getClaimAt(trustee.getPosition()));
        if (claim.isEmpty()) {
            plugin.getLocales().getLocale("error_not_in_claim")
                    .ifPresent(executor::sendMessage);
            return;
        }

        final ClaimWorld world = claimWorld.get();
        final Claim target = claim.get();
        final Optional<Trustable> saved = parseStringArg(args, 0).flatMap(n -> resolveTrustable(trustee, target.getOwner().orElse(null), n));
        if (saved.isEmpty()) {
            plugin.getLocales().getLocale("error_invalid_syntax")
                    .ifPresent(executor::sendMessage);
            return;
        }

        if (!target.isPrivilegeAllowed(TrustLevel.Privilege.MANAGE_TRUSTEES, trustee, world, plugin)) {
            plugin.getLocales().getLocale("no_managing_permission")
                    .ifPresent(executor::sendMessage);
            return;
        }

        final Optional<Integer> ranked = target.getEffectiveTrustLevel(saved.get(), world, plugin).map(TrustLevel::getWeight);
        final Optional<Integer> ranker = target.getEffectiveTrustLevel(trustee, world, plugin).map(TrustLevel::getWeight);
        if (ranked.isEmpty() || ranker.isEmpty() || ranked.get() >= ranker.get()) {
            plugin.getLocales().getLocale("error_trust_level_rank")
                    .ifPresent(executor::sendMessage);
            return;
        }

        target.setTrustLevel(saved.get(), level);
        plugin.getDatabase().updateClaimWorld(world);
        plugin.getLocales().getLocale("trust_level_set", trustee.getTrustIdentifier(plugin), level.getDisplayName());
    }

    private Optional<? extends Trustable> resolveTrustable(@NotNull OnlineUser trustee, @Nullable UUID claimOwner,
                                                           @NotNull String name) {
        // Resolve group
        final Settings.UserGroupSettings groups = plugin.getSettings().getUserGroups();
        if (groups.isEnabled() && name.startsWith(groups.getGroupSpecifierPrefix())) {
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
