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

package net.william278.huskclaims.command;

import net.william278.huskclaims.HuskClaims;
import net.william278.huskclaims.claim.Claim;
import net.william278.huskclaims.claim.ClaimWorld;
import net.william278.huskclaims.trust.TrustLevel;
import net.william278.huskclaims.user.OnlineUser;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class ClaimPrivateCommand extends InClaimCommand {

    protected ClaimPrivateCommand(@NotNull HuskClaims plugin) {
        super(List.of("claimprivate"), "", TrustLevel.Privilege.MAKE_PRIVATE, plugin);
    }

    @Override
    public void execute(@NotNull OnlineUser user, @NotNull ClaimWorld world,
                        @NotNull Claim claim, @NotNull String[] args) {
        claim.setPrivateClaim(!claim.isPrivateClaim());
        plugin.getDatabase().updateClaimWorld(world);

        if (claim.isPrivateClaim()) {
            plugin.getLocales().getLocale("claim_private_enabled")
                    .ifPresent(user::sendMessage);

            // teleport all untrusted players outside the claim
            plugin.getOnlineUsers().stream()
                    .filter(u -> plugin.getClaimAt(u.getPosition()).map(c -> c.equals(claim)).orElse(false))
                    .filter(u -> world.cannotNavigatePrivateClaim(u, claim, plugin))
                    .forEach(plugin::teleportOutOfClaim);
            return;
        }
        plugin.getLocales().getLocale("claim_private_disabled")
                .ifPresent(user::sendMessage);
    }

}
