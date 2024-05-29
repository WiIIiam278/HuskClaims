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

package net.william278.huskclaims.util;

import io.papermc.lib.PaperLib;
import net.william278.huskclaims.BukkitHuskClaims;
import net.william278.huskclaims.position.Position;
import net.william278.huskclaims.position.World;
import org.bukkit.Chunk;
import org.bukkit.ChunkSnapshot;
import org.bukkit.Location;
import org.bukkit.Material;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

public interface BukkitSafeTeleportProvider extends SafeTeleportProvider {

    @NotNull
    List<Function<Material, Boolean>> SAFETY_CHECKS = List.of(
            m -> m.isSolid() || m == Material.WATER, Material::isAir, Material::isAir
    );

    @Override
    @NotNull
    default CompletableFuture<Optional<Position>> findSafePosition(@NotNull Position position) {
        final Location location = BukkitHuskClaims.Adapter.adapt(position);
        return PaperLib.getChunkAtAsync(location).thenApply(Chunk::getChunkSnapshot).thenApply(
                (chunk) -> getSafe(position.getWorld(), chunk, location.getBlockX(), location.getBlockZ())
        );
    }

    private Optional<Position> getSafe(@NotNull World world, @NotNull ChunkSnapshot chunk, int x, int z) {
        int chunkX = x & 0xF;
        int chunkZ = z & 0xF;
        int y = chunk.getHighestBlockYAt(chunkX, chunkZ);
        for (Function<Material, Boolean> check : SAFETY_CHECKS) {
            if (!check.apply(chunk.getBlockType(chunkX, y, chunkZ))) {
                return Optional.empty();
            }
            y++;
        }
        return Optional.of(Position.at(x, y - 1, z, world));
    }

}
