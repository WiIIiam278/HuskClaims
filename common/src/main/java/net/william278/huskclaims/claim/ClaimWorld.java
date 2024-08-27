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

package net.william278.huskclaims.claim;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import net.william278.cloplib.operation.Operation;
import net.william278.cloplib.operation.OperationType;
import net.william278.huskclaims.HuskClaims;
import net.william278.huskclaims.command.IgnoreClaimsCommand;
import net.william278.huskclaims.position.BlockPosition;
import net.william278.huskclaims.position.Position;
import net.william278.huskclaims.user.OnlineUser;
import net.william278.huskclaims.user.Preferences;
import net.william278.huskclaims.user.User;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ClaimWorld {

    // The UUID of the admin claim
    public final static UUID ADMIN_CLAIM = UUID.fromString("00000000-0000-0000-0000-000000000000");
    // The current schema version
    public static final int CURRENT_SCHEMA = 1;

    // The ID of the ClaimWorld
    private transient int id;

    @Expose
    @SerializedName("user_cache")
    private ConcurrentMap<UUID, String> userCache;
    @Expose
    @SerializedName("wilderness_flags")
    private Set<OperationType> wildernessFlags;
    @Expose
    @SerializedName("cached_claims")
    private Long2ObjectOpenHashMap<Set<Claim>> cachedClaims;
    @Expose(deserialize = false, serialize = false)
    private transient Map<UUID, Set<Claim>> userClaims;
    @Expose
    @Setter
    @SerializedName("schema_version")
    private int schemaVersion;

    private ClaimWorld(@NotNull HuskClaims plugin) {
        this.id = 0;
        this.userCache = Maps.newConcurrentMap();
        this.wildernessFlags = Sets.newHashSet(plugin.getSettings().getClaims().getWildernessRules());
        this.cachedClaims = new Long2ObjectOpenHashMap<>();
        this.userClaims = Maps.newConcurrentMap();
        this.schemaVersion = CURRENT_SCHEMA;
    }

    /**
     * Convert a legacy ClaimWorld instance to a new ClaimWorld instance
     *
     * @param claims          The claims to convert
     * @param userCache       The user cache to convert
     * @param wildernessFlags The wilderness flags to convert
     * @return the new ClaimWorld instance
     * @since 1.3
     */
    @NotNull
    @ApiStatus.Internal
    public static ClaimWorld convert(@NotNull Set<Claim> claims,
                                     @NotNull Map<UUID, String> userCache, @NotNull Set<OperationType> wildernessFlags) {
        final ClaimWorld world = new ClaimWorld();
        world.userCache = new ConcurrentHashMap<>(userCache);
        world.wildernessFlags = Sets.newConcurrentHashSet(wildernessFlags);
        world.cachedClaims = new Long2ObjectOpenHashMap<>();
        world.userClaims = Maps.newConcurrentMap();
        world.schemaVersion = CURRENT_SCHEMA;
        claims.forEach(world::cacheClaim);
        return world;
    }

    /**
     * Create a new ClaimWorld instance
     *
     * @param plugin the HuskClaims plugin instance
     * @return the new ClaimWorld instance
     * @since 1.0
     */
    @NotNull
    @ApiStatus.Internal
    public static ClaimWorld create(@NotNull HuskClaims plugin) {
        return new ClaimWorld(plugin);
    }

    /**
     * Update the ID of the ClaimWorld
     *
     * @param id the new ID
     * @since 1.0
     */
    @ApiStatus.Internal
    public void updateId(int id) {
        this.id = id;
    }

    /**
     * Add a claim to the ClaimWorld
     *
     * @param claim the claim to add
     * @since 1.0
     */
    public void addClaim(@NotNull Claim claim) {
        cacheClaim(claim);
    }

    /**
     * Remove a claim from the ClaimWorld
     *
     * @param claim the claim to remove
     * @since 1.0
     */
    public void removeClaim(@NotNull Claim claim) {
        if (claim.isChildClaim()) {
            throw new IllegalArgumentException("Cannot remove a child claim directly");
        }

        final UUID owner = claim.getOwner().orElse(ADMIN_CLAIM);
        final Set<Claim> ownedClaims = userClaims.get(owner);
        if (ownedClaims != null) {
            ownedClaims.remove(claim);
        }

        claim.getRegion().getChunks().forEach(chunk -> {
            final long asLong = ((long) chunk[0] << 32) | (chunk[1] & 0xffffffffL);
            final Set<Claim> chunkClaims = cachedClaims.get(asLong);
            if (chunkClaims != null) {
                chunkClaims.remove(claim);
            }
        });
    }

    /**
     * Resize a claim in the ClaimWorld, caching the new chunks.
     * <p>
     * It is important that this method is used to resize claims, instead of simply just setting the new claim region,
     * as this method ensures that the claim's chunks are correctly cached.
     *
     * @param claim     the claim to resize
     * @param newRegion the new region of the claim
     * @since 1.3.1
     */
    public void resizeClaim(@NotNull Claim claim, @NotNull Region newRegion) {
        if (claim.isChildClaim()) {
            throw new IllegalArgumentException("Cannot resize a child claim in a world context");
        }

        // Clear old region chunks
        final Region oldRegion = claim.getRegion();
        oldRegion.getChunks().forEach(chunk -> {
            final long asLong = ((long) chunk[0] << 32) | (chunk[1] & 0xffffffffL);
            final Set<Claim> chunkClaims = cachedClaims.get(asLong);
            if (chunkClaims != null) {
                chunkClaims.remove(claim);
            }
        });

        // Set new region, cache new chunks
        claim.setRegion(newRegion);
        newRegion.getChunks().forEach(chunk -> {
            final long asLong = ((long) chunk[0] << 32) | (chunk[1] & 0xffffffffL);
            final Set<Claim> chunkClaims = cachedClaims.computeIfAbsent(asLong, k -> Sets.newConcurrentHashSet());
            chunkClaims.add(claim);
        });
    }

    /**
     * Returns the name of the ClaimWorld associated with this instance.
     *
     * @param plugin the HuskClaims plugin instance
     * @return the name of the ClaimWorld
     * @throws IllegalStateException if the ClaimWorld is not registered
     * @since 1.0
     */
    @NotNull
    public String getName(@NotNull HuskClaims plugin) {
        return plugin.getClaimWorlds().entrySet().stream()
                .filter(entry -> entry.getValue().equals(this))
                .map(Map.Entry::getKey)
                .findFirst().orElseThrow(() -> new IllegalStateException("ClaimWorld not registered"));
    }

    /**
     * Get the number of claim blocks owned by a specific user in this world
     * (i.e., the total surface area of all claims owned by the user in this world)
     *
     * @param owner The user to check
     * @return the number of claim blocks owned by the user
     * @since 1.0
     */
    public long getSurfaceClaimedBy(@NotNull User owner) {
        return getClaimsByUser(owner.getUuid()).stream().mapToInt(c -> c.getRegion().getSurfaceArea()).sum();
    }

    /**
     * Remove all claims owned by a specific user
     *
     * @param owner The user to remove claims for
     * @return if any claims were removed
     * @since 1.0
     */
    public boolean removeClaimsBy(@Nullable User owner) {
        final UUID uuid = owner != null ? owner.getUuid() : ADMIN_CLAIM;
        if (!userClaims.containsKey(uuid)) {
            return false;
        }
        return userClaims.remove(uuid).stream().allMatch(claim -> claim.getRegion().getChunks().stream().allMatch(
                (chunk) -> {
                    final long asLong = ((long) chunk[0] << 32) | (chunk[1] & 0xffffffffL);
                    final Set<Claim> chunkClaims = cachedClaims.get(asLong);
                    if (chunkClaims != null) {
                        chunkClaims.remove(claim);
                        return true;
                    }
                    return false;
                }
        ));
    }

    /**
     * Delete all admin claims in this world
     *
     * @return if any admin claims were removed
     * @since 1.0
     */
    public boolean removeAdminClaims() {
        return removeClaimsBy(null);
    }

    /**
     * Get a cached user by UUID
     *
     * @param uuid The UUID of the user
     * @return an optional containing the user if they are cached in the world
     * @since 1.0
     */
    public Optional<User> getUser(@NotNull UUID uuid) {
        return Optional.ofNullable(userCache.get(uuid)).map(name -> User.of(uuid, name));
    }

    /**
     * Get all claims owned by a specific user in this world
     *
     * @param uuid The UUID of the user
     * @return a list of all claims owned by the user
     * @since 1.0
     */
    @NotNull
    @Unmodifiable
    public List<Claim> getClaimsByUser(@Nullable UUID uuid) {
        return List.copyOf(userClaims.getOrDefault(uuid, Collections.emptySet()));
    }

    /**
     * Get all admin claims in this world
     *
     * @return a list of all admin claims in this world
     * @since 1.0
     */
    @NotNull
    @Unmodifiable
    public List<Claim> getAdminClaims() {
        return getClaimsByUser(ADMIN_CLAIM);
    }

    /**
     * Get the parent claim at a specific position
     *
     * @param position The position to check
     * @return the parent claim at the position if one exists
     * @since 1.0
     */
    public Optional<Claim> getParentClaimAt(@NotNull BlockPosition position) {
        final long asLong = position.getLongChunkCoords();
        return Optional.ofNullable(cachedClaims.get(asLong)).stream()
                .flatMap(Collection::stream)
                .filter(c -> c.getRegion().contains(position))
                .findFirst();
    }

    /**
     * Get a claim at a specific position (including child claims)
     *
     * @param position The position to check
     * @return the claim at the position, if one exists
     * @since 1.0
     */
    public Optional<Claim> getClaimAt(@NotNull BlockPosition position) {
        return getParentClaimAt(position).map(parent -> parent.getChildren().stream()
                .filter(c -> c.getRegion().contains(position)).findFirst()
                .orElse(parent));
    }

    /**
     * Get all claims in this world
     *
     * @return a set of all claims in this world
     * @since 1.0
     */
    @NotNull
    @Unmodifiable
    public Set<Claim> getClaims() {
        return userClaims.values()
                .stream()
                .flatMap(Set::stream)
                .collect(Collectors.toSet());
    }

    /**
     * Get a list of claims overlapping a region
     *
     * @param region The region to check
     * @return list of overlapping claims
     * @since 1.0
     */
    @NotNull
    public List<Claim> getParentClaimsOverlapping(@NotNull Region region) {
        final List<Claim> overlappingClaims = Lists.newArrayList();
        region.getChunks().forEach(chunk -> {
            final long asLong = ((long) chunk[0] << 32) | (chunk[1] & 0xffffffffL);
            cachedClaims.getOrDefault(asLong, Collections.emptySet()).stream()
                    .filter(claim -> claim.getRegion().overlaps(region))
                    .forEach(overlappingClaims::add);
        });
        return overlappingClaims.stream().distinct().toList();
    }

    /**
     * Cache a user in the user cache
     *
     * @param user the user to cache
     * @since 1.0
     */
    @ApiStatus.Internal
    public void cacheUser(@NotNull User user) {
        userCache.put(user.getUuid(), user.getName());
    }

    /**
     * Get the claims a region overlaps with, except for certain claims
     *
     * @param region    The region to check
     * @param exceptFor claims to exclude from the check
     * @return list of overlapping claims
     * @since 1.0
     */
    @NotNull
    public List<Claim> getParentClaimsOverlapping(@NotNull Region region, @NotNull Region... exceptFor) {
        final List<Claim> claims = Lists.newArrayList(getParentClaimsOverlapping(region));
        for (Region except : exceptFor) {
            claims.removeIf(claim -> claim.getRegion().equals(except));
        }
        return claims;
    }

    /**
     * Get if a region is claimed
     *
     * @param region The region to check
     * @return if the region is claimed
     * @since 1.0
     */
    public boolean isRegionClaimed(@NotNull Region region) {
        return !getParentClaimsOverlapping(region).isEmpty();
    }

    /**
     * Get if a region is claimed, except for certain claims
     *
     * @param region    The region to check
     * @param exceptFor claims to exclude from the check
     * @return if the region is claimed
     * @since 1.0
     */
    public boolean isRegionClaimed(@NotNull Region region, @NotNull Region... exceptFor) {
        return !getParentClaimsOverlapping(region, exceptFor).isEmpty();
    }

    /**
     * Return if a claim is contained in this world
     *
     * @param claim The claim to check
     * @return {@code true} if this world contains the claim; {@code false} otherwise
     * @since 1.3.4
     */
    public boolean contains(@NotNull Claim claim) {
        return getClaims().contains(claim);
    }

    /**
     * Get if a region is claimed by a specific user
     *
     * @param operation The operation to check
     * @param plugin    The HuskClaims plugin instance
     * @return if the operation is allowed
     * @since 1.0
     */
    public boolean isOperationAllowed(@NotNull Operation operation, @NotNull HuskClaims plugin) {
        return getClaimAt((Position) operation.getOperationPosition())
                .map(claim -> isOperationAllowedInClaim(operation, claim, plugin))
                .orElse(isOperationAllowedInWilderness(operation, plugin));
    }

    /**
     * Get the total number of claims in this world
     *
     * @return the total number of claims in this world
     * @since 1.0
     */
    public int getClaimCount() {
        return userClaims.values().stream().mapToInt(Set::size).sum();
    }

    // Check if a user is banned from a claim
    @ApiStatus.Internal
    public boolean isBannedFromClaim(@NotNull OnlineUser user, @NotNull Claim claim, @NotNull HuskClaims plugin) {
        if (!plugin.getSettings().getClaims().getBans().isEnabled() || isIgnoringClaims(user, plugin)) {
            return false;
        }

        if (claim.isUserBanned(user)) {
            plugin.getLocales().getLocale("user_banned_you", claim.getOwnerName(this, plugin))
                    .ifPresent(user::sendMessage);
            return true;
        }
        return false;
    }

    // Check if a claim is private and cannot be navigated by a user
    @ApiStatus.Internal
    public boolean cannotNavigatePrivateClaim(@NotNull OnlineUser user, @NotNull Claim claim,
                                              @NotNull HuskClaims plugin) {
        if (!plugin.getSettings().getClaims().getBans().isPrivateClaims() || isIgnoringClaims(user, plugin)) {
            return false;
        }
        if (!claim.isPrivateClaim()) {
            return false;
        }

        final Optional<UUID> owner = claim.getOwner();
        if (owner.isPresent() && owner.get().equals(user.getUuid())) {
            return false;
        }

        if (claim.getTrustLevel(user, plugin).isEmpty()) {
            plugin.getLocales().getLocale("claim_enter_is_private", claim.getOwnerName(this, plugin))
                    .ifPresent(user::sendMessage);
            return true;
        }
        return false;
    }

    // Load claims (caching all of them)
    protected void loadClaims(@NotNull Set<Claim> claims) {
        this.cachedClaims = new Long2ObjectOpenHashMap<>();
        this.userCache = Maps.newConcurrentMap();
        this.userClaims = Maps.newConcurrentMap();
        this.wildernessFlags = Sets.newConcurrentHashSet();
        claims.forEach(this::cacheClaim);
    }

    // Cache a user claim
    private void cacheOwnedClaim(@NotNull Claim claim) {
        final Set<Claim> ownedClaims = userClaims.computeIfAbsent(claim.getOwner().orElse(ADMIN_CLAIM), k -> Sets.newConcurrentHashSet());
        ownedClaims.add(claim);
    }

    // Cache a claim in the world
    private void cacheClaim(@NotNull Claim claim) {
        if (claim.isChildClaim()) {
            throw new IllegalArgumentException("Cannot cache a child claim in a world context");
        }

        // Set parents
        claim.getChildren().forEach(c -> c.setParent(claim));
        cacheOwnedClaim(claim);

        claim.getRegion().getChunks().forEach(chunk -> {
            final long asLong = ((long) chunk[0] << 32) | (chunk[1] & 0xffffffffL);
            final Set<Claim> chunkClaims = cachedClaims.computeIfAbsent(asLong, k -> Sets.newConcurrentHashSet());
            chunkClaims.add(claim);
        });
    }

    // Check if an operation is allowed in a specific claim
    private boolean isOperationAllowedInClaim(@NotNull Operation operation, @NotNull Claim claim,
                                              @NotNull HuskClaims plugin) {
        if (isOperationIgnored(operation, plugin) || claim.isOperationAllowed(operation, plugin)) {
            return true;
        }

        // Send user error message if verbose
        if (operation.isVerbose()) {
            operation.getUser().ifPresent(user -> plugin.getLocales()
                    .getLocale("no_operation_permission", claim.getOwnerName(this, plugin))
                    .ifPresent(((OnlineUser) user)::sendMessage));
        }
        return false;
    }

    // Check if an operation is allowed in the wilderness
    private boolean isOperationAllowedInWilderness(@NotNull Operation operation, @NotNull HuskClaims plugin) {
        if (wildernessFlags.contains(operation.getType())) {
            return true;
        }

        // Send user error message if verbose
        if (operation.isVerbose()) {
            operation.getUser().ifPresent(user -> plugin.getLocales()
                    .getLocale("no_wilderness_permission")
                    .ifPresent(((OnlineUser) user)::sendMessage));
        }
        return false;
    }

    // Checks if the outcome of an operation is being ignored by its involved user
    private boolean isOperationIgnored(@NotNull Operation operation, @NotNull HuskClaims plugin) {
        return operation.getUser().map(user -> isIgnoringClaims((OnlineUser) user, plugin)).orElse(false);
    }

    // Checks if a user is ignoring claims, ensuring they also have permission to ignore claims
    private boolean isIgnoringClaims(@NotNull OnlineUser u, @NotNull HuskClaims plugin) {
        boolean toggled = plugin.getUserPreferences(u.getUuid()).map(Preferences::isIgnoringClaims).orElse(false);
        if (toggled && !plugin.canUseCommand(IgnoreClaimsCommand.class, u)) {
            plugin.runAsync(() -> {
                plugin.editUserPreferences(u, (preferences) -> preferences.setIgnoringClaims(false));
                plugin.getLocales().getLocale("respecting_claims")
                        .ifPresent(u::sendMessage);
            });
            return false;
        }
        return toggled;
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof ClaimWorld world && world.id == id;
    }

}
