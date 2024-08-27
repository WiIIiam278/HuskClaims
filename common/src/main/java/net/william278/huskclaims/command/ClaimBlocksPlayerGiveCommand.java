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

import java.util.List;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import net.william278.huskclaims.HuskClaims;
import net.william278.huskclaims.user.ClaimBlocksManager;
import net.william278.huskclaims.user.CommandUser;
import net.william278.huskclaims.user.OnlineUser;
import net.william278.huskclaims.user.User;

public class ClaimBlocksPlayerGiveCommand extends OnlineUserCommand implements UserListTabCompletable {

    protected ClaimBlocksPlayerGiveCommand(@NotNull HuskClaims plugin) {
        super(
                List.of("claimblocksgive"),
                "[user] [amount]",
                plugin);
    }

    @Override
    public void execute(@NotNull OnlineUser user, @NotNull String[] args) {
        final User toPlayer = resolveUser(user, args).get();
        final Long amount = parseClaimBlocksArg(args, 1).orElse(0L);

        if (amount > 0) {

            final long ownedClaimBLocks = getPlugin().getClaimBlocks(user);
            if (ownedClaimBLocks < amount) {
                getPlugin().getLocales().getLocale("error_not_enough_claim_blocks",
                        Long.toString(amount - ownedClaimBLocks)).ifPresent(user::sendMessage);
                return;
            }

            changeClaimBlocks(user, toPlayer, amount);
        }
    }

    private void changeClaimBlocks(@NotNull OnlineUser executor, @NotNull User user, long changeBy) {
        plugin.editClaimBlocks(
                executor,
                ClaimBlocksManager.ClaimBlockSource.USER_GIFTED,
                (blocks) -> Math.max(0,blocks - changeBy),
                (newBalance) -> plugin.getLocales().getLocale("claim_blocks_gifted", user.getName(),
                        Long.toString(changeBy)).ifPresent(executor::sendMessage));

        final OnlineUser gifted_player = plugin.getOnlineUsers().stream().filter(online_player -> online_player.getUuid().equals(user.getUuid())).findFirst().orElse(null);

        plugin.editClaimBlocks(
                user,
                ClaimBlocksManager.ClaimBlockSource.USER_GIFTED,
                (blocks) -> Math.max(0, blocks + changeBy),
                (newBalance) -> {
                    if (gifted_player != null) {
                        plugin.getLocales().getLocale("claim_blocks_gift_received", executor.getName(),
                        Long.toString(changeBy)).ifPresent(gifted_player::sendMessage);
                    }
                });
    }


    @Override
    @Nullable
    public List<String> suggest(@NotNull CommandUser user, @NotNull String[] args) {
        return switch (args.length) {
            case 0, 1 -> UserListTabCompletable.super.suggest(user, args);
            default -> null;
        };
    }

}
