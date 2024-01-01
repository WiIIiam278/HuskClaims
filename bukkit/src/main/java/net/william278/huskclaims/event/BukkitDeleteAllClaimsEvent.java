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
import net.william278.huskclaims.claim.ServerWorldClaim;
import net.william278.huskclaims.user.OnlineUser;
import net.william278.huskclaims.user.User;
import org.bukkit.event.Cancellable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;

@Getter
public class BukkitDeleteAllClaimsEvent extends BukkitPlayerEvent implements DeleteAllClaimsEvent, Cancellable {

    @Nullable
    private final User claimOwner;
    private final Collection<ServerWorldClaim> claims;
    @Setter
    private boolean cancelled;

    protected BukkitDeleteAllClaimsEvent(@NotNull OnlineUser user, @Nullable User claimOwner,
                                      @NotNull Collection<ServerWorldClaim> claims, @NotNull HuskClaims plugin) {
        super(user, plugin);
        this.claimOwner = claimOwner;
        this.claims = claims;
    }

}
