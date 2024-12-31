package net.william278.huskclaims.mixins;

import it.unimi.dsi.fastutil.shorts.Short2ObjectMap;
import net.minecraft.block.BlockState;
import net.minecraft.network.packet.s2c.play.ChunkDeltaUpdateS2CPacket;
import net.minecraft.util.math.ChunkSectionPos;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.*;

@Mixin(ChunkDeltaUpdateS2CPacket.class)
public class ChunkDeltaUpdateS2CPacketMixin {

    @Final
    @Mutable
    @Shadow
    private ChunkSectionPos sectionPos;
    @Final
    @Mutable
    @Shadow
    private short[] positions;
    @Final
    @Mutable
    @Shadow
    private BlockState[] blockStates;

    @Unique
    public void cloplib_setValues(@NotNull Short2ObjectMap<BlockState> changes, ChunkSectionPos sectionPos) {
        this.sectionPos = sectionPos;
        this.positions = changes.keySet().toShortArray();
        this.blockStates = changes.values().toArray(new BlockState[0]);
    }

}
