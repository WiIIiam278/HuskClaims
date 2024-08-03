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

import com.google.common.collect.Lists;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import net.william278.cloplib.operation.Operation;
import net.william278.cloplib.operation.OperationType;
import net.william278.huskclaims.HuskClaims;
import net.william278.huskclaims.claim.Claim;
import net.william278.huskclaims.claim.ClaimWorld;
import net.william278.huskclaims.claim.Region;
import net.william278.huskclaims.claim.ServerWorldClaim;
import net.william278.huskclaims.highlighter.Highlighter;
import net.william278.huskclaims.hook.EconomyHook;
import net.william278.huskclaims.position.BlockPosition;
import net.william278.huskclaims.position.Position;
import net.william278.huskclaims.position.ServerWorld;
import net.william278.huskclaims.position.World;
import net.william278.huskclaims.trust.TrustLevel;
import net.william278.huskclaims.trust.TrustTag;
import net.william278.huskclaims.trust.Trustable;
import net.william278.huskclaims.trust.UserGroup;
import net.william278.huskclaims.user.ClaimBlocksManager;
import net.william278.huskclaims.user.OnlineUser;
import net.william278.huskclaims.user.SavedUser;
import net.william278.huskclaims.user.User;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Range;
import org.jetbrains.annotations.Unmodifiable;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * The <a href="https://william278.net/docs/huskclaims/api">HuskClaims API</a>.
 * <p>
 * Get the singleton instance with {@link #getInstance()}.
 *
 * @since 1.0
 */
@AllArgsConstructor(access = AccessLevel.PROTECTED)
@SuppressWarnings("unused")
public class HuskClaimsAPI {

    // Singleton API instance
    protected static HuskClaimsAPI instance;

    // Plugin instance
    protected final HuskClaims plugin;

    /**
     * Get a {@link SavedUser} instance for a player, by UUID.
     *
     * @param uuid the UUID of the player
     * @return a {@link CompletableFuture} containing an {@link Optional} of the {@link SavedUser} instance, if found.
     * This contains persisted (offline) player data pertinent to HuskClaims.
     * @since 1.0
     */
    public CompletableFuture<Optional<SavedUser>> getUser(@NotNull UUID uuid) {
        return plugin.supplyAsync(() -> plugin.getDatabase().getUser(uuid));
    }

    /**
     * Get a {@link SavedUser} instance for a player, by username.
     *
     * @param name the name of the player
     * @return a {@link CompletableFuture} containing an {@link Optional} of the {@link SavedUser} instance, if found.
     * This contains persisted (offline) player data pertinent to HuskClaims.
     * @since 1.0
     */
    public CompletableFuture<Optional<SavedUser>> getUser(@NotNull String name) {
        return plugin.supplyAsync(() -> plugin.getDatabase().getUser(name));
    }

    /**
     * Get a {@link User}'s claim blocks.
     * <p>
     * If you are checking the claim blocks of an online player, it is recommended to use
     * {@link #getClaimBlocks(OnlineUser)} instead.
     *
     * @param user the user
     * @return a {@link CompletableFuture} containing the number of claim blocks the user has.
     * @since 1.0
     */
    public CompletableFuture<Long> getClaimBlocks(@NotNull User user) {
        return plugin.supplyAsync(() -> plugin.getClaimBlocks(user));
    }

    /**
     * Get a {@link User}'s claim blocks.
     * <p>
     * If you are checking the claim blocks of an online player, it is recommended to use
     * {@link #getClaimBlocks(OnlineUser)} instead.
     *
     * @param uuid the UUID of the user
     * @return a {@link CompletableFuture} containing the number of claim blocks the user has.
     * @since 1.0
     */
    public CompletableFuture<Long> getClaimBlocks(@NotNull UUID uuid) {
        return plugin.supplyAsync(() -> plugin.getClaimBlocks(uuid));
    }

    /**
     * Check if a {@link User} has more than a certain amount of claim blocks.
     * <p>
     * If you are checking the claim blocks of an online player, it is recommended to use
     * {@link #getClaimBlocks(OnlineUser)} instead.
     *
     * @param user   the user
     * @param amount the amount of claim blocks to check for (positive integer)
     * @return a {@link CompletableFuture} containing {@code true} if the user has more than the specified amount of
     * claim blocks, else {@code false}
     * @since 1.0
     */
    public CompletableFuture<Boolean> hasClaimBlocks(@NotNull User user, @Range(from = 0, to = Long.MAX_VALUE) long amount) {
        return getClaimBlocks(user).thenApply(blocks -> blocks >= amount);
    }

    /**
     * Get a {@link User}'s claim blocks.
     *
     * @param user the user
     * @return a {@link CompletableFuture} containing the number of claim blocks the user has.
     * @since 1.0
     */
    public long getClaimBlocks(@NotNull OnlineUser user) {
        return plugin.getClaimBlocks(user);
    }

    /**
     * Check if an {@link OnlineUser} has more than a certain amount of claim blocks.
     *
     * @param user   the user
     * @param amount the amount of claim blocks to check for (positive integer)
     * @return {@code true} if the user has more than the specified amount of claim blocks, else {@code false}
     * @since 1.0
     */
    public boolean hasClaimBlocks(@NotNull OnlineUser user, @Range(from = 0, to = Long.MAX_VALUE) long amount) {
        return getClaimBlocks(user) >= amount;
    }

    /**
     * Edit a user's claim blocks. This will occur asynchronously and the outcome may be affected by the
     * cancellable {@link net.william278.huskclaims.event.ClaimBlocksChangeEvent} event.
     *
     * @param user   the user
     * @param editor the editor
     * @since 1.0
     */
    public void editClaimBlocks(@NotNull User user, @NotNull Function<Long, Long> editor) {
        plugin.editClaimBlocks(user, ClaimBlocksManager.ClaimBlockSource.API, editor);
    }

    /**
     * Edit a user's claim blocks. This will occur asynchronously and the outcome may be affected by the
     * cancellable {@link net.william278.huskclaims.event.ClaimBlocksChangeEvent} event.
     *
     * @param user     the user
     * @param editor   the editor
     * @param callback a callback to run after the claim blocks have been edited. This will not be run if the
     *                 {@link net.william278.huskclaims.event.ClaimBlocksChangeEvent} event is cancelled.
     * @since 1.0
     */
    public void editClaimBlocks(@NotNull User user, @NotNull Function<Long, Long> editor,
                                @NotNull Consumer<Long> callback) {
        plugin.editClaimBlocks(user, ClaimBlocksManager.ClaimBlockSource.API, editor, callback);
    }

    /**
     * Deduct claim blocks from a user. This will occur asynchronously and the outcome may be affected by the
     * cancellable {@link net.william278.huskclaims.event.ClaimBlocksChangeEvent} event.
     *
     * @param user   the user
     * @param amount the amount of claim blocks to give (positive integer)
     * @throws IllegalArgumentException if the transaction would produce a negative claim block balance
     * @since 1.0
     */
    public void takeClaimBlocks(@NotNull OnlineUser user, @Range(from = 0, to = Long.MAX_VALUE) long amount) throws IllegalArgumentException {
        if (!hasClaimBlocks(user, amount)) {
            throw new IllegalArgumentException("Claim blocks cannot be negative");
        }
        editClaimBlocks(user, (blocks) -> blocks - amount);
    }

    /**
     * Take claim blocks from a user. This will occur asynchronously and the outcome may be affected by the
     * cancellable {@link net.william278.huskclaims.event.ClaimBlocksChangeEvent} event.
     *
     * @param user   the user
     * @param amount the amount of claim blocks to take (positive integer)
     * @since 1.0
     */
    public void giveClaimBlocks(@NotNull OnlineUser user, @Range(from = 0, to = Long.MAX_VALUE) long amount) {
        editClaimBlocks(user, (blocks) -> blocks + amount);
    }

    /**
     * Set a user's claim blocks. This will occur asynchronously and the outcome may be affected by the
     * cancellable {@link net.william278.huskclaims.event.ClaimBlocksChangeEvent} event.
     *
     * @param user   the user
     * @param amount the amount of claim blocks to set (positive integer)
     * @since 1.0
     */
    public void setClaimBlocks(@NotNull OnlineUser user, @Range(from = 0, to = Long.MAX_VALUE) long amount) {
        editClaimBlocks(user, (blocks) -> amount);
    }

    /**
     * Edit a {@link SavedUser}''s data.
     *
     * @param userUuid the UUID of the user
     * @param editor   the editor
     * @since 1.0
     */
    public void editSavedUser(@NotNull UUID userUuid, @NotNull Consumer<SavedUser> editor) {
        if (plugin.getSavedUser(userUuid).isEmpty()) {
            getUser(userUuid).thenAccept(optional -> optional.ifPresent((saved) -> {
                editor.accept(saved);
                plugin.getDatabase().updateUser(saved);
            }));
            return;
        }
        plugin.runQueued(() -> plugin.editSavedUser(userUuid, editor));
    }

    /**
     * Get the {@link OnlineUser} instance representing an online player.
     *
     * @param uuid the UUID of the player
     * @return the {@link OnlineUser} instance
     * @throws IllegalArgumentException if the player is not online
     * @since 1.0
     */
    @NotNull
    public OnlineUser getOnlineUser(@NotNull UUID uuid) {
        return plugin.getOnlineUsers().stream()
                .filter(user -> user.getUuid().equals(uuid)).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("No user online with UUID " + uuid));
    }

    /**
     * Get the {@link World} instance representing a server world.
     *
     * @param worldName the name of the world
     * @return the {@link World} instance
     * @throws IllegalArgumentException if the world does not exist
     * @since 1.0
     */
    @NotNull
    public World getWorld(@NotNull String worldName) {
        return plugin.getWorlds().stream()
                .filter(world -> world.getName().equals(worldName)).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("No world with name " + worldName));
    }

    /**
     * Get the list of claim worlds on this server.
     *
     * @return the list of claim worlds
     * @since 1.0
     */
    @Unmodifiable
    @NotNull
    public List<ClaimWorld> getClaimWorlds() {
        return plugin.getClaimWorlds().values().stream().toList();
    }

    /**
     * Get a {@link CompletableFuture} containing a {@link Map} of {@link ServerWorld}s to {@link ClaimWorld}s; this
     * contains all claim worlds on all networked servers.
     *
     * @return a {@link CompletableFuture} containing a {@link Map} of {@link ServerWorld}s to {@link ClaimWorld}s.
     * @since 1.0
     */
    public CompletableFuture<@Unmodifiable Map<ServerWorld, ClaimWorld>> getGlobalClaimWorlds() {
        return plugin.supplyAsync(() -> plugin.getDatabase().getAllClaimWorlds());
    }

    /**
     * Get the {@link ClaimWorld} instance representing a world.
     *
     * @param world the world
     * @return the {@link ClaimWorld} instance
     * @since 1.0
     */
    public Optional<ClaimWorld> getClaimWorld(@NotNull World world) {
        return plugin.getClaimWorld(world);
    }

    /**
     * Get the {@link ClaimWorld} instance representing a world.
     *
     * @param worldName the name of the world
     * @return the {@link ClaimWorld} instance
     * @since 1.0
     */
    public Optional<ClaimWorld> getClaimWorld(@NotNull String worldName) {
        return plugin.getClaimWorld(worldName);
    }

    /**
     * Get whether a world is claimable (i.e, has an associated {@link ClaimWorld}).
     *
     * @param world the world
     * @return {@code true} if the world is claimable, else {@code false}
     * @since 1.0
     */
    public boolean isWorldClaimable(@NotNull World world) {
        return getClaimWorld(world).isPresent();
    }

    /**
     * Get whether a world is claimable (i.e, has an associated {@link ClaimWorld}).
     *
     * @param worldName the name of the world
     * @return {@code true} if the world is claimable, else {@code false}
     * @since 1.0
     */
    public boolean isWorldClaimable(@NotNull String worldName) {
        return getClaimWorld(worldName).isPresent();
    }

    /**
     * Edit the {@link ClaimWorld} instance for a world.
     *
     * @param world      the world
     * @param editor     the editor
     * @param notPresent the action to perform if the {@link ClaimWorld} instance is not present
     * @since 1.0
     */
    public void editClaimWorld(@NotNull World world, @NotNull Consumer<ClaimWorld> editor,
                               @NotNull Runnable notPresent) {
        plugin.runQueued(() -> getClaimWorld(world).ifPresentOrElse(
                (claimWorld) -> {
                    editor.accept(claimWorld);
                    plugin.getDatabase().updateClaimWorld(claimWorld);
                },
                notPresent
        ));
    }

    /**
     * Edit the {@link ClaimWorld} instance for a world.
     *
     * @param world  the world
     * @param editor the editor
     * @since 1.0
     */
    public void editClaimWorld(@NotNull World world, @NotNull Consumer<ClaimWorld> editor) {
        editClaimWorld(world, editor, () -> {
        });
    }

    /**
     * Get the {@link ClaimWorld} instance for a world at a {@link Position}.
     *
     * @param position the position
     * @return the {@link ClaimWorld} instance
     * @since 1.0
     */
    public Optional<ClaimWorld> getClaimWorldAt(@NotNull Position position) {
        return getClaimWorld(position.getWorld());
    }

    /**
     * Get the {@link ClaimWorld} instance for the world an {@link OnlineUser} is on.
     *
     * @param user the user
     * @return the {@link ClaimWorld} instance
     * @since 1.0
     */
    public Optional<ClaimWorld> getClaimWorldAt(@NotNull OnlineUser user) {
        return getClaimWorld(user.getWorld());
    }

    /**
     * Get a list of all {@link Claim}s made by a {@link User} on this server. This will not include claims made
     * on other servers.
     *
     * @param user the user
     * @return a {@link Map} of {@link ClaimWorld}s to a list of {@link Claim}s made by the user on that server
     * @since 1.0
     */
    @Unmodifiable
    @NotNull
    public Map<ClaimWorld, List<Claim>> getUserClaims(@NotNull User user) {
        return plugin.getClaimWorlds().values().stream()
                .map(claimWorld -> Map.entry(claimWorld, claimWorld.getClaimsByUser(user.getUuid())))
                .filter(entry -> !entry.getValue().isEmpty())
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    /**
     * Get a list of all {@link Claim}s made by a {@link User} on all servers.
     *
     * @param user the user
     * @return a {@link CompletableFuture} containing a list of {@link Claim}s made by the user across all servers
     * @since 1.0
     */
    public CompletableFuture<@Unmodifiable List<ServerWorldClaim>> getGlobalUserClaims(@NotNull User user) {
        return getGlobalClaimWorlds().thenApply(map -> map.entrySet().stream()
                .flatMap(
                        (entry) -> entry.getValue().getClaimsByUser(user.getUuid()).stream()
                                .map(claim -> new ServerWorldClaim(entry.getKey(), claim))
                )
                .toList());
    }

    /**
     * Edit the {@link ClaimWorld} instance for a world at a {@link Position}.
     *
     * @param position   the position
     * @param editor     the editor
     * @param notPresent the action to perform if the {@link ClaimWorld} instance is not present
     * @since 1.0
     */
    public void editClaimWorldAt(@NotNull Position position, @NotNull Consumer<ClaimWorld> editor,
                                 @NotNull Runnable notPresent) {
        editClaimWorld(position.getWorld(), editor, notPresent);
    }

    /**
     * Edit the {@link ClaimWorld} instance for a world at a {@link Position}.
     *
     * @param position the position
     * @param editor   the editor
     * @since 1.0
     */
    public void editClaimWorldAt(@NotNull Position position, @NotNull Consumer<ClaimWorld> editor) {
        editClaimWorld(position.getWorld(), editor);
    }

    /**
     * Edit the {@link ClaimWorld} instance for a world an {@link OnlineUser} is on.
     *
     * @param user       the user
     * @param editor     the editor
     * @param notPresent the action to perform if the {@link ClaimWorld} instance is not present
     * @since 1.0
     */
    public void editClaimWorldAt(@NotNull OnlineUser user, @NotNull Consumer<ClaimWorld> editor,
                                 @NotNull Runnable notPresent) {
        editClaimWorldAt(user.getPosition(), editor, notPresent);
    }

    /**
     * Edit the {@link ClaimWorld} instance for the world an {@link OnlineUser} is on.
     *
     * @param user   the user
     * @param editor the editor
     * @since 1.0
     */
    public void editClaimWorldAt(@NotNull OnlineUser user, @NotNull Consumer<ClaimWorld> editor) {
        editClaimWorldAt(user.getPosition(), editor);
    }

    /**
     * Get the parent {@link Claim} at a {@link Position}. This will not return the child of the parent at the position
     * if one exists.
     *
     * @param position the position
     * @return the {@link Claim} instance
     * @since 1.0
     */
    public Optional<Claim> getClaimAt(@NotNull Position position) {
        return plugin.getClaimWorld(position.getWorld()).flatMap(claimWorld -> claimWorld.getParentClaimAt(position));
    }

    /**
     * Get a list of parent claims overlapping a {@link Region} in a {@link ClaimWorld}.
     *
     * @param claimWorld the claim world
     * @param region     the region
     * @return a list of overlapping {@link Claim}s
     * @since 1.0
     */
    @NotNull
    @Unmodifiable
    public List<Claim> getClaimsOverlapping(@NotNull ClaimWorld claimWorld, @NotNull Region region) {
        return claimWorld.getParentClaimsOverlapping(region);
    }

    /**
     * Get a list of parent claims overlapping a {@link Region} in a {@link World}.
     *
     * @param world  the world
     * @param region the region
     * @return a list of overlapping {@link Claim}s
     * @since 1.0
     */
    @NotNull
    @Unmodifiable
    public List<Claim> getClaimsOverlapping(@NotNull World world, @NotNull Region region) {
        return getClaimWorld(world).map(claimWorld -> getClaimsOverlapping(claimWorld, region)).orElse(List.of());
    }

    /**
     * Get if a {@link Region} is claimed in a {@link World}.
     *
     * @param world  the world
     * @param region the region
     * @return the {@link Claim} instance
     * @since 1.0
     */
    public boolean isRegionClaimed(@NotNull World world, @NotNull Region region) {
        return getClaimWorld(world).map(claimWorld -> isRegionClaimed(claimWorld, region)).orElse(false);
    }

    /**
     * Returns if a {@link Region} is claimed in a {@link ClaimWorld}.
     *
     * @param claimWorld the claim world
     * @param region     the region
     * @return {@code true} if the {@link Region} intersects with any other claims, else {@code false}
     * @since 1.0
     */
    public boolean isRegionClaimed(@NotNull ClaimWorld claimWorld, @NotNull Region region) {
        return claimWorld.isRegionClaimed(region);
    }

    /**
     * Get the exact {@link Claim} at a {@link Position}; if the position is in a child claim, this will return that
     *
     * @param position the position
     * @return the {@link Claim} instance
     * @since 1.0
     */
    public Optional<Claim> getExactClaimAt(@NotNull Position position) {
        return plugin.getClaimAt(position);
    }

    /**
     * Get a child claim at a {@link BlockPosition} within a parent {@link Claim}.
     *
     * @param parent   the parent claim
     * @param position the position
     * @return the {@link Claim} instance
     * @since 1.0
     */
    public Optional<Claim> getChildClaimAt(@NotNull Claim parent, @NotNull BlockPosition position) {
        return parent.getChildClaimAt(position);
    }

    /**
     * Get a list of child claims within a parent {@link Claim} at a {@link Region}.
     *
     * @param parent the parent claim
     * @param region the region
     * @return the {@link Claim} instance
     * @since 1.0
     */
    @Unmodifiable
    @NotNull
    public List<Claim> getChildClaimsWithin(@NotNull Claim parent, @NotNull Region region) {
        return parent.getChildClaimsWithin(region);
    }

    /**
     * Returns if a {@link Region} is claimed in a {@link Claim}.
     *
     * @param parent the parent claim
     * @param region the region
     * @return {@code true} if the {@link Region} intersects with any other claims, else {@code false}
     * @throws IllegalArgumentException if the provided region is not fully enclosed by the parent claim
     * @since 1.0
     */
    public boolean isChildClaimed(@NotNull Claim parent, @NotNull Region region) {
        if (!parent.getRegion().fullyEncloses(region)) {
            throw new IllegalArgumentException("Region must be fully enclosed by the parent claim");
        }
        return !parent.getChildClaimsWithin(region).isEmpty();
    }

    /**
     * Get the parent {@link Claim} at the position an {@link OnlineUser} is standing. This will not return the child of
     * the parent at the position if one exists.
     *
     * @param user the user
     * @return the {@link Claim} instance
     * @since 1.0
     */
    public Optional<Claim> getClaimAt(@NotNull OnlineUser user) {
        return getClaimAt(user.getPosition());
    }

    /**
     * Get the exact {@link Claim} at the position an {@link OnlineUser} is standing; if the user is standing in a
     * child claim, this will return that claim.
     *
     * @param user the user
     * @return the {@link Claim} instance
     * @since 1.0
     */
    public Optional<Claim> getExactClaimAt(@NotNull OnlineUser user) {
        return getExactClaimAt(user.getPosition());
    }

    /**
     * Returns the owner {@link User} of a {@link Claim}.
     * <p>
     * This will return an empty {@link Optional} if the {@link Claim} is an admin claim, or if the user has not
     * been cached in the {@link ClaimWorld}.
     *
     * @param claim      the claim
     * @param claimWorld the claim world that the claim is in
     * @return the owner {@link User} of the {@link Claim}
     * @since 1.0
     */
    public Optional<User> getClaimOwner(@NotNull Claim claim, @NotNull ClaimWorld claimWorld) {
        return claim.getOwner().flatMap(claimWorld::getUser);
    }

    /**
     * Returns the owner {@link User} of a {@link Claim} at a {@link Position}.
     * <p>
     * Since admin claims do not have owners, this will return an empty {@link Optional} both if no claim exists
     * at the position, or if the claim at that position is an admin claim.
     *
     * @param position the position
     * @return the owner {@link User} of the {@link Claim}
     */
    public Optional<User> getClaimOwnerAt(@NotNull Position position) {
        return getClaimWorldAt(position).flatMap(
                world -> world.getParentClaimAt(position).flatMap(claim -> getClaimOwner(claim, world))
        );
    }

    /**
     * Returns the name string of the owner of a {@link Claim}.
     *
     * @param claim      the claim
     * @param claimWorld the claim world that the claim is in
     * @return the name string of the owner of the {@link Claim}. If the claim is an admin claim, this will return the
     * administrator username from the plugin locales; otherwise if the username could not be resolved "N/A."
     * @since 1.0
     */
    @NotNull
    public String getClaimOwnerName(@NotNull Claim claim, @NotNull ClaimWorld claimWorld) {
        return claim.getOwnerName(claimWorld, plugin);
    }

    /**
     * Returns the name string of the owner of a {@link Claim} at a {@link Position}, or empty if no claim exists.
     *
     * @param position the position
     * @return the username of the owner of the {@link Claim}. If the claim is an admin claim, this will return the
     * administrator username from the plugin locales; otherwise if the username could not be resolved "N/A."
     * @since 1.0
     */
    public Optional<String> getClaimOwnerNameAt(@NotNull Position position) {
        return getClaimWorldAt(position).flatMap(
                world -> world.getParentClaimAt(position).map(claim -> claim.getOwnerName(world, plugin))
        );
    }

    /**
     * Get whether there is a {@link Claim} at a {@link Position}.
     *
     * @param position the position
     * @return {@code true} if there is a claim at the position, else {@code false}
     * @since 1.0
     */
    public boolean isClaimAt(@NotNull Position position) {
        return getClaimAt(position).isPresent();
    }

    /**
     * Get whether there is a {@link Claim} at where an {@link OnlineUser} is standing.
     *
     * @param user the user
     * @return {@code true} if there is a claim at the position, else {@code false}
     * @since 1.0
     */
    public boolean isClaimAt(@NotNull OnlineUser user) {
        return getClaimAt(user).isPresent();
    }

    /**
     * Returns the configured minimum length/width of user-created claims
     *
     * @return the minimum size of user-created claims
     * @since 1.0
     */
    public long getMinimumClaimSize() {
        return plugin.getSettings().getClaims().getMinimumClaimSize();
    }

    /**
     * Create a claim on a {@link ClaimWorld} over a {@link Region} with a {@link User} as the owner.
     * <p>
     * This will create the claim in the world, and adjust the user's claim block balance accordingly.
     *
     * @param world  the claim world
     * @param region the region
     * @param owner  the owner
     * @return a completable future containing the created claim. This future can complete exceptionally if the claim
     * could not be created: if the user does not have enough claim blocks; if the region is too small as per the
     * plugin settings; or if the region would overlap existing claims.
     * <p>
     * Before calling this method, you should validate that the claim can be created using the
     * {@link #isRegionClaimed(ClaimWorld, Region)} and {@link #hasClaimBlocks(OnlineUser, long)} methods.
     * @since 1.0
     */
    @NotNull
    public CompletableFuture<Claim> createClaim(@NotNull ClaimWorld world, @NotNull Region region,
                                                @NotNull User owner) {
        return plugin.supplyAsync(() -> plugin.createClaimAt(world, region, owner));
    }

    /**
     * Create an admin claim on a {@link ClaimWorld} over a {@link Region}.
     *
     * @param world  the claim world
     * @param region the region
     * @return a completable future containing the created claim. This future can complete exceptionally if the claim
     * would overlap existing claims.
     * @since 1.0
     */
    @NotNull
    public CompletableFuture<Claim> createAdminClaim(@NotNull ClaimWorld world, @NotNull Region region) {
        return plugin.supplyAsync(() -> plugin.createAdminClaimAt(world, region));
    }

    /**
     * Resize a {@link Claim} to a {@link Region}.
     *
     * @param world     the claim world
     * @param claim     the claim
     * @param newRegion the new region
     * @return a completable future containing the resized claim. This future can complete exceptionally if the claim
     * could not be resized: if the user does not have enough claim blocks; if the region is too small as per the
     * plugin settings; if the claim is a parent and would not fully enclose its children;
     * or if the new region would overlap existing claims.
     * @since 1.0
     */
    public CompletableFuture<Claim> resizeClaim(@NotNull ClaimWorld world, @NotNull Claim claim,
                                                @NotNull Region newRegion) {
        return plugin.supplyAsync(() -> {
            plugin.resizeClaim(world, claim, newRegion);
            return claim;
        });
    }

    /**
     * Resize a child {@link Claim} to a {@link Region}.
     *
     * @param world     the claim world
     * @param claim     the parent claim
     * @param newRegion the new region
     * @return a completable future containing the resized claim. This future can complete exceptionally if the claim
     * could not be resized: if the child would not be fully enclosed by the parent
     * @since 1.0
     */
    public CompletableFuture<Claim> resizeChildClaim(@NotNull ClaimWorld world, @NotNull Claim claim, @NotNull Region newRegion) {
        return plugin.supplyAsync(() -> {
            plugin.resizeChildClaim(world, claim, newRegion);
            return claim;
        });
    }

    /**
     * Delete a {@link Claim}.
     *
     * @param world the claim world
     * @param claim the claim
     * @since 1.0
     */
    public void deleteClaim(@NotNull ClaimWorld world, @NotNull Claim claim) {
        plugin.runQueued(() -> plugin.deleteClaim(world, claim));
    }

    /**
     * Delete a child {@link Claim}.
     *
     * @param world the claim world
     * @param claim the child claim
     * @throws IllegalArgumentException if the claim is not a child claim
     */
    public void deleteChildClaim(@NotNull ClaimWorld world, @NotNull Claim claim) {
        final Claim parent = claim.getParent().orElseThrow(
                () -> new IllegalArgumentException("Claim is not a child claim or parent could not be found")
        );
        plugin.runQueued(() -> plugin.deleteChildClaim(world, parent, claim));
    }

    /**
     * Get whether an {@link Operation} should be allowed to occur.
     * <p>
     * Whether an operation can occur is determined by who performed the operation, the type of operation, and the
     * position of the operation; based on a user's effective trust level in a claim (taking into account groups
     * and trusted tags), the claim's default allowed operation types, the claim world wilderness default operation
     * types, and unclaimed wilderness default operation types.
     *
     * @param operation the operation
     * @return {@code true} if the operation is allowed, else {@code false}
     * @since 1.0
     */
    public boolean isOperationAllowed(@NotNull Operation operation) {
        return !plugin.cancelOperation(operation);
    }

    /**
     * Get whether an {@link Operation}, consisting of an {@link OnlineUser}, {@link OperationType} and
     * {@link Position}, should be allowed to occur.
     * <p>
     * Whether an operation can occur is determined by who performed the operation, the type of operation, and the
     * position of the operation; based on a user's effective trust level in a claim (taking into account groups
     * and trusted tags), the claim's default allowed operation types, the claim world wilderness default operation
     * types, and unclaimed wilderness default operation types.
     *
     * @param user     the user
     * @param type     the operation type
     * @param position the position of the operation
     * @return {@code true} if the operation is allowed, else {@code false}
     */
    public boolean isOperationAllowed(@NotNull OnlineUser user, @NotNull OperationType type,
                                      @NotNull Position position) {
        return isOperationAllowed(Operation.of(user, type, position));
    }

    /**
     * Get whether an {@link Operation}, consisting of a {@link Position} and {@link OperationType}, should be allowed
     * to occur.
     * <p>
     * Whether an operation can occur is determined by who performed the operation, the type of operation, and the
     * position of the operation; based on a user's effective trust level in a claim (taking into account groups
     * and trusted tags), the claim's default allowed operation types, the claim world wilderness default operation
     * types, and unclaimed wilderness default operation types.
     *
     * @param position the position of the operation
     * @param type     the operation type
     * @return {@code true} if the operation is allowed, else {@code false}
     * @since 1.0
     */
    public boolean isOperationAllowed(@NotNull Position position, @NotNull OperationType type) {
        return isOperationAllowed(Operation.of(type, position));
    }

    /**
     * Returns whether a {@link User} can exercise a {@link TrustLevel.Privilege} in a {@link Claim}.
     *
     * @param privilege the privilege
     * @param user      the user
     * @param claim     the claim
     * @param world     the claim world that the claim is in
     * @return {@code true} if the user can exercise the privilege in the claim, else {@code false}
     * @since 1.0
     */
    public boolean isPrivilegeAllowed(@NotNull TrustLevel.Privilege privilege, @NotNull User user,
                                      @NotNull Claim claim, @NotNull ClaimWorld world) {
        return claim.isPrivilegeAllowed(privilege, user, plugin);
    }

    /**
     * Returns whether a {@link User} can exercise a {@link TrustLevel.Privilege} in a {@link Claim} at a
     * {@link Position}.
     *
     * @param privilege the privilege
     * @param user      the user
     * @param position  the position
     * @return {@code true} if the user can exercise the privilege in the claim (if one exists), else {@code false}
     * @since 1.0
     */
    public boolean isPrivilegeAllowed(@NotNull TrustLevel.Privilege privilege, @NotNull User user,
                                      @NotNull Position position) {
        return getClaimWorldAt(position).flatMap(claimWorld -> claimWorld.getClaimAt(position)
                        .map(claim -> isPrivilegeAllowed(privilege, user, claim, claimWorld)))
                .orElse(false);
    }

    /**
     * Returns whether an {@link OnlineUser} can exercise a {@link TrustLevel.Privilege} in a {@link Claim} at where
     * they are standing.
     *
     * @param privilege  the privilege
     * @param onlineUser the user
     * @return {@code true} if the user can exercise the privilege in the claim (if one exists), else {@code false}
     * @since 1.0
     */
    public boolean isPrivilegeAllowed(@NotNull TrustLevel.Privilege privilege, @NotNull OnlineUser onlineUser) {
        return getClaimWorldAt(onlineUser).flatMap(claimWorld -> claimWorld.getClaimAt(onlineUser.getPosition())
                        .map(claim -> isPrivilegeAllowed(privilege, onlineUser, claim, claimWorld)))
                .orElse(false);
    }

    /**
     * Set the trust level of a {@link Trustable} in a {@link Claim} at a {@link Position}. If the claim at the position
     * is a child claim, they will be trusted in that and not the parent claim.
     *
     * @param claim      the claim
     * @param claimWorld the claim world that the claim is in
     * @param trustable  the {@link Trustable}; a {@link User}, {@link UserGroup}, or {@link TrustTag}
     * @param level      the trust level
     * @since 1.0
     */
    public void setTrustLevel(@NotNull Claim claim, @NotNull ClaimWorld claimWorld,
                              @NotNull Trustable trustable, @NotNull TrustLevel level) {
        plugin.runAsync(() -> {
            claim.setTrustLevel(trustable, level);
            plugin.getDatabase().updateClaimWorld(claimWorld);
        });
    }

    /**
     * Set the trust level of a {@link Trustable} in a {@link Claim} at a {@link Position}. If the claim at the position
     * is a child claim, they will be trusted in that and not the parent claim.
     *
     * @param position  the position
     * @param trustable the user
     * @param level     the trust level
     * @since 1.0
     */
    public void setTrustLevelAt(@NotNull Position position, @NotNull Trustable trustable, @NotNull TrustLevel level) {
        getClaimWorldAt(position).ifPresent(claimWorld -> claimWorld.getClaimAt(position)
                .ifPresent(claim -> setTrustLevel(claim, claimWorld, trustable, level)));
    }

    /**
     * Get the trust level of a {@link Trustable} in a {@link Claim}.
     *
     * @param claim      the claim
     * @param claimWorld the claim world that the claim is in
     * @param trustable  the {@link Trustable} to check; a {@link User}, {@link UserGroup}, or {@link TrustTag}
     * @return the trust level, if the user is trusted in the claim.
     * @since 1.0
     */
    public Optional<TrustLevel> getTrustLevel(@NotNull Claim claim, @NotNull ClaimWorld claimWorld,
                                              @NotNull Trustable trustable) {
        return claim.getTrustLevel(trustable, plugin);
    }

    /**
     * Get the effective trust level of a {@link Trustable} in a {@link Claim} at a {@link Position}.
     * <p>
     * "Effective" trust level takes into account if the position is in a child claim, and whether that child claim
     * is restricted to not inherit parent claim permissions.
     *
     * @param position  the position of the trustable
     * @param trustable the {@link Trustable} to check; a {@link User}, {@link UserGroup}, or {@link TrustTag}
     * @return the trust level, if the trustable is trusted in the claim at the position.
     * @since 1.0
     */
    public Optional<TrustLevel> getTrustLevelAt(@NotNull Position position, @NotNull Trustable trustable) {
        return getClaimWorldAt(position).flatMap(claimWorld -> claimWorld.getClaimAt(position)
                .flatMap(claim -> getTrustLevel(claim, claimWorld, trustable)));
    }

    /**
     * Get the effective trust level of a {@link OnlineUser} where they are standing.
     * <p>
     * "Effective" trust level takes into account if the position is in a child claim, and whether that child claim
     * is restricted to not inherit parent claim permissions.
     *
     * @param user the user to check
     * @return the trust level, if the user is trusted in the claim they are standing in.
     * @since 1.0
     */
    public Optional<TrustLevel> getTrustLevelAt(@NotNull OnlineUser user) {
        return getClaimWorldAt(user).flatMap(claimWorld -> claimWorld.getClaimAt(user.getPosition())
                .flatMap(claim -> getTrustLevel(claim, claimWorld, user)));
    }

    /**
     * Get the list of {@link TrustLevel}s registered to the plugin.
     *
     * @return the list of {@link TrustLevel}s
     * @since 1.0
     */
    @NotNull
    @Unmodifiable
    public List<TrustLevel> getTrustLevels() {
        return plugin.getTrustLevels();
    }

    /**
     * Get a {@link TrustLevel} by name.
     *
     * @param name the name of the level
     * @return the trust level
     * @since 1.0
     */
    public Optional<TrustLevel> getTrustLevelByName(@NotNull String name) {
        return plugin.getTrustLevel(name);
    }

    /**
     * Get a list of a {@link User}'s created {@link UserGroup}s.
     *
     * @param user the user
     * @return a list of the user's created {@link UserGroup}s
     * @since 1.0
     */
    @Unmodifiable
    @NotNull
    public List<UserGroup> getUserGroups(@NotNull User user) {
        return getUserGroups(user.getUuid());
    }

    /**
     * Get a list of a {@link User}'s created {@link UserGroup}s.
     *
     * @param userUuid the user's UUID
     * @return a list of the user's created {@link UserGroup}s
     * @since 1.0
     */
    @Unmodifiable
    @NotNull
    public List<UserGroup> getUserGroups(@NotNull UUID userUuid) {
        return Lists.newArrayList(plugin.getUserGroups(userUuid));
    }

    /**
     * Get a {@link UserGroup} by name.
     *
     * @param user the user who owns the group
     * @param name the name of the group
     * @return the user group
     * @since 1.0
     */
    public Optional<UserGroup> getUserGroupByName(@NotNull User user, @NotNull String name) {
        return getUserGroups(user).stream().filter(group -> group.name().equalsIgnoreCase(name)).findFirst();
    }

    /**
     * Get the set of {@link TrustTag} registered to the plugin
     *
     * @return the {@link Set} of {@link TrustTag}s
     * @since 1.0
     */
    @Unmodifiable
    @NotNull
    public Set<TrustTag> getTrustTags() {
        return plugin.getTrustTags();
    }

    /**
     * Get a {@link TrustTag} by name.
     *
     * @param name the name of the tag
     * @return the trust tag
     * @since 1.0
     */
    public Optional<TrustTag> getTrustTagByName(@NotNull String name) {
        return plugin.getTrustTag(name);
    }

    /**
     * Register a {@link TrustTag} to the plugin.
     * <p>
     * Trust tags are not persistent and must be registered every time the plugin is loaded. Unregistered trust tags
     * will be greyed out in trust lists and will not grant associated users any privileges
     *
     * @param trustTag the trust tag
     * @throws IllegalArgumentException if a trust tag with the same name has already been registered
     * @since 1.0
     */
    public void registerTrustTag(@NotNull TrustTag trustTag) {
        plugin.registerTrustTag(trustTag);
    }

    /**
     * Get the {@link Highlighter}, used for highlighting claims to users.
     *
     * @return the highlighter
     * @since 1.0
     */
    @NotNull
    public Highlighter getHighlighter() {
        return plugin.getHighlighter();
    }

    /**
     * Set the highlighter, used for highlighting claims to users. This method can be used to customize what a user
     * sees when they inspect or create claim(s).
     *
     * @param highlighter the highlighter
     * @since 1.0
     */
    public void setHighlighter(@NotNull Highlighter highlighter) {
        plugin.setHighlighter(highlighter);
    }

    /**
     * Register a custom economy hook for processing transactions.
     * <p>
     * This will replace any existing active economy hook.
     *
     * @param hook the hook to register
     * @since 1.1
     */
    public <T extends EconomyHook> void registerEconomyHook(@NotNull T hook) {
        plugin.getHook(EconomyHook.class).ifPresent(p -> plugin.getHooks().remove(p));
        plugin.getHooks().add(hook);
    }

    /**
     * Get the name of this server.
     *
     * @return the name of this server
     * @since 1.0
     */
    @NotNull
    public String getServerName() {
        return plugin.getServerName();
    }

    /**
     * Get a {@link Position} instance representing a location.
     *
     * @param x         the x coordinate
     * @param y         the y coordinate
     * @param z         the z coordinate
     * @param worldName the name of the world
     * @return the {@link Position} instance
     * @since 1.0
     */
    @NotNull
    public Position getPosition(double x, double y, double z, @NotNull String worldName) {
        return Position.at(x, y, z, getWorld(worldName));
    }

    /**
     * Get a {@link Position} instance representing a location.
     *
     * @param x     the x coordinate
     * @param y     the y coordinate
     * @param z     the z coordinate
     * @param world the world
     * @return the {@link Position} instance
     * @since 1.0
     */
    @NotNull
    public Position getPosition(double x, double y, double z, @NotNull World world) {
        return Position.at(x, y, z, world);
    }

    /**
     * Get a {@link Position} instance representing a location.
     *
     * @param x     the x coordinate
     * @param y     the y coordinate
     * @param z     the z coordinate
     * @param yaw   the yaw angle
     * @param pitch the pitch angle
     * @param world the world
     * @return the {@link Position} instance
     * @since 1.0
     */
    @NotNull
    public Position getPosition(double x, double y, double z, float yaw, float pitch, @NotNull World world) {
        return Position.at(x, y, z, yaw, pitch, world);
    }

    /**
     * Get a {@link Position} instance representing a location.
     *
     * @param blockPosition the block position
     * @param y             the y coordinate
     * @param worldName     the name of the world
     * @return the {@link Position} instance
     * @since 1.0
     */
    @NotNull
    public Position getPosition(@NotNull BlockPosition blockPosition, double y, @NotNull String worldName) {
        return Position.at(blockPosition, y, getWorld(worldName));
    }

    /**
     * Get a {@link Position} instance representing a location.
     *
     * @param blockPosition the block position
     * @param y             the y coordinate
     * @param world         the world
     * @return the {@link Position} instance
     * @since 1.0
     */
    @NotNull
    public Position getPosition(@NotNull BlockPosition blockPosition, double y, @NotNull World world) {
        return Position.at(blockPosition, y, world);
    }

    /**
     * Get a {@link BlockPosition} instance representing a 2D block location.
     *
     * @param x the x coordinate
     * @param z the z coordinate
     * @return the {@link BlockPosition} instance
     * @since 1.0
     */
    @NotNull
    public BlockPosition getBlockPosition(int x, int z) {
        return Region.Point.at(x, z);
    }

    /**
     * Get an instance of the HuskClaims API.
     *
     * @return instance of the HuskClaims API
     * @throws NotRegisteredException if the API has not yet been registered.
     * @since 1.0
     */
    @NotNull
    public static HuskClaimsAPI getInstance() throws NotRegisteredException {
        if (instance == null) {
            throw new NotRegisteredException();
        }
        return instance;
    }

    /**
     * <b>(Internal use only)</b> - Unregister the API instance.
     *
     * @since 1.0
     */
    @ApiStatus.Internal
    public static void unregister() {
        instance = null;
    }

    /**
     * <b>(Internal use only)</b> - Get the plugin instance
     *
     * @since 1.1
     */
    @ApiStatus.Internal
    public HuskClaims getPlugin() {
        return plugin;
    }

    /**
     * An exception indicating the plugin has been accessed before it has been registered.
     *
     * @since 1.0
     */
    public static final class NotRegisteredException extends IllegalStateException {

        private static final String MESSAGE = """
                Could not access the HuskClaims API as it has not yet been registered. This could be because:
                1) HuskClaims has failed to enable successfully
                2) Your plugin isn't set to load after HuskClaims has
                   (Check if it set as a (soft)depend in plugin.yml or to load: BEFORE in paper-plugin.yml?)
                3) You are attempting to access HuskClaims on plugin construction/before your plugin has enabled.""";

        NotRegisteredException() {
            super(MESSAGE);
        }

    }

}