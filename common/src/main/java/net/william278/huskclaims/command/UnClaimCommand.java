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
import net.william278.huskclaims.claim.TrustLevel;
import net.william278.huskclaims.user.OnlineUser;
import org.jetbrains.annotations.NotNull;

import javax.print.DocFlavor;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

public class UnClaimCommand extends InClaimCommand {

    protected UnClaimCommand(@NotNull HuskClaims plugin) {
        super(
                List.of("unclaim", "abandonclaim"),
                "[confirm]",
                null,
                plugin
        );
        addAdditionalPermissions(Map.of("other", true));
    }

    @Override
    public void execute(@NotNull OnlineUser executor, @NotNull ClaimWorld world,
                        @NotNull Claim claim, @NotNull String[] args) {
        final boolean confirmed = parseStringArg(args, 0).map(a -> a.toLowerCase(Locale.ENGLISH))
                .map(a -> a.equals("confirm")).orElse(false);
        final Optional<Claim> optionalParent = claim.getParent(world);
        if (optionalParent.isPresent()) {
            deleteChildClaim(executor, world, claim, optionalParent.get());
            return;
        }

        // Check if the user can delete the claim
        boolean isAdminClaim = claim.getOwner().isEmpty();
        if ((isAdminClaim && !ClaimingMode.ADMIN_CLAIMS.canUse(executor))
                || (claim.getOwner().map(owner -> !owner.equals(executor.getUuid())).orElse(true)
                && !hasPermission(executor, "other"))) {
            plugin.getLocales().getLocale("no_deletion_permission")
                    .ifPresent(executor::sendMessage);
            return;
        }

        // Require confirmation for deleting a parent claim
        if (plugin.getSettings().getClaims().isConfirmDeletingParentClaims() &&
                !claim.getChildren().isEmpty() && !confirmed) {
            plugin.getLocales().getLocale("confirm_deletion_parent_claim",
                    String.format("/%s confirm", getName())).ifPresent(executor::sendMessage);
            return;
        }

        plugin.deleteClaim(world, claim);
        plugin.getHighlighter().stopHighlighting(executor);

        // Send the correct deletion message
        if (!isAdminClaim) {
            plugin.getLocales().getLocale("claim_deleted")
                    .ifPresent(executor::sendMessage);
        }
        plugin.getLocales().getLocale("admin_claim_deleted")
                .ifPresent(executor::sendMessage);
    }

    // Handle deletion of a child claim
    private void deleteChildClaim(@NotNull OnlineUser executor, @NotNull ClaimWorld world,
                                  @NotNull Claim claim, @NotNull Claim parent) {
        if (!claim.isPrivilegeAllowed(TrustLevel.Privilege.MANAGE_CHILD_CLAIMS, executor, world, plugin)) {
            plugin.getLocales().getLocale("no_child_deletion_permission")
                    .ifPresent(executor::sendMessage);
            return;
        }

        plugin.deleteChildClaim(world, parent, claim);
        plugin.getHighlighter().startHighlighting(executor, executor.getWorld(), parent);
        plugin.getLocales().getLocale("child_claim_deleted")
                .ifPresent(executor::sendMessage);
    }

}
