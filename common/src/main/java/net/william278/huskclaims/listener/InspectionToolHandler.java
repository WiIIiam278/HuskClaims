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

import com.google.common.collect.Lists;
import net.william278.cloplib.operation.OperationPosition;
import net.william278.cloplib.operation.OperationUser;
import net.william278.huskclaims.HuskClaims;
import net.william278.huskclaims.claim.Claim;
import net.william278.huskclaims.claim.ClaimWorld;
import net.william278.huskclaims.claim.Region;
import net.william278.huskclaims.config.Settings;
import net.william278.huskclaims.position.Position;
import net.william278.huskclaims.user.OnlineUser;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Optional;

/**
 * Handler for when the claim creation and resize tool is used
 */
public interface InspectionToolHandler {

    // When the inspection tool (default: stick) is used
    default void onInspectionToolUse(@NotNull OperationUser opUser, @NotNull OperationPosition opPosition) {
        final OnlineUser user = (OnlineUser) opUser;
        final Position position = (Position) opPosition;

        // Check that the world is claimable
        final Optional<ClaimWorld> optionalWorld = getPlugin().getClaimWorld(position.getWorld());
        if (optionalWorld.isEmpty()) {
            getPlugin().getLocales().getLocale("world_not_claimable")
                    .ifPresent(user::sendMessage);
            return;
        }
        final ClaimWorld claimWorld = optionalWorld.get();

        getPlugin().runAsync(() -> {
            // Handle nearby claim inspecting
            final Settings.ClaimSettings settings = getPlugin().getSettings().getClaims();
            if (user.isSneaking() && settings.isAllowNearbyClaimInspection()) {
                final int distance = settings.getInspectionDistance();

                final List<Claim> claims = claimWorld.getParentClaimsWithin(Region.around(position, distance));
                if (claims.isEmpty()) {
                    getPlugin().getLocales().getLocale("no_nearby_claims", Integer.toString(distance))
                            .ifPresent(user::sendMessage);
                    return;
                }
                highlightClaims(user, claims, distance);
                return;
            }

            // Handle single claim inspecting
            final Optional<Claim> claim = claimWorld.getClaimAt(position);
            if (claim.isEmpty()) {
                getPlugin().getLocales().getLocale("land_not_claimed")
                        .ifPresent(user::sendMessage);
                return;
            }
            highlightClaim(user, claim.get(), claimWorld);
        });
    }

    // Highlight the claim for the user and send a message
    private void highlightClaim(@NotNull OnlineUser user, @NotNull Claim claim, @NotNull ClaimWorld world) {
        final List<Claim> claims = Lists.newArrayList(claim);
        claims.addAll(claim.getChildren());
        getPlugin().getHighlighter().startHighlighting(user, user.getWorld(), claims);
        getPlugin().getLocales().getLocale("land_claimed_by", claim.getOwnerName(world, getPlugin()))
                .ifPresent(user::sendMessage);
    }

    // Highlight multiple claims
    private void highlightClaims(@NotNull OnlineUser user, @NotNull List<Claim> claims, int distance) {
        getPlugin().getHighlighter().startHighlighting(user, user.getWorld(), claims);
        getPlugin().getLocales().getLocale("nearby_claims", Integer.toString(claims.size()),
                Integer.toString(distance)).ifPresent(user::sendMessage);
    }

    @NotNull
    HuskClaims getPlugin();

}
