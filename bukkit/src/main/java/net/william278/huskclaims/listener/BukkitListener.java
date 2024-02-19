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

import net.william278.cloplib.listener.BukkitOperationListener;
import net.william278.cloplib.operation.OperationPosition;
import net.william278.cloplib.operation.OperationUser;
import net.william278.huskclaims.BukkitHuskClaims;
import net.william278.huskclaims.HuskClaims;
import net.william278.huskclaims.user.BukkitUser;
import net.william278.huskclaims.user.User;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Tameable;
import org.bukkit.event.Cancellable;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

public class BukkitListener extends BukkitOperationListener implements BukkitPetListener, ClaimsListener, UserListener {

    private final BukkitHuskClaims plugin;

    public BukkitListener(@NotNull BukkitHuskClaims plugin) {
        super(plugin, plugin);
        this.plugin = plugin;
    }

    @Override
    public void register() {
        ClaimsListener.super.register();
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler
    public void onPlayerJoin(@NotNull PlayerJoinEvent e) {
        this.onUserJoin(BukkitUser.adapt(e.getPlayer(), plugin));
    }

    @EventHandler
    public void onPlayerQuit(@NotNull PlayerQuitEvent e) {
        this.onUserQuit(BukkitUser.adapt(e.getPlayer(), plugin));
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onPlayerSwitchHeldItem(@NotNull PlayerItemHeldEvent e) {
        final ItemStack mainHand = e.getPlayer().getInventory().getItem(e.getNewSlot());
        final ItemStack offHand = e.getPlayer().getInventory().getItemInOffHand();
        this.onUserSwitchHeldItem(
                BukkitUser.adapt(e.getPlayer(), plugin),
                (mainHand != null ? mainHand.getType() : Material.AIR).getKey().toString(),
                offHand.getType().getKey().toString()
        );
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onUserSwapHands(@NotNull PlayerSwapHandItemsEvent e) {
        final ItemStack mainHand = e.getMainHandItem();
        final ItemStack offHand = e.getOffHandItem();
        this.onUserSwitchHeldItem(
                BukkitUser.adapt(e.getPlayer(), plugin),
                (mainHand != null ? mainHand.getType() : Material.AIR).getKey().toString(),
                (offHand != null ? offHand.getType() : Material.AIR).getKey().toString()
        );
    }

    @Override
    public void onUserTamedEntityAction(@NotNull Cancellable event, @Nullable Entity player, @NotNull Entity entity) {
        // If pets are enabled, check if the entity is tamed
        if (player == null || !getPlugin().getSettings().getPets().isEnabled() || !(entity instanceof Tameable tamed)) {
            return;
        }

        // Check it was damaged by a player
        final Optional<Player> source = getPlayerSource(player);
        final Optional<User> owner = ((BukkitHuskClaims) getPlugin()).getPetOwner(tamed);
        if (source.isEmpty() || owner.isEmpty()) {
            return;
        }

        // Don't cancel the event if there's no mismatch
        if (getPlugin().cancelPetOperation(BukkitUser.adapt(source.get(), getPlugin()), owner.get())) {
            event.setCancelled(true);
        }
    }

    @Override
    @NotNull
    public OperationPosition getPosition(@NotNull Location location) {
        return BukkitHuskClaims.Adapter.adapt(location);
    }

    @Override
    @NotNull
    public OperationUser getUser(@NotNull Player player) {
        return BukkitUser.adapt(player, plugin);
    }

    @NotNull
    @Override
    public HuskClaims getPlugin() {
        return plugin;
    }

    @Override
    public void setInspectionDistance(int i) {
        throw new UnsupportedOperationException("Cannot change inspection distance");
    }

}
