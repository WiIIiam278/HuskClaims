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

package net.william278.huskclaims.util;

import com.google.common.collect.Lists;
import net.william278.huskclaims.HuskClaims;
import net.william278.huskclaims.claim.Claim;
import net.william278.huskclaims.claim.Region;
import net.william278.huskclaims.position.Position;
import net.william278.huskclaims.position.World;
import net.william278.huskclaims.user.OnlineUser;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

public interface SafeTeleportProvider {

    int MAX_TRIES = 8;

    default void teleportOutOfClaim(@NotNull OnlineUser user, @NotNull Consumer<Boolean> done) {
        final Optional<Region> region = getPlugin().getClaimAt(user.getPosition()).map(Claim::getRegion);
        if (region.isEmpty()) {
            done.accept(false);
            return;
        }
        this.teleportOutOfClaim(user, this.getPotentialPositions(user.getPosition(), region.get()), done, MAX_TRIES);
    }

    private void teleportOutOfClaim(@NotNull OnlineUser user, @NotNull List<Position> positions,
                                    @NotNull Consumer<Boolean> done, int tries) {
        final Position position = this.getPerimeterPosition(positions, tries);
        if (tries <= 0) {
            done.accept(false);
            return;
        }
        findSafePosition(position).thenAccept(found -> found.ifPresentOrElse(
                (safe) -> getPlugin().runSync(() -> {
                    user.teleport(safe);
                    done.accept(true);
                }),
                () -> teleportOutOfClaim(user, positions, done, tries - 1)
        ));
    }

    @NotNull
    private Position getPerimeterPosition(@NotNull List<Position> positions, int tries) {
        return positions.get((tries - 1) % positions.size());
    }

    private List<Position> getPotentialPositions(@NotNull Position position, @NotNull Region region) {
        final World world = position.getWorld();
        final List<Position> positions = Lists.newArrayList(
                Position.at(region.getNearCorner().getBlockX() - 1, 64, position.getBlockZ(), world),
                Position.at(region.getFarCorner().getBlockX() + 1, 64, position.getBlockZ(), world),
                Position.at(position.getBlockX(), 64, region.getNearCorner().getBlockZ() - 1, world),
                Position.at(position.getBlockX(), 64, region.getFarCorner().getBlockZ() + 1, world)
        );
        positions.addAll(region.getCorners().stream().map(corner -> Position.at(corner, 64, world)).toList());
        return positions;
    }

    @NotNull
    CompletableFuture<Optional<Position>> findSafePosition(@NotNull Position position);

    @NotNull
    HuskClaims getPlugin();

}
