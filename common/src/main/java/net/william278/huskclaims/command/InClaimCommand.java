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
import net.william278.huskclaims.config.Settings;
import net.william278.huskclaims.trust.TrustLevel;
import net.william278.huskclaims.trust.TrustTag;
import net.william278.huskclaims.trust.Trustable;
import net.william278.huskclaims.trust.UserGroup;
import net.william278.huskclaims.user.OnlineUser;
import net.william278.huskclaims.user.SavedUser;
import net.william278.huskclaims.user.User;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.StringJoiner;

public abstract class InClaimCommand extends OnlineUserCommand {

    private final TrustLevel.Privilege privilege;

    protected InClaimCommand(@NotNull List<String> aliases, @NotNull String usage,
                             @Nullable TrustLevel.Privilege privilege, @NotNull HuskClaims plugin) {
        super(aliases, usage, plugin);
        this.privilege = privilege;
        addAdditionalPermissions(Map.of("other", true));
    }

    protected InClaimCommand(@NotNull List<String> aliases, @NotNull String usage,
                             @NotNull HuskClaims plugin) {
        this(aliases, usage, null, plugin);
    }

    @Override
    public void execute(@NotNull OnlineUser user, @NotNull String[] args) {
        final Optional<ClaimWorld> optionalWorld = plugin.getClaimWorld(user.getWorld());
        if (optionalWorld.isEmpty()) {
            plugin.getLocales().getLocale("world_not_claimable")
                    .ifPresent(user::sendMessage);
            return;
        }

        final ClaimWorld world = optionalWorld.get();
        final Optional<Claim> optionalClaim = world.getClaimAt(user.getPosition());
        if (optionalClaim.isEmpty()) {
            plugin.getLocales().getLocale("error_not_in_claim")
                    .ifPresent(user::sendMessage);
            return;
        }

        final Claim claim = optionalClaim.get();
        if (privilege != null && !claim.isPrivilegeAllowed(privilege, user, plugin)
                && !hasPermission(user, "other")) {
            plugin.getLocales().getLocale("no_claim_privilege")
                    .ifPresent(user::sendMessage);
            return;
        }

        this.execute(user, world, claim, args);
    }

    /**
     * Returns if an {@link OnlineUser} has access to manage the trust level of a {@link Trustable}
     *
     * @param executor  The executor
     * @param trustable The trustable
     * @param claim     The claim
     * @return if the executor has access
     * @since 1.0
     */
    protected boolean checkUserHasAccess(@NotNull OnlineUser executor, @NotNull Trustable trustable,
                                         @NotNull Claim claim) {
        if (claim.getOwner().map(o -> o.equals(executor.getUuid())).orElse(false)) {
            return true;
        }

        // Check the trustable is not banned from the claim
        if (trustable instanceof User user && claim.isUserBanned(user)) {
            plugin.getLocales().getLocale("error_user_banned", user.getName())
                    .ifPresent(executor::sendMessage);
            return false;
        }

        // Check if the executor is allowed to set the trust level of the trustable
        final Optional<Integer> trustableWeight = claim.getEffectiveTrustLevel(trustable, plugin)
                .map(TrustLevel::getWeight);
        final int executorWeight = claim.getEffectiveTrustLevel(executor, plugin)
                .map(TrustLevel::getWeight).orElse(Integer.MAX_VALUE);
        if (trustableWeight.isPresent() && executorWeight < trustableWeight.get()) {
            plugin.getLocales().getLocale("error_trust_level_rank", trustable.getTrustIdentifier(plugin))
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

        // Resolve tag
        final Settings.TrustTagSettings tags = plugin.getSettings().getTrustTags();
        if (tags.isEnabled() && name.startsWith(tags.getTagSpecifierPrefix())) {
            return resolveTag(user, name.substring(tags.getTagSpecifierPrefix().length()), claim);
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
                .map(SavedUser::getUser)
                .or(() -> {
                    plugin.getLocales().getLocale("error_invalid_user", name)
                            .ifPresent(user::sendMessage);
                    return Optional.empty();
                });
    }

    protected Optional<TrustTag> resolveTag(@NotNull OnlineUser user, @NotNull String name, @NotNull Claim claim) {
        return plugin.getTrustTag(name)
                .or(() -> {
                    plugin.getLocales().getLocale("error_invalid_tag", name)
                            .ifPresent(user::sendMessage);
                    return Optional.empty();
                })
                .flatMap(tag -> {
                    if (!tag.canUse(user)) {
                        plugin.getLocales().getLocale("error_no_permission_tag", tag.getName())
                                .ifPresent(user::sendMessage);
                        return Optional.empty();
                    }
                    return Optional.of(tag);
                });
    }

    @NotNull
    protected static String getUsageText(@NotNull Settings settings) {
        final StringJoiner joiner = new StringJoiner("|");
        joiner.add("players");
        if (settings.getUserGroups().isEnabled()) {
            joiner.add("%sgroups".formatted(settings.getUserGroups().getGroupSpecifierPrefix()));
        }
        if (settings.getTrustTags().isEnabled()) {
            joiner.add("%stags".formatted(settings.getTrustTags().getTagSpecifierPrefix()));
        }
        return "<%s>".formatted(joiner.toString());
    }

    public abstract void execute(@NotNull OnlineUser executor, @NotNull ClaimWorld world, @NotNull Claim claim,
                                 @NotNull String[] args);

}
