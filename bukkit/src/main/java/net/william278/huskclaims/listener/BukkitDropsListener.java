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

import net.william278.huskclaims.BukkitHuskClaims;
import net.william278.huskclaims.HuskClaims;
import net.william278.huskclaims.moderation.DropsListener;
import net.william278.huskclaims.moderation.DropsProtector;
import net.william278.huskclaims.user.BukkitUser;
import net.william278.huskclaims.user.OnlineUser;
import net.william278.huskclaims.user.User;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.Optional;

public interface BukkitDropsListener extends DropsListener, Listener {

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGH)
    default void onPlayerDeath(@NotNull PlayerDeathEvent e) {
        handleDeathDrops(
                BukkitUser.adapt(e.getEntity(), getPlugin()),
                e.getDrops().stream().map(BukkitGroundItem::new).toList()
        );
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOW)
    default void onItemDestroy(@NotNull EntityDamageEvent e) {
        if (!(e.getEntity() instanceof Item item)) {
            return;
        }
        if (cancelItemDestroy(new BukkitGroundItem(item.getItemStack()))) {
            e.setCancelled(true);
            item.setInvulnerable(true);
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOW)
    default void onItemPickup(@NotNull EntityPickupItemEvent e) {
        final Optional<OnlineUser> pickerUpper = e.getEntity() instanceof Player player
                ? Optional.of(BukkitUser.adapt(player, getPlugin())) : Optional.empty();
        if (cancelItemPickup(pickerUpper.orElse(null), new BukkitGroundItem(e.getItem().getItemStack()))) {
            e.setCancelled(true);
        }
    }

    record BukkitGroundItem(@NotNull ItemStack stack) implements DropsProtector.GroundItem {

        @Override
        public Optional<User> getLockedBy(@NotNull HuskClaims plugin) {
            if (!stack().hasItemMeta()) {
                return Optional.empty();
            }
            final ItemMeta meta = Objects.requireNonNull(stack().getItemMeta(), "Couldn't get null ItemMeta");
            return getLockedBy(meta.getPersistentDataContainer(), plugin);
        }

        @Override
        public void setLocked(@Nullable User user, @NotNull HuskClaims plugin) {
            if (!stack.hasItemMeta()) {
                return;
            }
            final ItemMeta meta = Objects.requireNonNull(stack().getItemMeta(), "Couldn't set null ItemMeta");
            setLockedBy(meta.getPersistentDataContainer(), user, plugin);
        }

        @NotNull
        private static NamespacedKey getLockedKey(@NotNull HuskClaims plugin) {
            return Objects.requireNonNull(NamespacedKey.fromString("locked", (BukkitHuskClaims) plugin));
        }

        private Optional<User> getLockedBy(@NotNull PersistentDataContainer container, @NotNull HuskClaims plugin) {
            return Optional.ofNullable(container.get(getLockedKey(plugin), PersistentDataType.STRING))
                    .map(plugin::getUserFromJson);
        }

        void setLockedBy(@NotNull PersistentDataContainer container, @Nullable User user, @NotNull HuskClaims plugin) {
            if (user == null) {
                container.remove(getLockedKey(plugin));
                return;
            }
            container.set(getLockedKey(plugin), PersistentDataType.STRING, plugin.getGson().toJson(user));
        }

    }

}
