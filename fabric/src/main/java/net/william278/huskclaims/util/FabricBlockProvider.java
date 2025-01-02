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
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.StainedGlassBlock;
import net.minecraft.block.StainedGlassPaneBlock;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.Pair;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.william278.huskclaims.FabricHuskClaims;
import net.william278.huskclaims.FabricHuskClaims.Adapter;
import net.william278.huskclaims.highlighter.BlockHighlighter;
import net.william278.huskclaims.highlighter.Highlightable;
import net.william278.huskclaims.position.BlockPosition;
import net.william278.huskclaims.position.Position;
import net.william278.huskclaims.position.World;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.Set;

import static net.william278.huskclaims.highlighter.BlockHighlighter.VIEWING_RANGE;

public interface FabricBlockProvider extends BlockProvider {

    @NotNull
    @Override
    default MaterialBlock getBlockFor(@NotNull String materialKey) {
        return BlockMaterialBlock.create(Registries.BLOCK.get(Identifier.of(materialKey)));
    }

    @NotNull
    @Override
    default Map<BlockHighlighter.HighlightBlock, Highlightable.Type> getSurfaceBlocksAt(
            @NotNull Map<? extends BlockPosition, Highlightable.Type> positions,
            @NotNull World surfaceWorld, @NotNull Position viewerPos
    ) {
        final Map<BlockHighlighter.HighlightBlock, Highlightable.Type> blocks = Maps.newHashMap();
        final BlockPos viewerBlock = new BlockPos(viewerPos.getBlockX(), (int) viewerPos.getY(), viewerPos.getBlockZ());
        final ServerWorld world = Adapter.adapt(surfaceWorld, getPlugin().getMinecraftServer());
        positions.forEach((pos, highlightType) -> {
            final BlockPos.Mutable loc = new BlockPos.Mutable(pos.getBlockX() - 1, viewerBlock.getY(), pos.getBlockZ() - 1);
            final boolean loaded = world.isChunkLoaded(loc.getX() >> 4, loc.getZ() >> 4);
            if (loaded && loc.toCenterPos().distanceTo(viewerBlock.toCenterPos()) <= VIEWING_RANGE) {
                final Pair<BlockPos, Block> block = getSurfaceBlockAt(
                        loc, world, world.getBottomY(), world.getHeight()
                );
                blocks.put(new BlockHighlighter.HighlightBlock(
                        Adapter.adapt(new Location(world, block.getLeft())),
                        new BlockMaterialBlock(block.getRight())
                ), highlightType);
            }
        });
        return blocks;
    }

    // Scans up or down relative to where the player is standing to find a good surface block
    @NotNull
    private Pair<BlockPos, Block> getSurfaceBlockAt(@NotNull BlockPos.Mutable block, @NotNull ServerWorld world,
                                                    int minHeight, int maxHeight) {
        final Direction direction = !isOccluding(world.getBlockState(block)) ? Direction.DOWN : Direction.UP;
        while (isObscured(block, world, minHeight, maxHeight)) {
            block = block.move(direction);
        }
        return new Pair<>(block, world.getBlockState(block).getBlock());
    }

    // Returns if a block would be obscured by the block above it (and that it's within the world boundaries)
    private boolean isObscured(@NotNull BlockPos.Mutable block, @NotNull ServerWorld world,
                               int minHeight, int maxHeight) {
        return (isOccluding(world.getBlockState(block.offset(Direction.UP, 1))) ||
               !isOccluding(world.getBlockState(block))) &&
               block.getY() >= minHeight && (block.getY() < maxHeight - 1);
    }

    // Returns if a block occludes vision/light
    private boolean isOccluding(@NotNull BlockState block) {
        return !block.isTransparent() || isAllowedMaterial(block.getBlock());
    }

    private boolean isAllowedMaterial(@NotNull Block material) {
        return Set.of(Items.GLASS, Items.ICE, Items.PACKED_ICE, Items.BLUE_ICE).contains(material.asItem()) ||
               material instanceof StainedGlassBlock || material instanceof StainedGlassPaneBlock;
    }

    @NotNull
    FabricHuskClaims getPlugin();

}
