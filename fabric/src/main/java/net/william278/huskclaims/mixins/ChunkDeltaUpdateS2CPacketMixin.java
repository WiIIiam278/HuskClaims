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
