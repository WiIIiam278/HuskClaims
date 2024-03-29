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

import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import com.google.common.collect.Sets;
import lombok.AllArgsConstructor;
import lombok.Getter;
import net.william278.huskclaims.HuskClaims;
import net.william278.huskclaims.claim.ClaimWorld;
import net.william278.huskclaims.claim.Region;
import net.william278.huskclaims.position.Position;
import net.william278.huskclaims.position.World;
import net.william278.huskclaims.user.OnlineUser;
import net.william278.huskclaims.util.BlockProvider;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

public abstract class BlockHighlighter<B extends BlockHighlighter.HighlightBlock> implements Highlighter {

    public static final int VIEWING_RANGE = 78;

    protected final HuskClaims plugin;
    protected final Multimap<UUID, HighlightBlock> replacedBlocks;

    protected BlockHighlighter(@NotNull HuskClaims plugin) {
        this.plugin = plugin;
        this.replacedBlocks = Multimaps.newListMultimap(
                Maps.newConcurrentMap(), CopyOnWriteArrayList::new
        );
    }

    @Override
    public void startHighlighting(@NotNull OnlineUser user, @NotNull World world,
                                  @NotNull Collection<? extends Highlightable> toHighlight, boolean showOverlap) {
        final Optional<ClaimWorld> optionalClaimWorld = plugin.getClaimWorld(world);
        if (optionalClaimWorld.isEmpty()) {
            return;
        }
        final ClaimWorld claimWorld = optionalClaimWorld.get();

        // Get the point map to be highlighted
        final Set<B> highlightBlocks = Sets.newHashSet();
        final Position position = user.getPosition();
        final Map<Region.Point, Highlightable.Type> points = Maps.newHashMap();
        toHighlight.forEach(h -> points.putAll(h.getHighlightPoints(claimWorld, showOverlap, position, VIEWING_RANGE)));

        // Synchronously highlight
        plugin.runSync(() -> {
            final Map<HighlightBlock, Highlightable.Type> blocks = plugin.getSurfaceBlocksAt(points, world, position);
            final Collection<HighlightBlock> userBlocks = replacedBlocks.get(user.getUuid());
            if (!userBlocks.isEmpty() && userBlocks.size() == blocks.size()
                    && blocks.entrySet().stream().allMatch(b -> userBlocks.stream()
                    .anyMatch(b2 -> b2.getPosition().equals(b.getKey().position)))) {
                return;
            }

            this.stopHighlighting(user);
            blocks.forEach((b, t) -> {
                B block = getHighlightBlock(b.getPosition(), t, plugin);
                cacheBlock(user, b, block);
                highlightBlocks.add(block);
            });

            this.showBlocks(user, highlightBlocks);
        });
    }

    public abstract void cacheBlock(@NotNull OnlineUser user, @NotNull HighlightBlock origin, @NotNull B block);

    @NotNull
    public abstract B getHighlightBlock(@NotNull Position position, @NotNull Highlightable.Type type,
                                        @NotNull HuskClaims plugin);

    public abstract void showBlocks(@NotNull OnlineUser user, @NotNull Collection<B> blocks);

    /**
     * Represents a highlighted block
     *
     * @since 1.0
     */
    @Getter
    @AllArgsConstructor
    public static class HighlightBlock {

        protected final Position position;
        protected final BlockProvider.MaterialBlock block;


        @NotNull
        private Map.Entry<Position, BlockProvider.MaterialBlock> toEntry() {
            return Map.entry(getPosition(), getBlock());
        }

        @NotNull
        protected static Map<Position, BlockProvider.MaterialBlock> getMap(@NotNull Collection<? extends HighlightBlock> blocks) {
            return blocks.stream().map(HighlightBlock::toEntry).collect(
                    Maps::newHashMap,
                    (m, v) -> m.put(v.getKey(), v.getValue()),
                    Map::putAll
            );
        }
    }
}
