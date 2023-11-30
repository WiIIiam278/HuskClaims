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

package net.william278.huskclaims.claim;

import net.william278.cloplib.operation.OperationPosition;
import net.william278.cloplib.operation.OperationWorld;
import net.william278.huskclaims.position.BlockPosition;
import net.william278.huskclaims.position.Position;
import net.william278.huskclaims.position.World;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Interface for managing {@link ClaimWorld}s
 *
 * @since 1.0
 */
public interface ClaimManager extends ClaimHandler {

    /**
     * Get a trust level by name
     *
     * @return the trust level, if found
     * @since 1.0
     */
    @NotNull
    Map<OperationWorld, ClaimWorld> getClaimWorlds();

    /**
     * Get a claim world by world
     *
     * @param world The world to get the claim world for
     * @return the claim world, if found
     * @since 1.0
     */
    default Optional<ClaimWorld> getClaimWorld(@NotNull OperationWorld world) {
        return Optional.ofNullable(getClaimWorlds().get(world));
    }

    /**
     * Get a claim at a position
     *
     * @param position The position to get the claim at
     * @return the claim, if found
     * @since 1.0
     */
    default Optional<Claim> getClaimAt(@NotNull OperationPosition position) {
        return getClaimWorld(position.getWorld()).flatMap(world -> world.getClaimAt((Position) position));
    }

    /**
     * Load the claim worlds from the database
     *
     * @since 1.0
     */
    default void loadClaimWorlds() {
        //todo load from DB
    }

    /**
     * Load the platform-specific operation listener
     *
     * @since 1.0
     */
    void loadOperationListener();

    /**
     * Get the claim highlighter
     *
     * @return the claim highlighter
     * @since 1.0
     */
    @NotNull
    ClaimHighlighter getClaimHighlighter();

}
