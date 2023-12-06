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

package net.william278.huskclaims.user;

import net.william278.huskclaims.config.Settings;
import net.william278.huskclaims.database.Database;
import org.jetbrains.annotations.Blocking;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Consumer;
import java.util.function.Function;

public interface UserManager {

    @NotNull
    ConcurrentHashMap<UUID, Preferences> getUserPreferences();

    default void setUserPreferences(@NotNull UUID uuid, @NotNull Preferences preferences) {
        getUserPreferences().put(uuid, preferences);
    }

    default Optional<Preferences> getUserPreferences(@NotNull UUID uuid) {
        return Optional.ofNullable(getUserPreferences().get(uuid));
    }

    @Blocking
    default void editUserPreferences(@NotNull User user, @NotNull Consumer<Preferences> consumer) {
        final Optional<Preferences> optionalPreferences = getUserPreferences(user.getUuid());
        if (optionalPreferences.isEmpty()) {
            return;
        }
        final Preferences preferences = optionalPreferences.get();
        consumer.accept(preferences);
        setUserPreferences(user.getUuid(), preferences);
        getDatabase().updateUserPreferences(user, preferences);
    }

    @NotNull
    ConcurrentMap<UUID, Long> getClaimBlocks();

    default void setClaimBlocks(@NotNull UUID uuid, long claimBlocks) {
        getClaimBlocks().put(uuid, claimBlocks);
    }

    default long getClaimBlocks(@NotNull UUID uuid) throws IllegalArgumentException {
        if (!getClaimBlocks().containsKey(uuid)) {
            throw new IllegalArgumentException("No claim blocks found for UUID " + uuid);
        }
        return getClaimBlocks().get(uuid);
    }

    @Blocking
    default void editClaimBlocks(@NotNull User user, @NotNull Function<Long, Long> consumer)
            throws IllegalArgumentException {
        final long claimBlocks = consumer.apply(getClaimBlocks(user.getUuid()));
        setClaimBlocks(user.getUuid(), claimBlocks);
        getDatabase().updateUserClaimBlocks(user, claimBlocks);
    }

    @Blocking
    default void loadUserData(@NotNull User user) {
        getDatabase().getUser(user.getUuid()).ifPresentOrElse(data -> {
            setUserPreferences(user.getUuid(), data.preferences());
            setClaimBlocks(user.getUuid(), data.claimBlocks());
        }, () -> {
            final Preferences defaults = Preferences.DEFAULTS;
            final long defaultClaimBlocks = getSettings().getClaims().getStartingClaimBlocks();
            getDatabase().createUser(user, defaultClaimBlocks, defaults);
            setUserPreferences(user.getUuid(), defaults);
            setClaimBlocks(user.getUuid(), defaultClaimBlocks);
        });
    }

    @NotNull
    Database getDatabase();

    @NotNull
    Settings getSettings();

}
