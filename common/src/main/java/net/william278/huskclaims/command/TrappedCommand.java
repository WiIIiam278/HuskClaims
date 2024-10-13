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

import net.william278.cloplib.operation.Operation;
import net.william278.cloplib.operation.OperationType;
import net.william278.huskclaims.HuskClaims;
import net.william278.huskclaims.claim.Claim;
import net.william278.huskclaims.claim.ClaimWorld;
import net.william278.huskclaims.user.OnlineUser;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class TrappedCommand extends InClaimCommand {

    protected TrappedCommand(@NotNull HuskClaims plugin) {
        super(
                List.of("trapped"),
                "",
                plugin
        );
    }

    @Override
    public void execute(@NotNull OnlineUser executor, @NotNull ClaimWorld world,
                        @NotNull Claim claim, @NotNull String[] args) {
        if (canBuild(claim, executor)) {
            plugin.getLocales().getLocale("error_not_trapped")
                    .ifPresent(executor::sendMessage);
            return;
        }

        plugin.getLocales().getLocale("trapped_teleporting")
                .ifPresent(executor::sendMessage);
        plugin.teleportOutOfClaim(executor, false);
    }

    private boolean canBuild(@NotNull Claim claim, @NotNull OnlineUser user) {
        return claim.isOperationAllowed(Operation.of(user, OperationType.BLOCK_PLACE, user.getPosition()), plugin)
               && claim.isOperationAllowed(Operation.of(user, OperationType.BLOCK_BREAK, user.getPosition()), plugin);
    }

}
