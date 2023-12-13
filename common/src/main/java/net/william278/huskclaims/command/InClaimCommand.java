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
import net.william278.huskclaims.group.UserGroup;
import net.william278.huskclaims.user.OnlineUser;
import net.william278.huskclaims.user.SavedUser;
import net.william278.huskclaims.user.User;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;

public abstract class InClaimCommand extends OnlineUserCommand {

    private final TrustLevel.Privilege privilege;

    protected InClaimCommand(@NotNull List<String> aliases, @Nullable TrustLevel.Privilege privilege,
                             @NotNull HuskClaims plugin) {
        super(aliases, getUsageText(plugin.getSettings().getUserGroups()), plugin);
        this.privilege = privilege;
    }

    @Override
    public void execute(@NotNull OnlineUser user, @NotNull String[] args) {
        final Optional<ClaimWorld> world = plugin.getClaimWorld(user.getWorld());
        if (world.isEmpty()) {
            plugin.getLocales().getLocale("world_not_claimable")
                    .ifPresent(user::sendMessage);
            return;
        }

        final Optional<Claim> claim = world.get().getClaimAt(user.getPosition());
        if (claim.isEmpty()) {
            plugin.getLocales().getLocale("error_not_in_claim")
                    .ifPresent(user::sendMessage);
            return;
        }

        this.execute(user, world.get(), claim.get(), args);
    }

    /**
     * Returns if an {@link OnlineUser} has access to manage the trust level of a {@link Trustable}
     *
     * @param executor  The executor
     * @param trustable The trustable
     * @param world     The world
     * @param claim     The claim
     * @return if the executor has access
     * @since 1.0
     */
    protected boolean checkUserHasAccess(@NotNull OnlineUser executor, @NotNull Trustable trustable,
                                         @NotNull ClaimWorld world, @NotNull Claim claim) {
        if (claim.getOwner().map(o -> o.equals(executor.getUuid())).orElse(false)) {
            return true;
        }

        if (privilege != null && !claim.isPrivilegeAllowed(privilege, executor, world, plugin)) {
            plugin.getLocales().getLocale("no_claim_privilege")
                    .ifPresent(executor::sendMessage);
            return false;
        }

        // Check if the executor is allowed to set the trust level of the trustable
        final Optional<Integer> trustableWeight = claim.getEffectiveTrustLevel(trustable, world, plugin)
                .map(TrustLevel::getWeight);
        final int executorWeight = claim.getEffectiveTrustLevel(executor, world, plugin)
                .map(TrustLevel::getWeight).orElse(Integer.MAX_VALUE);
        if (trustableWeight.isPresent() && executorWeight < trustableWeight.get()) {
            plugin.getLocales().getLocale("error_trust_level_rank")
                    .ifPresent(executor::sendMessage);
            return false;
        }
        return true;
    }

    protected Optional<? extends Trustable> resolveTrustable(@NotNull OnlineUser user, @NotNull String name,
                                                             @NotNull Claim claim) {
        // Resolve group
        final Settings.UserGroupSettings groups = plugin.getSettings().getUserGroups();
        if (groups.isEnabled() && name.startsWith(groups.getGroupSpecifierPrefix()) && claim.getOwner().isPresent()) {
            return resolveGroup(user, name.substring(groups.getGroupSpecifierPrefix().length()), claim, groups);
        }

        // Resolve user
        return resolveUser(user, name);
    }

    protected Optional<UserGroup> resolveGroup(@NotNull OnlineUser user, @NotNull String name,
                                               @NotNull Claim claim, @NotNull Settings.UserGroupSettings groups) {
        return claim.getOwner().flatMap(uuid -> plugin.getUserGroup(uuid, name))
                .or(() -> {
                    plugin.getLocales().getLocale("error_invalid_group", name)
                            .ifPresent(user::sendMessage);
                    return Optional.empty();
                });
    }

    protected Optional<User> resolveUser(@NotNull OnlineUser user, @NotNull String name) {
        return plugin.getDatabase().getUser(name)
                .map(SavedUser::user)
                .or(() -> {
                    plugin.getLocales().getLocale("error_invalid_user", name)
                            .ifPresent(user::sendMessage);
                    return Optional.empty();
                });
    }

    @NotNull
    protected static String getUsageText(@NotNull Settings.UserGroupSettings groupSettings) {
        return String.format(
                "[<player%s>]",
                groupSettings.isEnabled() ? String.format("|%s<group>", groupSettings.getGroupSpecifierPrefix()) : ""
        );
    }

    public abstract void execute(@NotNull OnlineUser executor, @NotNull ClaimWorld world, @NotNull Claim claim,
                                 @NotNull String[] args);

}
