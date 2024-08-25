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

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.gson.*;
import lombok.RequiredArgsConstructor;
import net.william278.cloplib.operation.OperationType;
import net.william278.huskclaims.HuskClaims;
import net.william278.huskclaims.util.GsonProvider;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Type;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentMap;
import java.util.logging.Level;

@RequiredArgsConstructor
public class ClaimWorldSerializer implements JsonSerializer<ClaimWorld>, JsonDeserializer<ClaimWorld> {

    private final HuskClaims plugin;

    @Override
    @NotNull
    public JsonElement serialize(ClaimWorld claimWorld, Type type, JsonSerializationContext jsonSerializationContext) {
        final JsonObject jsonObject = new JsonObject();
        final JsonArray claimsArray = new JsonArray();
        claimWorld.getClaims().forEach(c -> claimsArray.add(plugin.getGson().toJsonTree(c, Claim.class)));
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
    @NotNull
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

        claimWorld.setSchemaVersion(jsonObject.has("schema_version") ? jsonObject.get("schema_version").getAsInt() : ClaimWorld.CURRENT_SCHEMA);
        return claimWorld;
    }

    @NotNull
    protected static Claim getUpgradableClaim(@NotNull JsonObject co, @NotNull Gson gson) {
        final JsonObject regionObject = co.getAsJsonObject("region");
        final JsonObject nearCornerObject = regionObject.getAsJsonObject("near_corner");
        final JsonObject farCornerObject = regionObject.getAsJsonObject("far_corner");
        final int x1 = nearCornerObject.get("x").getAsInt();
        final int z1 = nearCornerObject.get("z").getAsInt();
        final int x2 = farCornerObject.get("x").getAsInt();
        final int z2 = farCornerObject.get("z").getAsInt();
        final Region.Point nearCorner = Region.Point.at(x1, z1);
        final Region.Point farCorner = Region.Point.at(x2, z2);
        final Region region = Region.from(nearCorner, farCorner);
        final UUID owner = co.has("owner") ? UUID.fromString(co.get("owner").getAsString()) : null;
        final ConcurrentMap<UUID, String> trustedUsers = co.has("trusted_users") ?
                gson.fromJson(co.getAsJsonObject("trusted_users"), GsonProvider.UUID_STRING_MAP_TOKEN) :
                Maps.newConcurrentMap();
        final ConcurrentMap<String, String> trustedGroups = co.has("trusted_groups") ?
                gson.fromJson(co.getAsJsonObject("trusted_groups"), GsonProvider.STRING_STRING_MAP_TOKEN) :
                Maps.newConcurrentMap();
        final ConcurrentMap<String, String> trustedTags = co.has("trusted_tags") ?
                gson.fromJson(co.getAsJsonObject("trusted_tags"), GsonProvider.STRING_STRING_MAP_TOKEN) :
                Maps.newConcurrentMap();
        final JsonArray childrenArray = co.getAsJsonArray("children");
        final Set<Claim> children = Sets.newConcurrentHashSet();
        childrenArray.forEach(c -> children.add(getUpgradableClaim(c.getAsJsonObject(), gson)));
        final Set<OperationType> defaultFlags = gson.fromJson(co.getAsJsonArray("default_flags"),
                GsonProvider.OPERATION_TYPE_SET_TOKEN);
        final boolean inheritParent = co.get("inherit_parent").getAsBoolean();
        return new Claim(owner, region, trustedUsers, trustedGroups, trustedTags, Maps.newConcurrentMap(), children,
                inheritParent, defaultFlags, false);
    }

    @ApiStatus.Internal
    @NotNull
    public static ClaimWorld upgradeSchema(@NotNull String json, @NotNull Gson gson, @NotNull HuskClaims plugin, int id) {
        final JsonObject jsonObject = gson.fromJson(json, JsonObject.class);
        final int schemaVersion = jsonObject.has("schema_version") ? jsonObject.get("schema_version").getAsInt() : 0;
        if (schemaVersion >= ClaimWorld.CURRENT_SCHEMA) {
            return gson.fromJson(json, ClaimWorld.class);
        }

        final Set<Claim> claims = new HashSet<>();
        final JsonArray claimsArray = jsonObject.getAsJsonArray("claims");
        claimsArray.forEach(c -> {
            final Claim claim = ClaimWorldSerializer.getUpgradableClaim(c.getAsJsonObject(), gson);
            claims.add(claim);
        });

        final ConcurrentMap<UUID, String> userCache = jsonObject.has("user_cache") ?
                gson.fromJson(jsonObject.getAsJsonObject("user_cache"), GsonProvider.UUID_STRING_MAP_TOKEN) :
                Maps.newConcurrentMap();
        final Set<OperationType> wildernessFlags = jsonObject.has("wilderness_flags") ?
                Sets.newConcurrentHashSet(gson.fromJson(jsonObject.getAsJsonArray("wilderness_flags"), GsonProvider.OPERATION_TYPE_SET_TOKEN)) :
                Sets.newConcurrentHashSet();
        final ClaimWorld claimWorld = ClaimWorld.convert(claims, userCache, wildernessFlags);
        claimWorld.updateId(id);
        claimWorld.setSchemaVersion(ClaimWorld.CURRENT_SCHEMA);
        plugin.getDatabase().updateClaimWorld(claimWorld); // Update the database with the new format
        plugin.log(Level.INFO, "Converted old claim world with ID " + id + " to the latest schema version");
        return claimWorld;
    }
}
