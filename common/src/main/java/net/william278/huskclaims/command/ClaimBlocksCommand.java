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
import de.themoep.minedown.adventure.MineDown;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.william278.huskclaims.HuskClaims;
import net.william278.huskclaims.claim.ServerWorldClaim;
import net.william278.huskclaims.config.Locales;
import net.william278.huskclaims.config.Settings;
import net.william278.huskclaims.user.ClaimBlocksManager;
import net.william278.huskclaims.user.CommandUser;
import net.william278.huskclaims.user.OnlineUser;
import net.william278.huskclaims.user.User;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class ClaimBlocksCommand extends Command implements UserListTabCompletable, GlobalClaimsProvider {

    protected ClaimBlocksCommand(@NotNull HuskClaims plugin) {
        super(
                List.of("claimblocks", "adjustclaimblocks"),
                "[user] [<set|add|remove|gift> <amount>]",
                plugin
        );
        addAdditionalPermissions(Map.of(
                "other", true,
                "gift", true,
                "edit", true
        ));
    }

    @Override
    public void execute(@NotNull CommandUser executor, @NotNull String[] args) {
        final Optional<User> optionalUser = resolveUser(executor, args);
        final ClaimBlockOption option = parseClaimBlockOption(args).orElse(ClaimBlockOption.SHOW);
        final Optional<Long> amount = parseClaimBlocksArg(args, 2);
        if (optionalUser.isEmpty() || (amount.isEmpty() && option != ClaimBlockOption.SHOW)) {
            plugin.getLocales().getLocale("error_invalid_syntax", getUsage())
                    .ifPresent(executor::sendMessage);
            return;
        }

        final User user = optionalUser.get();
        if (!option.hasPermission(executor, this) || (!hasPermission(executor, "other")
                && (executor instanceof OnlineUser other && !other.equals(user)))) {
            plugin.getLocales().getLocale("error_no_permission")
                    .ifPresent(executor::sendMessage);
            return;
        }
        performClaimBlockOperation(executor, user, option, amount.orElse(0L));
    }

    private void performClaimBlockOperation(@NotNull CommandUser executor, @NotNull User user,
                                            @NotNull ClaimBlockOption option, long amount) {
        switch (option) {
            case SHOW -> showClaimBlocks(executor, user);
            case ADD -> changeClaimBlocks(executor, user, amount, false);
            case REMOVE -> changeClaimBlocks(executor, user, -amount, false);
            case SET -> changeClaimBlocks(executor, user, amount, true);
            case GIFT -> changeClaimBlocksPlayerGift(executor, user, amount);
        }
    }

    private void showClaimBlocks(@NotNull CommandUser executor, @NotNull User user) {
        final List<ServerWorldClaim> claims = getUserClaims(user);
        final Settings.ClaimSettings settings = getPlugin().getSettings().getClaims();
        final Locales locales = plugin.getLocales();

        // Calculate the user's claim block totals
        long started = settings.getStartingClaimBlocks();
        long available = Math.max(0, plugin.getClaimBlocks(user));
        long spent = Math.max(0, claims.stream().map(ServerWorldClaim::getSurfaceArea).reduce(0L, Long::sum));
        long earned = (available + spent) - started;
        long accrued = started + earned;

        // Prepare from locales
        final String name = user.getName();
        final String count = Integer.toString(claims.size());
        final List<MineDown> lines = Lists.newArrayList();
        locales.getLocale("claim_block_balance_header", name).ifPresent(lines::add);
        locales.getLocale("claim_block_balance_started", Long.toString(started)).ifPresent(lines::add);
        if (earned >= 0) {
            locales.getLocale("claim_block_balance_acquired", Long.toString(earned)).ifPresent(lines::add);
        }
        locales.getLocale("claim_block_balance_accrued", Long.toString(accrued)).ifPresent(lines::add);
        if (earned < 0) {
            locales.getLocale("claim_block_balance_deducted", Long.toString(-earned)).ifPresent(lines::add);
        }
        if (spent > 0) {
            locales.getLocale("claim_block_balance_spent", Long.toString(spent), count, name).ifPresent(lines::add);
        }
        locales.getLocale("claim_block_balance_available", Long.toString(available)).ifPresent(lines::add);

        // Build component & dispatch
        final TextComponent.Builder builder = Component.text();
        lines.stream().map(MineDown::toComponent).forEach(c -> builder.append(c).append(Component.newline()));
        executor.sendMessage(builder.build());
    }

    private void changeClaimBlocks(@NotNull CommandUser executor, @NotNull User user, long changeBy, boolean set) {
        plugin.editClaimBlocks(
                user,
                ClaimBlocksManager.ClaimBlockSource.ADMIN_ADJUSTMENT,
                (blocks) -> Math.max(0, set ? changeBy : blocks + changeBy),
                (newBalance) -> plugin.getLocales().getLocale("claim_blocks_updated", user.getName(),
                        Long.toString(newBalance)).ifPresent(executor::sendMessage)
        );
    }

    private void changeClaimBlocksPlayerGift(@NotNull CommandUser executor, @NotNull User user, long changeBy) {
        if (!(executor instanceof OnlineUser onlineExecuter)) {
            getPlugin().getLocales().getLocale("error_command_in_game_only").ifPresent(executor::sendMessage);
            return;
        }

        // can't gift to yourself (Needed else you dupe claimblocks because of race condition on claimblocks change event)
        if (onlineExecuter.getUuid().equals(user.getUuid())) {
            return;
        }

        final long ownedClaimBLocks = getPlugin().getClaimBlocks(onlineExecuter);
        if (ownedClaimBLocks < changeBy) {
            getPlugin().getLocales().getLocale("error_not_enough_claim_blocks",
                    Long.toString(changeBy - ownedClaimBLocks)).ifPresent(executor::sendMessage);
            return;
        }

        plugin.editClaimBlocks(
                onlineExecuter,
                ClaimBlocksManager.ClaimBlockSource.USER_GIFTED,
                (blocks) -> Math.max(0, blocks - changeBy),
                (newBalance) -> plugin.getLocales().getLocale("claim_blocks_gifted", user.getName(),
                        Long.toString(changeBy)).ifPresent(executor::sendMessage));

        final OnlineUser giftedPlayer = plugin.getOnlineUsers().stream().filter(online_player -> online_player.getUuid().equals(user.getUuid())).findFirst().orElse(null);

        plugin.editClaimBlocks(
                user,
                ClaimBlocksManager.ClaimBlockSource.USER_GIFTED,
                (blocks) -> Math.max(0, blocks + changeBy),
                (newBalance) -> {
                    if (giftedPlayer != null) {
                        plugin.getLocales().getLocale("claim_blocks_gift_received", onlineExecuter.getName(),
                        Long.toString(changeBy)).ifPresent(giftedPlayer::sendMessage);
                    }
                });
    }

    private Optional<ClaimBlockOption> parseClaimBlockOption(@NotNull String[] args) {
        return parseStringArg(args, 1).flatMap(ClaimBlockOption::matchClaimBlockOption);
    }

    private enum ClaimBlockOption {
        SHOW,
        SET,
        ADD,
        GIFT,
        REMOVE;

        public static Optional<ClaimBlockOption> matchClaimBlockOption(@NotNull String text) {
            return Arrays.stream(values()).filter(o -> o.getId().equals(text.toLowerCase(Locale.ENGLISH))).findFirst();
        }

        @NotNull
        public static List<String> getSuggestions(@NotNull CommandUser user, @NotNull Command command) {
            return Arrays.stream(values())
                    .filter(o -> o.hasPermission(user, command))
                    .map(ClaimBlockOption::getId).toList();
        }

        public boolean hasPermission(@NotNull CommandUser user, @NotNull Command command) {
            return switch (this) {
                case SHOW -> command.hasPermission(user);
                case SET, ADD, REMOVE -> command.hasPermission(user, "edit");
                case GIFT -> command.hasPermission(user, "gift");
            };
        }

        @NotNull
        public String getId() {
            return name().toLowerCase(Locale.ENGLISH);
        }
    }

    @Override
    @Nullable
    public List<String> suggest(@NotNull CommandUser user, @NotNull String[] args) {
        return switch (args.length) {
            case 0, 1 -> UserListTabCompletable.super.suggest(user, args);
            case 2 -> ClaimBlockOption.getSuggestions(user, this);
            default -> null;
        };
    }
}
