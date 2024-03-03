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

package net.william278.huskclaims.moderation;

import com.google.common.collect.Lists;
import net.william278.huskclaims.HuskClaims;
import net.william278.huskclaims.config.Settings;
import net.william278.huskclaims.user.OnlineUser;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Optional;
import java.util.StringJoiner;
import java.util.regex.Pattern;

/**
 * Interface for handling sign edits / placements, for moderation.
 *
 * @since 1.1
 */
public interface SignListener {


    /**
     * Handles the editing of a sign
     *
     * @param edit the sign edit
     * @return the processed sign edit
     */
    @NotNull
    default SignWrite handleSignEdit(@NotNull SignWrite edit) {
        filterSignText(edit.getText()).ifPresent(e -> {
            edit.setFilteredText(e);
            getPlugin().getLocales().getLocale("sign_filtered")
                    .ifPresent(l -> ((OnlineUser) edit.getEditor()).sendMessage(l));
        });
        getPlugin().notifyAllSignModerators(edit);
        return edit;
    }

    // Filter sign text against regex
    private Optional<List<String>> filterSignText(@NotNull List<String> text) {
        final Settings.ModerationSettings.SignSettings signs = getPlugin().getSettings().getModeration().getSigns();
        if (!signs.isFilterMessages() || signs.getFilteredWords().isEmpty()) {
            return Optional.empty();
        }

        boolean isFiltered = false;
        final List<String> filtered = Lists.newArrayList();
        for (String line : text) {
            final String afterFilter = getFiltered(line, signs.getFilteredWords(), signs.getReplacementCharacter());
            filtered.add(afterFilter);
            isFiltered = isFiltered || !afterFilter.equals(line);
        }
        return isFiltered ? Optional.of(filtered) : Optional.empty();
    }

    @NotNull
    private String getFiltered(@NotNull String line, @NotNull List<String> words, char replaceWith) {
        return Pattern.compile(getFiltered(words), Pattern.CASE_INSENSITIVE).matcher(line)
                .replaceAll(String.valueOf(replaceWith));
    }

    @NotNull
    static String getFiltered(@NotNull List<String> illegal) {
        final StringJoiner pattern = new StringJoiner("|");
        illegal.forEach(word -> pattern.add(
                String.format("(?<=(?=%s).{0,%d}).", Pattern.quote(word), word.length() - 1)
        ));
        return pattern.toString();
    }

    @NotNull
    HuskClaims getPlugin();

}
