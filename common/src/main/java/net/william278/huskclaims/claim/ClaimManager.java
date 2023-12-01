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
import net.william278.huskclaims.position.Position;
import net.william278.huskclaims.position.World;
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
     * Load the claim worlds from the database
     *
     * @since 1.0
     */
    default void loadClaimWorlds() throws IllegalStateException {
        getPlugin().log(Level.INFO, "Loading claims from the database...");
        LocalTime startTime = LocalTime.now();

        // Only load claim worlds for this server
        final Map<String, ClaimWorld> loadedWorlds = new HashMap<>();
        final Map<World, ClaimWorld> worlds = getPlugin().getDatabase().getClaimWorlds(getPlugin().getServerName());
        worlds.forEach((world, claimWorld) -> loadedWorlds.put(world.getName(), claimWorld));
        for (final World serverWorld : getPlugin().getWorlds()) {
            if (getPlugin().getSettings().getClaims().isWorldUnclaimable(serverWorld)) {
                continue;
            }

            if (worlds.keySet().stream().map(World::getName).noneMatch(uuid -> uuid.equals(serverWorld.getName()))) {
                getPlugin().log(Level.INFO, String.format("Creating new claim world for %s...", serverWorld.getName()));
                loadedWorlds.put(serverWorld.getName(), getPlugin().getDatabase().createClaimWorld(serverWorld));
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
     * Set the highlighter to use for highlighting claims
     *
     * @param claimHighlighter The claim highlighter to set
     * @since 1.0
     */
    void setClaimHighlighter(@NotNull ClaimHighlighter claimHighlighter);

}
