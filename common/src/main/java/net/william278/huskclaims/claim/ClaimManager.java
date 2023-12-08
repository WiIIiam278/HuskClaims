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
import net.william278.cloplib.operation.OperationPosition;
import net.william278.cloplib.operation.OperationWorld;
import net.william278.huskclaims.database.Database;
import net.william278.huskclaims.position.Position;
import net.william278.huskclaims.position.World;
import net.william278.huskclaims.user.OnlineUser;
import net.william278.huskclaims.user.User;
import org.jetbrains.annotations.Blocking;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentMap;
import java.util.logging.Level;

/**
 * Interface for managing {@link ClaimWorld}s
 *
 * @since 1.0
 */
public interface ClaimManager extends ClaimHandler, ClaimEditor {

    /**
     * Get the claim worlds
     *
     * @return map of claim worlds
     * @since 1.0
     */
    @NotNull
    ConcurrentMap<OperationWorld, ClaimWorld> getClaimWorlds();

    /**
     * Set the claim worlds
     *
     * @param claimWorlds The claim worlds to set
     */
    void setClaimWorlds(@NotNull ConcurrentMap<OperationWorld, ClaimWorld> claimWorlds);

    /**
     * Get a claim world by world
     *
     * @param world The world to get the claim world for
     * @return the claim world, if found
     * @since 1.0
     */
    default Optional<ClaimWorld> getClaimWorld(@NotNull OperationWorld world) {
        return Optional.ofNullable(getClaimWorlds().get(world));
    }

    /**
     * Get a claim at a position
     *
     * @param position The position to get the claim at
     * @return the claim, if found
     * @since 1.0
     */
    default Optional<Claim> getClaimAt(@NotNull OperationPosition position) {
        return getClaimWorld(position.getWorld()).flatMap(world -> world.getClaimAt((Position) position));
    }

    /**
     * Create a claim and update the owner's claim block count
     *
     * @param world  The world to create the claim in
     * @param region The region to create the claim over
     * @param owner  The owner of the claim
     * @throws IllegalArgumentException if the region is already claimed, or if no claim world is present for the world
     * @since 1.0
     */
    @Blocking
    default void createClaimAt(@NotNull OperationWorld world, @NotNull Region region,
                               @Nullable User owner) throws IllegalArgumentException {
        if (!getClaimWorlds().containsKey(world)) {
            throw new IllegalArgumentException("No claim world is present for world " + world.getName());
        }
        final ClaimWorld claimWorld = getClaimWorlds().get(world);

        // Validate region is not yet claimed
        if (claimWorld.isRegionClaimed(region)) {
            throw new IllegalArgumentException("Region is already claimed");
        }

        // Create the claim and add it
        final Claim claim = owner != null
                ? Claim.create(owner, region, getPlugin())
                : Claim.createAdminClaim(region, getPlugin());
        claimWorld.getClaims().add(claim);
        getDatabase().updateClaimWorld(claimWorld);

        // Adjust the owner's claim block count
        if (owner != null) {
            getPlugin().editClaimBlocks(owner, (blocks -> blocks - region.getSurfaceArea()));
        }
    }

    /**
     * Create an admin claim
     *
     * @param world  The world to create the claim in
     * @param region The region to create the claim over
     * @throws IllegalArgumentException if the region is already claimed, or if no claim world is present for the world
     * @since 1.0
     */
    default void createAdminClaimAt(@NotNull OperationWorld world, @NotNull Region region)
            throws IllegalArgumentException {
        createClaimAt(world, region, null);
    }

    /**
     * Resize a claim and update the owner's claim block count
     *
     * @param world     The world the claim is in
     * @param claim     The claim to resize
     * @param newRegion The new region of the claim
     * @throws IllegalArgumentException if the new region overlaps with another claim, if no claim world is present,
     *                                  or if the new region does not fully enclose all the claim's children
     */
    default void resizeClaim(@NotNull OperationWorld world, @NotNull Claim claim,
                             @NotNull Region newRegion) throws IllegalArgumentException {
        if (!getClaimWorlds().containsKey(world)) {
            throw new IllegalArgumentException("No claim world is present for world " + world.getName());
        }
        final ClaimWorld claimWorld = getClaimWorlds().get(world);

        // Ensure this is not a child claim and doesn't overlap with other claims
        if (claim.isChildClaim(claimWorld)) {
            throw new IllegalArgumentException("Cannot resize a child claim at the world level");
        }
        if (claimWorld.isRegionClaimed(newRegion, claim.getRegion())) {
            throw new IllegalArgumentException("New claim region would overlap with other claim(s)");
        }

        // Ensure claim encloses all of its children
        if (!claim.getChildren().stream().map(Claim::getRegion).allMatch(newRegion::fullyEncloses)) {
            throw new IllegalArgumentException("Region does not fully enclose its children");
        }
        final int oldSurfaceArea = claim.getRegion().getSurfaceArea();

        // Update the claim
        claim.setRegion(newRegion);
        getDatabase().updateClaimWorld(claimWorld);

        // Adjust the owner's claim block count
        claim.getOwner().flatMap(claimWorld::getUser).ifPresent(user -> getPlugin().editClaimBlocks(
                user, (blocks -> blocks + (oldSurfaceArea - newRegion.getSurfaceArea())))
        );
    }

    default void deleteClaim(@NotNull OperationWorld world, @NotNull Claim claim) {
        if (!getClaimWorlds().containsKey(world)) {
            throw new IllegalArgumentException("No claim world is present for world " + world.getName());
        }
        final ClaimWorld claimWorld = getClaimWorlds().get(world);

        // Ensure this is not a child claim
        if (claim.isChildClaim(claimWorld)) {
            throw new IllegalArgumentException("Cannot delete a child claim at the world level");
        }

        // Delete the claim
        final long surfaceArea = claim.getRegion().getSurfaceArea();
        claimWorld.getClaims().remove(claim);
        getDatabase().updateClaimWorld(claimWorld);

        // Adjust the owner's claim block count
        claim.getOwner().flatMap(claimWorld::getUser).ifPresent(user -> getPlugin().editClaimBlocks(
                user, (blocks -> blocks + surfaceArea))
        );
    }

    /**
     * Create a child claim over a region
     *
     * @param world   The world to create the claim in
     * @param region  The region to create the claim over
     * @param creator The creator of the child claim
     * @throws IllegalArgumentException if the region is already claimed, or if no claim world is present for the world
     * @since 1.0
     */
    @Blocking
    default void createChildClaimAt(@NotNull OperationWorld world, @NotNull Region region,
                                    @NotNull User creator) throws IllegalArgumentException {
        if (!getClaimWorlds().containsKey(world)) {
            throw new IllegalArgumentException("No claim world is present for world " + world.getName());
        }
        final ClaimWorld claimWorld = getClaimWorlds().get(world);

        // Create claim
        final Claim parent = claimWorld.getClaimAt(region.getNearCorner())
                .orElseThrow(() -> new IllegalArgumentException("No parent claim found"));
        if (parent.getRegion().equals(region)) {
            throw new IllegalArgumentException("Parent claim and child claim regions cannot be the same");
        }

        // Validate privileges
        if (parent.isPrivilegeAllowed(TrustLevel.Privilege.MANAGE_CHILD_CLAIMS, creator, claimWorld, getPlugin())) {
            throw new IllegalArgumentException("User lacks sufficient child claim creation privileges");
        }

        // Create and add child claim
        parent.createAndAddChild(region, claimWorld, getPlugin());
        getDatabase().updateClaimWorld(claimWorld);
    }

    /**
     * Load the claim worlds from the database
     *
     * @since 1.0
     */
    @Blocking
    default void loadClaimWorlds() throws IllegalStateException {
        getPlugin().log(Level.INFO, "Loading claims from the database...");
        final LocalTime startTime = LocalTime.now();

        // Only load claim worlds for this server
        final Map<World, ClaimWorld> worlds = getDatabase().getClaimWorlds(getPlugin().getServerName());
        final ConcurrentMap<OperationWorld, ClaimWorld> loadedWorlds = Maps.newConcurrentMap();
        loadedWorlds.putAll(worlds);
        for (final World serverWorld : getPlugin().getWorlds()) {
            if (getPlugin().getSettings().getClaims().isWorldUnclaimable(serverWorld)) {
                continue;
            }

            if (worlds.keySet().stream().map(World::getName).noneMatch(uuid -> uuid.equals(serverWorld.getName()))) {
                getPlugin().log(Level.INFO, String.format("Creating new claim world for %s...", serverWorld.getName()));
                loadedWorlds.put(serverWorld, getDatabase().createClaimWorld(serverWorld));
            }
        }
        setClaimWorlds(loadedWorlds);

        final Collection<ClaimWorld> claimWorlds = getClaimWorlds().values();
        final int claimCount = claimWorlds.stream().mapToInt(ClaimWorld::getClaimCount).sum();
        getPlugin().log(Level.INFO, String.format("Loaded %s claim(s) across %s world(s) in %s seconds",
                claimCount, claimWorlds.size(), ChronoUnit.MILLIS.between(startTime, LocalTime.now()) / 1000d));
    }

    /**
     * Get the claim highlighter
     *
     * @return the claim highlighter
     * @since 1.0
     */
    @NotNull
    ClaimHighlighter getClaimHighlighter();

    /**
     * Highlight a claim at a position for a user
     *
     * @param user     The user to highlight the claim for
     * @param position The position to highlight the claim at
     * @since 1.0
     */
    default void highlightClaimAt(@NotNull OnlineUser user, @NotNull OperationPosition position) {
        getClaimWorld(position.getWorld()).ifPresent(world -> world.getClaimAt((Position) position)
                .ifPresent(claim -> getClaimHighlighter().highlightClaim(user, world, claim)));
    }

    /**
     * Set the highlighter to use for highlighting claims
     *
     * @param claimHighlighter The claim highlighter to set
     * @since 1.0
     */
    void setClaimHighlighter(@NotNull ClaimHighlighter claimHighlighter);

    @NotNull
    Database getDatabase();

    /**
     * Types of claim selection modes
     *
     * @since 1.0
     */
    enum ClaimingMode {
        CLAIMS,
        CHILD_CLAIMS
    }
}
