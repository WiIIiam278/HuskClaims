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

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.flags.StateFlag;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedCuboidRegion;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import net.william278.huskclaims.BukkitHuskClaims;
import net.william278.huskclaims.HuskClaims;
import net.william278.huskclaims.claim.Region;
import net.william278.huskclaims.position.World;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

@PluginHook(
        name = "WorldGuard",
        register = PluginHook.Register.ON_LOAD
)
public class BukkitWorldGuardHook extends WorldGuardHook {

    public BukkitWorldGuardHook(@NotNull HuskClaims plugin) {
        super(plugin);
    }

    public boolean isRestricted(@NotNull Region region, @NotNull World world) {
        final org.bukkit.World bukkitWorld = BukkitHuskClaims.Adapter.adapt(world);
        final Optional<RegionManager> regionManager = getRegionManager(bukkitWorld);
        if (regionManager.isEmpty()) {
            return false;
        }
        final ApplicableRegionSet set = getOverlappingRegions(region, regionManager.get(), bukkitWorld);
        for (ProtectedRegion protectedRegion : set.getRegions()) {
            return protectedRegion.getFlag(CLAIMING) == StateFlag.State.DENY;
        }
        return false;
    }

    @NotNull
    private static ApplicableRegionSet getOverlappingRegions(@NotNull Region region, @NotNull RegionManager manager,
                                                             @NotNull org.bukkit.World bukkitWorld) {
        final Region.Point minCorner = region.getNearCorner();
        final Region.Point maxCorner = region.getFarCorner();
        return manager.getApplicableRegions(new ProtectedCuboidRegion(
                "dummy",
                BlockVector3.at(minCorner.getBlockX(), bukkitWorld.getMinHeight(), minCorner.getBlockZ()),
                BlockVector3.at(maxCorner.getBlockX(), bukkitWorld.getMaxHeight(), maxCorner.getBlockZ())
        ));
    }

    private Optional<RegionManager> getRegionManager(@NotNull org.bukkit.World world) {
        return Optional.ofNullable(
                WorldGuard.getInstance().getPlatform().getRegionContainer().get(BukkitAdapter.adapt(world))
        );
    }

}
