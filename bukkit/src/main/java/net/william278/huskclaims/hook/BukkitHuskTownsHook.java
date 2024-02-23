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
import net.william278.huskclaims.claim.Region;
import net.william278.huskclaims.position.World;
import net.william278.husktowns.claim.Chunk;
import net.william278.husktowns.events.ClaimEvent;
import net.william278.husktowns.user.OnlineUser;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.jetbrains.annotations.NotNull;

public class BukkitHuskTownsHook extends Hook {

    protected BukkitHuskTownsHook(@NotNull HuskClaims plugin) {
        super("HuskTowns", plugin);
    }

    @Override
    public void load() {
        registerEvents();
    }

    private void registerEvents() {
        final BukkitHuskClaims plugin = (BukkitHuskClaims) this.plugin;
        plugin.getServer().getPluginManager().registerEvents(new TownListener(this), plugin);
    }

    @Override
    public void unload() {
    }

    @AllArgsConstructor
    public static class TownListener implements Listener {
        private BukkitHuskTownsHook hook;

        @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
        public void onTownCreateClaim(@NotNull ClaimEvent e) {
            final OnlineUser user = e.getUser();
            if (claimOverlaps(e.getTownClaim().claim().getChunk(), user.getWorld())) {
                hook.getPlugin().getLocales().getLocale("husktowns_claim_overlaps")
                        .ifPresent(m -> user.sendMessage(m.toComponent()));
                e.setCancelled(true);
            }
        }

        private boolean claimOverlaps(@NotNull Chunk chunk, @NotNull net.william278.husktowns.claim.World world) {
            return hook.getPlugin()
                    .getClaimWorld(Adapter.adapt(world))
                    .map(value -> value.isRegionClaimed(Adapter.adapt(chunk)))
                    .orElse(false);
        }
    }

    public static class Adapter {
        @NotNull
        private static Region adapt(@NotNull Chunk pos) {
            final Region.Point point = Region.Point.at(16 * pos.getX(), 16 * pos.getZ());
            return Region.from(point, point.plus(15, 15));
        }

        @NotNull
        private static World adapt(@NotNull net.william278.husktowns.claim.World world) {
            return World.of(world.getName(), world.getUuid(), world.getEnvironment());
        }
    }

}
