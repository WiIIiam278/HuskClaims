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

import net.william278.huskclaims.BukkitHuskClaims;
import net.william278.huskclaims.HuskClaims;
import net.william278.huskclaims.PaperHuskClaims;
import net.william278.huskclaims.position.Position;
import net.william278.huskclaims.user.BukkitUser;
import net.william278.huskclaims.user.OnlineUser;
import net.william278.huskclaims.util.BlockDataBlock;
import net.william278.huskclaims.util.BlockProvider;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Display;
import org.bukkit.entity.EntityType;
import org.bukkit.util.Transformation;
import org.jetbrains.annotations.NotNull;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

import java.util.Collection;

/**
 * Highlighter that uses {@link BlockDisplay} entities - used to highlight {@link Highlightable}s to a user in-game
 */
public class BlockDisplayHighlighter extends BlockHighlighter<BlockDisplayHighlighter.DisplayHighlightBlock> {


    public BlockDisplayHighlighter(@NotNull HuskClaims plugin) {
        super(plugin);
    }

    @NotNull
    @Override
    public DisplayHighlightBlock getHighlightBlock(@NotNull Position position, @NotNull Highlightable.Type type,
                                                   @NotNull HuskClaims plugin) {
        return new DisplayHighlightBlock(position, type, plugin);
    }

    @Override
    public void showBlocks(@NotNull OnlineUser user, @NotNull Collection<DisplayHighlightBlock> blocks) {
        blocks.forEach(block -> block.show(plugin, user));
    }

    @Override
    public void stopHighlighting(@NotNull OnlineUser user) {
        plugin.runSync(() -> replacedBlocks.removeAll(user.getUuid()).forEach(block -> {
            if (block instanceof DisplayHighlightBlock display) {
                display.remove();
            }
        }));
    }

    public static final class DisplayHighlightBlock extends HighlightBlock {

        // Block display brightness value
        private static final Display.Brightness FULL_BRIGHT = new Display.Brightness(15, 15);

        // Block display scale constants
        private static final float SCALAR = 0.002f;
        private static final Transformation SCALE_TRANSFORMATION = new Transformation(
                new Vector3f(-(SCALAR / 2), -(SCALAR / 2), -(SCALAR / 2)),
                new AxisAngle4f(0, 0, 0, 0),
                new Vector3f(1 + SCALAR, 1 + SCALAR, 1 + SCALAR),
                new AxisAngle4f(0, 0, 0, 0)
        );

        private final BlockDisplay display;

        private DisplayHighlightBlock(@NotNull Position position, @NotNull Highlightable.Type type,
                                      @NotNull HuskClaims plugin) {
            super(position, plugin.getSettings().getHighlighter().getBlock(type, plugin));
            this.display = createEntity(plugin, type);
        }

        @SuppressWarnings("UnstableApiUsage")
        @NotNull
        private BlockDisplay createEntity(@NotNull HuskClaims plugin, @NotNull Highlightable.Type type) {
            final Location location = BukkitHuskClaims.Adapter.adapt(position);
            if (!location.isWorldLoaded() || !location.getChunk().isLoaded()) {
                throw new IllegalStateException("World/chunk is not loaded");
            }

            // Create block display
            final BlockDisplay display = (BlockDisplay) location.getWorld().spawnEntity(
                    location, EntityType.BLOCK_DISPLAY
            );
            display.setBlock(((BlockDataBlock) this.block).getData());
            display.setViewRange(BlockProvider.BLOCK_VIEW_DISTANCE);
            display.setGravity(false);
            display.setPersistent(false);
            display.setBrightness(FULL_BRIGHT);
            display.setVisibleByDefault(false);

            // Scale to prevent z-fighting
            display.setTransformation(SCALE_TRANSFORMATION);

            // Glow if needed
            if (plugin.getSettings().getHighlighter().isGlowEffect()) {
                display.setGlowing(true);
                display.setGlowColorOverride(Color.fromRGB(
                        plugin.getSettings().getHighlighter().getGlowColor(type).getRgb()
                ));
            }
            return display;
        }

        @SuppressWarnings("UnstableApiUsage")
        public void show(@NotNull HuskClaims plugin, @NotNull OnlineUser user) {
            ((BukkitUser) user).getBukkitPlayer().showEntity((PaperHuskClaims) plugin, display);
        }

        public void remove() {
            if (display != null) {
                display.remove();
            }
        }

    }
}
