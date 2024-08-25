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
import net.william278.huskclaims.trust.TrustLevel;
import net.william278.huskclaims.user.CommandUser;
import net.william278.huskclaims.user.OnlineUser;
import net.william278.huskclaims.user.User;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class ClaimBanCommand extends InClaimCommand implements UserListTabCompletable {

    protected ClaimBanCommand(@NotNull HuskClaims plugin) {
        super(List.of("claimban"), "<ban|unban|list> [username]", TrustLevel.Privilege.MANAGE_BANS, plugin);
    }

    @Override
    public void execute(@NotNull OnlineUser user, @NotNull ClaimWorld world, @NotNull Claim claim,
                        @NotNull String[] args) {
        final Optional<BanOption> action = parseStringArg(args, 0).flatMap(BanOption::from);
        if (action.isEmpty()) {
            plugin.getLocales().getLocale("error_invalid_syntax", getUsage())
                    .ifPresent(user::sendMessage);
            return;
        }

        switch (action.get()) {
            case BAN -> parseUser(user, removeFirstArg(args)).ifPresent(u -> banUser(user, world, claim, u));
            case UNBAN -> parseUser(user, removeFirstArg(args)).ifPresent(u -> unBanUser(user, world, claim, u));
            case LIST -> showBanList(user, world, claim);
        }
    }

    private void banUser(@NotNull OnlineUser executor, @NotNull ClaimWorld world, @NotNull Claim claim,
                         @NotNull User user) {
        if (!checkCanBan(executor, claim, user)) {
            return;
        }

        plugin.fireClaimBanEvent(executor, claim, world, user, (event) -> {
            claim.banUser(user, executor);
            world.cacheUser(user);
            world.cacheUser(executor);
            plugin.getDatabase().updateClaimWorld(world);
            plugin.invalidateClaimListCache(claim.getOwner().orElse(null));
            plugin.getLocales().getLocale("user_banned", user.getName())
                    .ifPresent(executor::sendMessage);

            // Teleport the user out of the claim
            plugin.getOnlineUsers().stream().filter(u -> u.equals(user))
                    .filter(u -> plugin.getClaimAt(u.getPosition()).map(c -> c.equals(claim)).orElse(false))
                    .filter(u -> world.isBannedFromClaim(u, claim, plugin))
                    .findFirst().ifPresent(plugin::teleportOutOfClaim);
        });
    }

    private void unBanUser(@NotNull OnlineUser executor, @NotNull ClaimWorld world, @NotNull Claim claim,
                           @NotNull User user) {
        if (!claim.getBannedUsers().containsKey(user.getUuid())) {
            plugin.getLocales().getLocale("error_user_not_banned", user.getName())
                    .ifPresent(executor::sendMessage);
            return;
        }

        plugin.fireClaimUnBanEvent(executor, claim, world, user, (event) -> {
            claim.unBanUser(user);
            plugin.getDatabase().updateClaimWorld(world);
            plugin.invalidateClaimListCache(claim.getOwner().orElse(null));
            plugin.getLocales().getLocale("user_unbanned", user.getName())
                    .ifPresent(executor::sendMessage);
        });
    }

    private void showBanList(@NotNull OnlineUser executor, @NotNull ClaimWorld world, @NotNull Claim claim) {
        final List<BanEntry> bans = BanEntry.from(claim.getBannedUsers(), world);
        if (bans.isEmpty()) {
            plugin.getLocales().getLocale("error_ban_list_empty")
                    .ifPresent(executor::sendMessage);
            return;
        }

        // Send header
        final String owner = claim.getOwnerName(world, plugin);
        plugin.getLocales().getRawLocale("ban_list_header", plugin.getLocales().getRawLocale(
                        "ban_list_%sclaim".formatted(claim.isChildClaim() ? "child_" : ""), owner).orElse(owner))
                .map(plugin.getLocales()::format)
                .ifPresent(executor::sendMessage);

        // Send banned users
        final StringJoiner joiner = new StringJoiner(plugin.getLocales().getListJoiner());
        bans.forEach(entry -> joiner.add(plugin.getLocales().getRawLocale("ban_list_item",
                entry.banned().getName(), entry.arbiter().getName()).orElse(entry.banned().getName())));
        executor.sendMessage(plugin.getLocales().format(joiner.toString()));
    }

    private boolean checkCanBan(@NotNull OnlineUser executor, @NotNull Claim claim, @NotNull User user) {
        if (claim.getBannedUsers().containsKey(user.getUuid())) {
            plugin.getLocales().getLocale("error_user_banned", user.getName())
                    .ifPresent(executor::sendMessage);
            return false;
        }
        if (executor.equals(user)) {
            plugin.getLocales().getLocale("error_cannot_ban_self")
                    .ifPresent(executor::sendMessage);
            return false;
        }
        if (claim.getOwner().map(owner -> owner.equals(user.getUuid())).orElse(false)) {
            plugin.getLocales().getLocale("error_cannot_ban_owner")
                    .ifPresent(executor::sendMessage);
            return false;
        }
        return checkUserHasAccess(executor, user, claim);
    }

    private Optional<User> parseUser(@NotNull OnlineUser executor, @NotNull String[] args) {
        final Optional<String> username = parseStringArg(args, 0);
        if (username.isEmpty()) {
            plugin.getLocales().getLocale("error_invalid_syntax", getUsage())
                    .ifPresent(executor::sendMessage);
            return Optional.empty();
        }

        final Optional<User> user = resolveUser(executor, username.get());
        if (user.isEmpty()) {
            plugin.getLocales().getLocale("error_user_not_found", username.get())
                    .ifPresent(executor::sendMessage);
            return Optional.empty();
        }
        return user;
    }

    @Override
    @Nullable
    public List<String> suggest(@NotNull CommandUser user, @NotNull String[] args) {
        return switch (args.length) {
            case 0, 1 -> BanOption.getSuggestions();
            case 2 -> switch (args[0].toLowerCase(Locale.ENGLISH)) {
                case "ban" -> UserListTabCompletable.super.suggest(user, args);
                case "unban", "pardon" -> user instanceof OnlineUser o ? getBannedNamesAtUser(o) : null;
                default -> null;
            };
            default -> null;
        };
    }

    @Nullable
    public List<String> getBannedNamesAtUser(@NotNull OnlineUser user) {
        return plugin.getClaimWorld(user.getWorld())
                .flatMap(world -> world.getClaimAt(user.getPosition())
                        .map(claim -> claim.getBannedUsers().keySet().stream()
                                .map(uuid -> world.getUser(uuid).map(User::getName).orElse(null))
                                .filter(Objects::nonNull).toList()))
                .orElse(null);
    }

    enum BanOption {
        BAN("ban"),
        UNBAN("unban", "pardon"),
        LIST("list", "banlist");

        private final String[] aliases;

        BanOption(@NotNull String... aliases) {
            this.aliases = aliases;
        }

        public static Optional<BanOption> from(@NotNull String input) {
            return Arrays.stream(values())
                    .filter(action -> Arrays.stream(action.aliases).anyMatch(alias -> alias.equalsIgnoreCase(input)))
                    .findFirst();
        }

        @NotNull
        public static List<String> getSuggestions() {
            return Arrays.stream(values())
                    .map(action -> action.aliases[0])
                    .toList();
        }
    }

    record BanEntry(@NotNull User banned, @NotNull User arbiter) {

        @Nullable
        private static BanEntry from(@NotNull Map.Entry<UUID, UUID> entry, @NotNull ClaimWorld world) {
            final Optional<User> banned = world.getUser(entry.getKey());
            final Optional<User> arbiter = world.getUser(entry.getValue());
            if (banned.isEmpty() || arbiter.isEmpty()) {
                return null;
            }
            return new BanEntry(banned.get(), arbiter.get());
        }

        @NotNull
        private static List<BanEntry> from(@NotNull Map<UUID, UUID> entries, @NotNull ClaimWorld world) {
            return entries.entrySet().stream()
                    .map(entry -> from(entry, world))
                    .filter(Objects::nonNull)
                    .sorted((a, b) -> a.banned().getName().compareToIgnoreCase(b.banned().getName()))
                    .toList();
        }

    }

}
