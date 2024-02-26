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

import net.kyori.adventure.text.Component;
import net.william278.huskclaims.BukkitHuskClaims;
import net.william278.huskclaims.moderation.SignWrite;
import net.william278.huskclaims.user.BukkitUser;
import org.bukkit.block.sign.Side;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.SignChangeEvent;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public class PaperListener extends BukkitListener {

    public PaperListener(@NotNull BukkitHuskClaims plugin) {
        super(plugin);
    }

    @Override
    public void register() {
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        setInspectorCallbacks();
    }

    @EventHandler
    public void onSignEdit(@NotNull SignChangeEvent e) {
        filterSign(
                handleSignEdit(SignWrite.create(
                        BukkitUser.adapt(e.getPlayer(), plugin),
                        BukkitHuskClaims.Adapter.adapt(e.getBlock().getLocation()),
                        e.getSide() == Side.FRONT ? SignWrite.Type.SIGN_EDIT_FRONT : SignWrite.Type.SIGN_EDIT_BACK,
                        e.lines(),
                        plugin.getServerName()
                )), e
        );
    }

    // Apply filter edits to a sign if needed
    private void filterSign(@NotNull SignWrite write, @NotNull SignChangeEvent e) {
        if (!write.isFiltered()) {
            return;
        }
        for (int l = 0; l < e.lines().size(); l++) {
            e.line(l, Component.text(
                    write.getText().get(l),
                    Objects.requireNonNull(e.line(l)).color()
            ));
        }
    }

}
