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

package net.william278.huskclaims.util;

import net.william278.huskclaims.HuskClaims;
import net.william278.huskclaims.user.User;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

/**
 * A provider for the plugin user list, tracking online users across the network
 *
 * @since 1.0
 */
public interface UserListProvider {

    @NotNull
    Map<String, List<User>> getGlobalUserList();

    @NotNull
    default List<User> getUserList() {
        return Stream.concat(
                getGlobalUserList().values().stream().flatMap(Collection::stream),
                getPlugin().getOnlineUsers().stream()
        ).distinct().sorted().toList();
    }

    default void setUserList(@NotNull String server, @NotNull List<User> players) {
        getGlobalUserList().values().forEach(list -> {
            list.removeAll(players);
            list.removeAll(getPlugin().getOnlineUsers());
        });
        getGlobalUserList().put(server, players);
    }

    default boolean isUserOnline(@NotNull User user) {
        return getUserList().contains(user);
    }

    @NotNull
    HuskClaims getPlugin();

}
