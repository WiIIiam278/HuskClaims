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

package net.william278.huskclaims.network;

import net.william278.huskclaims.HuskClaims;
import net.william278.huskclaims.user.OnlineUser;
import net.william278.huskclaims.user.User;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface MessageHandler {

    // Handle inbound user list requests
    default void handleRequestUserList(@NotNull Message message, @Nullable OnlineUser receiver) {
        if (receiver == null) {
            return;
        }

        Message.builder()
                .type(Message.MessageType.UPDATE_USER_LIST)
                .target(message.getSourceServer(), Message.TargetType.SERVER)
                .payload(Payload.userList(getPlugin().getOnlineUsers().stream().map(online -> (User) online).toList()))
                .build().send(getBroker(), receiver);
    }

    // Handle inbound user list updates (returned from requests)
    default void handleUpdateUserList(@NotNull Message message) {
        message.getPayload().getUserList().ifPresent(
                (players) -> getPlugin().setUserList(message.getSourceServer(), players)
        );
    }

    // Handle inbound delete all claims requests
    default void handleDeleteAllClaims(@NotNull Message message) {
        message.getPayload().getUuid().ifPresentOrElse(
                // Delete all claims by a UUID-given user
                (uuid) -> {
                    getPlugin().getSavedUser(uuid).ifPresent(
                            (saved) -> getPlugin().getClaimWorlds().entrySet().stream()
                                    .filter((world) -> world.getValue().removeClaimsBy(saved.getUser()))
                                    .forEach((world) -> getPlugin().getDatabase().updateClaimWorld(world.getValue()))
                    );
                    getPlugin().invalidateUserCache(uuid);
                },
                // Delete all admin claims
                () -> {
                    getPlugin().getClaimWorlds().values().forEach((world) -> {
                        if (!world.getAdminClaims().isEmpty()) {
                            world.getAdminClaims().clear();
                            getPlugin().getDatabase().updateClaimWorld(world);
                        }
                    });
                    getPlugin().invalidateAdminClaimListCache();
                });
    }

    // Handle inbound user group updates
    default void handleInvalidateUserGroups(@NotNull Message message) {
        message.getPayload().getUuid().ifPresent(
                (uuid) -> getPlugin().setUserGroups(uuid, getPlugin().getDatabase().getUserGroups(uuid))
        );
    }

    // Handle inbound user cache invalidation
    default void handleInvalidateUserCache(@NotNull Message message) {
        message.getPayload().getUuid().ifPresent(
                (uuid) -> getPlugin().invalidateUserCache(uuid)
        );
    }

    @NotNull
    Broker getBroker();

    @NotNull
    HuskClaims getPlugin();

}
