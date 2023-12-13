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

package net.william278.huskclaims.claim;

import lombok.AllArgsConstructor;
import lombok.Getter;
import net.william278.huskclaims.config.Locales;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Locale;

/**
 * Types of claim selection modes
 *
 * @since 1.0
 */
@Getter
@AllArgsConstructor
public enum ClaimingMode {
    CLAIMS(List.of("claim")),
    CHILD_CLAIMS(List.of("childclaim", "subdivideclaims")),
    ADMIN_CLAIMS(List.of("adminclaim"));

    private List<String> commandAliases;

    @NotNull
    public String getDisplayName(@NotNull Locales locales) {
        return locales.getRawLocale(String.format("claiming_mode_%s", getId())).orElse(getId());
    }

    @NotNull
    private String getId() {
        return name().toLowerCase(Locale.ENGLISH);
    }
}
