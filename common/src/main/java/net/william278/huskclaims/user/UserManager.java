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

import net.william278.huskclaims.HuskClaims;
import net.william278.huskclaims.network.Message;
import net.william278.huskclaims.network.Payload;
import org.jetbrains.annotations.Blocking;
import org.jetbrains.annotations.NotNull;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Consumer;

public interface UserManager extends ClaimBlocksManager {

    @NotNull
    ConcurrentMap<UUID, SavedUser> getUserCache();

    default void invalidateUserCache(@NotNull UUID uuid) {
        getUserCache().remove(uuid);
    }

    @Blocking
    default Optional<SavedUser> getSavedUser(@NotNull UUID uuid) {
        return Optional.ofNullable(getUserCache().getOrDefault(
                uuid, getPlugin().getDatabase().getUser(uuid).orElse(null)
        ));
    }

    @Blocking
    default Optional<Preferences> getUserPreferences(@NotNull UUID uuid) {
        return getSavedUser(uuid).map(SavedUser::getPreferences);
    }

    @Blocking
    default void editSavedUser(@NotNull UUID uuid, @NotNull Consumer<SavedUser> consumer) {
        final Optional<SavedUser> optionalUser = getSavedUser(uuid);
        if (optionalUser.isEmpty()) {
            throw new IllegalArgumentException("Could not find user with UUID: " + uuid);
        }

        final SavedUser user = optionalUser.get();
        consumer.accept(user);
        getUserCache().put(uuid, user);
        getPlugin().getDatabase().updateUser(user);
        getPlugin().getBroker().ifPresent(broker -> getPlugin().getOnlineUsers().stream().findAny().ifPresent(
                sender -> Message.builder()
                        .type(Message.MessageType.INVALIDATE_USER_CACHE)
                        .payload(Payload.uuid(uuid))
                        .build()
                        .send(broker, sender))
        );
    }

    @Blocking
    default void editUserPreferences(@NotNull User user, @NotNull Consumer<Preferences> consumer) {
        editSavedUser(user.getUuid(), savedUser -> consumer.accept(savedUser.getPreferences()));
    }

    @Blocking
    default void loadUserData(@NotNull User user) {
        getPlugin().getDatabase().getUser(user.getUuid()).ifPresentOrElse(
                data -> getUserCache().put(user.getUuid(), data),
                () -> {
                    final Preferences defaults = Preferences.DEFAULTS;
                    final long defaultClaimBlocks = getPlugin().getSettings().getClaims().getStartingClaimBlocks();
                    getPlugin().getDatabase().createUser(user, defaultClaimBlocks, defaults);
                    getUserCache().put(user.getUuid(), new SavedUser(
                            user,
                            defaults,
                            OffsetDateTime.now(),
                            defaultClaimBlocks,
                            0
                    ));
                });
    }

    @NotNull
    HuskClaims getPlugin();

}
