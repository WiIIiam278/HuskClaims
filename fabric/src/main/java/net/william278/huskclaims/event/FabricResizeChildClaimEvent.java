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

package net.william278.huskclaims.event;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.util.ActionResult;
import net.william278.huskclaims.HuskClaims;
import net.william278.huskclaims.claim.Claim;
import net.william278.huskclaims.claim.ClaimWorld;
import net.william278.huskclaims.claim.Region;
import net.william278.huskclaims.user.OnlineUser;
import org.jetbrains.annotations.NotNull;

public interface FabricResizeChildClaimEvent extends FabricEventCallback<ResizeChildClaimEvent> {

    @NotNull
    Event<FabricResizeChildClaimEvent> EVENT = EventFactory.createArrayBacked(FabricResizeChildClaimEvent.class,
            (listeners) -> (event) -> {
                for (FabricResizeChildClaimEvent listener : listeners) {
                    final ActionResult result = listener.invoke(event);
                    if (event.isCancelled()) {
                        return ActionResult.CONSUME;
                    } else if (result != ActionResult.PASS) {
                        event.setCancelled(true);
                        return result;
                    }
                }

                return ActionResult.PASS;
            });

    @Getter
    @RequiredArgsConstructor(access = AccessLevel.PACKAGE)
    final class Callback implements ResizeChildClaimEvent {

        private final OnlineUser onlineUser;
        private final Claim parentClaim;
        private final Claim claim;
        private final Region newChildRegion;
        private final ClaimWorld claimWorld;
        private final HuskClaims plugin;

        @Setter
        private boolean cancelled = false;

        @NotNull
        public Event<FabricResizeChildClaimEvent> getEvent() {
            return EVENT;
        }

    }

}
