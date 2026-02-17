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
import net.william278.huskclaims.claim.ClaimingMode;
import net.william278.huskclaims.user.OnlineUser;
import net.william278.huskclaims.user.SavedUser;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Optional;

public class TransferClaimCommand extends InClaimOwnerCommand implements UserListTabCompletable {

    protected TransferClaimCommand(@NotNull HuskClaims plugin) {
        super(
                List.of("transferclaim"),
                "<username>",
                plugin
        );
        setOperatorCommand(true);
    }

    @Override
    public void executeChild(@NotNull OnlineUser executor, @NotNull ClaimWorld world,
                             @NotNull Claim child, @NotNull Claim parent, @NotNull String[] args) {
        super.execute(executor, world, parent, args);
    }

    @Override
    public void executeParent(@NotNull OnlineUser executor, @NotNull ClaimWorld world,
                              @NotNull Claim claim, @NotNull String[] args) {
        // Get the name
        final Optional<String> username = parseStringArg(args, 0);
        if (username.isEmpty()) {
            plugin.getLocales().getLocale("error_invalid_syntax", getUsage())
                    .ifPresent(executor::sendMessage);
            return;
        }

        transferClaim(executor, username.get(), world, claim);
    }

    private void transferClaim(@NotNull OnlineUser executor, @NotNull String targetUser,
                               @NotNull ClaimWorld world, @NotNull Claim claim) {
        // Ensure the user has permission to transfer the claim
        if ((claim.getOwner().isEmpty() && !ClaimingMode.ADMIN_CLAIMS.canUse(executor)) || (claim.getOwner().isPresent()
                && !claim.getOwner().get().equals(executor.getUuid()) && !hasPermission(executor, "other"))) {
            plugin.getLocales().getLocale("no_transfer_permission")
                    .ifPresent(executor::sendMessage);
            return;
        }

        // Get the name of the target user
        final Optional<SavedUser> user = plugin.getDatabase().getUser(targetUser);
        if (user.isEmpty()) {
            plugin.getLocales().getLocale("error_invalid_user", targetUser)
                    .ifPresent(executor::sendMessage);
            return;
        }

        plugin.userTransferClaim(executor, claim, world, user.get().getUser());
    }
}
