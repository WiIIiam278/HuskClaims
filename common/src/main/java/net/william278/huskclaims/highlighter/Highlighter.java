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

import net.william278.huskclaims.claim.ClaimWorld;
import net.william278.huskclaims.position.World;
import net.william278.huskclaims.user.OnlineUser;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.List;

/**
 * A highlighter for claims - used to highlight {@link Highlightable}s to a user in-game
 *
 * @since 1.0
 */
public interface Highlighter {

    /**
     * Highlight something for a user
     *
     * @param user        The user to visualize the highlightable to
     * @param world       The {@link ClaimWorld} the highlightable is in
     * @param toHighlight a collection of {@link Highlightable}s to visualize
     * @param showOverlap whether to highlight overlapping claims
     * @since 1.0
     */
    void startHighlighting(@NotNull OnlineUser user, @NotNull World world,
                           @NotNull Collection<? extends Highlightable> toHighlight,
                           boolean showOverlap);

    /**
     * Clear highlights for a user
     *
     * @param user The user to stop visualizing the highlightable to
     * @since 1.0
     */
    void stopHighlighting(@NotNull OnlineUser user);

    /**
     * Highlight something for a user
     *
     * @param user        The user to visualize the highlightable to
     * @param world       The {@link ClaimWorld} the highlightable is in
     * @param toHighlight a collection of {@link Highlightable}s to visualize
     * @since 1.0
     */
    default void startHighlighting(@NotNull OnlineUser user, @NotNull World world,
                                   @NotNull Collection<? extends Highlightable> toHighlight) {
        startHighlighting(user, world, toHighlight, false);
    }

    /**
     * Highlight something for a user
     *
     * @param user          The user to visualize the highlightable to
     * @param world         The {@link ClaimWorld} the highlightable is in
     * @param highlightable The {@link Highlightable} to visualize
     * @since 1.0
     */
    default void startHighlighting(@NotNull OnlineUser user, @NotNull World world,
                                   @NotNull Highlightable highlightable, boolean showOverlap) {
        startHighlighting(user, world, List.of(highlightable), showOverlap);
    }

    /**
     * Highlight something for a user
     *
     * @param user          The user to visualize the highlightable to
     * @param world         The {@link ClaimWorld} the highlightable is in
     * @param highlightable The {@link Highlightable} to visualize
     * @since 1.0
     */
    default void startHighlighting(@NotNull OnlineUser user, @NotNull World world,
                                   @NotNull Highlightable highlightable) {
        startHighlighting(user, world, highlightable, false);
    }

    /**
     * Returns {@code true} if this highlighter can be used. Override this to return {@code false} in cases where the
     * highlighter is not compatible with this user (e.g. Highlighters that won't work for Bedrock players on Geyser)
     *
     * @param user {@link OnlineUser} to check
     * @return {@code true} if this highlighter can be used with a {@link OnlineUser user}; {@code false} otherwise.
     * @since 1.4.3
     */
    default boolean canUse(@NotNull OnlineUser user) {
        return true;
    }

    /**
     * Returns the priority {@code short} of this highlighter used for determining which highlighter will be used.
     * <p>
     * Higher values mean that highlighter takes priority, unless that highlighter
     * {@link #canUse(OnlineUser) cannot be used} by that player.
     *
     * @return a {@code short} value from {@code -32768} to {@code 32767}.
     * @since 1.4.3
     */
    default short getPriority() {
        return 10;
    }

}
