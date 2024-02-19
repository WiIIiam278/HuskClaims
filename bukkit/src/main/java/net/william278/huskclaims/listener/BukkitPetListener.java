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

import net.william278.huskclaims.HuskClaims;
import org.bukkit.entity.Entity;
import org.bukkit.event.Cancellable;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface BukkitPetListener extends Listener {

    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOW)
    default void onPlayerTamedInteract(@NotNull PlayerInteractEntityEvent e) {
        this.onUserTamedEntityAction(e, e.getPlayer(), e.getRightClicked());
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOW)
    default void onPlayerDamageTamed(@NotNull EntityDamageByEntityEvent e) {
        this.onUserTamedEntityAction(e, e.getDamager(), e.getEntity());
    }

    void onUserTamedEntityAction(@NotNull Cancellable event, @Nullable Entity player, @NotNull Entity entity);

    @NotNull
    HuskClaims getPlugin();

}
