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

import net.kyori.adventure.key.Key;
import net.william278.huskclaims.claim.ClaimManager;
import net.william278.huskclaims.command.CommandProvider;
import net.william278.huskclaims.config.ConfigProvider;
import net.william278.huskclaims.database.DatabaseProvider;
import net.william278.huskclaims.listener.ListenerProvider;
import net.william278.huskclaims.network.BrokerProvider;
import net.william278.huskclaims.trust.GroupManager;
import net.william278.huskclaims.trust.TrustTagManager;
import net.william278.huskclaims.user.OnlineUser;
import net.william278.huskclaims.user.UserManager;
import net.william278.huskclaims.util.*;
import org.intellij.lang.annotations.Subst;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.logging.Level;

/**
 * Common interface for the HuskClaims plugin
 *
 * @since 1.0
 */
public interface HuskClaims extends Task.Supplier, ConfigProvider, DatabaseProvider, GsonProvider, UserManager,
        ClaimManager, GroupManager, TrustTagManager, ListenerProvider, UserListProvider, CommandProvider,
        BrokerProvider, TextValidator, AudiencesProvider, BlockProvider, MetaProvider {

    /**
     * Initialize all plugin systems
     *
     * @since 1.0
     */
    default void initialize() {
        log(Level.INFO, String.format("Initializing HuskClaims v%s...", getPluginVersion()));
        try {
            loadSettings();
            loadServer();
            loadTrustLevels();
            loadLocales();
            loadDatabase();
            loadClaimWorlds();
            loadClaimHighlighter();
            loadUserGroups();
            loadTrustTags();
            loadBroker();
            loadCommands();
            loadListeners();
        } catch (Throwable e) {
            log(Level.SEVERE, "An error occurred whilst initializing HuskClaims", e);
            disablePlugin();
            return;
        }
        log(Level.INFO, String.format("Successfully initialized HuskClaims v%s", getPluginVersion()));
        checkForUpdates();
    }

    /**
     * Shutdown plugin subsystems
     *
     * @since 1.0
     */
    default void shutdown() {
        log(Level.INFO, String.format("Disabling down HuskClaims v%s...", getPluginVersion()));
        try {
            closeDatabase();
            closeBroker();
        } catch (Throwable e) {
            log(Level.SEVERE, "An error occurred whilst disabling HuskClaims", e);
        }
        log(Level.INFO, String.format("Successfully disabled HuskClaims v%s", getPluginVersion()));
    }

    /**
     * Disable the plugin
     *
     * @since 1.0
     */
    void disablePlugin();

    /**
     * Get a list of all {@link OnlineUser online users} on this server
     *
     * @return A list of {@link OnlineUser}s
     * @since 1.0
     */
    List<? extends OnlineUser> getOnlineUsers();

    /**
     * Log a message to the console.
     *
     * @param level      the level to log at
     * @param message    the message to log
     * @param exceptions any exceptions to log
     * @since 1.0
     */
    void log(@NotNull Level level, @NotNull String message, Throwable... exceptions);

    /**
     * Get a {@link Key} for the plugin
     *
     * @param value the value of the key
     * @return the key
     */
    @NotNull
    default Key getKey(@NotNull String... value) {
        @Subst("bar") String text = String.join("/", value);
        return Key.key("huskclaims", text);
    }

    /**
     * Get the plugin instance
     *
     * @return the plugin instance
     * @since 1.0
     */
    @NotNull
    default HuskClaims getPlugin() {
        return this;
    }

}
