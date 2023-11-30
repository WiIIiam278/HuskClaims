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

package net.william278.huskclaims;

import net.william278.huskclaims.claim.ClaimHighlighter;
import net.william278.huskclaims.claim.ClaimWorld;
import net.william278.huskclaims.config.ConfigProvider;
import net.william278.huskclaims.position.World;
import org.jetbrains.annotations.NotNull;

import java.io.InputStream;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Level;

/**
 * Common interface for the HuskClaims plugin
 *
 * @since 1.0
 */
public interface HuskClaims extends ConfigProvider, ClaimHighlighter {

    /**
     * Get a trust level by name
     *
     * @return the trust level, if found
     * @since 1.0
     */
    @NotNull
    Map<World, ClaimWorld> getClaimWorlds();

    /**
     * Get a claim world by world
     *
     * @param world The world to get the claim world for
     * @return the claim world, if found
     * @since 1.0
     */
    default Optional<ClaimWorld> getClaimWorld(@NotNull World world) {
        return Optional.ofNullable(getClaimWorlds().get(world));
    }

    /**
     * Get a plugin resource
     *
     * @param name The name of the resource
     * @return the resource, if found
     * @since 1.0
     */
    InputStream getResource(@NotNull String name);

    /**
     * Log a message to the console.
     *
     * @param level      the level to log at
     * @param message    the message to log
     * @param exceptions any exceptions to log
     * @since 1.0
     */
    void log(@NotNull Level level, @NotNull String message, Throwable... exceptions);

}
