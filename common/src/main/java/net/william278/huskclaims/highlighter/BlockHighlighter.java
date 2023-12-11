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

package net.william278.huskclaims.highlighter;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import net.william278.huskclaims.HuskClaims;
import net.william278.huskclaims.position.Position;
import net.william278.huskclaims.position.World;
import net.william278.huskclaims.user.OnlineUser;
import net.william278.huskclaims.util.BlockProvider;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentMap;

/**
 * Highlighter that uses ghost blocks - used to highlight {@link Highlightable}s to a user in-game
 */
public class BlockHighlighter implements Highlighter {

    private final HuskClaims plugin;
    private final ConcurrentMap<UUID, List<HighlightedBlock>> activeHighlights;

    public BlockHighlighter(@NotNull HuskClaims plugin) {
        this.plugin = plugin;
        this.activeHighlights = Maps.newConcurrentMap();
    }

    @Override
    public void startHighlighting(@NotNull OnlineUser user, @NotNull World world,
                                  @NotNull Collection<Highlightable> toHighlight) {
        plugin.runSync(() -> {
            stopHighlighting(user);

            final List<HighlightedBlock> activeBlocks = Lists.newArrayList();
            final List<HighlightedBlock> highlightBlocks = Lists.newArrayList();

            for (Highlightable highlight : toHighlight) {
                plugin.getHighestBlocksAt(highlight.getHighlightPositions().keySet(), world)
                        .forEach((position, block) -> {
                            activeBlocks.add(new HighlightedBlock(
                                    position, block
                            ));
                            highlightBlocks.add(new HighlightedBlock(
                                    position, getBlockFor(highlight.getHighlightPositions().get(position))
                            ));
                        });
            }

            activeHighlights.put(user.getUuid(), activeBlocks);
            plugin.sendBlockUpdates(user, HighlightedBlock.getMap(highlightBlocks));
        });
    }

    @NotNull
    private BlockProvider.MaterialBlock getBlockFor(@NotNull Highlightable.HighlightType type) {
        return plugin.getBlockFor(plugin.getSettings().getClaims().getBlockHighlighterTypes()
                .getOrDefault(type, "minecraft:yellow_concrete"));
    }

    @Override
    public void stopHighlighting(@NotNull OnlineUser user) {
        final List<HighlightedBlock> blocks = activeHighlights.remove(user.getUuid());
        if (blocks != null) {
            plugin.sendBlockUpdates(user, HighlightedBlock.getMap(blocks));
        }
    }


    /**
     * Represents a highlighted block
     *
     * @param position The position of the block
     * @param block    The Material/BlockData to highlight the block with
     * @since 1.0
     */
    private record HighlightedBlock(@NotNull Position position, @NotNull BlockProvider.MaterialBlock block) {

        @NotNull
        private Map.Entry<Position, BlockProvider.MaterialBlock> toEntry() {
            return Map.entry(position, block);
        }

        @NotNull
        private static Map<Position, BlockProvider.MaterialBlock> getMap(@NotNull Collection<HighlightedBlock> blocks) {
            return blocks.stream().map(HighlightedBlock::toEntry).collect(
                    Maps::newHashMap,
                    (m, v) -> m.put(v.getKey(), v.getValue()),
                    Map::putAll
            );
        }

    }

}
