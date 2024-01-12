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

package net.william278.huskclaims.util;

import net.william278.desertwell.util.UpdateChecker;
import net.william278.desertwell.util.Version;
import net.william278.huskclaims.HuskClaims;
import net.william278.huskclaims.claim.ClaimWorld;
import org.jetbrains.annotations.NotNull;

import java.util.logging.Level;

/**
 * Interface for providing plugin metadata and update checking
 *
 * @since 1.0
 */
public interface MetaProvider {

    int SPIGOT_RESOURCE_ID = 114467;
    int BSTATS_PLUGIN_ID = 20728;

    /**
     * Get a list of all {@link ClaimWorld}s
     *
     * @return A list of all {@link ClaimWorld}s
     * @since 1.0
     */
    @NotNull
    Version getPluginVersion();

    /**
     * Get the plugin {@link UpdateChecker}
     *
     * @return the plugin {@link UpdateChecker}
     * @since 1.0
     */
    @NotNull
    default UpdateChecker getUpdateChecker() {
        return UpdateChecker.builder()
                .currentVersion(getPluginVersion())
                .endpoint(UpdateChecker.Endpoint.SPIGOT)
                .resource(Integer.toString(SPIGOT_RESOURCE_ID))
                .build();
    }

    /**
     * Check for updates and log a warning if an update is available
     *
     * @since 1.0
     */
    default void checkForUpdates() {
        if (!getPlugin().getSettings().isCheckForUpdates()) {
            return;
        }
        getUpdateChecker().check().thenAccept(checked -> {
            if (checked.isUpToDate()) {
                return;
            }
            getPlugin().log(Level.WARNING, String.format(
                    "A new version of HuskClaims is available: v%s (running v%s)",
                    checked.getLatestVersion(), getPluginVersion())
            );
        });
    }

    /**
     * Get the server type
     *
     * @return the server type
     * @since 1.0
     */
    @NotNull
    String getServerType();

    /**
     * Get the Minecraft version
     *
     * @return the Minecraft version
     * @since 1.0
     */
    @NotNull
    Version getMinecraftVersion();

    @NotNull
    HuskClaims getPlugin();

}
