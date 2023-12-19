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
import net.william278.huskclaims.claim.ClaimingMode;
import net.william278.huskclaims.user.OnlineUser;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class RestrictClaimCommand extends InClaimCommand {

    protected RestrictClaimCommand(@NotNull HuskClaims plugin) {
        super(List.of("restrictclaim"), "", null, plugin);
    }

    @Override
    public void execute(@NotNull OnlineUser executor, @NotNull ClaimWorld world,
                        @NotNull Claim claim, @NotNull String[] args) {
        // Only the owner can restrict claims as they bypass trust list requirements
        if ((claim.getOwner().isEmpty() && !ClaimingMode.ADMIN_CLAIMS.canUse(executor))
                || claim.getOwner().map(owner -> !owner.equals(executor.getUuid())).orElse(true)) {
            plugin.getLocales().getLocale("no_resizing_permission")
                    .ifPresent(executor::sendMessage);
            return;
        }
        restrictChildClaim(claim, world, executor);
    }

    private void restrictChildClaim(@NotNull Claim claim, @NotNull ClaimWorld world, @NotNull OnlineUser user) {
        if (!claim.isChildClaim(world)) {
            plugin.getLocales().getLocale("error_restrict_not_child")
                    .ifPresent(user::sendMessage);
            return;
        }
        claim.setInheritParent(!claim.isInheritParent());
        plugin.getDatabase().updateClaimWorld(world);
        plugin.getLocales().getLocale(claim.isInheritParent() ? "child_claims_inherit" : "child_claims_do_not_inherit")
                .ifPresent(user::sendMessage);
    }

}
