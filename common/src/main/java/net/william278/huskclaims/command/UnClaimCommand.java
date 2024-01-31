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
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class UnClaimCommand extends InClaimOwnerCommand {

    protected UnClaimCommand(@NotNull HuskClaims plugin) {
        super(
                List.of("unclaim", "abandonclaim"),
                "[confirm]",
                plugin
        );
    }

    @Override
    public void executeChild(@NotNull OnlineUser executor, @NotNull ClaimWorld world,
                             @NotNull Claim child, @NotNull Claim parent, @NotNull String[] args) {
        plugin.userDeleteChildClaim(executor, world, child, parent);
    }

    @Override
    public void executeParent(@NotNull OnlineUser executor, @NotNull ClaimWorld world,
                              @NotNull Claim claim, @NotNull String[] args) {
        final boolean confirmed = parseConfirmArg(args);

        // Require confirmation for deleting a claim which has children
        if (plugin.getSettings().getClaims().isConfirmDeletingParentClaims() &&
                !claim.getChildren().isEmpty() && !confirmed) {
            plugin.getLocales().getLocale("confirm_deletion_parent_claim",
                    String.format("%s confirm", getName())).ifPresent(executor::sendMessage);
            return;
        }

        // Delete the claim
        plugin.userDeleteClaim(executor, world, claim);
    }

}
