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

import net.william278.huskclaims.claim.Claim;
import net.william278.huskclaims.claim.ClaimWorld;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

public interface MapHookProvider {

    @NotNull
    @Unmodifiable
    default Set<MapHook> getMapHooks() {
        return getHooks().stream()
                .filter(hook -> hook instanceof MapHook)
                .map(hook -> (MapHook) hook)
                .collect(Collectors.toSet());
    }

    default void addMappedClaim(@NotNull Claim claim, @NotNull ClaimWorld claimWorld) {
        getMapHooks().forEach(hook -> hook.markClaim(claim, claimWorld));
    }

    default void removeMappedClaim(@NotNull Claim claim, @NotNull ClaimWorld claimWorld) {
        getMapHooks().forEach(hook -> hook.unMarkClaim(claim, claimWorld));
    }

    default void removeAllMappedClaims(@Nullable UUID owner) {
        getMapHooks().forEach(hook -> hook.unMarkClaimsBy(owner));
    }

    default void removeAllMappedAdminClaims() {
        getMapHooks().forEach(MapHook::unMarkAdminClaims);
    }

    default void clearAllMapMarkers() {
        getMapHooks().forEach(MapHook::unMarkAllClaims);
    }

    default void markAllClaims() {
        getMapHooks().forEach(MapHook::markAllClaims);
    }

    @NotNull
    Set<Hook> getHooks();

}
