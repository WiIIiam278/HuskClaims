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
import net.william278.annotaml.Annotaml;
import net.william278.huskclaims.HuskClaims;
import net.william278.huskclaims.claim.TrustLevel;
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
    YamlConfigurationProperties YAML_CONFIGURATION_PROPERTIES = YamlConfigurationProperties.newBuilder()
            .header(Settings.CONFIG_HEADER)
            .setNameFormatter(NameFormatters.LOWER_UNDERSCORE)
            .build();

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
                YAML_CONFIGURATION_PROPERTIES
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
        final String localesFile = String.format("messages-%s.yml", getSettings().getLanguage());
        try {
            setLocales(Annotaml.create(
                    getConfigDirectory().resolve(localesFile).toFile(),
                    Annotaml.create(
                            Locales.class,
                            getPlugin().getResource(String.format("locales/%s.yml", getSettings().getLanguage()))
                    ).get()
            ).get());
        } catch (Throwable e) {
            throw new IllegalStateException("Unable to load locales file: " + localesFile, e);
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
                YAML_CONFIGURATION_PROPERTIES
        ).sortByWeight());
    }

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
