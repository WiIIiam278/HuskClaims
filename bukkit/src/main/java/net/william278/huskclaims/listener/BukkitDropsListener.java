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
import net.william278.huskclaims.BukkitHuskClaims;
import net.william278.huskclaims.moderation.DropsHandler;
import org.bukkit.Location;
import org.bukkit.entity.Item;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.ItemSpawnEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.UUID;

public interface BukkitDropsListener extends Listener {

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGH)
    default void onPlayerDeath(@NotNull PlayerDeathEvent e) {
        getPlugin().markItemsForLocking(
                getPlugin().getOnlineUser(e.getEntity()),
                e.getDrops().stream().map(item -> new BukkitDroppedItem(item, e.getEntity().getLocation())).toList()
        );
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGH)
    default void onItemSpawn(@NotNull ItemSpawnEvent e) {
        getPlugin().checkDroppedItem(new BukkitGroundItem(e.getEntity(), getPlugin()));
    }

    @NotNull
    BukkitHuskClaims getPlugin();

    @Getter
    class BukkitDroppedItem implements DropsHandler.DroppedItem {

        private final ItemStack stack;
        private final Location dropLocation;

        BukkitDroppedItem(@NotNull ItemStack stack, @NotNull Location dropLocation) {
            this.stack = stack;
            this.dropLocation = dropLocation;
        }

        public double distance(@NotNull BukkitDroppedItem other) {
            return Objects.equals(dropLocation.getWorld(), other.dropLocation.getWorld())
                    ? dropLocation.distance(other.getDropLocation()) : Double.MAX_VALUE;
        }

        @Override
        public boolean equals(Object obj) {
            return obj instanceof BukkitDroppedItem item
                   && dropLocation.getWorld() != null && this.distance(item) <= DEATH_DROPS_EQUAL_RANGE
                   && item.getStack() != null && item.getStack().equals(getStack());
        }
    }

    class BukkitGroundItem implements DropsHandler.GroundStack {

        @NotNull
        @Getter
        private final Item entity;

        @NotNull
        private final BukkitHuskClaims plugin;

        @Nullable
        private UUID owner;

        public BukkitGroundItem(@NotNull Item entity, @NotNull BukkitHuskClaims plugin) {
            this.entity = entity;
            this.plugin = plugin;
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
            plugin.runSync(entity, () -> {
                entity.setInvulnerable(owner != null && preventDestruction);
                entity.setOwner(owner);
            });
        }

        @Override
        @NotNull
        public DropsHandler.DroppedItem getStack() {
            return new BukkitDroppedItem(entity.getItemStack(), entity.getLocation());
        }
    }
}
