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

import net.william278.huskclaims.position.BlockPosition;
import net.william278.huskclaims.position.Position;
import net.william278.huskclaims.position.World;
import net.william278.huskclaims.user.OnlineUser;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.List;
import java.util.Map;

public interface BlockProvider {

    /**
     * Get the server worlds
     *
     * @return the server worlds
     * @since 1.0
     */
    @NotNull
    List<World> getWorlds();

    /**
     * Get the {@link MaterialBlock} for a material by key
     *
     * @param materialKey the material key
     * @return the {@link MaterialBlock}
     * @since 1.0
     */
    @NotNull
    MaterialBlock getBlockFor(@NotNull String materialKey);

    /**
     * Get a Map of the highest blocks at a collection of positions
     *
     * @param positions the positions
     * @param world    the world
     * @return a Map of the highest block {@link Position}s to their {@link MaterialBlock}s
     * @since 1.0
     */
    @NotNull
    Map<Position, MaterialBlock> getHighestBlocksAt(@NotNull Collection<? extends BlockPosition> positions,
                                                    @NotNull World world);

    /**
     * Send a batch of block updates to a user
     *
     * @param user   the user
     * @param blocks the blocks
     * @since 1.0
     */
    void sendBlockUpdates(@NotNull OnlineUser user, @NotNull Map<Position, MaterialBlock> blocks);

    /**
     * Sends block updates to a specific online user based on the given positions.
     *
     * @param user      the online user to send the block updates to
     * @param positions the list of positions to update blocks at
     * @since 1.0
     */
    void sendBlockUpdates(@NotNull OnlineUser user, @NotNull List<Position> positions);

    /**
     * Abstract representation of a block with material data
     */
    abstract class MaterialBlock {

        @NotNull
        public abstract String getMaterialKey();

    }

}
