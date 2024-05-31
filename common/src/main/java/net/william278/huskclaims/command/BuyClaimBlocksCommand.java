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
import net.william278.huskclaims.hook.EconomyHook;
import net.william278.huskclaims.user.ClaimBlocksManager;
import net.william278.huskclaims.user.OnlineUser;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Optional;

public class BuyClaimBlocksCommand extends OnlineUserCommand {

    public BuyClaimBlocksCommand(@NotNull HuskClaims plugin) {
        super(
                List.of("buyclaimblocks"),
                "<amount>",
                plugin
        );
    }

    @Override
    public void execute(@NotNull OnlineUser executor, @NotNull String[] args) {
        final Optional<Long> amount = parseClaimBlocksArg(args, 0);
        if (amount.isEmpty()) {
            plugin.getLocales().getLocale("error_invalid_syntax", getUsage())
                    .ifPresent(executor::sendMessage);
            return;
        }

        final Optional<EconomyHook> hook = plugin.getHook(EconomyHook.class);
        if (hook.isEmpty()) {
            plugin.getLocales().getLocale("error_economy_not_found")
                    .ifPresent(executor::sendMessage);
            return;
        }
        buyClaimBlocks(executor, amount.get(), hook.get());
    }

    private void buyClaimBlocks(@NotNull OnlineUser executor, long amount, @NotNull EconomyHook hook) {
        final double cost = getBlockPrice(amount);
        if (!hook.takeMoney(executor, cost, EconomyHook.EconomyReason.BUY_CLAIM_BLOCKS)) {
            plugin.getLocales().getLocale("error_insufficient_funds")
                    .ifPresent(executor::sendMessage);
            return;
        }
        plugin.editClaimBlocks(
                executor,
                ClaimBlocksManager.ClaimBlockSource.PURCHASE,
                (blocks) -> blocks + amount,
                (newBalance) -> plugin.getLocales().getLocale("claim_blocks_purchased",
                                Long.toString(amount), hook.format(cost), Long.toString(newBalance))
                        .ifPresent(executor::sendMessage)
        );
    }

    private double getBlockPrice(long amount) {
        return amount * Math.max(0.0, plugin.getSettings().getHooks().getEconomy().getCostPerBlock());
    }


}
