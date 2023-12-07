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
import com.google.common.collect.Queues;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import net.william278.cloplib.operation.Operation;
import net.william278.cloplib.operation.OperationType;
import net.william278.huskclaims.HuskClaims;
import net.william278.huskclaims.position.BlockPosition;
import net.william278.huskclaims.position.Position;
import net.william278.huskclaims.user.User;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentMap;

@Getter
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class ClaimWorld {

    private transient int id;
    @Expose
    private ConcurrentLinkedQueue<Claim> claims;
    @Expose
    @SerializedName("user_cache")
    private ConcurrentMap<UUID, String> userCache;
    @Expose
    @SerializedName("wilderness_flags")
    private List<OperationType> wildernessFlags;

    private ClaimWorld(@NotNull HuskClaims plugin) {
        this.id = 0;
        this.claims = Queues.newConcurrentLinkedQueue();
        this.userCache = Maps.newConcurrentMap();
        this.wildernessFlags = Lists.newArrayList(plugin.getSettings().getClaims().getWildernessRules());
    }

    @NotNull
    public static ClaimWorld create(@NotNull HuskClaims plugin) {
        return new ClaimWorld(plugin);
    }

    public Optional<User> getUser(@NotNull UUID uuid) {
        return Optional.ofNullable(userCache.get(uuid)).map(name -> User.of(uuid, name));
    }

    public Optional<Claim> getParentClaimAt(@NotNull BlockPosition position) {
        return getClaims().stream().filter(claim -> claim.getRegion().contains(position)).findFirst();
    }

    public Optional<Claim> getClaimAt(@NotNull BlockPosition position) {
        return getParentClaimAt(position).map(parent -> parent.getChildren().stream()
                .filter(c -> c.getRegion().contains(position)).findFirst()
                .orElse(parent));
    }

    @NotNull
    public List<Claim> getParentClaimsWithin(@NotNull Region region) {
        return getClaims().stream().filter(claim -> claim.getRegion().intersects(region)).toList();
    }

    /**
     * Get if a region is claimed
     *
     * @param region The region to check
     * @return if the region is claimed
     * @since 1.0
     */
    public boolean isRegionClaimed(@NotNull Region region) {
        return !getParentClaimsWithin(region).isEmpty();
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
        final List<Claim> claims = Lists.newArrayList(getParentClaimsWithin(region));
        for (Region except : exceptFor) {
            claims.removeIf(claim -> claim.getRegion().equals(except));
        }
        return claims.isEmpty();
    }

    public boolean isOperationAllowed(@NotNull Operation operation, @NotNull HuskClaims plugin) {
        return getClaimAt((Position) operation.getOperationPosition())
                .map(claim -> claim.isOperationAllowed(operation, this, plugin))
                .orElse(wildernessFlags.contains(operation.getType()));
    }

    public int getClaimCount() {
        return getClaims().size();
    }

    public void updateId(int id) {
        this.id = id;
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof ClaimWorld world && world.id == id;
    }

}
