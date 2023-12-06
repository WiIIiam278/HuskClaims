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

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.william278.huskclaims.user.User;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class Payload {

    @Nullable
    @Expose
    private UUID uuid;
    @Nullable
    @Expose
    private Integer integer;
    @Nullable
    @Expose
    private Boolean bool;
    @Nullable
    @Expose
    private String string;
    @Nullable
    @Expose
    @SerializedName("user_list")
    private List<User> userList;

    @NotNull
    public static Payload empty() {
        return new Payload();
    }

    @NotNull
    public static Payload uuid(@NotNull UUID uuid) {
        final Payload payload = new Payload();
        payload.uuid = uuid;
        return payload;
    }

    @NotNull
    public static Payload integer(int integer) {
        final Payload payload = new Payload();
        payload.integer = integer;
        return payload;
    }

    @NotNull
    public static Payload bool(boolean bool) {
        final Payload payload = new Payload();
        payload.bool = bool;
        return payload;
    }

    @NotNull
    public static Payload string(@NotNull String string) {
        final Payload payload = new Payload();
        payload.string = string;
        return payload;
    }

    @NotNull
    public static Payload userList(@NotNull List<User> list) {
        final Payload payload = new Payload();
        payload.userList = list;
        return payload;
    }

    public Optional<UUID> getUuid() {
        return Optional.ofNullable(uuid);
    }

    public Optional<Integer> getInteger() {
        return Optional.ofNullable(integer);
    }

    public Optional<Boolean> getBool() {
        return Optional.ofNullable(bool);
    }

    public Optional<String> getString() {
        return Optional.ofNullable(string);
    }

    public Optional<List<User>> getUserList() {
        return Optional.ofNullable(userList);
    }

}
