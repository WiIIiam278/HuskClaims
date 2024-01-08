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

import com.google.common.collect.Lists;
import net.william278.huskclaims.HuskClaims;
import net.william278.huskclaims.claim.Claim;
import net.william278.huskclaims.claim.ClaimWorld;
import net.william278.huskclaims.config.Settings;
import net.william278.huskclaims.trust.TrustLevel;
import net.william278.huskclaims.trust.TrustTag;
import net.william278.huskclaims.trust.Trustable;
import net.william278.huskclaims.trust.UserGroup;
import net.william278.huskclaims.user.CommandUser;
import net.william278.huskclaims.user.OnlineUser;
import net.william278.huskclaims.user.User;
import org.jetbrains.annotations.Blocking;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;

public class UnTrustCommand extends InClaimCommand implements TabCompletable {

    protected UnTrustCommand(@NotNull HuskClaims plugin) {
        super(
                List.of("untrust"),
                getUsageText(plugin.getSettings()),
                TrustLevel.Privilege.MANAGE_TRUSTEES,
                plugin
        );
    }

    @Override
    public void execute(@NotNull OnlineUser executor, @NotNull ClaimWorld world,
                        @NotNull Claim claim, @NotNull String[] args) {
        final List<String> toUnTrust = parseStringList(args, 0);
        if (toUnTrust.isEmpty()) {
            plugin.getLocales().getLocale("error_invalid_syntax", getUsage())
                    .ifPresent(executor::sendMessage);
            return;
        }

        toUnTrust.forEach(name -> removeTrust(executor, name, world, claim));
    }

    // Resolve the trustable and check the executor has access
    private void removeTrust(@NotNull OnlineUser executor, @NotNull String name,
                             @NotNull ClaimWorld world, @NotNull Claim claim) {
        resolveTrustable(executor, name, claim)
                .flatMap(t -> checkUserHasAccess(executor, t, world, claim) ? Optional.of(t) : Optional.empty())
                .ifPresent(trustable -> removeTrust(executor, trustable, world, claim));
    }

    @Blocking
    private void removeTrust(@NotNull OnlineUser executor, @NotNull Trustable toUntrust,
                             @NotNull ClaimWorld world, @NotNull Claim claim) {
        final String identifier = toUntrust.getTrustIdentifier(plugin);
        final Optional<TrustLevel> trustLevel = claim.getTrustLevel(toUntrust, plugin);
        if (trustLevel.isEmpty()) {
            plugin.getLocales().getLocale("error_not_trusted", identifier)
                    .ifPresent(executor::sendMessage);
            return;
        }

        // Remove the trust level from the trustable
        plugin.fireUntrustEvent(executor, trustLevel.get(), toUntrust, claim, world, (event) -> {
            claim.removeTrustLevel(toUntrust, world);
            plugin.getLocales().getLocale("trust_level_removed", identifier)
                    .ifPresent(executor::sendMessage);
            plugin.getDatabase().updateClaimWorld(world);
            plugin.invalidateClaimListCache(claim.getOwner().orElse(null));
        });
    }

    // We allow the resolution of deleted user groups as we need to be able to remove them from the claim
    @Override
    protected Optional<UserGroup> resolveGroup(@NotNull OnlineUser user, @NotNull String name, @NotNull Claim claim,
                                               @NotNull Settings.UserGroupSettings groups) {
        if (!claim.getTrustedGroups().containsKey(name)) {
            return Optional.empty();
        }
        return claim.getOwner().map(o -> new UserGroup(o, name, List.of()));
    }

    @Override
    protected Optional<TrustTag> resolveTag(@NotNull OnlineUser user, @NotNull String name, @NotNull Claim claim) {
        if (!claim.getTrustedTags().containsKey(name)) {
            return Optional.empty();
        }
        return Optional.of(TrustTag.getDeletedTag(name));
    }

    @Nullable
    @Override
    public List<String> suggest(@NotNull CommandUser user, @NotNull String[] args) {
        return user instanceof OnlineUser online ? getGroupEntries(online) : null;
    }

    @Nullable
    public List<String> getGroupEntries(@NotNull OnlineUser user) {
        return plugin.getClaimWorld(user.getWorld())
                .flatMap(world -> world.getClaimAt(user.getPosition()).map(claim -> {
                    final List<String> names = Lists.newArrayList();
                    claim.getTrustedUsers().keySet().stream()
                            .map(uuid -> world.getUser(uuid).map(User::getName))
                            .forEach(optionalName -> optionalName.ifPresent(names::add));
                    claim.getTrustedGroups().keySet().stream()
                            .map(group -> plugin.getSettings().getUserGroups().getGroupSpecifierPrefix() + group)
                            .forEach(names::add);
                    claim.getTrustedTags().keySet().stream()
                            .map(tag -> plugin.getSettings().getTrustTags().getTagSpecifierPrefix() + tag)
                            .forEach(names::add);
                    return names;
                }))
                .orElse(null);
    }

}
