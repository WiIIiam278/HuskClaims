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

package net.william278.huskclaims.util;

import net.william278.huskclaims.HuskClaims;
import net.william278.huskclaims.config.Settings;
import org.jetbrains.annotations.NotNull;

public interface TextValidator {

    int MAX_GROUP_NAME_LENGTH = 32;

    // Check a group name contains only valid characters
    default boolean isValidGroupName(@NotNull String name) {
        return (name.matches(getSettings().getUserGroups().getGroupNameRegex())
                || !getSettings().getUserGroups().isRestrictGroupNames())
                && !name.contains("\u0000")
                && isValidNameLength(name)
                && !containsWhitespace(name)
                && !name.contains(getSettings().getUserGroups().getGroupSpecifierPrefix());
    }

    // Check a group name is of a valid length
    private boolean isValidNameLength(@NotNull String name) {
        return name.length() <= MAX_GROUP_NAME_LENGTH && !name.isEmpty();
    }

    // Check if a string contains whitespace
    private boolean containsWhitespace(@NotNull String string) {
        return string.matches(".*\\s.*");
    }

    @NotNull
    HuskClaims getPlugin();

    @NotNull
    Settings getSettings();

}
