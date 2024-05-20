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

import com.google.gson.*;
import lombok.RequiredArgsConstructor;
import net.william278.huskclaims.HuskClaims;

import java.lang.reflect.Type;

@RequiredArgsConstructor
@SuppressWarnings("unused")
public class RegionSerializer implements JsonSerializer<Region>, JsonDeserializer<Region> {

    private final HuskClaims plugin;

    @Override
    public JsonElement serialize(Region region, Type type, JsonSerializationContext jsonSerializationContext) {
        final JsonArray jsonArray = new JsonArray();
        jsonArray.add(region.getNearCorner().getBlockX());
        jsonArray.add(region.getNearCorner().getBlockZ());
        jsonArray.add(region.getFarCorner().getBlockX());
        jsonArray.add(region.getFarCorner().getBlockZ());
        return jsonArray;
    }

    @Override
    public Region deserialize(JsonElement jsonElement, Type type, JsonDeserializationContext jsonDeserializationContext) throws JsonParseException {
        final JsonArray jsonArray = jsonElement.getAsJsonArray();
        final int x1 = jsonArray.get(0).getAsInt();
        final int z1 = jsonArray.get(1).getAsInt();
        final int x2 = jsonArray.get(2).getAsInt();
        final int z2 = jsonArray.get(3).getAsInt();
        final Region.Point nearCorner = Region.Point.at(x1, z1);
        final Region.Point farCorner = Region.Point.at(x2, z2);
        return Region.from(nearCorner, farCorner);
    }
}
