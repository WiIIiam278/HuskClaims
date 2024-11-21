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
import net.william278.huskclaims.claim.Region;
import net.william278.huskclaims.config.Settings;
import net.william278.huskclaims.user.OnlineUser;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;

public class ExtendClaimCommand extends InClaimOwnerCommand {

    protected ExtendClaimCommand(@NotNull HuskClaims plugin) {
        super(
                List.of("extendclaim"),
                "<blocks>",
                plugin
        );
        setNoPrivilegeMessage("no_resizing_permission");
    }

    @Override
    public void executeChild(@NotNull OnlineUser executor, @NotNull ClaimWorld world,
                             @NotNull Claim child, @NotNull Claim parent, @NotNull String[] args) {
        extendClaim(executor, world, child, parent, args);
    }

    @Override
    public void executeParent(@NotNull OnlineUser executor, @NotNull ClaimWorld world,
                              @NotNull Claim claim, @NotNull String[] args) {
        extendClaim(executor, world, claim, null, args);
    }

    private void extendClaim(@NotNull OnlineUser executor, @NotNull ClaimWorld world,
                             @NotNull Claim claim, @Nullable Claim parent, @NotNull String[] args) {
        final Settings.ClaimSettings claims = plugin.getSettings().getClaims();
        if (claims.isRequireToolForCommands() && !executor.isHolding(claims.getClaimToolData())) {
            plugin.getLocales().getLocale("claim_tool_required")
                    .ifPresent(executor::sendMessage);
            return;
        }

        // Validate and parse input distance
        final Optional<Integer> distance = parseIntArg(args, 0);
        if (distance.isEmpty() || distance.get() <= 0) {
            plugin.getLocales().getLocale("error_invalid_syntax", getUsage())
                    .ifPresent(executor::sendMessage);
            return;
        }

        // Protect against extending claims outside the world limits
        final ExtendDirection direction = ExtendDirection.getFrom(executor.getPosition().getYaw());
        if (Region.Point.isOutOfRange(
                executor.getPosition(),
                direction.xAxis() ? distance.get() : 0,
                direction.xAxis() ? 0 : distance.get()
        )) {
            plugin.getLocales().getLocale("region_outside_world_limits")
                    .ifPresent(executor::sendMessage);
            return;
        }

        // Extend the region, resize the claim
        final Region extendedRegion = getExtendedRegion(claim.getRegion(), direction, distance.get());
        if (parent != null) {
            plugin.userResizeChildClaim(executor, world, claim, extendedRegion);
            return;
        }

        plugin.userResizeClaim(executor, world, claim, extendedRegion);
    }

    // Calculates the number of blocks to extend in each direction and returns a resized region
    @NotNull
    private Region getExtendedRegion(@NotNull Region region, @NotNull ExtendDirection facing, int amount) {
        int north = 0, south = 0, east = 0, west = 0;
        switch (facing) {
            case NORTH -> north = amount;
            case SOUTH -> south = amount;
            case EAST -> east = amount;
            case WEST -> west = amount;
        }
        return region.getResized(north, south, east, west);
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

        boolean xAxis() {
            return this == EAST || this == WEST;
        }
    }

}
