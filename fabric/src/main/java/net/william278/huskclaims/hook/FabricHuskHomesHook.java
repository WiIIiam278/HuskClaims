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

// todo HuskHomes buildscript changes clearly needed here. Fabric API stuff is not working :(
//#if MC==12104
package net.william278.huskclaims.hook;

import net.william278.huskclaims.HuskClaims;
import net.william278.huskclaims.position.Position;
import net.william278.huskclaims.position.World;
import net.william278.huskclaims.user.OnlineUser;
import net.william278.huskhomes.api.FabricHuskHomesAPI;
import org.jetbrains.annotations.NotNull;

@PluginHook(
        name = "HuskHomes",
        register = PluginHook.Register.ON_ENABLE
)
public class FabricHuskHomesHook extends HuskHomesHook {

    private FabricHuskHomesAPI huskHomes;

    protected FabricHuskHomesHook(@NotNull HuskClaims plugin) {
        super(plugin);
    }

    @Override
    public void load() throws IllegalStateException {
        super.load();
        huskHomes = FabricHuskHomesAPI.getInstance();

        // todo broken due to mappings issues. Investigate HuskHomes.
//        HomeCreateCallback.EVENT.register((event) -> {
//            if (!(event.getCreator() instanceof FabricUser player)) {
//                return PASS;
//            }
//            return cancelSetHomeAt(getPlugin().getOnlineUser(player.getUuid()),
//                    Adapter.adapt(event.getPosition())) ? FAIL : PASS;
//        });
//        HomeEditCallback.EVENT.register((event) -> {
//            if (!(event.getEditor() instanceof FabricUser player)
//                    || !hasMoved(event.getOriginalHome(), event.getHome())) {
//                return PASS;
//            }
//            return cancelSetHomeAt(getPlugin().getOnlineUser(player.getUuid()),
//                    Adapter.adapt(event.getHome())) ? FAIL : PASS;
//        });
    }

    @Override
    public void teleport(@NotNull OnlineUser user, @NotNull Position position, @NotNull String server) {
        huskHomes.teleportBuilder()
                .teleporter(Adapter.adapt(user, huskHomes))
                .target(Adapter.adapt(position, server))
                .updateLastPosition(true)
                .buildAndComplete(true);
    }

    @Override
    public void unload() {
        huskHomes = null;
    }

    private boolean hasMoved(@NotNull net.william278.huskhomes.position.Position position1,
                             @NotNull net.william278.huskhomes.position.Position position2) {
        return position1.getX() != position2.getX()
                || position1.getY() != position2.getY()
                || position1.getZ() != position2.getZ()
                || position1.getYaw() != position2.getYaw()
                || position1.getPitch() != position2.getPitch();
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
                                                                      @NotNull FabricHuskHomesAPI api) {
            return api.getOnlineUser(user.getUuid()).orElseThrow(
                    () -> new IllegalStateException("Could not fetch online HuskClaims user from HuskHomes user"));
        }
    }

}
//#endif