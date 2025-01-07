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

import com.google.common.collect.Maps;
import de.exlll.configlib.Configuration;
import de.themoep.minedown.adventure.MineDown;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.william278.huskclaims.HuskClaims;
import net.william278.huskclaims.command.HuskClaimsCommand;
import net.william278.huskclaims.hook.HuskHomesHook;
import net.william278.huskclaims.position.BlockPosition;
import net.william278.huskclaims.position.ServerWorld;
import net.william278.huskclaims.user.CommandUser;
import net.william278.huskclaims.user.OnlineUser;
import net.william278.huskclaims.util.PaginatedListProvider;
import org.apache.commons.text.StringEscapeUtils;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * Plugin locale configuration
 *
 * @since 1.0
 */
@SuppressWarnings("FieldMayBeFinal")
@Configuration
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class Locales implements PaginatedListProvider {

    static final String CONFIG_HEADER = """
            ┏━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┓
            ┃     HuskClaims - Locales     ┃
            ┃    Developed by William278   ┃
            ┣━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┛
            ┣╸ See plugin about menu for international locale credits
            ┣╸ Formatted in MineDown: https://github.com/Phoenix616/MineDown
            ┗╸ Translate HuskClaims: https://william278.net/docs/huskclaims/translations""";

    protected static final String DEFAULT_LOCALE = "en-gb";

    // The raw set of locales loaded from yaml
    Map<String, String> locales = Maps.newTreeMap();

    /**
     * Returns a raw, unformatted locale loaded from the locales file
     *
     * @param localeId String identifier of the locale, corresponding to a key in the file
     * @return An {@link Optional} containing the locale corresponding to the id, if it exists
     */
    public Optional<String> getRawLocale(@NotNull String localeId) {
        return Optional.ofNullable(locales.get(localeId)).map(StringEscapeUtils::unescapeJava);
    }

    /**
     * Returns a raw, un-formatted locale loaded from the locales file, with replacements applied
     * <p>
     * Note that replacements will not be MineDown-escaped; use {@link #escapeText(String)} to escape replacements
     *
     * @param localeId     String identifier of the locale, corresponding to a key in the file
     * @param replacements Ordered array of replacement strings to fill in placeholders with
     * @return An {@link Optional} containing the replacement-applied locale corresponding to the id, if it exists
     */
    public Optional<String> getRawLocale(@NotNull String localeId, @NotNull String... replacements) {
        return getRawLocale(localeId).map(locale -> applyReplacements(locale, replacements));
    }

    /**
     * Returns a MineDown-formatted locale from the locales file
     *
     * @param localeId String identifier of the locale, corresponding to a key in the file
     * @return An {@link Optional} containing the formatted locale corresponding to the id, if it exists
     */
    public Optional<MineDown> getLocale(@NotNull String localeId) {
        return getRawLocale(localeId).map(this::format);
    }

    /**
     * Returns a MineDown-formatted locale from the locales file, with replacements applied
     * <p>
     * Note that replacements will be MineDown-escaped before application
     *
     * @param localeId     String identifier of the locale, corresponding to a key in the file
     * @param replacements Ordered array of replacement strings to fill in placeholders with
     * @return An {@link Optional} containing the replacement-applied, formatted locale corresponding to the id, if it exists
     */
    public Optional<MineDown> getLocale(@NotNull String localeId, @NotNull String... replacements) {
        return getRawLocale(localeId, Arrays.stream(replacements).map(Locales::escapeText)
                .toArray(String[]::new)).map(this::format);
    }

    /**
     * Returns a MineDown-formatted string
     *
     * @param text The text to format
     * @return A {@link MineDown} object containing the formatted text
     */
    @NotNull
    public MineDown format(@NotNull String text) {
        return new MineDown(text);
    }

    /**
     * Apply placeholder replacements to a raw locale
     *
     * @param rawLocale    The raw, unparsed locale
     * @param replacements Ordered array of replacement strings to fill in placeholders with
     * @return the raw locale, with inserted placeholders
     */
    @NotNull
    private String applyReplacements(@NotNull String rawLocale, @NotNull String... replacements) {
        int replacementIndexer = 1;
        for (String replacement : replacements) {
            String replacementString = "%" + replacementIndexer + "%";
            rawLocale = rawLocale.replace(replacementString, replacement);
            replacementIndexer += 1;
        }
        return rawLocale;
    }

    /**
     * Escape a string from {@link MineDown} formatting for use in a MineDown-formatted locale
     *
     * @param string The string to escape
     * @return The escaped string
     */
    @NotNull
    public static String escapeText(@NotNull String string) {
        final StringBuilder value = new StringBuilder();
        for (int i = 0; i < string.length(); ++i) {
            char c = string.charAt(i);
            boolean isEscape = c == '\\';
            boolean isColorCode = i + 1 < string.length() && (c == 167 || c == '&');
            boolean isEvent = c == '[' || c == ']' || c == '(' || c == ')';
            if (isEscape || isColorCode || isEvent) {
                value.append('\\');
            }

            value.append(c);
        }
        return value.toString().replace("__", "_\\_");
    }


    @NotNull
    public String getPositionText(@NotNull BlockPosition position, int yPosition, @NotNull ServerWorld serverWorld,
                                  @NotNull CommandUser user, @NotNull HuskClaims plugin) {
        final boolean crossServer = plugin.getSettings().getCrossServer().isEnabled();
        return plugin.getLocales().getRawLocale(
                switch (serverWorld.world().getEnvironment().toLowerCase(Locale.ENGLISH)) {
                    case "the_nether", "nether" -> "position_nether";
                    case "the_end", "end" -> "position_end";
                    default -> "position_overworld";
                },
                crossServer ? serverWorld.toString() : serverWorld.world().getName(),
                Integer.toString(position.getBlockX()),
                Integer.toString(position.getBlockZ()),
                getRawLocale(
                        "position_%sworld_tooltip".formatted(!crossServer ? "" : "server_")
                ).orElse(""),
                user instanceof OnlineUser online ? getTeleportText(
                        position, yPosition, serverWorld, online, plugin
                ) : ""
        ).orElse("");
    }


    @NotNull
    private String getTeleportText(@NotNull BlockPosition position, int yPosition, @NotNull ServerWorld serverWorld,
                                   @NotNull CommandUser user, @NotNull HuskClaims plugin) {
        final Optional<HuskHomesHook> homesHook = plugin.getHook(HuskHomesHook.class);
        if (homesHook.isEmpty() || !plugin.canUseCommand(HuskClaimsCommand.class, user, "teleport")) {
            return "";
        }
        return plugin.getHook(HuskHomesHook.class).map(hook -> String.format(
                "%s run_command=/huskclaims teleport %s %s %s %s %s",
                getRawLocale("position_teleport_tooltip").orElse(""),
                serverWorld.server(),
                position.getBlockX(),
                yPosition,
                position.getBlockZ(),
                serverWorld.world().getName()
        )).orElse("");
    }

    @NotNull
    public String truncateText(@NotNull String string, int truncateAfter) {
        if (string.isBlank()) {
            return string;
        }
        return string.length() > truncateAfter ? string.substring(0, truncateAfter) + "…" : string;
    }

    @NotNull
    public String getNotApplicable() {
        return getRawLocale("not_applicable").orElse("N/A");
    }

    @NotNull
    public String getListJoiner() {
        return getRawLocale("list_separator").orElse(", ");
    }

    @NotNull
    public String getNone() {
        return getRawLocale("none").orElse("(none)");
    }

}
