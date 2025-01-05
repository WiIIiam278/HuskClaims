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

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.decoration.Brightness;
import net.minecraft.entity.decoration.DisplayEntity;
import net.minecraft.server.network.EntityTrackerEntry;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.AffineTransformation;
import net.minecraft.util.math.BlockPos;
import net.william278.huskclaims.FabricHuskClaims;
import net.william278.huskclaims.HuskClaims;
import net.william278.huskclaims.hook.GeyserHook;
import net.william278.huskclaims.position.Position;
import net.william278.huskclaims.user.FabricUser;
import net.william278.huskclaims.user.OnlineUser;
import net.william278.huskclaims.util.BlockMaterialBlock;
import net.william278.huskclaims.util.Location;
import org.jetbrains.annotations.NotNull;
import org.joml.Vector3f;

import java.util.Collection;

/**
 * Highlighter that uses {@link DisplayEntity.BlockDisplayEntity}s - used to highlight {@link Highlightable}s to a user in-game
 */
public class FabricBlockDisplayHighlighter extends BlockHighlighter<FabricBlockDisplayHighlighter.DisplayHighlightBlock> {

    private static final int PRIORITY = 1;

    public FabricBlockDisplayHighlighter(@NotNull HuskClaims plugin) {
        super(plugin);
    }

    @Override
    public void cacheBlock(@NotNull OnlineUser user, @NotNull HighlightBlock origin, @NotNull DisplayHighlightBlock block) {
        replacedBlocks.put(user.getUuid(), block);
    }

    @NotNull
    @Override
    public DisplayHighlightBlock getHighlightBlock(@NotNull Position position, @NotNull Highlightable.Type type,
                                                   @NotNull HuskClaims plugin) {
        return new DisplayHighlightBlock(position, type, plugin);
    }

    @Override
    public void showBlocks(@NotNull OnlineUser user, @NotNull Collection<DisplayHighlightBlock> blocks) {
        blocks.forEach(block -> block.show(user));
    }

    @Override
    public void stopHighlighting(@NotNull OnlineUser user) {
        if (!replacedBlocks.containsKey(user.getUuid())) {
            return;
        }
        replacedBlocks.removeAll(user.getUuid()).forEach(block -> {
            if (block instanceof DisplayHighlightBlock display) {
                display.remove(user);
            }
        });
    }

    @Override
    public boolean canUse(@NotNull OnlineUser user) {
        return plugin.getHook(GeyserHook.class).map(g -> !g.isBedrockPlayer(user.getUuid())).orElse(true);
    }

    @Override
    public short getPriority() {
        return PRIORITY;
    }

    public static final class DisplayHighlightBlock extends HighlightBlock {

        // Block display brightness value
        private static final Brightness FULL_BRIGHT = new Brightness(15, 15);

        // Block display scale constants
        private static final float SCALAR = 0.002f;
        private static final AffineTransformation SCALE_TRANSFORMATION = new AffineTransformation(
                new Vector3f(-(SCALAR / 2), -(SCALAR / 2), -(SCALAR / 2)),
                AffineTransformation.identity().getLeftRotation(),
                new Vector3f(AffineTransformation.identity().getScale()).add(SCALAR, SCALAR, SCALAR),
                AffineTransformation.identity().getRightRotation()
        );

        private final DisplayEntity.BlockDisplayEntity display;
        private Location location;
        private EntityTrackerEntry tracker;

        private DisplayHighlightBlock(@NotNull Position position, @NotNull Highlightable.Type type,
                                      @NotNull HuskClaims plugin) {
            super(position, plugin.getSettings().getHighlighter().getBlock(type, plugin));
            this.display = createEntity(plugin, type);
        }

        @NotNull
        private DisplayEntity.BlockDisplayEntity createEntity(@NotNull HuskClaims plugin, @NotNull Highlightable.Type type) {
            this.location = FabricHuskClaims.Adapter.adapt(position, ((FabricHuskClaims) plugin).getMinecraftServer());
            final BlockPos blockPos = location.blockPos();
            //#if MC==12104
            if (!location.world().isPosLoaded(blockPos)) {
            //#else
            //$$ if (!location.world().isChunkLoaded(blockPos)) {
            //#endif
                throw new IllegalStateException("World/chunk is not loaded");
            }

            // Create block display
            final DisplayEntity.BlockDisplayEntity display = new DisplayEntity.BlockDisplayEntity(
                    EntityType.BLOCK_DISPLAY, location.world());
            display.refreshPositionAndAngles(blockPos.getX(), blockPos.getY(), blockPos.getZ(), 0.0f, 0.0f);

            // Set parameters
            display.setBlockState(((BlockMaterialBlock) this.block).getData().getDefaultState());
            display.setViewRange(BlockHighlighter.VIEWING_RANGE);
            display.setNoGravity(true);
            display.setBrightness(FULL_BRIGHT);

            // Scale to prevent z-fighting
            display.setTransformation(SCALE_TRANSFORMATION);

            // Glow if needed
            if (plugin.getSettings().getHighlighter().isGlowEffect()) {
                display.setGlowing(true);
                display.setGlowColorOverride(plugin.getSettings().getHighlighter().getGlowColor(type).getArgb());
            }

            return display;
        }

        public void show(@NotNull OnlineUser user) {
            final ServerPlayerEntity player = ((FabricUser) user).getFabricPlayer();
            this.tracker = new EntityTrackerEntry(
                    location.world(), display, 
                    display.getType().getTrackTickInterval(), 
                    display.velocityDirty, 
                    player.networkHandler::sendPacket
            );
            tracker.startTracking(player);
        }

        public void remove(@NotNull OnlineUser user) {
            final ServerPlayerEntity player = ((FabricUser) user).getFabricPlayer();
            if (tracker != null) {
                tracker.stopTracking(player);
            }
            display.remove(Entity.RemovalReason.DISCARDED);
        }

    }

}