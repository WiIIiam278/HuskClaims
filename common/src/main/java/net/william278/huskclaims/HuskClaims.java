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
import net.william278.huskclaims.api.HuskClaimsAPI;
import net.william278.huskclaims.claim.ClaimManager;
import net.william278.huskclaims.command.CommandProvider;
import net.william278.huskclaims.config.ConfigProvider;
import net.william278.huskclaims.database.DatabaseProvider;
import net.william278.huskclaims.event.EventDispatcher;
import net.william278.huskclaims.highlighter.HighlighterProvider;
import net.william278.huskclaims.hook.HookProvider;
import net.william278.huskclaims.hook.PluginHook;
import net.william278.huskclaims.listener.ListenerProvider;
import net.william278.huskclaims.moderation.DropsHandler;
import net.william278.huskclaims.moderation.SignNotifier;
import net.william278.huskclaims.network.BrokerProvider;
import net.william278.huskclaims.pet.PetHandler;
import net.william278.huskclaims.tax.PropertyTaxManager;
import net.william278.huskclaims.trust.GroupManager;
import net.william278.huskclaims.trust.TrustTagManager;
import net.william278.huskclaims.user.SavedUserProvider;
import net.william278.huskclaims.user.UserProvider;
import net.william278.huskclaims.util.*;
import org.intellij.lang.annotations.Subst;
import org.jetbrains.annotations.NotNull;

import java.util.logging.Level;

/**
 * Common interface for the HuskClaims plugin
 *
 * @since 1.0
 */
public interface HuskClaims extends Task.Supplier, ConfigProvider, UserProvider, SavedUserProvider, DatabaseProvider,
        GsonProvider, SignNotifier, ClaimManager, GroupManager, TrustTagManager, ListenerProvider, CommandProvider,
        PetHandler, DropsHandler, BrokerProvider, TextValidator, AudiencesProvider, BlockProvider, SafeTeleportProvider,
        MetaProvider, EventDispatcher, HookProvider, HighlighterProvider, DumpProvider, PropertyTaxManager {

    /**
     * Load plugin systems
     *
     * @since 1.3.2
     */
    default void load() {
        try {
            loadSettings();
            loadServer();
            loadTrustLevels();
            loadLocales();
            loadHooks(PluginHook.Register.values());
            registerHooks(PluginHook.Register.ON_LOAD);
        } catch (Throwable e) {
            log(Level.SEVERE, "An error occurred whilst loading HuskClaims", e);
            disablePlugin();
            return;
        }
        log(Level.INFO, String.format("Successfully loaded HuskClaims v%s", getPluginVersion()));
    }

    /**
     * Enable all plugin systems
     *
     * @since 1.0
     */
    default void enable() {
        try {
            loadDatabase();
            loadClaimWorlds();
            loadHighlighters();
            loadUserGroups();
            loadTrustTags();
            loadBroker();
            loadCommands();
            loadListeners();
            loadClaimBlockScheduler();
            registerHooks(PluginHook.Register.ON_ENABLE);
            loadAPI();
            loadMetrics();
            startQueuePoller();
        } catch (Throwable e) {
            log(Level.SEVERE, "An error occurred whilst enabling HuskClaims", e);
            disablePlugin();
            return;
        }
        log(Level.INFO, String.format("Successfully enabled HuskClaims v%s", getPluginVersion()));
        checkForUpdates();
    }

    /**
     * Shutdown plugin subsystems
     *
     * @since 1.0
     */
    default void shutdown() {
        log(Level.INFO, String.format("Disabling HuskClaims v%s...", getPluginVersion()));
        try {
            unloadHooks(PluginHook.Register.values());
            closeBroker();
            closeDatabase();
            cancelTasks();
            unloadAPI();
        } catch (Throwable e) {
            log(Level.SEVERE, "An error occurred whilst disabling HuskClaims", e);
        }
        log(Level.INFO, String.format("Successfully disabled HuskClaims v%s", getPluginVersion()));
    }

    /**
     * Register the API instance
     *
     * @since 1.0
     */
    void loadAPI();

    /**
     * Unregister the API instance
     *
     * @since 1.0
     */
    default void unloadAPI() {
        HuskClaimsAPI.unregister();
    }

    /**
     * Load plugin metrics
     *
     * @since 1.0
     */
    void loadMetrics();

    /**
     * Disable the plugin
     *
     * @since 1.0
     */
    void disablePlugin();

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

    /**
     * Check if a claim is overdue for tax payment
     *
     * @param claim the claim to check
     * @param world the claim world
     * @param userTaxBalance the user's tax balance
     * @return true if the claim is overdue
     * @since 1.5
     */
    default boolean isClaimOverdue(@NotNull net.william278.huskclaims.claim.Claim claim,
                                   @NotNull net.william278.huskclaims.claim.ClaimWorld world,
                                   double userTaxBalance) {
        return getPropertyTaxManager().isClaimOverdue(claim, world, userTaxBalance);
    }

    /**
     * Get the property tax manager
     *
     * @return the property tax manager
     * @since 1.5
     */
    @NotNull
    default PropertyTaxManager getPropertyTaxManager() {
        return this;
    }

}
