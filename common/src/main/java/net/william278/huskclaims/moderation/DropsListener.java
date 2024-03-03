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
import net.william278.huskclaims.config.Settings;
import net.william278.huskclaims.user.OnlineUser;
import net.william278.huskclaims.user.User;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Optional;

public interface DropsListener {

    default void handleDeathDrops(@NotNull OnlineUser user, @NotNull Collection<? extends DropsProtector.GroundItem> items) {
        if (!getSettings().isLockItems()) {
            return;
        }
        getPlugin().lockDrops(items, user);
        getPlugin().getLocales().getLocale("death_drops_locked")
                .ifPresent(user::sendMessage);
    }

    default boolean cancelItemPickup(@Nullable OnlineUser pickerUpper, @NotNull DropsProtector.GroundItem item) {
        if (!getSettings().isLockItems() || pickerUpper == null) {
            return false;
        }
        final Optional<User> locker = item.getLockedBy(getPlugin());
        if (locker.isPresent() && locker.get().equals(pickerUpper)) {
            item.unlock(getPlugin());
            return false;
        }
        return true;
    }

    default boolean cancelItemDestroy(@NotNull DropsProtector.GroundItem item) {
        if (!getSettings().isLockItems() || !getSettings().isPreventDestruction()) {
            return false;
        }
        return item.isLocked(getPlugin());
    }

    @NotNull
    default Settings.ModerationSettings.DropSettings getSettings() {
        return getPlugin().getSettings().getModeration().getDrops();
    }

    @NotNull
    HuskClaims getPlugin();

}
