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

package net.william278.huskclaims.config;

import de.exlll.configlib.NameFormatters;
import de.exlll.configlib.YamlConfigurationProperties;
import de.exlll.configlib.YamlConfigurationStore;
import de.exlll.configlib.YamlConfigurations;
import net.william278.cloplib.operation.OperationType;
import net.william278.huskclaims.HuskClaims;
import net.william278.huskclaims.trust.TrustLevel;
import org.jetbrains.annotations.NotNull;

import java.io.InputStream;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;

/**
 * Interface for getting and setting data from plugin configuration files
 *
 * @since 1.0
 */
public interface ConfigProvider {

    @NotNull
    YamlConfigurationProperties.Builder<?> YAML_CONFIGURATION_PROPERTIES = YamlConfigurationProperties.newBuilder()
            .setNameFormatter(NameFormatters.LOWER_UNDERSCORE);

    /**
     * Get the plugin settings, read from the config file
     *
     * @return the plugin settings
     * @since 1.0
     */
    @NotNull
    Settings getSettings();

    /**
     * Set the plugin settings
     *
     * @param settings The settings to set
     * @since 1.0
     */
    void setSettings(@NotNull Settings settings);

    /**
     * Load the plugin settings from the config file
     *
     * @since 1.0
     */
    default void loadSettings() {
        setSettings(YamlConfigurations.update(
                getConfigDirectory().resolve("config.yml"),
                Settings.class,
                YAML_CONFIGURATION_PROPERTIES.header(Settings.CONFIG_HEADER).build()
        ));
    }

    /**
     * Get the locales for the plugin
     *
     * @return the locales for the plugin
     * @since 1.0
     */
    @NotNull
    Locales getLocales();

    /**
     * Set the locales for the plugin
     *
     * @param locales The locales to set
     * @since 1.0
     */
    void setLocales(@NotNull Locales locales);

    /**
     * Load the locales from the config file
     *
     * @since 1.0
     */
    default void loadLocales() {
        final YamlConfigurationStore<Locales> store = new YamlConfigurationStore<>(
                Locales.class, YAML_CONFIGURATION_PROPERTIES.header(Locales.CONFIG_HEADER).build()
        );
        try (InputStream input = getResource(String.format("locales/%s.yml", getSettings().getLanguage()))) {
            final Locales locales = store.read(input);
            store.save(
                    locales,
                    getConfigDirectory().resolve(String.format("messages-%s.yml", getSettings().getLanguage()))
            );
            setLocales(locales);
        } catch (Throwable e) {
            getPlugin().log(Level.SEVERE, "An error occurred loading the locales (invalid lang code?)", e);
        }
    }

    @NotNull
    String getServerName();

    void setServer(@NotNull Server server);

    default void loadServer() {
        if (getSettings().getCrossServer().isEnabled()) {
            setServer(YamlConfigurations.update(
                    getConfigDirectory().resolve("server.yml"),
                    Server.class,
                    YAML_CONFIGURATION_PROPERTIES.header(Server.CONFIG_HEADER).build()
            ));
        }
    }

    /**
     * Get a list of all trust levels
     *
     * @return a list of all trust levels
     * @since 1.0
     */
    @NotNull
    List<TrustLevel> getTrustLevels();

    /**
     * Get the trust level with the highest weight
     *
     * @return the highest trust level
     * @since 1.0
     */
    @NotNull
    default TrustLevel getHighestTrustLevel() {
        return getTrustLevels().stream().max(TrustLevel::compareTo)
                .orElseThrow(() -> new IllegalStateException("No trust levels found"));
    }

    /**
     * Get the trust level with the lowest weight
     *
     * @return the lowest trust level
     * @since 1.0
     */
    @NotNull
    default TrustLevel getLowestTrustLevel() {
        return getTrustLevels().stream().min(TrustLevel::compareTo)
                .orElseThrow(() -> new IllegalStateException("No trust levels found"));
    }

    /**
     * Get the lowest trust level that grants build trust
     *
     * @return the lowest trust level that grants build trust
     * @since 1.0
     */
    @NotNull
    default Optional<TrustLevel> getBuildTrustLevel() {
        return getTrustLevels().stream()
                .filter(level -> level.getFlags().contains(OperationType.BLOCK_PLACE))
                .min(TrustLevel::compareTo);
    }

    /**
     * Get a trust level by ID
     *
     * @param id The ID of the trust level
     * @return the trust level, if found
     * @since 1.0
     */
    default Optional<TrustLevel> getTrustLevel(@NotNull String id) {
        return getTrustLevels().stream().filter(trustLevel -> trustLevel.getId().equalsIgnoreCase(id)).findFirst();
    }

    /**
     * Set the trust levels
     *
     * @param trustLevels The trust levels to set
     * @since 1.0
     */
    void setTrustLevels(@NotNull TrustLevels trustLevels);

    /**
     * Load the trust levels from the config file
     *
     * @since 1.0
     */
    default void loadTrustLevels() {
        setTrustLevels(YamlConfigurations.update(
                getConfigDirectory().resolve("trust_levels.yml"),
                TrustLevels.class,
                YAML_CONFIGURATION_PROPERTIES.header(TrustLevels.CONFIG_HEADER).build()
        ).sortByWeight());
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
     * Get the plugin config directory
     *
     * @return the plugin config directory
     * @since 1.0
     */
    @NotNull
    Path getConfigDirectory();

    @NotNull
    HuskClaims getPlugin();

}
