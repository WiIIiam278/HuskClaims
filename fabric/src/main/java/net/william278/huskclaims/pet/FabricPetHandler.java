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

package net.william278.huskclaims.pet;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
//#if MC==12105
import net.minecraft.entity.LazyEntityReference;
//#endif
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.passive.TameableEntity;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.william278.huskclaims.FabricHuskClaims;
import net.william278.huskclaims.user.FabricUser;
import net.william278.huskclaims.user.OnlineUser;
import net.william278.huskclaims.user.User;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;
import java.util.UUID;

public interface FabricPetHandler extends PetHandler {

    int TRANSFER_RANGE = 6;

    @Override
    default void userTransferPet(@NotNull OnlineUser user, @NotNull User newOwner, boolean forceTransfer) {
        getPlugin().runSync(() -> {
            final ServerPlayerEntity player = ((FabricUser) user).getFabricPlayer();
            final Optional<TameableEntity> lookingAt = getLookingAtTamed(player);
            final Optional<User> owner = lookingAt.flatMap(this::getPetOwner);
            if (lookingAt.isEmpty() || owner.isEmpty()) {
                getPlugin().getLocales().getLocale("error_look_at_pet_transfer")
                    .ifPresent(user::sendMessage);
                return;
            }

            if (forceTransfer || !cancelPetOperation(user, owner.get())) {
                this.transferPet(lookingAt.get(), user, newOwner);
            }
        });
    }

    private void transferPet(@NotNull TameableEntity tamed, @NotNull OnlineUser owner, @NotNull User newOwner) {
        //#if MC>=12105
        tamed.setOwner(new LazyEntityReference<>(newOwner.getUuid()));
        //#else
        //$$ tamed.setOwnerUuid(newOwner.getUuid());
        //#endif

        //#if MC>=12104
        Component nameComponent = getPlugin().getAudiences().asAdventure(tamed.getName());
        //#else
        //$$ Component nameComponent = tamed.getName().asComponent();
        //#endif
        String plainName = PlainTextComponentSerializer.plainText().serialize(nameComponent);
        getPlugin().getLocales().getLocale("pet_transferred", plainName, newOwner.getName())
            .ifPresent(owner::sendMessage);
    }

    default Optional<User> getPetOwner(@NotNull TameableEntity entity) {
        if (!entity.isTamed()) {
            return Optional.empty();
        }
        //#if MC>=12105
        final UUID ownerUuid = entity.getOwnerReference() != null ? entity.getOwnerReference().getUuid() : null;
        //#else
        //$$ final UUID ownerUuid = entity.getOwnerUuid();
        //#endif
        if (ownerUuid == null) {
            return Optional.empty();
        }

        final LivingEntity owner = entity.getOwner();
        return Optional.of(User.of(ownerUuid, !(owner instanceof ServerPlayerEntity playerOwner)
            ? getPlugin().getLocales().getNotApplicable() : playerOwner.getGameProfile().getName()));
    }

    private Optional<TameableEntity> getLookingAtTamed(@NotNull ServerPlayerEntity player) {
        Vec3d minPos = player.getCameraPosVec(0);
        Vec3d connect = player.getRotationVec(0).multiply(TRANSFER_RANGE);
        Vec3d maxPos = minPos.add(connect);
        Box box = player.getBoundingBox().stretch(connect).expand(1);
        EntityHitResult hitResult = ProjectileUtil.raycast(player, minPos, maxPos, box, entity -> entity instanceof TameableEntity, TRANSFER_RANGE * TRANSFER_RANGE);
        if (hitResult == null) {
            return Optional.empty();
        }
        return Optional.of((TameableEntity) hitResult.getEntity());
    }

    @NotNull
    FabricHuskClaims getPlugin();

}
