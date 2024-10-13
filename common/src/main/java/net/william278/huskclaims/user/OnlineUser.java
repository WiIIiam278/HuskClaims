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
import net.william278.cloplib.listener.InspectorCallbackProvider;
import net.william278.cloplib.operation.OperationUser;
import net.william278.huskclaims.HuskClaims;
import net.william278.huskclaims.position.Position;
import net.william278.huskclaims.position.World;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;
import java.util.UUID;

/**
 * Platform-agnostic representation of an online user
 */
public abstract class OnlineUser extends User implements OperationUser, CommandUser {

    protected final HuskClaims plugin;

    protected OnlineUser(@NotNull String username, @NotNull UUID uuid, @NotNull HuskClaims plugin) {
        super(username, uuid);
        this.plugin = plugin;
    }

    @NotNull
    public abstract Position getPosition();

    @NotNull
    public final World getWorld() {
        return getPosition().getWorld();
    }

    @NotNull
    public Audience getAudience() {
        return plugin.getAudience(getUuid());
    }

    public void sendMessage(@NotNull Component message) {
        getAudience().sendMessage(message);
    }

    public void sendMessage(@NotNull MineDown mineDown) {
        sendMessage(mineDown.toComponent());
    }

    public abstract void sendPluginMessage(@NotNull String channel, byte[] message);

    public abstract boolean hasPermission(@NotNull String permission, boolean isDefault);

    public abstract boolean hasPermission(@NotNull String permission);

    public abstract boolean isHolding(@NotNull InspectorCallbackProvider.InspectionTool tool);

    /**
     * Returns whether the user is holding an item with the specified material
     *
     * @param material The material to check for
     * @return Whether the user is holding the specified material
     * @deprecated Use {@link #isHolding(InspectorCallbackProvider.InspectionTool)} instead
     */
    @Deprecated(since = "1.3", forRemoval = true)
    public boolean isHolding(@NotNull String material) {
        return this.isHolding(InspectorCallbackProvider.InspectionTool.builder().material(material).build());
    }

    public abstract Optional<Long> getNumericalPermission(@NotNull String prefix);

    public abstract boolean isSneaking();

    public abstract void teleport(@NotNull Position position, boolean instant);


}
