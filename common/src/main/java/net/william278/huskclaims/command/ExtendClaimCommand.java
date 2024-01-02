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

import lombok.AllArgsConstructor;
import net.william278.huskclaims.HuskClaims;
import net.william278.huskclaims.claim.Claim;
import net.william278.huskclaims.claim.ClaimWorld;
import net.william278.huskclaims.claim.ClaimingMode;
import net.william278.huskclaims.claim.Region;
import net.william278.huskclaims.config.Settings;
import net.william278.huskclaims.user.OnlineUser;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Optional;

public class ExtendClaimCommand extends InClaimCommand {

    protected ExtendClaimCommand(@NotNull HuskClaims plugin) {
        super(
                List.of("extendclaim"),
                "<blocks>",
                plugin
        );
    }

    @Override
    public void execute(@NotNull OnlineUser executor, @NotNull ClaimWorld world,
                        @NotNull Claim claim, @NotNull String[] args) {
        if ((claim.getOwner().isEmpty() && !ClaimingMode.ADMIN_CLAIMS.canUse(executor)) || (claim.getOwner().isPresent()
                && claim.getOwner().get().equals(executor.getUuid()) && !hasPermission(executor, "other"))) {
            plugin.getLocales().getLocale("no_resizing_permission")
                    .ifPresent(executor::sendMessage);
            return;
        }

        final Optional<Integer> distance = parseIntArg(args, 0);
        if (distance.isEmpty() || distance.get() <= 0) {
            plugin.getLocales().getLocale("error_invalid_syntax", getUsage())
                    .ifPresent(executor::sendMessage);
            return;
        }
        extendClaim(executor, world, claim, ExtendDirection.getFrom(executor.getPosition().getYaw()), distance.get());
    }

    private void extendClaim(@NotNull OnlineUser user, @NotNull ClaimWorld world, @NotNull Claim claim,
                             @NotNull ExtendDirection facing, int amount) {
        final Settings.ClaimSettings claims = plugin.getSettings().getClaims();
        if (claims.isRequireToolForCommands() && !user.isHolding(claims.getClaimTool())) {
            plugin.getLocales().getLocale("claim_tool_required")
                    .ifPresent(user::sendMessage);
            return;
        }

        // Calculate the number of blocks to extend in each direction
        int north = 0, south = 0, east = 0, west = 0;
        switch (facing) {
            case NORTH -> north = amount;
            case SOUTH -> south = amount;
            case EAST -> east = amount;
            case WEST -> west = amount;
        }

        // Resize the claim
        final Region resized = claim.getRegion().getResized(north, south, east, west);
        plugin.userResizeClaim(user, world, claim, resized);
    }

    @AllArgsConstructor
    private enum ExtendDirection {
        NORTH,
        EAST,
        SOUTH,
        WEST;

        @NotNull
        private static ExtendDirection getFrom(float yaw) {
            yaw = yaw % 360;
            if (yaw < 0) {
                yaw += 360.0f;
            }
            if (yaw >= 315 || yaw < 45) {
                return SOUTH;
            } else if (yaw < 135) {
                return WEST;
            } else if (yaw < 225) {
                return NORTH;
            } else {
                return EAST;
            }
        }
    }

}
