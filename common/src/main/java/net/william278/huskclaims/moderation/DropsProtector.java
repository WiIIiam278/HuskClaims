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

import net.william278.huskclaims.HuskClaims;
import net.william278.huskclaims.user.User;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Optional;
import java.util.Set;

public interface DropsProtector {

    @NotNull
    Set<GroundItem> getTrackedDrops();

    default void lockDrops(@NotNull Collection<? extends GroundItem> drops, @NotNull User lockedBy) {
        drops.stream()
                .peek(d -> d.setLocked(lockedBy, getPlugin()))
                .forEach(getTrackedDrops()::add);
    }

    default long unlockDrops(@NotNull User user) {
        return getTrackedDrops().stream()
                .filter(a -> a.isLockedBy(user, getPlugin()))
                .peek(g -> g.unlock(getPlugin()))
                .peek(getTrackedDrops()::remove)
                .count();
    }

    @NotNull
    HuskClaims getPlugin();

    interface GroundItem {

        Optional<User> getLockedBy(@NotNull HuskClaims plugin);

        default boolean isLocked(@NotNull HuskClaims plugin) {
            return getLockedBy(plugin).isPresent();
        }

        void setLocked(@Nullable User user, @NotNull HuskClaims plugin);

        default void unlock(@NotNull HuskClaims plugin) {
            setLocked(null, plugin);
        }

        default boolean isLockedBy(@NotNull User user, @NotNull HuskClaims plugin) {
            return getLockedBy(plugin).isPresent() && getLockedBy(plugin).get().equals(user);
        }

    }

}
