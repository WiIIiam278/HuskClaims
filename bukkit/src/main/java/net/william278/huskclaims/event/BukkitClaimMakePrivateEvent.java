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

import lombok.Getter;
import lombok.Setter;
import net.william278.huskclaims.HuskClaims;
import net.william278.huskclaims.claim.Claim;
import net.william278.huskclaims.claim.ClaimWorld;
import net.william278.huskclaims.user.OnlineUser;
import org.bukkit.event.Cancellable;
import org.jetbrains.annotations.NotNull;

@Getter
public class BukkitClaimMakePrivateEvent extends BukkitPlayerEvent implements ClaimMakePrivateEvent, Cancellable {

    private final Claim claim;
    private final ClaimWorld claimWorld;
    @Setter
    private boolean cancelled;

    protected BukkitClaimMakePrivateEvent(@NotNull OnlineUser user, @NotNull Claim claim,
                                          @NotNull ClaimWorld claimWorld, @NotNull HuskClaims plugin) {
        super(user, plugin);
        this.claim = claim;
        this.claimWorld = claimWorld;
    }

}
