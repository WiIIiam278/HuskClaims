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
import net.william278.huskclaims.user.OnlineUser;
import net.william278.huskclaims.user.User;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Optional;

public class TransferClaimCommand extends InClaimCommand {

    protected TransferClaimCommand(@NotNull HuskClaims plugin) {
        super(List.of("transferclaim"), "<username>", null, plugin);
        setOperatorCommand(true);
    }

    @Override
    public void execute(@NotNull OnlineUser executor, @NotNull ClaimWorld world,
                        @NotNull Claim claim, @NotNull String[] args) {
        // Ensure we're dealing with the parent claim
        final Optional<Claim> parent = claim.getParent(world);
        if (parent.isPresent()) {
            claim = parent.get();
        }

        // Ensure we're not dealing with an admin claim and that the user has permission to transfer
        if (claim.isAdminClaim(world)) {
            plugin.getLocales().getLocale("error_admin_claim_transfer")
                    .ifPresent(executor::sendMessage);
            return;
        }
        if (claim.getOwner().map(o -> o.equals(executor.getUuid())).orElse(false)
                || hasPermission(executor, "other")) {
            plugin.getLocales().getLocale("no_transfer_permission")
                    .ifPresent(executor::sendMessage);
            return;
        }

        // Get the name of the target user
        final Optional<User> user = resolveUser(executor, args);
        if (user.isEmpty()) {
            plugin.getLocales().getLocale("error_invalid_syntax", getUsage())
                    .ifPresent(executor::sendMessage);
            return;
        }

        plugin.userTransferClaim(executor, claim, world, user.get());
    }
}
