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

package net.william278.huskclaims.api;

import net.william278.huskclaims.BukkitHuskClaims;
import net.william278.huskclaims.HuskClaims;
import net.william278.huskclaims.position.Position;
import net.william278.huskclaims.position.World;
import net.william278.huskclaims.user.BukkitUser;
import net.william278.huskclaims.user.OnlineUser;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

/**
 * The Bukkit implementation of the HuskClaims API. Get the instance with {@link #getInstance()}.
 */
@SuppressWarnings("unused")
public class BukkitHuskClaimsAPI extends HuskClaimsAPI {

    /**
     * <b>(Internal use only)</b> - Constructor, instantiating the API.
     *
     * @param plugin The HuskClaims plugin instance
     * @since 1.0
     */
    protected BukkitHuskClaimsAPI(@NotNull HuskClaims plugin) {
        super(plugin);
    }

    /**
     * Get an instance of the HuskClaims API.
     *
     * @return instance of the HuskClaims API
     * @throws NotRegisteredException if the API has not yet been registered.
     * @since 1.0
     */
    @NotNull
    public static BukkitHuskClaimsAPI getInstance() throws NotRegisteredException {
        return (BukkitHuskClaimsAPI) HuskClaimsAPI.getInstance();
    }

    /**
     * <b>(Internal use only)</b> - Unregister the API instance.
     *
     * @since 1.0
     */
    @ApiStatus.Internal
    public static void register(@NotNull BukkitHuskClaims plugin) {
        instance = new BukkitHuskClaimsAPI(plugin);
    }

    /**
     * Get the {@link World} object for a given {@link org.bukkit.World}
     *
     * @param world The Bukkit world
     * @return The HuskClaims world
     * @since 1.0
     */
    @NotNull
    public World getWorld(@NotNull org.bukkit.World world) {
        return BukkitHuskClaims.Adapter.adapt(world);
    }

    /**
     * Get the {@link org.bukkit.World} object for a given {@link World}
     *
     * @param world The HuskClaims world
     * @return The Bukkit world
     * @since 1.0
     */
    @NotNull
    public org.bukkit.World getWorld(@NotNull World world) {
        return BukkitHuskClaims.Adapter.adapt(world);
    }

    /**
     * Get the {@link Position} object for a given {@link org.bukkit.Location}
     *
     * @param location The Bukkit location
     * @return The HuskClaims position
     * @since 1.0
     */
    @NotNull
    public Position getPosition(@NotNull org.bukkit.Location location) {
        return BukkitHuskClaims.Adapter.adapt(location);
    }

    /**
     * Get the {@link org.bukkit.Location} object for a given {@link Position}
     *
     * @param position The HuskClaims position
     * @return The Bukkit location
     * @since 1.0
     */
    @NotNull
    public org.bukkit.Location getLocation(@NotNull Position position) {
        return BukkitHuskClaims.Adapter.adapt(position);
    }

    /**
     * Get the {@link OnlineUser} object for a given {@link org.bukkit.entity.Player}
     *
     * @param player The Bukkit player
     * @return The HuskClaims online user
     * @since 1.0
     */
    @NotNull
    public OnlineUser getOnlineUser(@NotNull org.bukkit.entity.Player player) {
        return ((BukkitHuskClaims) plugin).getOnlineUser(player);
    }

    /**
     * Get the {@link org.bukkit.entity.Player} object for a given {@link OnlineUser}
     *
     * @param user The HuskClaims online user
     * @return The Bukkit player
     * @since 1.0
     */
    @NotNull
    public org.bukkit.entity.Player getPlayer(@NotNull OnlineUser user) {
        return ((BukkitUser) user).getBukkitPlayer();
    }

}
