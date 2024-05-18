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

package net.william278.huskclaims.claim;

import com.google.common.collect.Sets;
import com.google.gson.*;
import lombok.RequiredArgsConstructor;
import net.william278.cloplib.operation.OperationType;
import net.william278.huskclaims.HuskClaims;

import java.lang.reflect.Type;
import java.util.Set;
import java.util.UUID;

@RequiredArgsConstructor
public class ClaimWorldSerializer implements JsonSerializer<ClaimWorld>, JsonDeserializer<ClaimWorld> {

    private final HuskClaims plugin;

    @Override
    public JsonElement serialize(ClaimWorld claimWorld, Type type, JsonSerializationContext jsonSerializationContext) {
        final JsonObject jsonObject = new JsonObject();
        final JsonArray claimsArray = new JsonArray();
        claimWorld.getAllClaims().forEach(c -> claimsArray.add(plugin.getGson().toJsonTree(c, Claim.class)));
        jsonObject.add("claims", claimsArray);

        final JsonObject userCache = new JsonObject();
        claimWorld.getUserCache().forEach((uuid, user) -> userCache.addProperty(uuid.toString(), user));
        jsonObject.add("user_cache", userCache);

        final JsonArray wildernessFlags = new JsonArray();
        claimWorld.getWildernessFlags().forEach(w -> wildernessFlags.add(w.toString()));
        jsonObject.add("wilderness_flags", wildernessFlags);

        jsonObject.add("schema_version", new JsonPrimitive(claimWorld.getSchemaVersion()));

        return jsonObject;
    }

    @Override
    public ClaimWorld deserialize(JsonElement jsonElement, Type type, JsonDeserializationContext jsonDeserializationContext) throws JsonParseException {
        final JsonObject jsonObject = jsonElement.getAsJsonObject();
        final ClaimWorld claimWorld = new ClaimWorld();
        final Set<Claim> claims = Sets.newHashSet();
        final JsonArray claimsArray = jsonObject.has("claims") ? jsonObject.getAsJsonArray("claims") : new JsonArray();
        claimsArray.forEach(claimElement -> claims.add(plugin.getGson().fromJson(claimElement, Claim.class)));
        claimWorld.loadClaims(claims);

        final JsonObject userCache = jsonObject.has("user_cache") ? jsonObject.getAsJsonObject("user_cache") : new JsonObject();
        userCache.entrySet().forEach(entry -> claimWorld.getUserCache().put(UUID.fromString(entry.getKey()), entry.getValue().getAsString()));

        final JsonArray wildernessFlags = jsonObject.has("wilderness_flags") ? jsonObject.getAsJsonArray("wilderness_flags") : new JsonArray();
        wildernessFlags.forEach(w -> claimWorld.getWildernessFlags().add(OperationType.valueOf(w.getAsString())));

        claimWorld.setSchemaVersion(jsonObject.has("schema_version") ? jsonObject.get("schema_version").getAsInt() : 1);
        return claimWorld;
    }
}
