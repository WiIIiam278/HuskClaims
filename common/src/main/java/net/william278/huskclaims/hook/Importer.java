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

package net.william278.huskclaims.hook;

import com.google.common.collect.Maps;
import lombok.Getter;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.william278.huskclaims.HuskClaims;
import net.william278.huskclaims.user.CommandUser;
import net.william278.huskclaims.user.OnlineUser;
import org.jetbrains.annotations.NotNull;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.logging.Level;
import java.util.stream.Collectors;

public abstract class Importer extends Hook {
    protected Map<String, String> configParameters;
    protected Map<String, String> defaultParameters;
    @Getter
    private final List<ImportData> supportedData;
    @Getter
    private Map<String, Boolean> requiredParameters;
    @Getter
    private ImportState state;

    protected Importer(@NotNull String name, @NotNull List<ImportData> supportedData, @NotNull HuskClaims plugin,
                       @NotNull Map<String, Boolean> requiredParams, @NotNull Map<String, String> defaultParams) {
        super(name, plugin);
        this.state = ImportState.WAITING;
        this.supportedData = supportedData;
        this.requiredParameters = requiredParams;
        this.defaultParameters = defaultParams;
        this.configParameters = Maps.newHashMap(defaultParams);
    }

    @Override
    public final void load() {
    }

    @Override
    public final void unload() {
        if (state == ImportState.ACTIVE) {
            getPlugin().log(Level.WARNING, "Importer %s was unloaded while active, cancelling import.".formatted(name));
            state = ImportState.WAITING;
            finish();
        }
    }

    public void setValue(@NotNull CommandUser user, @NotNull String param, @NotNull String value, boolean sensitive) {
        if (!requiredParameters.containsKey(param)) {
            log(user, Level.WARNING, "❌ Unknown parameter: %s".formatted(param));
            return;
        }
        configParameters.put(param, value);
        log(user, Level.INFO, "✔ Set %s → %s".formatted(param, !sensitive ? value : "*".repeat(value.length())));
    }

    public void reset(@NotNull CommandUser user) {
        this.state = ImportState.WAITING;
        log(user, Level.INFO, "✔ Reset import state");
    }

    public final void start(@NotNull CommandUser user) {
        // Ensure the importer is enabled
        if (state != ImportState.WAITING) {
            log(user, Level.WARNING, "❌ Import is already %s.".formatted(state.name().toLowerCase(Locale.ENGLISH)));
            return;
        }

        // Ensure parameters are set
        final String missingParameters = requiredParameters.entrySet().stream()
                .filter(parameter -> !configParameters.containsKey(parameter.getKey()) && parameter.getValue())
                .map(Map.Entry::getKey).collect(Collectors.joining(", "));
        if (!missingParameters.isEmpty()) {
            log(user, Level.WARNING, "❌ Missing required parameters: %s".formatted(missingParameters));
            return;
        }

        // Start import
        try {
            prepare();
        } catch (Throwable e) {
            log(user, Level.WARNING, "❌ Failed to prepare import: %s".formatted(e.getMessage()), e);
            return;
        }
        final LocalDateTime startTime = LocalDateTime.now();
        log(user, Level.INFO, "⌚ Starting " + name + " data import...");
        state = ImportState.ACTIVE;

        // Import data
        for (ImportData data : supportedData) {
            try {
                log(user, Level.INFO, "⌚ Importing " + data.getName() + "...");
                final int entries = importData(data, user);
                log(user, Level.INFO, "✔ Imported " + data.getName() + " (" + entries + " entries)");
            } catch (Throwable e) {
                log(user, Level.WARNING, String.format("❌ Failed to import %s: %s", data.getName(), e.getMessage()), e);
                state = ImportState.WAITING;
                return;
            }
        }

        // Finish import
        try {
            finish();
        } catch (Throwable e) {
            log(user, Level.WARNING, "❌ Failed to finish import: %s".formatted(e.getMessage()), e);
            return;
        }
        final long timeTaken = startTime.until(LocalDateTime.now(), ChronoUnit.SECONDS);
        log(user, Level.INFO, "✔ Completed import from " + name + " (took " + timeTaken + "s)");
        state = ImportState.DONE;
    }

    protected abstract void prepare();

    protected abstract int importData(@NotNull ImportData importData, @NotNull CommandUser executor);

    protected abstract void finish();

    protected final void log(@NotNull CommandUser user, @NotNull Level level, @NotNull String message,
                             @NotNull Throwable... e) {
        message = "[Importer] " + message;
        if (user instanceof OnlineUser online) {
            final TextColor color = level == Level.SEVERE || level == Level.WARNING
                    ? TextColor.color(0xff3300) : TextColor.color(0xC3C3C3);
            online.sendMessage(Component.text(message, color));
        }
        plugin.log(level, message, e);
    }

    /**
     * Represents types of data that can be imported.
     */
    public enum ImportData {
        USERS("User Data"),
        CLAIMS("Claims");

        private final String name;

        ImportData(@NotNull String name) {
            this.name = name;
        }

        @NotNull
        public String getName() {
            return name;
        }
    }

    /**
     * Represents the state of an import.
     */
    public enum ImportState {
        WAITING,
        ACTIVE,
        DONE
    }

}
