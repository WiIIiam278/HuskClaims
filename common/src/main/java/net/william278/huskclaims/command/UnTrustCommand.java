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
import net.william278.huskclaims.claim.Trustable;
import net.william278.huskclaims.group.UserGroup;
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
        super(List.of("untrust"), plugin);
    }

    @Override
    public void execute(@NotNull OnlineUser executor, @NotNull ClaimWorld world,
                        @NotNull Claim claim, @NotNull String[] args) {
        final Optional<String> toUntrust = parseStringArg(args, 0);
        if (toUntrust.isEmpty()) {
            plugin.getLocales().getLocale("error_invalid_syntax")
                    .ifPresent(executor::sendMessage);
            return;
        }

        // Resolve the trustable and check the executor has access
        resolveTrustable(toUntrust.get(), claim)
                .flatMap(t -> checkUserHasAccess(executor, t, world, claim) ? Optional.of(t) : Optional.empty())
                .map(t -> removeTrust(t, world, claim))
                .ifPresentOrElse(
                        t -> plugin.getLocales().getLocale(t ? "trust_level_removed" : "error_not_trusted",
                                toUntrust.get()).ifPresent(executor::sendMessage),
                        () -> plugin.getLocales().getLocale("error_not_trusted")
                                .ifPresent(executor::sendMessage)
                );
    }

    @Blocking
    private boolean removeTrust(@NotNull Trustable toUntrust, @NotNull ClaimWorld world, @NotNull Claim claim) {
        boolean removed = false;
        if (toUntrust instanceof User user) {
            removed = claim.getTrustedUsers().remove(user.getUuid()) != null;
            plugin.getDatabase().updateClaimWorld(world);
        } else if (toUntrust instanceof UserGroup group) {
            removed = claim.getTrustedGroups().remove(group.name()) != null;
            plugin.getDatabase().updateClaimWorld(world);
        }
        return removed;
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
                    return names;
                }))
                .orElse(null);
    }

}
