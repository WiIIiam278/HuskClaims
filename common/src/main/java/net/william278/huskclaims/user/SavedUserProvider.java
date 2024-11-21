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

public interface SavedUserProvider extends ClaimBlocksManager {

    @NotNull
    ConcurrentMap<UUID, SavedUser> getUserCache();

    default void invalidateSavedUserCache(@NotNull UUID uuid) {
        getPlugin().runAsync(() -> getPlugin().getDatabase().getUser(uuid)
                .ifPresent(saved -> getUserCache().put(uuid, saved)));
    }

    @Blocking
    default Optional<SavedUser> getSavedUser(@NotNull UUID uuid) {
        return Optional.ofNullable(getUserCache().get(uuid));
    }

    @Blocking
    default Optional<Preferences> getUserPreferences(@NotNull UUID uuid) {
        return getSavedUser(uuid).map(SavedUser::getPreferences);
    }

    @Blocking
    default void editSavedUser(@NotNull UUID uuid, @NotNull Consumer<SavedUser> consumer, @NotNull Runnable ifEmpty) {
        getSavedUser(uuid).ifPresentOrElse(user -> editSavedUser(user, consumer), ifEmpty);
    }

    default void editSavedUser(@NotNull UUID uuid, @NotNull Consumer<SavedUser> consumer) {
        final Optional<SavedUser> saved = getSavedUser(uuid);
        if (saved.isEmpty()) {
            throw new IllegalArgumentException("Failed to find user by UUID %s in cache or database!".formatted(uuid));
        }
        editSavedUser(saved.get(), consumer);
    }

    private void editSavedUser(@NotNull SavedUser user, @NotNull Consumer<SavedUser> consumer) {
        final UUID uuid = user.getUser().getUuid();
        consumer.accept(user);
        getUserCache().put(uuid, user);
        getPlugin().runQueued(() -> {
            getPlugin().getDatabase().updateUser(user);
            getPlugin().getBroker().ifPresent(broker -> getPlugin().getOnlineUsers().stream().findAny().ifPresent(
                    sender -> Message.builder()
                            .type(Message.MessageType.INVALIDATE_USER_CACHE)
                            .payload(Payload.uuid(uuid))
                            .target(Message.TARGET_ALL, Message.TargetType.SERVER).build()
                            .send(broker, sender))
            );
        });
    }

    @Blocking
    default void editUserPreferences(@NotNull User user, @NotNull Consumer<Preferences> consumer) {
        this.editSavedUser(user.getUuid(), savedUser -> consumer.accept(savedUser.getPreferences()));
    }

    @Blocking
    default void cacheSavedUser(@NotNull User user) {
        // Get the user object, or create a new one if they don't exist
        final SavedUser savedUser = getPlugin().getDatabase()
                .getUser(user.getUuid()).map(saved -> {
                    saved.setLastLogin(OffsetDateTime.now());
                    saved.getUser().setName(user.getName());
                    return saved;
                })
                .orElse(SavedUser.createNew(user, getPlugin()));

        // Update the cache and database (creating them if they don't exist)
        getUserCache().remove(user.getUuid());
        getPlugin().getDatabase().createOrUpdateUser(savedUser);
        getUserCache().put(user.getUuid(), savedUser);
    }

    @NotNull
    HuskClaims getPlugin();

}
