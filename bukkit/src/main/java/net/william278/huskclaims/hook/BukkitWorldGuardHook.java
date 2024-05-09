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
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedCuboidRegion;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import com.sk89q.worldguard.protection.regions.RegionType;
import net.william278.huskclaims.HuskClaims;
import net.william278.huskclaims.claim.Region;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.jetbrains.annotations.NotNull;

public class BukkitWorldGuardHook extends WorldGuardHook {


    public BukkitWorldGuardHook(@NotNull HuskClaims plugin) {
        super(plugin);
    }

    @Override
    public boolean isChunkInRestrictedRegion(@NotNull Region region, @NotNull String worldName) {
        final World bukkitWorld = Bukkit.getWorld(worldName);
        if (bukkitWorld == null) return false;
        final RegionManager regionManager = WorldGuard.getInstance().getPlatform().getRegionContainer()
                .get(BukkitAdapter.adapt(bukkitWorld));
        if (regionManager == null) {
            return false;
        }

        final BlockVector3 min = BlockVector3.at(region.getNearCorner().getBlockX(), -64, region.getNearCorner().getBlockZ());
        final BlockVector3 max = BlockVector3.at(region.getFarCorner().getBlockX(), 320, region.getFarCorner().getBlockZ());

        final ProtectedRegion chunkRegion = new ProtectedCuboidRegion("dummy", min, max);
        final ApplicableRegionSet applicableRegions = regionManager.getApplicableRegions(chunkRegion);
        return applicableRegions.getRegions().stream()
                .anyMatch(wgRegion -> !wgRegion.getType().equals(RegionType.GLOBAL));
    }

}
