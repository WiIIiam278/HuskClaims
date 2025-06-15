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

import net.minecraft.util.ActionResult;
import net.william278.huskclaims.claim.Claim;
import net.william278.huskclaims.claim.ClaimWorld;
import net.william278.huskclaims.claim.Region;
import net.william278.huskclaims.claim.ServerWorldClaim;
import net.william278.huskclaims.position.Position;
import net.william278.huskclaims.trust.TrustLevel;
import net.william278.huskclaims.trust.Trustable;
import net.william278.huskclaims.user.OnlineUser;
import net.william278.huskclaims.user.SavedUserProvider;
import net.william278.huskclaims.user.User;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Map;
import java.util.logging.Level;

public interface FabricEventDispatcher extends EventDispatcher {

    @SuppressWarnings("unchecked")
    @Override
    default <T extends Event> boolean fireIsCancelled(@NotNull T event) {
        try {
            final Method field = event.getClass().getDeclaredMethod("getEvent");
            field.setAccessible(true);

            net.fabricmc.fabric.api.event.Event<?> fabricEvent =
                    (net.fabricmc.fabric.api.event.Event<?>) field.invoke(event);

            final FabricEventCallback<T> invoker = (FabricEventCallback<T>) fabricEvent.invoker();
            return invoker.invoke(event) == ActionResult.FAIL;
        } catch (InvocationTargetException | NoSuchMethodException | IllegalAccessException e) {
            getPlugin().log(Level.WARNING, "Failed to fire event (" + event.getClass().getName() + ")", e);
            return false;
        }
    }

    @Override
    @NotNull
    default ClaimBlocksChangeEvent getClaimBlocksChangeEvent(@NotNull User user, long oldBlocks, long newBlocks,
                                                             @NotNull SavedUserProvider.ClaimBlockSource reason) {
        return new FabricClaimBlocksChangeEvent.Callback(user, oldBlocks, newBlocks, reason, getPlugin());
    }

    @Override
    @NotNull
    default CreateChildClaimEvent getCreateChildClaimEvent(@NotNull OnlineUser claimer, @NotNull Claim parent,
                                                           @NotNull Region region, @NotNull ClaimWorld claimWorld) {
        return new FabricCreateChildClaimEvent.Callback(claimer, parent, region, claimWorld, getPlugin());
    }

    @Override
    @NotNull
    default PostCreateChildClaimEvent getPostCreateChildClaimEvent(@NotNull OnlineUser claimer,
                                                                   @NotNull Claim newChildClaim,
                                                                   @NotNull ClaimWorld claimWorld) {
        return new FabricPostCreateChildClaimEvent.Callback(claimer, newChildClaim, claimWorld, getPlugin());
    }

    @Override
    @NotNull
    default CreateClaimEvent getCreateClaimEvent(@NotNull OnlineUser claimer, @Nullable User claimOwner,
                                                 @NotNull Region region, @NotNull ClaimWorld claimWorld) {
        return new FabricCreateClaimEvent.Callback(claimer, claimOwner, region, claimWorld, getPlugin());
    }

    @Override
    @NotNull
    default PostCreateClaimEvent getPostCreateClaimEvent(@NotNull OnlineUser claimer, @NotNull Claim newClaim,
                                                         @NotNull ClaimWorld claimWorld) {
        return new FabricPostCreateClaimEvent.Callback(claimer, newClaim, claimWorld, getPlugin());
    }

    @Override
    @NotNull
    default DeleteAllClaimsEvent getDeleteAllClaimsEvent(@NotNull OnlineUser deleter, @Nullable User claimOwner,
                                                         @NotNull Collection<ServerWorldClaim> claims) {
        return new FabricDeleteAllClaimsEvent.Callback(deleter, claimOwner, claims, getPlugin());
    }

    @Override
    @NotNull
    default DeleteChildClaimEvent getDeleteChildClaimEvent(@NotNull OnlineUser deleter, @NotNull Claim parent,
                                                           @NotNull ClaimWorld claimWorld, @NotNull Claim child) {
        return new FabricDeleteChildClaimEvent.Callback(deleter, parent, child, claimWorld, getPlugin());
    }

    @Override
    @NotNull
    default DeleteClaimEvent getDeleteClaimEvent(@NotNull OnlineUser deleter, @NotNull Claim claim,
                                                 @NotNull ClaimWorld claimWorld) {
        return new FabricDeleteClaimEvent.Callback(deleter, claim, claimWorld, getPlugin());
    }

    @Override
    @NotNull
    default EnterClaimEvent getEnterClaimEvent(@NotNull OnlineUser user, @NotNull Claim claim,
                                               @NotNull ClaimWorld claimWorld,
                                               @NotNull Position enteredFrom, @NotNull Position enteredTo) {
        return new FabricEnterClaimEvent.Callback(user, claim, claimWorld, enteredFrom, enteredTo, getPlugin());
    }

    @Override
    @NotNull
    default ExitClaimEvent getExitClaimEvent(@NotNull OnlineUser user, @NotNull Claim claim,
                                             @NotNull ClaimWorld claimWorld,
                                             @NotNull Position exitedFrom, @NotNull Position exitedTo) {
        return new FabricExitClaimEvent.Callback(user, claim, claimWorld, exitedFrom, exitedTo, getPlugin());
    }

    @Override
    @NotNull
    default ResizeChildClaimEvent getResizeChildClaimEvent(@NotNull OnlineUser resizer, @NotNull Claim parent,
                                                           @NotNull Claim child, @NotNull Region newRegion,
                                                           @NotNull ClaimWorld claimWorld) {
        return new FabricResizeChildClaimEvent.Callback(resizer, parent, child, newRegion, claimWorld, getPlugin());
    }

    @Override
    @NotNull
    default ResizeClaimEvent getResizeClaimEvent(@NotNull OnlineUser resizer, @NotNull Claim claim,
                                                 @NotNull Region newRegion, @NotNull ClaimWorld claimWorld) {
        return new FabricResizeClaimEvent.Callback(resizer, claim, newRegion, claimWorld, getPlugin());
    }

    @Override
    @NotNull
    default TrustEvent getTrustEvent(@NotNull OnlineUser user, @NotNull TrustLevel level, @NotNull Trustable trusted,
                                     @NotNull Claim claim, @NotNull ClaimWorld claimWorld) {
        return new FabricTrustEvent.Callback(user, claim, claimWorld, level, trusted, getPlugin());
    }

    @Override
    @NotNull
    default UnTrustEvent getUnTrustEvent(@NotNull OnlineUser user, @NotNull TrustLevel level,
                                         @NotNull Trustable unTrusted, @NotNull Claim claim,
                                         @NotNull ClaimWorld claimWorld) {
        return new FabricUnTrustEvent.Callback(user, claim, claimWorld, level, unTrusted, getPlugin());
    }

    @Override
    @NotNull
    default TransferClaimEvent getTransferClaimEvent(@NotNull OnlineUser user, @NotNull Claim claim,
                                                     @NotNull ClaimWorld claimWorld, @NotNull User newOwner) {
        return new FabricTransferClaimEvent.Callback(user, claim, claimWorld, newOwner, getPlugin());
    }

    @Override
    @NotNull
    default ClaimBanEvent getClaimBanEvent(@NotNull OnlineUser user, @NotNull Claim claim,
                                           @NotNull ClaimWorld world, @NotNull User bannedUser) {
        return new FabricClaimBanEvent.Callback(user, claim, world, bannedUser, getPlugin());
    }

    @Override
    @NotNull
    default ClaimUnBanEvent getClaimUnBanEvent(@NotNull OnlineUser user, @NotNull Claim claim,
                                               @NotNull ClaimWorld world, @NotNull User bannedUser) {
        return new FabricClaimUnBanEvent.Callback(user, claim, world, bannedUser, getPlugin());
    }

    @Override
    @NotNull
    default ClaimMakePrivateEvent getClaimMakePrivateEvent(@NotNull OnlineUser user, @NotNull Claim claim,
                                                           @NotNull ClaimWorld claimWorld) {
        return new FabricClaimMakePrivateEvent.Callback(user, claim, claimWorld, getPlugin());
    }

    @Override
    @NotNull
    default ClaimMakePublicEvent getClaimMakePublicEvent(@NotNull OnlineUser user, @NotNull Claim claim,
                                                         @NotNull ClaimWorld claimWorld) {
        return new FabricClaimMakePublicEvent.Callback(user, claim, claimWorld, getPlugin());
    }

    @Override
    @NotNull
    default ClaimWorldPruneEvent getClaimWorldPruneEvent(@NotNull ClaimWorld claimWorld,
                                                         @NotNull Map<User, Long> userMap) {
        return new FabricClaimWorldPruneEvent.Callback(claimWorld, userMap, getPlugin());
    }

}
