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
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

public class BukkitListener extends BukkitOperationListener implements ClaimsListener, UserListener {

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

    @EventHandler
    public void onPlayerSwitchHeldItem(@NotNull PlayerItemHeldEvent e) {
        final ItemStack selected = e.getPlayer().getInventory().getItem(e.getNewSlot());
        this.onUserSwitchHeldItem(
                BukkitUser.adapt(e.getPlayer(), plugin),
                (selected != null ? selected.getType() : Material.AIR).getKey().toString()
        );
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
