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

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.william278.huskclaims.FabricHuskClaims;
import net.william278.huskclaims.position.Position;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Predicate;

public interface FabricSafeTeleportProvider extends SafeTeleportProvider {

    List<Predicate<BlockState>> SAFETY_CHECKS = List.of(
            b -> b.isOpaque() || b.getBlock() == Blocks.WATER, BlockState::isAir, BlockState::isAir
    );

    @Override
    @NotNull
    default CompletableFuture<Optional<Position>> findSafePosition(@NotNull Position position) {
        final Location location = FabricHuskClaims.Adapter.adapt(position, getPlugin().getMinecraftServer());
        return CompletableFuture.completedFuture(getSafe(location.world(), location.blockPos()));
    }

    private Optional<Position> getSafe(@NotNull ServerWorld world, @NotNull BlockPos pos) {
        int y = getHighestYAt(world, pos);
        for (Predicate<BlockState> check : SAFETY_CHECKS) {
            if (!check.test(world.getBlockState(pos.withY(y)))) {
                return Optional.empty();
            }
            y++;
        }
        if (pos.getY() >= world.getHeight()) {
            return Optional.empty();
        }
        return Optional.of(FabricHuskClaims.Adapter.adapt(world, pos.withY(y - 1).toCenterPos(), 0f, 0f));
    }

    private int getHighestYAt(@NotNull ServerWorld blockView, @NotNull BlockPos pos) {
        final BlockPos.Mutable cursor = new BlockPos.Mutable(pos.getX(), blockView.getHeight(), pos.getZ());
        while (blockView.getBlockState(cursor).isAir() && cursor.getY() > blockView.getBottomY()) {
            cursor.move(Direction.DOWN);
        }
        return cursor.getY();
    }

    @NotNull
    FabricHuskClaims getPlugin();

}
