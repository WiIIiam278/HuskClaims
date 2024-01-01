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

import com.google.common.collect.Maps;
import net.william278.huskclaims.BukkitHuskClaims.Adapter;
import net.william278.huskclaims.highlighter.BlockHighlighter;
import net.william278.huskclaims.highlighter.Highlightable;
import net.william278.huskclaims.position.BlockPosition;
import net.william278.huskclaims.position.Position;
import net.william278.huskclaims.position.World;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.Objects;

public interface BukkitBlockProvider extends BlockProvider {

    @NotNull
    @Override
    default MaterialBlock getBlockFor(@NotNull String materialKey) {
        return BlockDataBlock.create(Objects.requireNonNull(
                Material.matchMaterial(materialKey),
                "Invalid material: " + materialKey
        ));
    }

    @NotNull
    @Override
    default Map<BlockHighlighter.HighlightBlock, Highlightable.HighlightType> getSurfaceBlocksAt(
            @NotNull Map<? extends BlockPosition, Highlightable.HighlightType> positions,
            @NotNull World surfaceWorld, @NotNull Position viewerPosition
    ) {
        final Map<BlockHighlighter.HighlightBlock, Highlightable.HighlightType> blocks = Maps.newHashMap();
        final Location viewerLocation = Adapter.adapt(viewerPosition);
        positions.forEach((position, highlightType) -> {
            final Location location = Adapter.adapt(Position.at(position, viewerPosition.getY(), surfaceWorld));
            final org.bukkit.World world = Objects.requireNonNull(location.getWorld(), "World is null");
            if (location.getChunk().isLoaded() && location.distance(viewerLocation) <= BLOCK_VIEW_DISTANCE) {
                final Block block = getSurfaceBlockAt(location.getBlock(), world.getMinHeight(), world.getMaxHeight());
                blocks.put(new BlockHighlighter.HighlightBlock(
                        Adapter.adapt(block.getLocation()),
                        new BlockDataBlock(block.getBlockData())
                ), highlightType);
            }
        });
        return blocks;
    }

    // Scans up or down relative to where the player is standing to find a good surface block
    @NotNull
    private Block getSurfaceBlockAt(@NotNull Block block, int minHeight, int maxHeight) {
        final BlockFace direction = !isOccluding(block) ? BlockFace.DOWN : BlockFace.UP;
        while (isObscured(block, minHeight, maxHeight)) {
            block = block.getRelative(direction);
        }
        return block;
    }

    // Returns if a block would be obscured by the block above it (and that it's within the world boundaries)
    private boolean isObscured(@NotNull Block block, int minHeight, int maxHeight) {
        return (isOccluding(block.getRelative(BlockFace.UP)) || !isOccluding(block)) &&
                block.getY() >= minHeight && (block.getY() < maxHeight - 1);
    }

    // Returns if a block occludes vision/light
    private boolean isOccluding(@NotNull Block block) {
        return (block.isLiquid() && block.getType() == Material.LAVA)
                || block.getType().isSolid() && block.getType().isOccluding();
    }

}
