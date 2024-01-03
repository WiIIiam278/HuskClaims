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
import net.william278.huskclaims.database.Database;
import net.william278.huskclaims.highlighter.BlockUpdateHighlighter;
import net.william278.huskclaims.highlighter.Highlighter;
import net.william278.huskclaims.network.Message;
import net.william278.huskclaims.network.Payload;
import net.william278.huskclaims.position.Position;
import net.william278.huskclaims.position.World;
import net.william278.huskclaims.user.OnlineUser;
import net.william278.huskclaims.user.User;
import net.william278.huskclaims.user.UserManager;
import org.jetbrains.annotations.Blocking;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

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
public interface ClaimManager extends ClaimHandler, ClaimEditor {

    /**
     * Get the claim worlds
     *
     * @return map of claim worlds
     * @since 1.0
     */
    @NotNull
    HashMap<String, ClaimWorld> getClaimWorlds();

    /**
     * Set the claim worlds
     *
     * @param claimWorlds The claim worlds to set
     */
    void setClaimWorlds(@NotNull HashMap<World, ClaimWorld> claimWorlds);

    /**
     * Get a claim world by world
     *
     * @param world The world to get the claim world for
     * @return the claim world, if found
     * @since 1.0
     */
    default Optional<ClaimWorld> getClaimWorld(@NotNull World world) {
        return Optional.ofNullable(getClaimWorlds().get(world.getName()));
    }

    /**
     * Get a claim at a position
     *
     * @param position The position to get the claim at
     * @return the claim, if found
     * @since 1.0
     */
    default Optional<Claim> getClaimAt(@NotNull Position position) {
        return getClaimWorld(position.getWorld()).flatMap(world -> world.getClaimAt(position));
    }

    /**
     * Create a claim and update the owner's claim block count
     *
     * @param world  The world to create the claim in
     * @param region The region to create the claim over
     * @param owner  The owner of the claim
     * @return the created claim
     * @throws IllegalArgumentException if the region is already claimed, or if no claim world is present for the world
     * @since 1.0
     */
    @Blocking
    @NotNull
    default Claim createClaimAt(@NotNull ClaimWorld world, @NotNull Region region, @Nullable User owner) {
        // Validate region is not yet claimed or too small
        if (world.isRegionClaimed(region)) {
            throw new IllegalArgumentException("Region is already claimed");
        }
        if (owner != null && region.getShortestEdge() < getPlugin().getSettings().getClaims().getMinimumClaimSize()) {
            throw new IllegalArgumentException("Region is too small");
        }
        if (owner != null && getPlugin().getClaimBlocks(owner) < region.getSurfaceArea()) {
            throw new IllegalArgumentException("Owner does not have enough claim blocks");
        }

        // Create the claim and add it
        final Claim claim = owner != null
                ? Claim.create(owner, region, getPlugin())
                : Claim.createAdminClaim(region, getPlugin());
        world.getClaims().add(claim);
        getDatabase().updateClaimWorld(world);

        // Adjust the owner's claim block count
        if (owner != null) {
            getPlugin().editClaimBlocks(
                    owner, UserManager.ClaimBlockSource.CLAIM_CREATED,
                    (blocks -> blocks - region.getSurfaceArea())
            );
        }
        getPlugin().invalidateClaimListCache(owner == null ? null : owner.getUuid());
        return claim;
    }

    /**
     * Create an admin claim
     *
     * @param world  The world to create the claim in
     * @param region The region to create the claim over
     * @return the created claim
     * @throws IllegalArgumentException if the region is already claimed, or if no claim world is present for the world
     * @since 1.0
     */
    @Blocking
    @NotNull
    default Claim createAdminClaimAt(@NotNull ClaimWorld world, @NotNull Region region) {
        return createClaimAt(world, region, null);
    }


    /**
     * Resize a claim and update the owner's claim block count
     *
     * @param world     The claim world the claim is in
     * @param claim     The claim to resize
     * @param newRegion The new region of the claim
     * @throws IllegalArgumentException if the new region overlaps with another claim, if no claim world is present,
     *                                  or if the new region does not fully enclose all the claim's children
     * @since 1.0
     */
    default void resizeClaim(@NotNull ClaimWorld world, @NotNull Claim claim, @NotNull Region newRegion) {
        // Ensure this is not a child claim and doesn't overlap with other claims
        if (claim.isChildClaim(world)) {
            throw new IllegalArgumentException("Cannot resize a child claim at the world level");
        }
        if (world.isRegionClaimed(newRegion, claim.getRegion())) {
            throw new IllegalArgumentException("New claim region would overlap with other claim(s)");
        }

        // Ensure claim encloses all of its children
        if (!claim.getChildren().stream().map(Claim::getRegion).allMatch(newRegion::fullyEncloses)) {
            throw new IllegalArgumentException("Region does not fully enclose its children");
        }

        // Ensure the owner has enough claim blocks
        long neededBlocks = newRegion.getSurfaceArea() - claim.getRegion().getSurfaceArea();
        if (claim.getOwner().isPresent() && neededBlocks > 0
                && getPlugin().getClaimBlocks(claim.getOwner().get()) < neededBlocks) {
            throw new IllegalArgumentException("Owner does not have enough claim blocks to resize claim");
        }

        // Update the claim
        claim.setRegion(newRegion);
        getDatabase().updateClaimWorld(world);
        getPlugin().invalidateClaimListCache(claim.getOwner().orElse(null));

        // Adjust the owner's claim block count
        claim.getOwner().flatMap(world::getUser).ifPresent(user -> getPlugin().editClaimBlocks(
                user, UserManager.ClaimBlockSource.HOURLY_BLOCKS, (blocks -> blocks - neededBlocks))
        );
    }

    default void deleteClaim(@NotNull ClaimWorld claimWorld, @NotNull Claim claim) {
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
                user, UserManager.ClaimBlockSource.CLAIM_DELETED, (blocks -> blocks + surfaceArea))
        );
        getPlugin().invalidateClaimListCache(claim.getOwner().orElse(null));
    }

    /**
     * Delete all claims owned by a user
     *
     * @param executor The user who is deleting the claims
     * @param user     The user whose claims are being deleted
     * @since 1.0
     */
    default void deleteAllClaims(@NotNull OnlineUser executor, @NotNull User user) {
        getPlugin().getDatabase().getAllClaimWorlds().entrySet().stream()
                .filter((world) -> world.getValue().removeClaimsBy(user))
                .forEach((world) -> {
                    if (getPlugin().getClaimWorld(world.getKey().world()).isPresent()) {
                        getPlugin().getClaimWorlds().put(world.getKey().world().getName(), world.getValue());
                    }
                    getDatabase().updateClaimWorld(world.getValue());
                });
        getPlugin().getBroker().ifPresent(broker -> Message.builder()
                .type(Message.MessageType.DELETE_ALL_CLAIMS)
                .payload(Payload.uuid(user.getUuid()))
                .target(Message.TARGET_ALL, Message.TargetType.SERVER).build()
                .send(broker, executor));
        getPlugin().invalidateClaimListCache(user.getUuid());
    }

    /**
     * Delete all admin claims
     *
     * @param executor The user who is deleting the claims
     * @since 1.0
     */
    default void deleteAllAdminClaims(@NotNull OnlineUser executor) {
        getPlugin().getDatabase().getAllClaimWorlds().entrySet().stream()
                .filter((world) -> world.getValue().removeAdminClaims())
                .forEach((world) -> {
                    if (getPlugin().getClaimWorld(world.getKey().world()).isPresent()) {
                        getPlugin().getClaimWorlds().put(world.getKey().world().getName(), world.getValue());
                    }
                    getDatabase().updateClaimWorld(world.getValue());
                });
        getPlugin().getBroker().ifPresent(broker -> Message.builder()
                .type(Message.MessageType.DELETE_ALL_CLAIMS)
                .target(Message.TARGET_ALL, Message.TargetType.SERVER).build()
                .send(broker, executor));
        getPlugin().invalidateAdminClaimListCache();
    }

    /**
     * Create a child claim over a region
     *
     * @param world  The claim world to create the claim in
     * @param region The region to create the claim over
     * @throws IllegalArgumentException if the region is already claimed, or if no claim world is present for the world
     * @since 1.0
     */
    @Blocking
    @NotNull
    default Claim createChildClaimAt(@NotNull ClaimWorld world, @NotNull Region region) {
        // Create claim
        final Claim parent = world.getClaimAt(region.getNearCorner())
                .orElseThrow(() -> new IllegalArgumentException("No parent claim found"));
        if (parent.getRegion().equals(region)) {
            throw new IllegalArgumentException("Parent claim and child claim regions cannot be the same");
        }

        // Create and add child claim
        final Claim child = parent.createAndAddChild(region, world, getPlugin());
        getDatabase().updateClaimWorld(world);
        getPlugin().invalidateClaimListCache(parent.getOwner().orElse(null));
        return child;
    }

    @Blocking
    default void deleteChildClaim(@NotNull ClaimWorld world, @NotNull Claim parent, @NotNull Claim child) {
        if (!parent.getChildren().remove(child)) {
            throw new IllegalArgumentException("Parent does not contain child");
        }
        getDatabase().updateClaimWorld(world);
        getPlugin().invalidateClaimListCache(parent.getOwner().orElse(null));
    }

    @Blocking
    default void resizeChildClaim(@NotNull ClaimWorld world, @NotNull Claim claim, @NotNull Region newRegion) {
        // Ensure this is a child claim
        final Optional<Claim> optionalParent = claim.getParent(world);
        if (optionalParent.isEmpty()) {
            throw new IllegalArgumentException("Cannot resize a non-child claim");
        }
        final Claim parent = optionalParent.get();

        // Ensure claim encloses all of its children
        if (!parent.getRegion().fullyEncloses(newRegion)) {
            throw new IllegalArgumentException("Parent region does not fully enclose new child region");
        }
        if (parent.getRegion().equals(newRegion)) {
            throw new IllegalArgumentException("Parent claim and child claim regions cannot be the same");
        }

        // Ensure this is not a child claim and doesn't overlap with other claims
        if (!parent.getChildClaimsWithin(newRegion, claim.getRegion()).isEmpty()) {
            throw new IllegalArgumentException("New claim region would overlap with other child claim(s)");
        }

        // Update the claim
        claim.setRegion(newRegion);
        getDatabase().updateClaimWorld(world);
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
        final HashMap<World, ClaimWorld> loadedWorlds = Maps.newHashMap();
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
     * Load the claim highlighter
     *
     * @since 1.0
     */
    default void loadClaimHighlighter() {
        setHighlighter(new BlockUpdateHighlighter(getPlugin()));
    }

    /**
     * Get the claim highlighter
     *
     * @return the claim highlighter
     * @since 1.0
     */
    @NotNull
    Highlighter getHighlighter();

    /**
     * Highlight a claim at a position for a user
     *
     * @param user     The user to highlight the claim for
     * @param position The position to highlight the claim at
     * @since 1.0
     */
    default void highlightClaimAt(@NotNull OnlineUser user, @NotNull Position position) {
        getClaimWorld(position.getWorld()).flatMap(world -> world.getClaimAt(position))
                .ifPresent(claim -> getHighlighter().startHighlighting(user, position.getWorld(), claim));
    }

    /**
     * Set the highlighter to use for highlighting claims
     *
     * @param highlighter The claim highlighter to set
     * @since 1.0
     */
    void setHighlighter(@NotNull Highlighter highlighter);

    @NotNull
    Database getDatabase();

}
