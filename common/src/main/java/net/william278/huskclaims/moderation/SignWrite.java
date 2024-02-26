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

import lombok.*;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import net.william278.huskclaims.config.Locales;
import net.william278.huskclaims.position.Position;
import net.william278.huskclaims.user.User;
import org.apache.commons.text.WordUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.List;

@Getter
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class SignWrite {

    @NotNull
    private User editor;
    @NotNull
    private Position position;
    @NotNull
    private String server;
    @NotNull
    private Type type;
    @NotNull
    private List<String> text;
    @Nullable
    @Setter
    private List<String> filteredText = null;

    @NotNull
    public static SignWrite create(@NotNull User editor, @NotNull Position position, @NotNull Type type,
                                   @NotNull List<Component> lines, @NotNull String server) {
        return new SignWrite(editor, position, server, type, joinLines(lines));
    }

    @NotNull
    public static SignWrite create(@NotNull User editor, @NotNull Position position, @NotNull Type type,
                                   @NotNull String[] lines, @NotNull String server) {
        return new SignWrite(editor, position, server, type, Arrays.stream(lines).toList());
    }

    @NotNull
    private static List<String> joinLines(@NotNull List<Component> lines) {
        return lines.stream()
                .map(line -> PlainTextComponentSerializer.plainText().serialize(line))
                .toList();
    }

    public boolean isFiltered() {
        return filteredText != null;
    }

    @Getter
    @AllArgsConstructor
    public enum Type {
        SIGN_EDIT_FRONT(true, "sign_write_on_front"),
        SIGN_EDIT_BACK(false, "sign_write_on_back");

        private final boolean front;
        private final String locale;

        @NotNull
        public String getLocale(@NotNull Locales locales, @NotNull String actorName) {
            return locales.getRawLocale(locale, Locales.escapeText(actorName))
                    .orElse(WordUtils.capitalizeFully(name().replace("_", " ")));
        }
    }

}
