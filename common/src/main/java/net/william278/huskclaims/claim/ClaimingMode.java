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
import net.william278.huskclaims.user.CommandUser;
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
    CLAIMS(List.of("claim"), "huskclaims.claim", false),
    CHILD_CLAIMS(List.of("childclaim", "subdivideclaims"), "huskclaims.child_claim", false),
    ADMIN_CLAIMS(List.of("adminclaim"), "huskclaims.admin_claim", true);

    // Command aliases
    private final List<String> commandAliases;

    // Permission required to create & resize
    private final String usePermission;

    // Should this claiming mode restricted to operators by default
    private final boolean adminRequired;

    @NotNull
    public String getDisplayName(@NotNull Locales locales) {
        return locales.getRawLocale(String.format("claiming_mode_%s", getId())).orElse(getId());
    }

    public boolean canUse(@NotNull CommandUser user) {
        return user.hasPermission(usePermission);
    }

    @NotNull
    private String getId() {
        return name().toLowerCase(Locale.ENGLISH);
    }

}
