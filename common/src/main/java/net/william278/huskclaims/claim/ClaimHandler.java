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

package net.william278.huskclaims.claim;

import net.william278.cloplib.handler.Handler;
import net.william278.cloplib.operation.Operation;
import net.william278.cloplib.operation.OperationPosition;
import net.william278.cloplib.operation.OperationUser;
import net.william278.cloplib.operation.OperationWorld;
import net.william278.huskclaims.HuskClaims;
import net.william278.huskclaims.position.Position;
import net.william278.huskclaims.position.World;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

/**
 * Handler for {@link Operation}s in {@link Claim}s
 *
 * @since 1.0
 */
public interface ClaimHandler extends Handler {

    @Override
    default boolean cancelOperation(@NotNull Operation operation) {
        return getClaimWorld((World) operation.getOperationPosition().getWorld())
                .map(world -> !world.isOperationAllowed(operation, getPlugin()))
                .orElse(false);
    }

    @Override
    default boolean cancelMovement(@NotNull OperationUser user,
                                   @NotNull OperationPosition from, @NotNull OperationPosition to) {
        // We don't care, just yet. Future todo: enter claim event
        return false;
    }

    @Override
    default boolean cancelNature(@NotNull OperationWorld world,
                                 @NotNull OperationPosition position1, @NotNull OperationPosition position2) {
        // If this isn't in a claim world, we don't care
        if (getClaimWorld((World) world).isEmpty()) {
            return false;
        }

        // If the two claims are the same, allow it, otherwise, deny it
        final Optional<Claim> claim1 = getClaimAt((Position) position1);
        final Optional<Claim> claim2 = getClaimAt((Position) position2);
        if (claim1.isPresent() && claim2.isPresent()) {
            return !claim1.get().equals(claim2.get());
        }

        // Otherwise allow it so long as there's no claim at either position
        return !(claim1.isEmpty() && claim2.isEmpty());
    }

    Optional<ClaimWorld> getClaimWorld(@NotNull World world);

    Optional<Claim> getClaimAt(@NotNull Position position);

    @NotNull
    HuskClaims getPlugin();

}
