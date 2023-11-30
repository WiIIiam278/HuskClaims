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

import com.google.common.collect.Maps;
import com.google.common.collect.Queues;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import net.william278.cloplib.operation.Operation;
import net.william278.cloplib.operation.OperationType;
import net.william278.huskclaims.HuskClaims;
import net.william278.huskclaims.user.User;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentMap;

/**
 * A {@link Region} in a {@link ClaimWorld} governed by user {@link TrustLevel}s and a set of
 * base {@link OperationType} flags
 *
 * @see Region
 * @since 1.0
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class Claim {

    /**
     * The claim region
     */
    @Expose
    private Region region;

    /**
     * The owner of the claim
     */
    @Expose
    private UUID owner;

    /**
     * Map of TrustLevels to a list of UUID players with that TrustLevel
     */
    @Expose
    @SerializedName("trusted_users")
    private ConcurrentMap<UUID, String> trustedUsers;

    /**
     * Map of TrustLevels to a list of UUID groups with that TrustLevel
     */
    @Expose
    @SerializedName("trusted_groups")
    private ConcurrentMap<String, String> trustedGroups;

    /**
     * List of child claims
     */
    @Expose
    private ConcurrentLinkedQueue<Claim> children;

    /**
     * List of OperationTypes allowed on this claim to everyone
     */
    @Expose
    @SerializedName("universal_flags")
    private List<OperationType> universalFlags;

    /**
     * If this is a child claim, whether to inherit member trust levels from the parent.
     * <p>
     * If set to false, this child claim will be restricted.
     */
    @Expose
    @SerializedName("inherit_parent")
    private boolean inheritParent;

    private Claim(@NotNull Region region, @NotNull ConcurrentMap<UUID, String> trustedUsers,
                  @NotNull ConcurrentLinkedQueue<Claim> children, @NotNull List<OperationType> universalFlags,
                  boolean inheritParent) {
        this.region = region;
        this.trustedUsers = trustedUsers;
        this.children = children;
        this.universalFlags = universalFlags;
        this.inheritParent = inheritParent;
    }

    private Claim(@NotNull Region region, @NotNull HuskClaims plugin) {
        this(
                region,
                Maps.newConcurrentMap(),
                Queues.newConcurrentLinkedQueue(),
                new ArrayList<>(),
                true
        );
    }

    @NotNull
    public Optional<TrustLevel> getEffectiveTrustLevel(@NotNull User user, @NotNull ClaimWorld world,
                                                       @NotNull HuskClaims plugin) {
        return Optional.ofNullable(trustedUsers.get(user.getUuid()))
                .flatMap(plugin::getTrustLevel)
                .or(() -> inheritParent
                        ? getParent(world).flatMap(parent -> parent.getEffectiveTrustLevel(user, world, plugin))
                        : Optional.empty());
    }

    public boolean isPrivilegeAllowed(@NotNull TrustLevel.Privilege privilege, @NotNull User user,
                                      @NotNull ClaimWorld world, @NotNull HuskClaims plugin) {
        return getEffectiveTrustLevel(user, world, plugin)
                .map(level -> level.getPrivileges().contains(privilege))
                .orElse(false);
    }

    public boolean isOperationAllowed(@NotNull Operation operation, @NotNull ClaimWorld world,
                                      @NotNull HuskClaims plugin) {
        // If the operation is explicitly allowed, return it
        return universalFlags.contains(operation.getType())

                // Or, if the user is the owner, return true
                || operation.getUser().map(user -> owner.equals(user.getUuid())).orElse(false)

                // Or, if there's a user involved in this operation, check their rights
                || operation.getUser().map(user -> trustedUsers.get(user.getUuid())).flatMap(plugin::getTrustLevel)
                .map(level -> level.getFlags().contains(operation.getType()))

                // If the user doesn't have a trust level here, try getting it from the parent
                .orElseGet(() -> inheritParent && getParent(world)
                        .map(parent -> parent.isOperationAllowed(operation, world, plugin))
                        .orElse(false));

    }

    public Optional<Claim> getParent(@NotNull ClaimWorld world) {
        return world.getClaims().stream()
                .filter(claim -> claim.getChildren().contains(this))
                .findFirst();
    }

    public boolean isChildClaim(@NotNull ClaimWorld world) {
        return getParent(world).isPresent();
    }

    @NotNull
    public Claim createAndAddChild(@NotNull Region subRegion, @NotNull ClaimWorld world, @NotNull HuskClaims plugin)
            throws IllegalArgumentException {
        if (isChildClaim(world)) {
            throw new IllegalArgumentException("A child claim cannot be within another child claim");
        }
        if (!region.contains(subRegion.getNearCorner()) || !region.contains(subRegion.getFarCorner())) {
            throw new IllegalArgumentException("Child claim must be contained within parent claim");
        }
        final Claim child = new Claim(region, plugin);
        children.add(child);
        return child;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Claim claim) {
            return claim.region.equals(region)
                    && claim.children.equals(children);
        }
        return false;
    }
}
