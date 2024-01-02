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

import com.google.gson.annotations.Expose;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import net.william278.huskclaims.HuskClaims;
import net.william278.huskclaims.trust.Trustable;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

@Getter
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@AllArgsConstructor(access = AccessLevel.PROTECTED)
public class User implements Trustable, Comparable<User> {

    @Expose
    @NotNull
    private String name;
    @Expose
    @NotNull
    private UUID uuid;

    @NotNull
    public static User of(@NotNull UUID uuid, @NotNull String name) {
        return new User(name, uuid);
    }

    @NotNull
    @Override
    public String getTrustIdentifier(@NotNull HuskClaims plugin) {
        return name;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof User user)) {
            return false;
        }
        return user.getUuid().equals(uuid);
    }

    @Override
    public int compareTo(@NotNull User o) {
        return name.compareTo(o.getName());
    }
}
