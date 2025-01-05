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

import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalBooleanRef;
import java.util.List;
import net.minecraft.block.entity.SignBlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.filter.FilteredMessage;
import net.minecraft.server.network.ServerPlayerEntity;
import net.william278.huskclaims.util.PlayerActionEvents;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(SignBlockEntity.class)
public abstract class SignBlockEntityMixin {

    @ModifyArg(method = "method_49845", at = @At(value = "INVOKE", target = "Lnet/minecraft/block/entity/SignBlockEntity;getTextWithMessages(Lnet/minecraft/entity/player/PlayerEntity;Ljava/util/List;Lnet/minecraft/block/entity/SignText;)Lnet/minecraft/block/entity/SignText;"))
    List<FilteredMessage> changeTextLambdaMixin(List<FilteredMessage> messages, @Local(argsOnly = true) PlayerEntity player,
            @Share("front") LocalBooleanRef frontRef) {
        return PlayerActionEvents.BEFORE_CHANGE_TEXT_ON_SIGN.invoker()
            .changeTextOnSign((SignBlockEntity) (Object) this, (ServerPlayerEntity) player, frontRef.get(), messages);
    }

}
