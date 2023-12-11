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

import lombok.Getter;
import net.william278.huskclaims.BukkitHuskClaims;
import net.william278.huskclaims.HuskClaims;
import net.william278.huskclaims.position.Position;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

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

}
