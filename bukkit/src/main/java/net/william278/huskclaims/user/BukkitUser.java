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

package net.william278.huskclaims.user;

import io.papermc.lib.PaperLib;
import lombok.Getter;
import net.william278.cloplib.listener.InspectorCallbackProvider;
import net.william278.huskclaims.BukkitHuskClaims;
import net.william278.huskclaims.HuskClaims;
import net.william278.huskclaims.hook.HuskHomesHook;
import net.william278.huskclaims.position.Position;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionDefault;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Getter
public class BukkitUser extends OnlineUser {

    private final Player bukkitPlayer;

    private BukkitUser(@NotNull Player bukkitPlayer, @NotNull HuskClaims plugin) {
        super(bukkitPlayer.getName(), bukkitPlayer.getUniqueId(), plugin);
        this.bukkitPlayer = bukkitPlayer;
    }

    @NotNull
    public static BukkitUser adapt(@NotNull Player player, @NotNull HuskClaims plugin) {
        return new BukkitUser(player, plugin);
    }

    @NotNull
    @Override
    public Position getPosition() {
        return BukkitHuskClaims.Adapter.adapt(bukkitPlayer.getLocation());
    }

    @Override
    public void sendPluginMessage(@NotNull String channel, byte[] message) {
        bukkitPlayer.sendPluginMessage((BukkitHuskClaims) plugin, channel, message);
    }

    @Override
    public boolean hasPermission(@NotNull String permission) {
        return bukkitPlayer.hasPermission(permission);
    }

    @Override
    public boolean hasPermission(@NotNull String permission, boolean isDefault) {
        return bukkitPlayer.hasPermission(new Permission(
                permission, isDefault ? PermissionDefault.TRUE : PermissionDefault.OP
        ));
    }

    @Override
    public boolean isHolding(@NotNull InspectorCallbackProvider.InspectionTool tool) {
        final PlayerInventory inventory = bukkitPlayer.getInventory();
        final List<ItemStack> toCheck = List.of(inventory.getItemInMainHand(), inventory.getItemInOffHand());
        return toCheck.stream().anyMatch(
                item -> item != null && item.getType().getKey().getKey().equals(tool.material()) && (
                        !tool.useCustomModelData() || item.hasItemMeta()
                                                      && item.getItemMeta() != null && item.getItemMeta().hasCustomModelData()
                                                      && item.getItemMeta().getCustomModelData() == tool.customModelData()
                )
        );
    }

    @Override
    public Optional<Long> getNumericalPermission(@NotNull String prefix) {
        return bukkitPlayer.getEffectivePermissions().stream()
                .filter(perm -> perm.getPermission().startsWith(prefix))
                .map(perm -> perm.getPermission().substring(prefix.length()))
                .map(value -> {
                    try {
                        return Long.parseLong(value);
                    } catch (NumberFormatException e) {
                        return null;
                    }
                })
                .filter(Objects::nonNull).sorted()
                .findFirst();
    }

    @Override
    public boolean isSneaking() {
        return bukkitPlayer.isSneaking();
    }

    @Override
    public void teleport(@NotNull Position position, boolean instant) {
        if (instant) {
            teleportInstant(position);
            return;
        }
        plugin.getHook(HuskHomesHook.class).ifPresentOrElse(
                homes -> homes.teleport(this, position, plugin.getServerName()),
                () -> teleportInstant(position)
        );
    }

    private void teleportInstant(@NotNull Position position) {
        PaperLib.teleportAsync(bukkitPlayer, BukkitHuskClaims.Adapter.adapt(position));
    }

}
