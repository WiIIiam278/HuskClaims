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
import java.util.function.Function;

public interface UserManager {

    String HOURLY_BLOCKS_PERMISSION = "huskclaims.hourly_blocks.";

    @NotNull
    ConcurrentMap<UUID, SavedUser> getUserCache();

    default void invalidateUserCache(@NotNull UUID uuid) {
        getUserCache().remove(uuid);
    }

    @Blocking
    default Optional<SavedUser> getSavedUser(@NotNull UUID uuid) {
        return Optional.ofNullable(getUserCache().get(uuid)).or(
                () -> getPlugin().getDatabase().getUser(uuid).map(data -> getUserCache().put(uuid, data))
        );
    }

    @Blocking
    default Optional<Preferences> getUserPreferences(@NotNull UUID uuid) {
        return getSavedUser(uuid).map(SavedUser::getPreferences);
    }

    @Blocking
    default Optional<Long> getClaimBlocks(@NotNull UUID uuid) {
        return getSavedUser(uuid).map(SavedUser::getClaimBlocks);
    }

    default long getClaimBlocks(@NotNull User user) {
        return getClaimBlocks(user.getUuid()).orElseThrow(
                () -> new IllegalArgumentException("Could not find claim blocks for user: " + user)
        );
    }

    @Blocking
    default void editUser(@NotNull UUID uuid, @NotNull Consumer<SavedUser> consumer) {
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
        editUser(user.getUuid(), savedUser -> consumer.accept(savedUser.getPreferences()));
    }

    @Blocking
    default void editClaimBlocks(@NotNull User user, @NotNull Function<Long, Long> consumer) {
        editUser(user.getUuid(), savedUser -> {
            final long newClaimBlocks = consumer.apply(savedUser.getClaimBlocks());
            if (newClaimBlocks < 0) {
                throw new IllegalArgumentException("Claim blocks cannot be negative");
            }
            savedUser.setClaimBlocks(newClaimBlocks);
        });
    }

    @Blocking
    default void grantHourlyClaimBlocks(@NotNull OnlineUser user) {
        final long hourlyBlocks = user.getNumericalPermission(HOURLY_BLOCKS_PERMISSION)
                .orElse(getPlugin().getSettings().getClaims().getHourlyClaimBlocks());
        if (hourlyBlocks <= 0) {
            return;
        }
        editClaimBlocks(user, blocks -> blocks + hourlyBlocks);
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
