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

import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.william278.huskclaims.util.PlayerActionEvents;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerPlayNetworkHandler.class)
public abstract class ServerPlayNetworkHandlerMixin {

    @Shadow
    public ServerPlayerEntity player;

    @Inject(method = "onPlayerAction", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/network/ServerPlayerEntity;clearActiveItem()V"))
    private void onPlayerActionMixin(PlayerActionC2SPacket packet, CallbackInfo ci) {
        PlayerActionEvents.AFTER_HELD_ITEM_CHANGE.invoker().swapHands(
                player, player.getMainHandStack(), player.getOffHandStack()
        );
    }

    @Inject(method = "onUpdateSelectedSlot", at = @At("TAIL"))
    private void onUpdateSelectedSlotMixin(UpdateSelectedSlotC2SPacket packet, CallbackInfo ci) {
        PlayerActionEvents.AFTER_HELD_ITEM_CHANGE.invoker().swapHands(
                player, player.getMainHandStack(), player.getOffHandStack()
        );
    }

}
