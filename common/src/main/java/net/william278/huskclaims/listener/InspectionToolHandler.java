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

package net.william278.huskclaims.listener;

import net.william278.cloplib.operation.OperationPosition;
import net.william278.cloplib.operation.OperationUser;
import net.william278.huskclaims.HuskClaims;
import net.william278.huskclaims.claim.Claim;
import net.william278.huskclaims.claim.ClaimWorld;
import net.william278.huskclaims.position.Position;
import net.william278.huskclaims.user.OnlineUser;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

/**
 * Handler for when the claim creation and resize tool is used
 */
public interface InspectionToolHandler {

    // When the inspection tool (default: stick) is used
    default void onInspectionToolUse(@NotNull OperationUser operationUser, @NotNull OperationPosition position) {
        final OnlineUser user = (OnlineUser) operationUser;

        // Check that the world is claimable
        final Optional<ClaimWorld> optionalWorld = getPlugin().getClaimWorld(position.getWorld());
        if (optionalWorld.isEmpty()) {
            getPlugin().getLocales().getLocale("world_not_claimable")
                    .ifPresent(user::sendMessage);
            return;
        }
        final ClaimWorld claimWorld = optionalWorld.get();

        // Check if there is a claim at the position
        //todo - future: highlight ALL nearby claims if the user is sneaking.
        final Optional<Claim> optionalClaim = claimWorld.getClaimAt((Position) position);
        if (optionalClaim.isEmpty()) {
            getPlugin().getLocales().getLocale("land_not_claimed")
                    .ifPresent(user::sendMessage);
            return;
        }
        getPlugin().runAsync(() -> highlightClaim(user, optionalClaim.get(), claimWorld));
    }

    // Highlight the claim for the user and send a message
    private void highlightClaim(@NotNull OnlineUser user, @NotNull Claim claim, @NotNull ClaimWorld world) {
        getPlugin().getClaimHighlighter().highlightClaim(user, world, claim);
        getPlugin().getLocales().getLocale("land_claimed_by", claim.getOwnerName(world, getPlugin()))
                .ifPresent(user::sendMessage);
    }

    @NotNull
    HuskClaims getPlugin();

}
