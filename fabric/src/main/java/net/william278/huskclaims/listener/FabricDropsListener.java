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

import lombok.Getter;
import net.minecraft.entity.ItemEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.william278.huskclaims.FabricHuskClaims;
import net.william278.huskclaims.moderation.DropsHandler;
import net.william278.huskclaims.user.OnlineUser;
import net.william278.huskclaims.util.Location;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Objects;
import java.util.UUID;

public interface FabricDropsListener {

    default void onPlayerDeath(ServerPlayerEntity player, Collection<ItemEntity> items) {
        if (items.isEmpty() || !getPlugin().getSettings().getModeration().getDrops().isLockItems()) {
            return;
        }
        OnlineUser user = getPlugin().getOnlineUser(player);
        getPlugin().lockDrops(
            user.getUuid(),
            items.stream().map(FabricGroundItem::new).toList()
        );
        getPlugin().getLocales().getLocale("death_drops_locked")
            .ifPresent(user::sendMessage);
    }

    @NotNull
    FabricHuskClaims getPlugin();

    @Getter
    class FabricDroppedItem implements DropsHandler.DroppedItem {

        private final ItemStack stack;
        private final Location dropLocation;

        FabricDroppedItem(@NotNull ItemStack stack, @NotNull Location dropLocation) {
            this.stack = stack;
            this.dropLocation = dropLocation;
        }

        public double distance(@NotNull FabricDroppedItem other) {
            return Objects.equals(dropLocation.world(), other.dropLocation.world())
                ? dropLocation.pos().distanceTo(other.getDropLocation().pos()) : Double.MAX_VALUE;
        }

        @Override
        public boolean equals(Object obj) {
            return obj instanceof FabricDroppedItem item
                && this.distance(item) <= DEATH_DROPS_EQUAL_RANGE
                && item.getStack() != null && item.getStack().equals(getStack());
        }

    }

    class FabricGroundItem implements DropsHandler.GroundStack {

        @NotNull
        @Getter
        private final ItemEntity entity;

        @Nullable
        private UUID owner;

        public FabricGroundItem(@NotNull ItemEntity entity) {
            this.entity = entity;
        }

        public void lock(@NotNull UUID owner, boolean preventDestruction) {
            this.owner = owner;
            updateEntity(preventDestruction);
        }

        public void unlock() {
            this.owner = null;
            updateEntity(false);
        }

        private void updateEntity(boolean preventDestruction) {
            entity.setInvulnerable(owner != null && preventDestruction);
            entity.setOwner(owner);
        }

        @Override
        @NotNull
        public DropsHandler.DroppedItem getStack() {
            return new FabricDroppedItem(entity.getStack(), new Location(entity));
        }

    }

}
