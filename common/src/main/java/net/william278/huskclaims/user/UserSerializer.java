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

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.UUID;

@NoArgsConstructor
public class UserSerializer extends TypeAdapter<User> {

    @Override
    public void write(@NotNull JsonWriter out, @NotNull User user) throws IOException {
        out.beginObject();
        out.name("name").value(user.getName());
        out.name("uuid").value(user.getUuid().toString());
        out.endObject();
    }

    @Override
    @NotNull
    public User read(@NotNull JsonReader in) throws IOException {
        in.beginObject();
        final User user = new User();
        while (in.hasNext()) {
            switch (in.nextName()) {
                case "name":
                    user.setName(in.nextString());
                    break;
                case "uuid":
                    user.setUuid(UUID.fromString(in.nextString()));
                    break;
            }
        }
        in.endObject();
        return user;
    }

}
