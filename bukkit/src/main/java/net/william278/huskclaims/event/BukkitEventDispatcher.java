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

import net.william278.huskclaims.BukkitHuskClaims;
import net.william278.huskclaims.claim.Claim;
import net.william278.huskclaims.claim.ClaimWorld;
import net.william278.huskclaims.claim.Region;
import net.william278.huskclaims.claim.ServerWorldClaim;
import net.william278.huskclaims.position.Position;
import net.william278.huskclaims.trust.TrustLevel;
import net.william278.huskclaims.trust.Trustable;
import net.william278.huskclaims.user.OnlineUser;
import net.william278.huskclaims.user.User;
import net.william278.huskclaims.user.UserManager;
import org.bukkit.event.Cancellable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Map;

public interface BukkitEventDispatcher extends EventDispatcher {

    @Override
    default <T extends Event> boolean fireIsCancelled(@NotNull T event) {
        ((BukkitHuskClaims) getPlugin()).getServer().getPluginManager().callEvent((org.bukkit.event.Event) event);
        return event instanceof Cancellable cancellable && cancellable.isCancelled();
    }

    @Override
    @NotNull
    default ClaimBlocksChangeEvent getClaimBlocksChangeEvent(@NotNull User user, long oldBlocks, long newBlocks,
                                                             @NotNull UserManager.ClaimBlockSource reason) {
        return new BukkitClaimBlocksChangeEvent(user, oldBlocks, newBlocks, reason, getPlugin());
    }

    @Override
    @NotNull
    default CreateChildClaimEvent getCreateChildClaimEvent(@NotNull OnlineUser claimer, @NotNull Claim parent,
                                                           @NotNull Region region, @NotNull ClaimWorld claimWorld) {
        return new BukkitCreateChildClaimEvent(claimer, parent, region, claimWorld, getPlugin());
    }

    @Override
    @NotNull
    default CreateClaimEvent getCreateClaimEvent(@NotNull OnlineUser claimer, @Nullable User claimOwner,
                                                 @NotNull Region region, @NotNull ClaimWorld claimWorld) {
        return new BukkitCreateClaimEvent(claimer, claimOwner, region, claimWorld, getPlugin());
    }

    @Override
    @NotNull
    default DeleteAllClaimsEvent getDeleteAllClaimsEvent(@NotNull OnlineUser deleter, @Nullable User claimOwner,
                                                         @NotNull Collection<ServerWorldClaim> claims) {
        return new BukkitDeleteAllClaimsEvent(deleter, claimOwner, claims, getPlugin());
    }

    @Override
    @NotNull
    default DeleteChildClaimEvent getDeleteChildClaimEvent(@NotNull OnlineUser deleter, @NotNull Claim parent,
                                                           @NotNull ClaimWorld claimWorld, @NotNull Claim child) {
        return new BukkitDeleteChildClaimEvent(deleter, parent, child, claimWorld, getPlugin());
    }

    @Override
    @NotNull
    default DeleteClaimEvent getDeleteClaimEvent(@NotNull OnlineUser deleter, @NotNull Claim claim,
                                                 @NotNull ClaimWorld claimWorld) {
        return new BukkitDeleteClaimEvent(deleter, claim, claimWorld, getPlugin());
    }

    @Override
    @NotNull
    default EnterClaimEvent getEnterClaimEvent(@NotNull OnlineUser user, @NotNull Claim claim,
                                               @NotNull ClaimWorld claimWorld,
                                               @NotNull Position enteredFrom, @NotNull Position enteredTo) {
        return new BukkitEnterClaimEvent(user, claim, claimWorld, enteredFrom, enteredTo, getPlugin());
    }

    @Override
    @NotNull
    default ExitClaimEvent getExitClaimEvent(@NotNull OnlineUser user, @NotNull Claim claim,
                                             @NotNull ClaimWorld claimWorld,
                                             @NotNull Position exitedFrom, @NotNull Position exitedTo) {
        return new BukkitExitClaimEvent(user, claim, claimWorld, exitedFrom, exitedTo, getPlugin());
    }

    @Override
    @NotNull
    default ResizeChildClaimEvent getResizeChildClaimEvent(@NotNull OnlineUser resizer, @NotNull Claim parent,
                                                           @NotNull Claim child, @NotNull Region newRegion,
                                                           @NotNull ClaimWorld claimWorld) {
        return new BukkitResizeChildClaimEvent(resizer, parent, child, newRegion, claimWorld, getPlugin());
    }

    @Override
    @NotNull
    default ResizeClaimEvent getResizeClaimEvent(@NotNull OnlineUser resizer, @NotNull Claim claim,
                                                 @NotNull Region newRegion, @NotNull ClaimWorld claimWorld) {
        return new BukkitResizeClaimEvent(resizer, claim, newRegion, claimWorld, getPlugin());
    }

    @Override
    @NotNull
    default TrustEvent getTrustEvent(@NotNull OnlineUser user, @NotNull TrustLevel level, @NotNull Trustable trusted,
                                     @NotNull Claim claim, @NotNull ClaimWorld claimWorld) {
        return new BukkitTrustEvent(user, claim, claimWorld, level, trusted, getPlugin());
    }

    @Override
    @NotNull
    default UnTrustEvent getUnTrustEvent(@NotNull OnlineUser user, @NotNull TrustLevel level,
                                         @NotNull Trustable unTrusted, @NotNull Claim claim,
                                         @NotNull ClaimWorld claimWorld) {
        return new BukkitUnTrustEvent(user, claim, claimWorld, level, unTrusted, getPlugin());
    }

    @Override
    @NotNull
    default TransferClaimEvent getTransferClaimEvent(@NotNull OnlineUser user, @NotNull Claim claim,
                                                     @NotNull ClaimWorld claimWorld, @NotNull User newOwner) {
        return new BukkitTransferClaimEvent(user, claim, claimWorld, newOwner, getPlugin());
    }

    @Override
    @NotNull
    default ClaimBanEvent getClaimBanEvent(@NotNull OnlineUser user, @NotNull Claim claim,
                                           @NotNull ClaimWorld world, @NotNull User bannedUser) {
        return new BukkitClaimBanEvent(user, claim, world, bannedUser, getPlugin());
    }

    @Override
    @NotNull
    default ClaimUnBanEvent getClaimUnBanEvent(@NotNull OnlineUser user, @NotNull Claim claim,
                                               @NotNull ClaimWorld world, @NotNull User bannedUser) {
        return new BukkitClaimUnBanEvent(user, claim, world, bannedUser, getPlugin());
    }

    @Override
    @NotNull
    default ClaimMakePrivateEvent getClaimMakePrivateEvent(@NotNull OnlineUser user, @NotNull Claim claim,
                                                           @NotNull ClaimWorld claimWorld) {
        return new BukkitClaimMakePrivateEvent(user, claim, claimWorld, getPlugin());
    }

    @Override
    @NotNull
    default ClaimMakePublicEvent getClaimMakePublicEvent(@NotNull OnlineUser user, @NotNull Claim claim,
                                                         @NotNull ClaimWorld claimWorld) {
        return new BukkitClaimMakePublicEvent(user, claim, claimWorld, getPlugin());
    }

    @Override
    @NotNull
    default ClaimWorldPruneEvent getClaimWorldPruneEvent(@NotNull ClaimWorld claimWorld,
                                                         @NotNull Map<User, Long> userMap) {
        return new BukkitClaimWorldPruneEvent(claimWorld, userMap, getPlugin());
    }
}
