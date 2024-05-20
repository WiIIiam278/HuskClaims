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

import com.fatboyindustrial.gsonjavatime.Converters;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;
import net.william278.cloplib.operation.OperationType;
import net.william278.huskclaims.HuskClaims;
import net.william278.huskclaims.claim.*;
import net.william278.huskclaims.network.Message;
import net.william278.huskclaims.user.Preferences;
import net.william278.huskclaims.user.User;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentMap;

/**
 * Interface for providing JSON adaptation utilities via Gson
 *
 * @since 1.0
 */
public interface GsonProvider {

    TypeToken<List<User>> USER_LIST_TOKEN = new TypeToken<>() {
    };
    TypeToken<ConcurrentMap<UUID, String>> UUID_STRING_MAP_TOKEN = new TypeToken<>() {
    };
    TypeToken<ConcurrentMap<String, String>> STRING_STRING_MAP_TOKEN = new TypeToken<>() {
    };
    TypeToken<Set<OperationType>> OPERATION_TYPE_SET_TOKEN = new TypeToken<>() {
    };

    @NotNull
    default GsonBuilder getGsonBuilder() {
        return Converters.registerOffsetDateTime(new GsonBuilder()
                .excludeFieldsWithoutExposeAnnotation()
                .registerTypeAdapter(ClaimWorld.class, new ClaimWorldSerializer(getPlugin()))
                .registerTypeAdapter(Region.class, new RegionSerializer(getPlugin()))
        );
    }

    @NotNull
    default Gson getGson() {
        return getGsonBuilder().create();
    }

    @NotNull
    default ClaimWorld getClaimWorldFromJson(int id, @NotNull String json) throws JsonSyntaxException {
        return ClaimWorldSerializer.upgradeSchema(json, getGson(), getPlugin(), id);
    }

    @NotNull
    default User getUserFromJson(@NotNull String json) throws JsonSyntaxException {
        return getGson().fromJson(json, User.class);
    }

    @NotNull
    default List<User> getUserListFromJson(@NotNull String json) throws JsonSyntaxException {
        return getGson().fromJson(json, USER_LIST_TOKEN);
    }

    @NotNull
    default Preferences getPreferencesFromJson(@NotNull String json) throws JsonSyntaxException {
        return getGson().fromJson(json, Preferences.class);
    }

    @NotNull
    default Message getMessageFromJson(@NotNull String json) throws JsonSyntaxException {
        return getGson().fromJson(json, Message.class);
    }

    @NotNull
    HuskClaims getPlugin();

}
