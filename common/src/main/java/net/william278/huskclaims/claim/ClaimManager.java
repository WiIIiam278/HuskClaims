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

import net.william278.cloplib.operation.OperationPosition;
import net.william278.cloplib.operation.OperationWorld;
import net.william278.huskclaims.database.Database;
import net.william278.huskclaims.position.Position;
import net.william278.huskclaims.position.World;
import net.william278.huskclaims.user.OnlineUser;
import net.william278.huskclaims.user.User;
import org.jetbrains.annotations.Blocking;
import org.jetbrains.annotations.NotNull;

import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Level;

/**
 * Interface for managing {@link ClaimWorld}s
 *
 * @since 1.0
 */
public interface ClaimManager extends ClaimHandler {

    /**
     * Get the claim worlds
     *
     * @return map of claim worlds
     * @since 1.0
     */
    @NotNull
    Map<OperationWorld, ClaimWorld> getClaimWorlds();

    /**
     * Set the claim worlds
     *
     * @param claimWorlds The claim worlds to set
     */
    void setClaimWorlds(@NotNull Map<String, ClaimWorld> claimWorlds);

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
     * Create a claim over a region
     *
     * @param world  The world to create the claim in
     * @param region The region to create the claim over
     * @param owner  The owner of the claim
     * @throws IllegalArgumentException if the region is already claimed, or if no claim world is present for the world
     * @since 1.0
     */
    @Blocking
    default void createClaimAt(@NotNull OperationWorld world, @NotNull Region region,
                               @NotNull User owner) throws IllegalArgumentException {
        if (!getClaimWorlds().containsKey(world)) {
            throw new IllegalArgumentException("No claim world is present for world " + world.getName());
        }
        final ClaimWorld claimWorld = getClaimWorlds().get(world);

        // Validate region is not yet claimed
        if (claimWorld.isRegionClaimed(region)) {
            throw new IllegalArgumentException("Region is already claimed");
        }

        // Create the claim and add it
        final Claim claim = Claim.create(owner, region, getPlugin());
        claimWorld.getClaims().add(claim);
        getDatabase().updateClaimWorld(claimWorld);
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
        final Map<String, ClaimWorld> loadedWorlds = new HashMap<>();
        final Map<World, ClaimWorld> worlds = getDatabase().getClaimWorlds(getPlugin().getServerName());
        worlds.forEach((world, claimWorld) -> loadedWorlds.put(world.getName(), claimWorld));
        for (final World serverWorld : getPlugin().getWorlds()) {
            if (getPlugin().getSettings().getClaims().isWorldUnclaimable(serverWorld)) {
                continue;
            }

            if (worlds.keySet().stream().map(World::getName).noneMatch(uuid -> uuid.equals(serverWorld.getName()))) {
                getPlugin().log(Level.INFO, String.format("Creating new claim world for %s...", serverWorld.getName()));
                loadedWorlds.put(serverWorld.getName(), getDatabase().createClaimWorld(serverWorld));
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
        getClaimWorld(position.getWorld())
                .ifPresent(world -> world.getClaimAt((Position) position)
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

}
