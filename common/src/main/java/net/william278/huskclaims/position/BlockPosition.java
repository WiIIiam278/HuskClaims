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

package net.william278.huskclaims.position;

import org.jetbrains.annotations.NotNull;

/**
 * Interface for the lateral position of a block
 *
 * @since 1.0
 */
public interface BlockPosition {

    /**
     * Get the X coordinate of the block
     *
     * @return the X coordinate of the block
     * @since 1.0
     */
    int getBlockX();

    /**
     * Get the Z coordinate of the block
     *
     * @return the Z coordinate of the block
     * @since 1.0
     */
    int getBlockZ();

    /**
     * Get the long chunk coordinates of the block position.
     *
     * @return the long chunk coordinates
     * @since 1.0
     */
    long getLongChunkCoords();

    /**
     * Get the square surface area between two block positions
     *
     * @param other The other block position
     * @return the square surface area between two block positions
     * @since 1.0
     */
    default int getSurfaceArea(@NotNull BlockPosition other) {
        return (1 + Math.abs(getBlockX() - other.getBlockX())) * (1 + Math.abs(getBlockZ() - other.getBlockZ()));
    }

    /**
     * Get the distance between two block positions
     *
     * @param other The other block position
     * @return the distance between two block positions
     * @since 1.0
     */
    default long distanceFrom(@NotNull BlockPosition other) {
        return Math.round(Math.sqrt(
                Math.pow(getBlockX() - other.getBlockX(), 2) + Math.pow(getBlockZ() - other.getBlockZ(), 2)
        ));
    }

}
