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
import net.william278.huskclaims.user.CommandUser;
import net.william278.huskclaims.user.OnlineUser;
import net.william278.huskclaims.user.User;
import net.william278.huskclaims.user.UserManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class ClaimBlocksCommand extends Command implements UserListTabCompletable {

    protected ClaimBlocksCommand(@NotNull HuskClaims plugin) {
        super(List.of("claimblocks", "adjustclaimblocks"), "[user] [<set|add|remove> <amount>]", plugin);
        addAdditionalPermissions(Map.of(
                "other", true,
                "edit", true
        ));
    }

    @Override
    public void execute(@NotNull CommandUser executor, @NotNull String[] args) {
        final Optional<User> optionalUser = resolveUser(executor, args);
        final ClaimBlockOption option = parseClaimBlockOption(args).orElse(ClaimBlockOption.SHOW);
        final Optional<Integer> amount = parseIntArg(args, 2).map(i -> Math.max(0, i));
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
        performClaimBlockOperation(executor, user, option, amount.orElse(0));
    }

    private void performClaimBlockOperation(@NotNull CommandUser executor, @NotNull User user,
                                            @NotNull ClaimBlockOption option, int amount) {
        switch (option) {
            case SHOW -> plugin.getLocales().getLocale("claim_block_balance", user.getName(),
                    Long.toString(plugin.getClaimBlocks(user))).ifPresent(executor::sendMessage);
            case ADD -> changeClaimBlocks(executor, user, amount, false);
            case REMOVE -> changeClaimBlocks(executor, user, -amount, false);
            case SET -> changeClaimBlocks(executor, user, amount, true);
        }
    }


    private void changeClaimBlocks(@NotNull CommandUser executor, @NotNull User user, int changeBy, boolean set) {
        plugin.editClaimBlocks(
                user,
                UserManager.ClaimBlockSource.ADMIN_ADJUSTMENT,
                (blocks) -> Math.max(0, set ? changeBy : blocks + changeBy),
                (newBalance) -> plugin.getLocales().getLocale("claim_blocks_updated", user.getName(),
                        Long.toString(newBalance)).ifPresent(executor::sendMessage)
        );
    }

    private Optional<ClaimBlockOption> parseClaimBlockOption(@NotNull String[] args) {
        return parseStringArg(args, 1).flatMap(ClaimBlockOption::matchClaimBlockOption);
    }

    private enum ClaimBlockOption {
        SHOW,
        SET,
        ADD,
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
