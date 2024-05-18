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
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import com.google.gson.reflect.TypeToken;
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

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.logging.Level;
import java.util.stream.Collectors;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ClaimWorld {

    // The UUID of the admin claim
    public final static UUID ADMIN_CLAIM = UUID.fromString("00000000-0000-0000-0000-000000000000");
    // The current schema version
    public static final int CURRENT_SCHEMA = 1;

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
    }

    public static ClaimWorld convert(int id, @NotNull Set<Claim> claims,
                                     @NotNull Map<UUID, String> userCache, @NotNull Set<OperationType> wildernessFlags) {
        final ClaimWorld world = new ClaimWorld();
        world.id = id;
        world.userCache = new ConcurrentHashMap<>(userCache);
        world.wildernessFlags = Sets.newConcurrentHashSet(wildernessFlags);
        world.cachedClaims = new Long2ObjectOpenHashMap<>();
        world.userClaims = Maps.newConcurrentMap();
        world.schemaVersion = CURRENT_SCHEMA;
        claims.forEach(world::cacheClaim);
        return world;
    }

    @ApiStatus.Internal
    @NotNull
    public static ClaimWorld upgradeSchema(@NotNull String json, @NotNull Gson gson, @NotNull HuskClaims plugin, int id) {
        final JsonObject jsonObject = gson.fromJson(json, JsonObject.class);
        final int schemaVersion = jsonObject.has("schema_version") ? jsonObject.get("schema_version").getAsInt() : 0;

        if (schemaVersion >= CURRENT_SCHEMA) {
            return gson.fromJson(json, ClaimWorld.class);
        }

        final Set<Claim> claims = new HashSet<>();
        final JsonArray claimsArray = jsonObject.getAsJsonArray("claims");
        claimsArray.forEach(c -> {
            final JsonObject claimObject = c.getAsJsonObject();
            final Claim claim = getUpgradableClaim(claimObject, gson);
            claims.add(claim);
        });

        final ConcurrentMap<UUID, String> userCache = jsonObject.has("user_cache") ? gson.fromJson(jsonObject.getAsJsonObject("user_cache"), new TypeToken<ConcurrentMap<UUID, String>>() {
        }.getType()) : new ConcurrentHashMap<>();
        final Set<OperationType> wildernessFlags = jsonObject.has("wilderness_flags") ? gson.fromJson(jsonObject.getAsJsonArray("wilderness_flags"), new TypeToken<Set<OperationType>>() {
        }.getType()) : Sets.newHashSet();
        final ClaimWorld claimWorld = ClaimWorld.convert(id, claims, userCache, wildernessFlags);
        claimWorld.schemaVersion = CURRENT_SCHEMA;
        plugin.getDatabase().updateClaimWorld(claimWorld); // Update the database with the new format
        plugin.log(Level.INFO, "Converted old claim world to new format");
        return claimWorld;
    }

    private static Claim getUpgradableClaim(@NotNull JsonObject claimObject, @NotNull Gson gson) {
        final JsonObject regionObject = claimObject.getAsJsonObject("region");
        final JsonObject nearCornerObject = regionObject.getAsJsonObject("near_corner");
        final JsonObject farCornerObject = regionObject.getAsJsonObject("far_corner");
        final int x1 = nearCornerObject.get("x").getAsInt();
        final int z1 = nearCornerObject.get("z").getAsInt();
        final int x2 = farCornerObject.get("x").getAsInt();
        final int z2 = farCornerObject.get("z").getAsInt();
        final Region.Point nearCorner = Region.Point.at(x1, z1);
        final Region.Point farCorner = Region.Point.at(x2, z2);
        final Region region = Region.from(nearCorner, farCorner);

        final UUID owner = UUID.fromString(claimObject.get("owner").getAsString());
        final ConcurrentMap<UUID, String> trustedUsers = new ConcurrentHashMap<>(gson.fromJson(claimObject.getAsJsonObject("trusted_users"), new TypeToken<Map<UUID, String>>() {
        }.getType()));
        final ConcurrentMap<String, String> trustedGroups = new ConcurrentHashMap<>(gson.fromJson(claimObject.getAsJsonObject("trusted_groups"), new TypeToken<Map<String, String>>() {
        }.getType()));
        final ConcurrentMap<String, String> trustedTags = new ConcurrentHashMap<>(gson.fromJson(claimObject.getAsJsonObject("trusted_tags"), new TypeToken<Map<String, String>>() {
        }.getType()));

        final JsonArray childrenArray = claimObject.getAsJsonArray("children");
        final Set<Claim> children = Sets.newConcurrentHashSet();
        childrenArray.forEach(c -> children.add(getUpgradableClaim(c.getAsJsonObject(), gson)));
        final Set<OperationType> defaultFlags = gson.fromJson(claimObject.getAsJsonArray("default_flags"), new TypeToken<Set<OperationType>>() {
        }.getType());
        final boolean inheritParent = claimObject.get("inherit_parent").getAsBoolean();

        return new Claim(owner, region, trustedUsers, trustedGroups, trustedTags, Maps.newConcurrentMap(), children, inheritParent, defaultFlags);
    }

    protected void loadClaims(@NotNull Set<Claim> claims) {
        this.cachedClaims = new Long2ObjectOpenHashMap<>();
        this.userCache = Maps.newConcurrentMap();
        this.userClaims = Maps.newConcurrentMap();
        this.wildernessFlags = Sets.newConcurrentHashSet();
        claims.forEach(this::cacheClaim);
    }

    private void cacheOwnedClaim(@NotNull Claim claim) {
        final Set<Claim> ownedClaims = userClaims.computeIfAbsent(claim.getOwner().orElse(ADMIN_CLAIM), k -> Sets.newConcurrentHashSet());
        ownedClaims.add(claim);
    }

    private void cacheClaim(@NotNull Claim claim) {
        claim.getChildren().forEach(c -> c.setParent(claim));
        cacheOwnedClaim(claim);

        claim.getRegion().getChunks().forEach(chunk -> {
            final long asLong = ((long) chunk[0] << 32) | (chunk[1] & 0xffffffffL);
            final Set<Claim> chunkClaims = cachedClaims.computeIfAbsent(asLong, k -> Sets.newConcurrentHashSet());
            chunkClaims.add(claim);
        });
    }

    public void addClaim(@NotNull Claim claim) {
        cacheClaim(claim);
    }

    public void removeClaim(@NotNull Claim claim) {
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

    @NotNull
    public static ClaimWorld create(@NotNull HuskClaims plugin) {
        return new ClaimWorld(plugin);
    }

    /**
     * Returns the name of the ClaimWorld associated with this instance.
     *
     * @param plugin the HuskClaims plugin instance
     * @return the name of the ClaimWorld
     * @throws IllegalStateException if the ClaimWorld is not registered
     */
    @NotNull
    public String getName(@NotNull HuskClaims plugin) {
        return plugin.getClaimWorlds().entrySet().stream()
                .filter(entry -> entry.getValue().equals(this))
                .map(Map.Entry::getKey)
                .findFirst().orElseThrow(() -> new IllegalStateException("ClaimWorld not registered"));
    }

    public long getSurfaceClaimedBy(@NotNull User owner) {
        return getClaimsByUser(owner.getUuid()).stream().mapToInt(c -> c.getRegion().getSurfaceArea()).sum();
    }

    public boolean removeClaimsBy(@Nullable User owner) {
        final UUID uuid = owner != null ? owner.getUuid() : ADMIN_CLAIM;
        return userClaims.remove(uuid).stream().allMatch(claim -> {
            return claim.getRegion().getChunks().stream().allMatch(chunk -> {
                final long asLong = ((long) chunk[0] << 32) | (chunk[1] & 0xffffffffL);
                final Set<Claim> chunkClaims = cachedClaims.get(asLong);
                if (chunkClaims != null) {
                    chunkClaims.remove(claim);
                    return true;
                }

                return false;
            });
        });
    }

    public boolean removeAdminClaims() {
        return removeClaimsBy(null);
    }

    public Optional<User> getUser(@NotNull UUID uuid) {
        return Optional.ofNullable(userCache.get(uuid)).map(name -> User.of(uuid, name));
    }

    @NotNull
    public List<Claim> getClaimsByUser(@Nullable UUID uuid) {
        return List.copyOf(userClaims.getOrDefault(uuid, Collections.emptySet()));
    }

    @NotNull
    public List<Claim> getAdminClaims() {
        return getClaimsByUser(ADMIN_CLAIM);
    }

    public Optional<Claim> getParentClaimAt(@NotNull BlockPosition position) {
        final int x = position.getBlockX() >> 4;
        final int z = position.getBlockZ() >> 4;
        final long asLong = ((long) x << 32) | (z & 0xffffffffL);
        return Optional.ofNullable(cachedClaims.get(asLong)).stream()
                .flatMap(Collection::stream)
                .filter(c -> c.getRegion().contains(position))
                .findFirst();
    }

    public Optional<Claim> getClaimAt(@NotNull BlockPosition position) {
        return getParentClaimAt(position).map(parent -> parent.getChildren().stream()
                .filter(c -> c.getRegion().contains(position)).findFirst()
                .orElse(parent));
    }

    @NotNull
    public Set<Claim> getAllClaims() {
        return userClaims.values()
                .stream()
                .flatMap(Set::stream)
                .collect(Collectors.toSet());
    }

    @NotNull
    public List<Claim> getParentClaimsOverlapping(@NotNull Region region) {
        final List<Claim> overlappingClaims = Lists.newArrayList();
        region.getChunks().forEach(chunk -> {
            final long asLong = ((long) chunk[0] << 32) | (chunk[1] & 0xffffffffL);
            cachedClaims.getOrDefault(asLong, Collections.emptySet()).stream()
                    .filter(claim -> claim.getRegion().overlaps(region))
                    .forEach(overlappingClaims::add);
        });
        return overlappingClaims;
    }

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

    public boolean isOperationAllowed(@NotNull Operation operation, @NotNull HuskClaims plugin) {
        return getClaimAt((Position) operation.getOperationPosition())
                .map(claim -> isOperationAllowedInClaim(operation, claim, plugin))
                .orElse(isOperationAllowedInWilderness(operation, plugin));
    }

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

    public int getClaimCount() {
        return userClaims.values().stream().mapToInt(Set::size).sum();
    }

    public void updateId(int id) {
        this.id = id;
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof ClaimWorld world && world.id == id;
    }

}
