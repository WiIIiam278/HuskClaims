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

package net.william278.huskclaims.listener;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.world.World;
import net.william278.huskclaims.HuskClaims;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface FabricPetListener {

    default ActionResult onPlayerTamedInteract(PlayerEntity player, World world, Hand hand, Entity entity, @Nullable EntityHitResult hitResult) {
        return this.onUserTamedEntityAction(player, entity) ? ActionResult.PASS : ActionResult.FAIL;
    }

    default boolean onPlayerDamageTamed(LivingEntity entity, DamageSource damageSource, float damage) {
        return this.onUserTamedEntityAction(damageSource.getAttacker(), entity);
    }

    boolean onUserTamedEntityAction(@Nullable Entity player, @NotNull Entity entity);

    @NotNull
    HuskClaims getPlugin();

}
