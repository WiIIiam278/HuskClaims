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

package net.william278.huskclaims.highlighter;

import net.william278.huskclaims.HuskClaims;
import net.william278.huskclaims.user.OnlineUser;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public interface HighlighterProvider {

    /**
     * Get the list of available claim highlighters
     *
     * @return the claim {@link Highlighter}
     * @since 1.4.3
     */
    @NotNull
    List<Highlighter> getHighlighters();

    /**
     * Get the cached map of highlighters for each user
     *
     * @return the {@link Highlighter} cache map
     */
    @NotNull
    Map<UUID, Highlighter> getHighlighterCache();

    /**
     * Get the highlighter to use for an {@link OnlineUser}
     *
     * @param user the user using the highlighter
     * @return the {@link Highlighter}
     */
    @NotNull
    default Highlighter getHighlighter(@NotNull OnlineUser user) {
        Highlighter highlighter = getHighlighterCache().get(user.getUuid());
        if (highlighter == null) {
            highlighter = getHighlighters().stream().filter(h -> h.canUse(user)).findFirst().orElseThrow(
                    () -> new IllegalStateException("No available highlighter for %s".formatted(user.getName())));
            getHighlighterCache().put(user.getUuid(), highlighter);
        }
        return highlighter;
    }

    /**
     * Register a claim highlighter
     *
     * @param highlighter the highlighter to register
     * @since 1.4.3
     */
    default void registerHighlighter(@NotNull Highlighter highlighter) {
        getHighlighters().add(highlighter);
        Collections.sort(getHighlighters());
        getHighlighterCache().clear();
    }

    /**
     * Load the built-in claim highlighters
     *
     * @since 1.0
     */
    default void loadHighlighters() {
        registerHighlighter(new BlockUpdateHighlighter(getPlugin()));
    }

    @NotNull
    HuskClaims getPlugin();

}
