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

import net.william278.cloplib.operation.OperationPosition;
import net.william278.cloplib.operation.OperationUser;
import net.william278.huskclaims.HuskClaims;
import net.william278.huskclaims.claim.ClaimWorld;
import net.william278.huskclaims.position.Position;
import net.william278.huskclaims.user.OnlineUser;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

/**
 * Handler for when the claim creation and resize tool is used
 */
public interface ClaimsToolHandler {


    // Handle a right click action on a block with a claim tool
    default void onClaimToolUse(@NotNull OperationUser opUser, @NotNull OperationPosition opPosition) {
        final OnlineUser user = (OnlineUser) opUser;
        final Position position = (Position) opPosition;
        final Optional<ClaimWorld> optionalWorld = getPlugin().getClaimWorld(position.getWorld());
        if (optionalWorld.isEmpty()) {
            getPlugin().getLocales().getLocale("world_not_claimable")
                    .ifPresent(user::sendMessage);
            return;
        }
        getPlugin().runQueued(() -> getPlugin().handleSelection(user, optionalWorld.get(), position));
    }

    @NotNull
    HuskClaims getPlugin();

}
