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
import lombok.Getter;
import lombok.experimental.Accessors;
import net.william278.huskclaims.HuskClaims;
import net.william278.huskclaims.PaperHuskClaims;
import net.william278.huskclaims.claim.ClaimWorld;
import net.william278.huskclaims.claim.Region;
import net.william278.huskclaims.position.Position;
import net.william278.huskclaims.position.World;
import net.william278.huskclaims.user.OnlineUser;
import net.william278.huskclaims.util.BlockDataBlock;
import net.william278.huskclaims.util.BlockProvider;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

public class GlowHighlighter implements Highlighter {

    private final PaperHuskClaims plugin;
    private final Multimap<UUID, HighlightedBlock> activeHighlights;

    public GlowHighlighter(@NotNull HuskClaims plugin) {
        this.plugin = (PaperHuskClaims) plugin;
        this.activeHighlights = Multimaps.newListMultimap(
                Maps.newConcurrentMap(), CopyOnWriteArrayList::new
        );
    }

    // Check the block map for duplicates
    private boolean checkDuplicate(@NotNull OnlineUser user, @NotNull Map<Position, BlockProvider.MaterialBlock> map) {
        final Collection<HighlightedBlock> existing = activeHighlights.get(user.getUuid());
        if (existing.isEmpty()) {
            return false;
        }

        return existing.stream().allMatch(e -> map.keySet().stream().anyMatch(e.position::equals));
    }

    public void startHighlighting(@NotNull OnlineUser user, @NotNull World world,
                                  @NotNull Collection<? extends Highlightable> toHighlight, boolean showOverlap) {
        plugin.runSync(() -> {
            final Optional<ClaimWorld> optionalClaimWorld = plugin.getClaimWorld(world);
            if (optionalClaimWorld.isEmpty()) {
                return;
            }

            final ClaimWorld claimWorld = optionalClaimWorld.get();
            final Set<HighlightedBlock> activeBlocks = Sets.newHashSet();
            final Player player = Bukkit.getPlayer(user.getUuid());
            if (player == null) {
                return;
            }

            final Map<Position, BlockProvider.MaterialBlock> blocks = Maps.newHashMap();
            final Map<Position, Highlightable> highlightPoints = Maps.newHashMap();
            final Map<Position, BlockProvider.MaterialBlock> original = Maps.newHashMap();
            final Map<Position, Highlightable.HighlightType> typeMap = Maps.newHashMap();

            for (Highlightable highlight : toHighlight) {
                final Map<Region.Point, Highlightable.HighlightType> type = highlight.getHighlightPoints(claimWorld, showOverlap);
                plugin.getSurfaceBlocksAt(type.keySet(), world, (int) player.getLocation().getY())
                        .forEach((pos, material) -> {
                            highlightPoints.put(pos, highlight);
                            blocks.put(pos, highlight.getBlockFor(claimWorld, pos, plugin, showOverlap));
                            original.put(pos, material);
                            typeMap.put(pos, type.entrySet()
                                    .stream()
                                    .filter(e -> e.getKey().equals(Region.Point.wrap(pos)))
                                    .map(Map.Entry::getValue)
                                    .findFirst()
                                    .orElse(Highlightable.HighlightType.SELECTION));
                        });
            }
            if (checkDuplicate(user, blocks)) {
                return;
            }

            stopHighlighting(user);

            blocks.forEach((pos, material) -> {
                activeBlocks.add(new HighlightedBlock(
                        pos,
                        highlightPoints.get(pos).getBlockFor(claimWorld, pos, plugin, showOverlap),
                        original.get(pos),
                        player, plugin, typeMap.get(pos)
                ));
            });

            final BlockDataBlock barrier = new BlockDataBlock(Material.BARRIER.createBlockData());
            final Set<Position> positions = blocks.keySet();

            activeHighlights.putAll(user.getUuid(), activeBlocks);
            plugin.sendBlockUpdates(user, positions.stream().collect(Collectors.toMap(p -> p, p -> barrier)));
        });
    }

    @Override
    public void stopHighlighting(@NotNull OnlineUser user) {
        final Collection<HighlightedBlock> blocks = activeHighlights.removeAll(user.getUuid());
        plugin.sendBlockUpdates(user, blocks.stream().map(b -> b.position).toList());
        blocks.stream().map(HighlightedBlock::getBlockDisplay).forEach(BlockDisplay::remove);
    }


    @Getter
    @Accessors(fluent = true)
    @NotNull
    private static final class HighlightedBlock {
        private final Position position;
        private final BlockProvider.MaterialBlock originalBlock;
        private final BlockProvider.MaterialBlock block;
        private final UUID entityId;

        public HighlightedBlock(@NotNull Position position, @NotNull BlockProvider.MaterialBlock block,
                                @NotNull BlockProvider.MaterialBlock originalBlock,
                                @NotNull Player player, @NotNull PaperHuskClaims plugin,
                                @NotNull Highlightable.HighlightType type) {
            this.position = position;
            this.block = block;
            this.originalBlock = originalBlock;
            this.entityId = createEntity(player, plugin, type);
        }

        @SuppressWarnings("UnstableApiUsage")
        private UUID createEntity(@NotNull Player player, @NotNull PaperHuskClaims plugin, @NotNull Highlightable.HighlightType type) {
            final org.bukkit.World world = Bukkit.getWorld(position.getWorld().getUuid());
            if (world == null) {
                throw new NullPointerException("World is null");
            }

            final Location location = new Location(world, position.getX(), position.getY(), position.getZ());
            final BlockDataBlock block = (BlockDataBlock) this.block;
            final BlockDisplay display = world.spawn(location, BlockDisplay.class);
            display.setBlock(block.getData());
            display.setGravity(false);
            display.setGlowing(true);
            display.setInvulnerable(true);
            display.setSilent(true);
            display.setPersistent(false);
            display.setCustomNameVisible(false);
            display.setVisibleByDefault(false);
            display.setGlowColorOverride(Color.fromRGB(plugin.getSettings().getClaims().getBlockHighlighterColor(type).getRgb()));
            player.showEntity(plugin, display);

            return display.getUniqueId();
        }

        private BlockDisplay getBlockDisplay() {
            return (BlockDisplay) Bukkit.getEntity(entityId);
        }

    }
}
