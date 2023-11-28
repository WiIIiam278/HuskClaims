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

import net.william278.huskclaims.user.OnlineUser;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.List;

/**
 * A highlighter for claims - used to highlight a claim to a user in-game
 *
 
 * @since 1.0
 */
public interface ClaimHighlighter {

    /**
     * Highlight claim(s) for a user
     *
     * @param user  The user to visualize the claim to
     * @param claim A collection of claims to visualize
     * @since 1.0
     */
    void highlightClaim(@NotNull OnlineUser user, @NotNull Collection<Claim> claims);

    /**
     * Highlight claim(s) for a user
     *
     * @param user   The user to visualize the claim to
     * @param claims The claim(s) to visualize
     * @since 1.0
     */
    default void highlightClaim(@NotNull OnlineUser user, @NotNull Claim... claims) {
        this.highlightClaim(user, List.of(claims));
    }

}
