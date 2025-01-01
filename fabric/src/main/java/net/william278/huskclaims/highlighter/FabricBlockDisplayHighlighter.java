package net.william278.huskclaims.highlighter;

import net.minecraft.entity.AreaEffectCloudEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.decoration.Brightness;
import net.minecraft.entity.decoration.DisplayEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.AffineTransformation;
import net.william278.huskclaims.FabricHuskClaims;
import net.william278.huskclaims.HuskClaims;
import net.william278.huskclaims.hook.GeyserHook;
import net.william278.huskclaims.position.Position;
import net.william278.huskclaims.user.FabricUser;
import net.william278.huskclaims.user.OnlineUser;
import net.william278.huskclaims.util.BlockMaterialBlock;
import net.william278.huskclaims.util.Location;
import org.jetbrains.annotations.NotNull;
import org.joml.AxisAngle4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.Collection;
import java.util.UUID;

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
        blocks.forEach(block -> block.show(plugin, user));
    }

    @Override
    public void stopHighlighting(@NotNull OnlineUser user) {
        if (!replacedBlocks.containsKey(user.getUuid())) {
            return;
        }
        plugin.runSync(() -> replacedBlocks.removeAll(user.getUuid()).forEach(block -> {
            if (block instanceof DisplayHighlightBlock display) {
                display.remove();
            }
        }));
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

        public static final TrackedData<String> VISIBLE_TO = DataTracker.registerData(
                DisplayEntity.BlockDisplayEntity.class, TrackedDataHandlerRegistry.STRING
        );

        // Block display brightness value
        private static final Brightness FULL_BRIGHT = new Brightness(15, 15);

        // Block display scale constants
        private static final float SCALAR = 0.002f;
        private static final AffineTransformation SCALE_TRANSFORMATION = new AffineTransformation(
                new Vector3f(-(SCALAR / 2), -(SCALAR / 2), -(SCALAR / 2)),
                new Quaternionf(0, 0, 0, 0),
                new Vector3f(1 + SCALAR, 1 + SCALAR, 1 + SCALAR),
                new Quaternionf(0, 0, 0, 0)
        );

        private final DisplayEntity.BlockDisplayEntity display;

        private DisplayHighlightBlock(@NotNull Position position, @NotNull Highlightable.Type type,
                                      @NotNull HuskClaims plugin) {
            super(position, plugin.getSettings().getHighlighter().getBlock(type, plugin));
            this.display = createEntity(plugin, type);
        }

        @NotNull
        private DisplayEntity.BlockDisplayEntity createEntity(@NotNull HuskClaims plugin, @NotNull Highlightable.Type type) {
            final Location location = FabricHuskClaims.Adapter.adapt(position, ((FabricHuskClaims) plugin).getMinecraftServer());
            if (!location.world().isPosLoaded(location.blockPos())) {
                throw new IllegalStateException("World/chunk is not loaded");
            }

            // Create block display
            final DisplayEntity.BlockDisplayEntity display = EntityType.BLOCK_DISPLAY
                    .spawn(location.world(), location.blockPos(), SpawnReason.COMMAND);
            if (display == null) {
                throw new IllegalStateException("Failed to spawn display");
            }
            display.setBlockState(((BlockMaterialBlock) this.block).getData().getDefaultState());
            display.setViewRange(BlockHighlighter.VIEWING_RANGE);
            display.setNoGravity(true);
            display.setInvisible(true);
            display.setBrightness(FULL_BRIGHT);
            display.setCustomNameVisible(false);

            // Scale to prevent z-fighting
            display.setTransformation(SCALE_TRANSFORMATION);

            // Glow if needed
            if (plugin.getSettings().getHighlighter().isGlowEffect()) {
                display.setGlowing(true);
                display.setGlowColorOverride(
                        plugin.getSettings().getHighlighter().getGlowColor(type).getArgb()
                );
            }
            return display;
        }

        public void show(@NotNull HuskClaims plugin, @NotNull OnlineUser user) {
            display.getDataTracker().set(VISIBLE_TO, user.getUuid().toString());
        }

        public void remove() {
            if (display != null) {
                display.remove(Entity.RemovalReason.DISCARDED);
            }
        }

    }
}