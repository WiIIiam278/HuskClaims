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

package net.william278.huskclaims.trust;

import net.william278.huskclaims.HuskClaims;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;
import java.util.Set;
import java.util.logging.Level;

/**
 * Interface for managing {@link TrustedTag}s &mdash; abstract tags for restricting trust levels
 *
 * @since 1.0
 */
public interface TrustedTagManager {

    @NotNull
    Set<TrustedTag> getTrustedTags();

    default void loadTrustedTags() {
        unloadTrustedTags();
        registerTrustedTag(PublicTrustedTag.create(getPlugin()));
    }

    default void unloadTrustedTags() {
        getTrustedTags().clear();
    }

    default void registerTrustedTag(@NotNull TrustedTag trustedTag) throws IllegalArgumentException {
        if (getTrustedTag(trustedTag.getName()).isPresent()) {
            throw new IllegalArgumentException("Trusted tag '" + trustedTag.getName() + "' is already registered");
        }
        if (!getPlugin().isValidGroupOrTagName(trustedTag.getName())) {
            throw new IllegalArgumentException("Trusted tag '" + trustedTag.getName() + "' has an invalid name!");
        }
        getPlugin().log(Level.INFO, "Registering trusted tag '" + trustedTag.getName() + "'");
        getTrustedTags().add(trustedTag);
    }

    default Optional<TrustedTag> getTrustedTag(@NotNull String tag) {
        return getTrustedTags().stream().filter(trustedTag -> trustedTag.getName().equalsIgnoreCase(tag)).findFirst();
    }

    @NotNull
    HuskClaims getPlugin();

}
