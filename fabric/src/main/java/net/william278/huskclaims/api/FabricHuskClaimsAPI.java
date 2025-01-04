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

import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Pair;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.TeleportTarget;
import net.william278.huskclaims.FabricHuskClaims;
import net.william278.huskclaims.HuskClaims;
import net.william278.huskclaims.position.Position;
import net.william278.huskclaims.position.World;
import net.william278.huskclaims.user.OnlineUser;
import net.william278.huskclaims.util.Location;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

/**
 * The Fabric implementation of the HuskClaims API. Get the instance with {@link #getInstance()}.
 */
@SuppressWarnings("unused")
public class FabricHuskClaimsAPI extends HuskClaimsAPI {

    /**
     * <b>(Internal use only)</b> - Constructor, instantiating the API.
     *
     * @param plugin The HuskClaims plugin instance
     * @since 1.5
     */
    protected FabricHuskClaimsAPI(@NotNull HuskClaims plugin) {
        super(plugin);
    }

    /**
     * Get an instance of the HuskClaims API.
     *
     * @return instance of the HuskClaims API
     * @throws NotRegisteredException if the API has not yet been registered.
     * @since 1.5
     */
    @NotNull
    public static FabricHuskClaimsAPI getInstance() throws NotRegisteredException {
        return (FabricHuskClaimsAPI) HuskClaimsAPI.getInstance();
    }

    /**
     * <b>(Internal use only)</b> - Unregister the API instance.
     *
     * @since 1.5
     */
    @ApiStatus.Internal
    public static void register(@NotNull FabricHuskClaims plugin) {
        instance = new FabricHuskClaimsAPI(plugin);
    }

    /**
     * Get the {@link World} object for a given {@link net.minecraft.world.World}
     *
     * @param world The Minecraft world object
     * @return The HuskClaims world
     * @since 1.5
     */
    @NotNull
    public World getWorld(@NotNull net.minecraft.world.World world) {
        return FabricHuskClaims.Adapter.adapt(world);
    }

    /**
     * Get the {@link net.minecraft.world.World} object for a given {@link World}
     *
     * @param world The HuskClaims world
     * @return The Minecraft world object
     * @since 1.5
     */
    @NotNull
    public net.minecraft.world.World getWorld(@NotNull World world) {
        return FabricHuskClaims.Adapter.adapt(world, ((FabricHuskClaims) getPlugin()).getMinecraftServer());
    }

    /**
     * Get the {@link Position} object for a given {@link BlockPos block position} in a {@link net.minecraft.world.World}
     *
     * @param world    the Minecraft world
     * @param position the Minecraft vector block position
     * @return The HuskClaims position
     * @since 1.5
     */
    @NotNull
    public Position getPosition(@NotNull net.minecraft.world.World world, @NotNull BlockPos position) {
        return FabricHuskClaims.Adapter.adapt(world, position.toCenterPos(), 0.0f, 0.0f);
    }

    /**
     * Get the {@link Position} object for a given {@link Vec3d vector position} in a {@link net.minecraft.world.World}
     *
     * @param world    the Minecraft world
     * @param position the Minecraft vector coordinate position
     * @return The HuskClaims position
     * @since 1.5
     */
    @NotNull
    public Position getPosition(@NotNull net.minecraft.world.World world, @NotNull Vec3d position) {
        return FabricHuskClaims.Adapter.adapt(world, position, 0.0f, 0.0f);
    }

    /**
     * Get the {@link Position} object for a given {@link Vec3d vector position} in a {@link net.minecraft.world.World}
     *
     * @param world    the Minecraft world
     * @param position the Minecraft vector coordinate position
     * @param yaw      the positional yaw angle
     * @param pitch    the positional pitch angle
     * @return The HuskClaims position
     * @since 1.5
     */
    @NotNull
    public Position getPosition(@NotNull net.minecraft.world.World world, @NotNull Vec3d position,
                                float yaw, float pitch) {
        return FabricHuskClaims.Adapter.adapt(world, position, yaw, pitch);
    }

    /**
     * Get the {@link Location} containing Minecraft positional object data for a {@link Position}
     *
     * @param position the HuskClaims position
     * @return The HuskClaims position
     * @since 1.5
     */
    @NotNull
    public Location getLocation(@NotNull Position position) {
        return FabricHuskClaims.Adapter.adapt(position, ((FabricHuskClaims) getPlugin()).getMinecraftServer());
    }

    /**
     * Get a {@link TeleportTarget} for a {@link Position}
     *
     * @param position the HuskClaims position
     * @return a {@link TeleportTarget teleportation target} object
     * @since 1.5
     */
    @NotNull
    public TeleportTarget getTeleportTarget(@NotNull Position position) {
        return getLocation(position).teleportTarget();
    }

    /**
     * Get the {@link net.minecraft.world.World} and {@link Vec3d} pair for a {@link Position}
     *
     * @param position the HuskClaims position
     * @return The {@link net.minecraft.world.World}-{@link Vec3d} {@link Pair}
     * @since 1.5
     */
    @NotNull
    public Pair<ServerWorld, Vec3d> getWorldVec3dPair(@NotNull Position position) {
        final Location loc = getLocation(position);
        return new Pair<>(loc.world(), loc.pos());
    }

    /**
     * Get the {@link net.minecraft.world.World} and {@link Vec3d} pair for a {@link Position}
     *
     * @param position the HuskClaims position
     * @return The {@link net.minecraft.world.World}-{@link BlockPos} {@link Pair}
     * @since 1.5
     */
    @NotNull
    public Pair<ServerWorld, BlockPos> getWorldBlockPosPair(@NotNull Position position) {
        final Location loc = getLocation(position);
        return new Pair<>(loc.world(), loc.blockPos());
    }

    /**
     * Get the {@link OnlineUser} object for a given {@link ServerPlayerEntity}
     *
     * @param player The Minecraft {@link ServerPlayerEntity server player entity}
     * @return The HuskClaims online user
     * @since 1.5
     */
    @NotNull
    public OnlineUser getOnlineUser(@NotNull ServerPlayerEntity player) {
        return ((FabricHuskClaims) plugin).getOnlineUser(player);
    }

}
