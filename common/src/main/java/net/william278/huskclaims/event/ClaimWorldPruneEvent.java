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

package net.william278.huskclaims.event;

import net.william278.huskclaims.user.User;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Unmodifiable;

import java.util.Map;
import java.util.Set;

/**
 * Event for when a world is pruned of claims.
 *
 * @since 1.3
 */
public interface ClaimWorldPruneEvent extends CancellableEvent, ClaimWorldEvent {

    /**
     * Get the map of users whose claims are to be pruned, to the number of claim blocks worth of claims to be pruned
     * <p>
     * You may modify this map to, for example, remove users from the prune list
     *
     * @return The user prune map
     * @since 1.3
     */
    @NotNull
    Map<User, Long> getUserBlocksMap();

    /**
     * Get the users whose claims are to be pruned
     *
     * @return The users to prune
     * @since 1.3
     */
    @NotNull
    @Unmodifiable
    default Set<User> getUsersToPrune() {
        return getUserBlocksMap().keySet();
    }

    /**
     * Get if a user's claims are being pruned in this world
     *
     * @param user The user to check
     * @return If the user is being pruned
     * @since 1.3
     */
    default boolean isUserBeingPruned(@NotNull User user) {
        return getUserBlocksMap().containsKey(user);
    }

    /**
     * Remove a user from the prune list
     *
     * @param user The user to remove
     * @since 1.3
     */
    default void removeUserFromPruneList(@NotNull User user) {
        getUserBlocksMap().remove(user);
    }

}
