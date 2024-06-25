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

package net.william278.huskclaims.hook;

import lombok.AllArgsConstructor;
import net.william278.huskclaims.BukkitHuskClaims;
import net.william278.huskclaims.HuskClaims;
import net.william278.huskclaims.position.Position;
import net.william278.huskclaims.position.World;
import net.william278.huskclaims.user.BukkitUser;
import net.william278.huskclaims.user.OnlineUser;
import net.william278.huskhomes.api.HuskHomesAPI;
import net.william278.huskhomes.event.HomeCreateEvent;
import net.william278.huskhomes.event.HomeEditEvent;
import net.william278.huskhomes.teleport.TeleportationException;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.jetbrains.annotations.NotNull;

@PluginHook(
        name = "HuskHomes",
        register = PluginHook.Register.ON_ENABLE
)
public class BukkitHuskHomesHook extends HuskHomesHook {

    private HuskHomesAPI huskHomes;

    public BukkitHuskHomesHook(@NotNull HuskClaims plugin) {
        super(plugin);
    }

    @Override
    public void load() throws IllegalStateException {
        super.load();
        huskHomes = HuskHomesAPI.getInstance();
        registerEvents();
    }

    @Override
    public void teleport(@NotNull OnlineUser user, @NotNull Position position, @NotNull String server) {
        final net.william278.huskhomes.user.OnlineUser teleporter = Adapter.adapt(user, huskHomes);
        try {
            huskHomes.teleportBuilder()
                    .teleporter(teleporter)
                    .target(Adapter.adapt(position, server))
                    .updateLastPosition(true)
                    .toTimedTeleport().execute();
        } catch (TeleportationException e) {
            e.displayMessage(teleporter);
        }
    }

    private void registerEvents() {
        final BukkitHuskClaims plugin = (BukkitHuskClaims) this.plugin;
        plugin.getServer().getPluginManager().registerEvents(new HomeListener(this), plugin);
    }

    @Override
    public void unload() {
        huskHomes = null;
    }

    @AllArgsConstructor
    public static class HomeListener implements Listener {
        private HuskHomesHook hook;

        @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
        public void onUserSetHome(@NotNull HomeCreateEvent event) {
            if (!(event.getCreator() instanceof net.william278.huskhomes.user.BukkitUser player)) {
                return;
            }
            if (hook.cancelHomeAt(
                    BukkitUser.adapt(player.getPlayer(), hook.getPlugin()), Adapter.adapt(event.getPosition())
            )) {
                event.setCancelled(true);
            }
        }

        @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
        public void onUserMoveHome(@NotNull HomeEditEvent event) {
            if (!(event.getEditor() instanceof net.william278.huskhomes.user.BukkitUser player)
                    || !hasMoved(event.getOriginalHome(), event.getHome())) {
                return;
            }
            if (hook.cancelHomeAt(
                    BukkitUser.adapt(player.getPlayer(), hook.getPlugin()), Adapter.adapt(event.getHome())
            )) {
                event.setCancelled(true);
            }
        }

        private boolean hasMoved(@NotNull net.william278.huskhomes.position.Position position1,
                                 @NotNull net.william278.huskhomes.position.Position position2) {
            return position1.getX() != position2.getX()
                    || position1.getY() != position2.getY()
                    || position1.getZ() != position2.getZ()
                    || position1.getYaw() != position2.getYaw()
                    || position1.getPitch() != position2.getPitch();
        }
    }

    public static class Adapter {
        @NotNull
        private static Position adapt(@NotNull net.william278.huskhomes.position.Position pos) {
            return Position.at(
                    pos.getX(), pos.getY(), pos.getZ(),
                    pos.getYaw(), pos.getPitch(),
                    World.of(
                            pos.getWorld().getName(),
                            pos.getWorld().getUuid(),
                            pos.getWorld().getEnvironment().name()
                    )
            );
        }

        @NotNull
        private static net.william278.huskhomes.position.Position adapt(@NotNull Position pos, @NotNull String server) {
            return net.william278.huskhomes.position.Position.at(
                    pos.getX(), pos.getY(), pos.getZ(),
                    pos.getYaw(), pos.getPitch(),
                    net.william278.huskhomes.position.World.from(
                            pos.getWorld().getName(),
                            pos.getWorld().getUuid()
                    ),
                    server
            );
        }

        @NotNull
        private static net.william278.huskhomes.user.OnlineUser adapt(@NotNull OnlineUser user,
                                                                      @NotNull HuskHomesAPI api) {
            return api.adaptUser(((BukkitUser) user).getBukkitPlayer());
        }
    }

}
