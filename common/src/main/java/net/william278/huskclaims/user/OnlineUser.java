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

import de.themoep.minedown.adventure.MineDown;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.william278.huskclaims.position.Position;
import net.william278.huskclaims.position.World;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

/**
 * Platform-agnostic representation of an online user
 */
public abstract class OnlineUser extends User {
    protected OnlineUser(@NotNull String username, @NotNull UUID uuid) {
        super(username, uuid);
    }

    @NotNull
    public abstract Position getPosition();

    @NotNull
    public final World getWorld() {
        return getPosition().getWorld();
    }

    @NotNull
    protected abstract Audience getAudience();

    public void sendMessage(@NotNull Component message) {
        getAudience().sendMessage(message);
    }

    public void sendMessage(@NotNull MineDown mineDown) {
        sendMessage(mineDown.toComponent());
    }

    public abstract void sendBlockChange(@NotNull Position position, @NotNull String blockId);

}
