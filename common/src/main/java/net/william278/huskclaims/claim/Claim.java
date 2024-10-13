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
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import net.william278.cloplib.operation.Operation;
import net.william278.cloplib.operation.OperationType;
import net.william278.huskclaims.HuskClaims;
import net.william278.huskclaims.highlighter.Highlightable;
import net.william278.huskclaims.position.BlockPosition;
import net.william278.huskclaims.trust.TrustLevel;
import net.william278.huskclaims.trust.TrustTag;
import net.william278.huskclaims.trust.Trustable;
import net.william278.huskclaims.trust.UserGroup;
import net.william278.huskclaims.user.OnlineUser;
import net.william278.huskclaims.user.User;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.OffsetDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentMap;

/**
 * A {@link Region} in a {@link ClaimWorld} governed by user {@link TrustLevel}s and a set of
 * base {@link OperationType} flags
 *
 * @see Region
 * @since 1.0
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class Claim implements Highlightable {

    /**
     * The claim region
     */
    @Expose
    @Getter
    @Setter
    private Region region;

    /**
     * The owner of the claim
     */
    @Expose
    @Nullable
    private UUID owner;

    /**
     * Map of UUID players to their {@link TrustLevel} in this claim
     */
    @Expose
    @Getter
    @SerializedName("trusted_users")
    private Map<UUID, String> trustedUsers;

    /**
     * Map of group names to their {@link TrustLevel} in this claim
     */
    @Expose
    @Getter
    @SerializedName("trusted_groups")
    private Map<String, String> trustedGroups;

    /**
     * Map of tag names to their {@link TrustLevel} in this claim
     */
    @Expose
    @Getter
    @SerializedName("trusted_tags")
    private Map<String, String> trustedTags;

    /**
     * Map of banned users in this claim (UUID of the banned player, UUID of the banner)
     */
    @Expose
    @Getter
    @SerializedName("banned_users")
    private Map<UUID, UUID> bannedUsers;

    /**
     * List of child claims
     */
    @Expose
    @Getter
    private Set<Claim> children;


    /**
     * The parent claim of this claim
     * It's double linked, so it's easier to get the parent claim from a child claim and vice versa
     */
    @Nullable
    @Setter
    @Expose(deserialize = false, serialize = false)
    private Claim parent;

    /**
     * List of OperationTypes allowed on this claim to everyone
     */
    @Expose
    @Getter
    @SerializedName("default_flags")
    private Set<OperationType> defaultFlags;

    /**
     * If this is a child claim, whether to inherit member trust levels from the parent.
     * <p>
     * If set to false, this child claim will be restricted.
     */
    @Expose
    @Getter
    @Setter
    @SerializedName("inherit_parent")
    private boolean inheritParent;

    /**
     * If the claim is private for other players
     * <p>
     * If set to true, the claim can only be entered by trusted players.
     */
    @Expose
    @Getter
    @Setter
    @SerializedName("private_claim")
    private boolean privateClaim;

    /**
     * The time the claim was created (as a stringified {@link OffsetDateTime})
     */
    @Expose
    @Nullable
    @SerializedName("creation_time")
    private String creationTime;

    protected Claim(@Nullable UUID owner, @NotNull Region region, @NotNull ConcurrentMap<UUID, String> users,
                    @NotNull ConcurrentMap<String, String> groups, @NotNull ConcurrentMap<String, String> tags,
                    @NotNull ConcurrentMap<UUID, UUID> bannedUsers, @NotNull Set<Claim> children, boolean inheritParent,
                    @NotNull Set<OperationType> defaultFlags, boolean privateClaim) {
        this.owner = owner;
        this.region = region;
        this.trustedUsers = users;
        this.trustedGroups = groups;
        this.trustedTags = tags;
        this.bannedUsers = bannedUsers;
        this.children = children;
        this.defaultFlags = defaultFlags;
        this.inheritParent = inheritParent;
        this.creationTime = OffsetDateTime.now().toString();
        this.privateClaim = privateClaim;
        children.forEach(child -> child.setParent(this));
    }

    private Claim(@Nullable UUID owner, @NotNull Region region, @NotNull HuskClaims plugin) {
        this(
                owner, region,
                Maps.newConcurrentMap(), Maps.newConcurrentMap(), Maps.newConcurrentMap(),
                Maps.newConcurrentMap(), Sets.newConcurrentHashSet(), true,
                Sets.newConcurrentHashSet(owner != null
                        ? plugin.getSettings().getClaims().getDefaultFlags()
                        : plugin.getSettings().getClaims().getAdminFlags()), 
                        false
        );
    }

    @NotNull
    public static Claim create(@NotNull User owner, @NotNull Region region, @NotNull HuskClaims plugin) {
        return new Claim(owner.getUuid(), region, plugin);
    }

    @NotNull
    public static Claim create(@Nullable UUID owner, @NotNull Region region, @NotNull HuskClaims plugin) {
        return new Claim(owner, region, plugin);
    }

    @NotNull
    public static Claim createAdminClaim(@NotNull Region region, @NotNull HuskClaims plugin) {
        return new Claim(null, region, plugin);
    }

    /**
     * Get the owner of the claim
     *
     * @return the owner of the claim. A claim is an "admin claim" (owned by the server) if there is no owner.
     * @since 1.0
     */
    public Optional<UUID> getOwner() {
        return Optional.ofNullable(owner);
    }

    /**
     * Set the owner of the claim
     *
     * @param uuid the UUID of the new owner
     * @since 1.0
     */
    public void setOwner(@NotNull UUID uuid) {
        this.owner = uuid;
        getChildren().forEach(child -> child.setOwner(uuid));
    }

    /**
     * Get the map of {@link TrustTag}s to {@link TrustLevel}s in this claim
     *
     * @param plugin the plugin instance
     * @return the map of trusted tags
     * @since 1.0
     */
    @NotNull
    private Map<TrustTag, TrustLevel> getTrustTagMap(@NotNull HuskClaims plugin) {
        final Map<TrustTag, TrustLevel> map = Maps.newHashMap();
        for (Map.Entry<String, String> entry : trustedTags.entrySet()) {
            plugin.getTrustTag(entry.getKey()).ifPresent(tag -> plugin.getTrustLevel(entry.getValue())
                    .ifPresent(level -> map.put(tag, level)));
        }
        return map;
    }

    /**
     * Get the map of {@link UserGroup}s to {@link TrustLevel}s in this claim
     *
     * @param plugin the plugin instance
     * @return the map of trusted groups
     * @since 1.0
     */
    @NotNull
    private Map<UserGroup, TrustLevel> getTrustedGroupMap(@NotNull HuskClaims plugin) {
        if (owner == null) {
            return Map.of();
        }

        final Map<UserGroup, TrustLevel> map = Maps.newHashMap();
        for (Map.Entry<String, String> entry : trustedGroups.entrySet()) {
            plugin.getUserGroup(owner, entry.getKey()).ifPresent(group -> plugin.getTrustLevel(entry.getValue())
                    .ifPresent(level -> map.put(group, level)));
        }
        return map;
    }

    /**
     * Returns whether the given user is allowed a privilege in this claim
     *
     * @param privilege the privilege to check
     * @param user      the user to check
     * @param plugin    the plugin instance
     * @return whether the user is allowed the privilege
     * @since 1.0
     */
    public boolean isPrivilegeAllowed(@NotNull TrustLevel.Privilege privilege, @NotNull User user,
                                      @NotNull HuskClaims plugin) {
        return user.getUuid().equals(owner) || getEffectiveTrustLevel(user, plugin)
                .map(level -> level.getPrivileges().contains(privilege))
                .orElse(false);
    }

    /**
     * Set the {@link TrustLevel} of the given user in this claim
     *
     * @param uuid  the user to set the trust level for
     * @param level the trust level to set
     * @throws IllegalArgumentException if the user is banned
     * @since 1.0
     */
    public void setUserTrustLevel(@NotNull UUID uuid, @NotNull TrustLevel level) {
        if (bannedUsers.containsKey(uuid)) {
            throw new IllegalArgumentException("Cannot set trust level for banned user");
        }
        trustedUsers.put(uuid, level.getId());
    }

    /**
     * Set the {@link TrustLevel} of the given group in this claim
     *
     * @param group the group to set the trust level for
     * @param level the trust level to set
     * @since 1.0
     */
    public void setGroupTrustLevel(@NotNull UserGroup group, @NotNull TrustLevel level) {
        trustedGroups.put(group.name(), level.getId());
    }

    /**
     * Set the {@link TrustLevel} of the given tag in this claim
     *
     * @param tag   the tag to set the trust level for
     * @param level the trust level to set
     * @since 1.0
     */
    public void setTagTrustLevel(@NotNull TrustTag tag, @NotNull TrustLevel level) {
        trustedTags.put(tag.getName(), level.getId());
    }

    /**
     * Set the {@link TrustLevel} of the given {@link Trustable} in this claim
     *
     * @param trustable the trustable to set the trust level for
     * @param level     the trust level to set
     * @throws IllegalArgumentException if the trustable is invalid for this claim
     * @since 1.0
     */
    public void setTrustLevel(@NotNull Trustable trustable, @NotNull TrustLevel level) {
        if (trustable instanceof User user) {
            setUserTrustLevel(user.getUuid(), level);
        } else if (trustable instanceof UserGroup group) {
            if (isAdminClaim()) {
                throw new IllegalArgumentException("Cannot set group trust level in admin claim");
            }
            setGroupTrustLevel(group, level);
        } else if (trustable instanceof TrustTag tag) {
            setTagTrustLevel(tag, level);
        } else {
            throw new IllegalArgumentException("Trustable must be a User, UserGroup, or TrustTag");
        }
    }

    /**
     * Remove the {@link TrustLevel} of the given {@link Trustable} in this claim
     *
     * @param trustable the trustable to remove the trust level for
     * @param world     the world the claim is in
     * @since 1.0
     */
    public void removeTrustLevel(@NotNull Trustable trustable, @NotNull ClaimWorld world) {
        if (trustable instanceof User user) {
            trustedUsers.remove(user.getUuid());
            world.cacheUser(user);
        } else if (trustable instanceof UserGroup group) {
            trustedGroups.remove(group.name());
        } else if (trustable instanceof TrustTag tag) {
            trustedTags.remove(tag.getName());
        } else {
            throw new IllegalArgumentException("Trustable must be a User, UserGroup, or TrustTag");
        }
    }

    /**
     * Get the {@link TrustLevel} of the given group in this claim
     *
     * @param group  the group to get the trust level for
     * @param plugin the plugin instance
     * @return the group's trust level, if they have one defined
     * @since 1.0
     */
    public Optional<TrustLevel> getGroupTrustLevel(@NotNull String group, @NotNull HuskClaims plugin) {
        if (trustedGroups.containsKey(group)) {
            return plugin.getTrustLevel(trustedGroups.get(group));
        }
        return Optional.empty();
    }

    /**
     * Get the {@link TrustLevel} of the given tag in this claim
     *
     * @param tag    the tag to get the trust level for
     * @param plugin the plugin instance
     * @return the tag's trust level, if they have one defined
     */
    public Optional<TrustLevel> getTagTrustLevel(@NotNull String tag, @NotNull HuskClaims plugin) {
        if (trustedTags.containsKey(tag)) {
            return plugin.getTrustLevel(trustedTags.get(tag));
        }
        return Optional.empty();
    }

    /**
     * Get the user's explicit {@link TrustLevel} in this claim. This does not take into account parent claims;
     * see {@link #getEffectiveTrustLevel(Trustable, HuskClaims)}.
     * <p>
     * Level priority checks are handled by order of explicitness:
     * <ol>
     *     <li>Individual {@link User}s</li>
     *     <li>{@link UserGroup}s</li>
     *     <li>{@link TrustTag}s</li>
     * </ol>
     * Banned users cannot have a trust level.
     *
     * @param user   the user to get the trust level for
     * @param plugin the plugin instance
     * @return the user's trust level, if they have one defined
     * @since 1.0
     */
    public Optional<TrustLevel> getUserTrustLevel(@NotNull User user, @NotNull HuskClaims plugin) {
        // If the user is banned, return empty
        if (isUserBanned(user)) {
            return Optional.empty();
        }

        // Handle explicit user permissions
        if (trustedUsers.containsKey(user.getUuid())) {
            return plugin.getTrustLevel(trustedUsers.get(user.getUuid()));
        }

        // Check if the user is in a trusted group
        final Map<UserGroup, TrustLevel> groups = getTrustedGroupMap(plugin);
        final Optional<TrustLevel> groupLevel = groups.entrySet().stream()
                .filter(entry -> entry.getKey().includes(user)).map(Map.Entry::getValue)
                .sorted().findFirst();
        if (groupLevel.isPresent()) {
            return groupLevel;
        }

        // Finally, check trusted tags
        final Map<TrustTag, TrustLevel> tags = getTrustTagMap(plugin);
        return tags.entrySet().stream()
                .filter(entry -> entry.getKey().includes(user)).map(Map.Entry::getValue)
                .sorted().findFirst();
    }

    /**
     * Get the {@link TrustLevel} of the given {@link Trustable} in this claim
     *
     * @param trustable the trustable to get the trust level for
     * @param plugin    the plugin instance
     * @return the trustable's current level, if they have one defined
     * @since 1.0
     */
    public Optional<TrustLevel> getTrustLevel(@NotNull Trustable trustable, @NotNull HuskClaims plugin) {
        if (trustable instanceof User user) {
            return getUserTrustLevel(user, plugin);
        } else if (trustable instanceof UserGroup group) {
            return getGroupTrustLevel(group.name(), plugin);
        } else if (trustable instanceof TrustTag tag) {
            return getTagTrustLevel(tag.getName(), plugin);
        }
        throw new IllegalArgumentException("Trustable must be a User, UserGroup, or TrustedTag");
    }

    /**
     * Get the user's effective {@link TrustLevel}, taking parent claims into account
     *
     * @param trustable the user to get the effective trust level for
     * @param plugin    the plugin instance
     * @return the user's effective trust level, if they have one defined
     * @since 1.0
     */
    @NotNull
    public Optional<TrustLevel> getEffectiveTrustLevel(@NotNull Trustable trustable, @NotNull HuskClaims plugin) {
        return getTrustLevel(trustable, plugin)
                .or(() -> inheritParent
                        ? getParent().flatMap(parent -> parent.getEffectiveTrustLevel(trustable, plugin))
                        : Optional.empty());
    }

    /**
     * Returns if the user is banned from this claim
     *
     * @param user the user to check
     * @return whether the user is banned
     * @since 1.3
     */
    public boolean isUserBanned(@NotNull User user) {
        return bannedUsers.containsKey(user.getUuid());
    }

    /**
     * Ban a user from this claim.
     * <p>
     * This action will also clear any trust levels the user has in this claim.
     *
     * @param user    the user to ban
     * @param arbiter the arbiter of the ban
     * @throws IllegalArgumentException if the user is the claim owner, or the arbiter is the user to ban
     * @since 1.3
     */
    public void banUser(@NotNull User user, @NotNull User arbiter) {
        if (user.getUuid().equals(owner)) {
            throw new IllegalArgumentException("Cannot ban the claim owner");
        }
        if (arbiter.equals(user)) {
            throw new IllegalArgumentException("Cannot ban self from claim");
        }
        trustedUsers.remove(user.getUuid());
        bannedUsers.put(user.getUuid(), arbiter.getUuid());
    }

    /**
     * Unban a user from this claim
     *
     * @param user the user to unban
     * @since 1.3
     */
    public void unBanUser(@NotNull User user) {
        bannedUsers.remove(user.getUuid());
    }

    /**
     * Returns whether the given operation is allowed on this claim
     *
     * @param operation the operation to check
     * @param plugin    the plugin instance
     * @return whether the operation is allowed
     * @since 1.0
     */
    public boolean isOperationAllowed(@NotNull Operation operation, @NotNull HuskClaims plugin) {
        // If the operation is explicitly allowed, return it
        return defaultFlags.contains(operation.getType())

                // Or, if the user is the owner, return true
                || (owner != null && operation.getUser()
                .filter(user -> owner.equals(user.getUuid()))
                .map(u -> plugin.allowedOwnerOperations().contains(operation.getType()))
                .orElse(false))

                // Or, if there's a user involved in this operation, check their rights
                || (operation.getUser()
                .flatMap(user -> getUserTrustLevel((OnlineUser) user, plugin)
                        .map(level -> level.getFlags().contains(operation.getType())))
                .orElse(false))

                // Or, if the user doesn't have a trust level here, try getting it from the parent
                || (inheritParent && getParent()
                .map(parent -> parent.isOperationAllowed(operation, plugin))
                .orElse(false));
    }

    public Optional<Claim> getParent() {
        return Optional.ofNullable(parent);
    }

    @Deprecated(forRemoval = true)
    @SuppressWarnings("unused")
    public Optional<Claim> getParent(@NotNull ClaimWorld claimWorld) {
        return getParent();
    }

    @Deprecated(forRemoval = true)
    @SuppressWarnings("unused")
    public boolean isChildClaim(@NotNull ClaimWorld claimWorld) {
        return isChildClaim();
    }

    public boolean isChildClaim() {
        return getParent().isPresent();
    }

    public boolean isAdminClaim() {
        return owner == null;
    }

    @NotNull
    public Optional<OffsetDateTime> getCreationTime() {
        return Optional.ofNullable(creationTime).map(OffsetDateTime::parse);
    }

    @NotNull
    public Optional<Claim> getChildClaimAt(@NotNull BlockPosition position) {
        return getChildren().stream().filter(claim -> claim.getRegion().contains(position)).findFirst();
    }

    @NotNull
    public List<Claim> getChildClaimsWithin(@NotNull Region region) {
        return getChildren().stream().filter(claim -> claim.getRegion().overlaps(region)).toList();
    }

    public boolean containsChild(@NotNull Claim child) {
        return children.contains(child);
    }

    /**
     * Get the child claims a region overlaps with, except for certain claims
     *
     * @param region    The region to check
     * @param exceptFor claims to exclude from the check
     * @return list of overlapping claims
     * @since 1.0
     */
    @NotNull
    public List<Claim> getChildClaimsWithin(@NotNull Region region, @NotNull Region... exceptFor) {
        final List<Claim> claims = Lists.newArrayList(getChildClaimsWithin(region));
        for (Region except : exceptFor) {
            claims.removeIf(claim -> claim.getRegion().equals(except));
        }
        return claims;
    }

    // Get the claim owner's username
    @NotNull
    public String getOwnerName(@NotNull ClaimWorld world, @NotNull HuskClaims plugin) {
        return getOwner()
                // Get the owner username from the cache
                .map(owner -> world.getUser(owner).map(User::getName)
                        .orElse(plugin.getLocales().getNotApplicable()))
                // Or, if it's an admin claim, get the admin username
                .orElse(plugin.getLocales().getRawLocale("administrator_username")
                        .orElse(plugin.getLocales().getNotApplicable()));
    }

    @NotNull
    @ApiStatus.Internal
    public Claim createAndAddChild(@NotNull Region subRegion, @NotNull HuskClaims plugin)
            throws IllegalArgumentException {
        if (isChildClaim()) {
            throw new IllegalArgumentException("A child claim cannot be within another child claim");
        }
        if (!region.fullyEncloses(subRegion)) {
            throw new IllegalArgumentException("Child claim must be fully enclosed within parent claim");
        }
        final Claim child = new Claim(owner, subRegion, plugin);
        children.add(child);
        child.setParent(this);
        return child;
    }

    @Override
    @NotNull
    public Map<Region.Point, Type> getHighlightPoints(@NotNull ClaimWorld world, boolean showOverlap,
                                                      @NotNull BlockPosition viewer, long range) {
        final Optional<Claim> parent = getParent();
        return region.getHighlightPoints(
                showOverlap,
                parent.isPresent(),
                parent.map(claim -> claim.getOwner().isEmpty())
                        .orElse(getOwner().isEmpty()),
                viewer, range
        );
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof Claim c && c.region.equals(region) && c.children.equals(children);
    }

}
