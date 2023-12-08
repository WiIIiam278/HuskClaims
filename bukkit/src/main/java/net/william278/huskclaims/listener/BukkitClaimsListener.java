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

package net.william278.huskclaims.listener;

import net.william278.cloplib.listener.BukkitOperationListener;
import net.william278.cloplib.operation.OperationPosition;
import net.william278.cloplib.operation.OperationUser;
import net.william278.huskclaims.BukkitHuskClaims;
import net.william278.huskclaims.HuskClaims;
import net.william278.huskclaims.user.BukkitUser;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class BukkitClaimsListener extends BukkitOperationListener implements ClaimsListener {

    private final HuskClaims plugin;

    public BukkitClaimsListener(@NotNull BukkitHuskClaims plugin) {
        super(plugin, plugin);
        this.plugin = plugin;
    }

    @Override
    @NotNull
    public OperationPosition getPosition(@NotNull Location location) {
        return BukkitHuskClaims.Adapter.adapt(location);
    }

    @Override
    @NotNull
    public OperationUser getUser(@NotNull Player player) {
        return BukkitUser.adapt(player, plugin);
    }

    @NotNull
    @Override
    public HuskClaims getPlugin() {
        return plugin;
    }

}
