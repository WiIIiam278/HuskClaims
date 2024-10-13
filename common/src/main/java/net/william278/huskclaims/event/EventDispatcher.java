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

import net.william278.huskclaims.HuskClaims;
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
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * Dispatcher for plugin events
 */
public interface EventDispatcher extends EventProvider {

    /**
     * Fire an event synchronously, then run a callback asynchronously
     *
     * @param event    The event to fire
     * @param callback The callback to run after the event has been fired
     * @param <T>      The type of event to fire
     * @since 1.0
     */
    default <T extends Event> void fireEvent(@NotNull T event, @Nullable Consumer<T> callback) {
        getPlugin().runSync(() -> {
            if (!fireIsCancelled(event) && callback != null) {
                getPlugin().runQueued(() -> callback.accept(event));
            }
        });
    }

    /**
     * Fire an event on this thread, and return whether the event was canceled
     *
     * @param event The event to fire
     * @param <T>   The type of event to fire
     * @return Whether the event was canceled
     * @since 1.0
     */
    <T extends Event> boolean fireIsCancelled(@NotNull T event);

    default void fireClaimBlocksChangeEvent(@NotNull User user, long oldClaimBlocks, long newClaimBlocks,
                                            @NotNull UserManager.ClaimBlockSource reason,
                                            @NotNull Consumer<ClaimBlocksChangeEvent> callback) {
        fireEvent(getClaimBlocksChangeEvent(user, oldClaimBlocks, newClaimBlocks, reason), callback);
    }

    default void fireCreateChildClaimEvent(@NotNull OnlineUser claimer, @NotNull Claim parent,
                                           @NotNull Region childRegion, @NotNull ClaimWorld claimWorld,
                                           @NotNull Consumer<CreateChildClaimEvent> callback) {
        fireEvent(getCreateChildClaimEvent(claimer, parent, childRegion, claimWorld), callback);
    }

    default void fireCreateClaimEvent(@NotNull OnlineUser claimer, @Nullable User claimOwner,
                                      @NotNull Region region, @NotNull ClaimWorld claimWorld,
                                      @NotNull Consumer<CreateClaimEvent> callback) {
        fireEvent(getCreateClaimEvent(claimer, claimOwner, region, claimWorld), callback);
    }

    default void fireDeleteAllClaimsEvent(@NotNull OnlineUser deleter, @Nullable User claimOwner,
                                          @NotNull Collection<ServerWorldClaim> claims,
                                          @NotNull Consumer<DeleteAllClaimsEvent> callback) {
        fireEvent(getDeleteAllClaimsEvent(deleter, claimOwner, claims), callback);
    }

    default void fireDeleteChildClaimEvent(@NotNull OnlineUser deleter, @NotNull Claim parent,
                                           @NotNull Claim child, @NotNull ClaimWorld claimWorld,
                                           @NotNull Consumer<DeleteChildClaimEvent> callback) {
        fireEvent(getDeleteChildClaimEvent(deleter, parent, claimWorld, child), callback);
    }

    default void fireDeleteClaimEvent(@NotNull OnlineUser deleter, @NotNull Claim claim,
                                      @NotNull ClaimWorld claimWorld,
                                      @NotNull Consumer<DeleteClaimEvent> callback) {
        fireEvent(getDeleteClaimEvent(deleter, claim, claimWorld), callback);
    }

    default boolean fireIsCancelledEnterClaimEvent(@NotNull OnlineUser user, @NotNull Claim claim,
                                                   @NotNull ClaimWorld claimWorld,
                                                   @NotNull Position enteredFrom, @NotNull Position enteredTo) {
        return fireIsCancelled(getEnterClaimEvent(user, claim, claimWorld, enteredFrom, enteredTo));
    }

    default boolean fireIsCancelledExitClaimEvent(@NotNull OnlineUser user, @NotNull Claim claim,
                                                  @NotNull ClaimWorld claimWorld,
                                                  @NotNull Position exitedFrom, @NotNull Position exitedTo) {
        return fireIsCancelled(getExitClaimEvent(user, claim, claimWorld, exitedFrom, exitedTo));
    }

    default void fireResizeChildClaimEvent(@NotNull OnlineUser resizer, @NotNull Claim parent,
                                           @NotNull Claim child, @NotNull Region newRegion,
                                           @NotNull ClaimWorld claimWorld,
                                           @NotNull Consumer<ResizeChildClaimEvent> callback) {
        fireEvent(getResizeChildClaimEvent(resizer, parent, child, newRegion, claimWorld), callback);
    }

    default void fireResizeClaimEvent(@NotNull OnlineUser resizer, @NotNull Claim claim,
                                      @NotNull Region newRegion, @NotNull ClaimWorld claimWorld,
                                      @NotNull Consumer<ResizeClaimEvent> callback) {
        fireEvent(getResizeClaimEvent(resizer, claim, newRegion, claimWorld), callback);
    }

    default void fireTrustEvent(@NotNull OnlineUser user, @NotNull TrustLevel level, @NotNull Trustable trusted,
                                @NotNull Claim claim, @NotNull ClaimWorld claimWorld,
                                @NotNull Consumer<TrustEvent> callback) {
        fireEvent(getTrustEvent(user, level, trusted, claim, claimWorld), callback);
    }

    default void fireUnTrustEvent(@NotNull OnlineUser user, @NotNull TrustLevel level, @NotNull Trustable untrusted,
                                  @NotNull Claim claim, @NotNull ClaimWorld claimWorld,
                                  @NotNull Consumer<UnTrustEvent> callback) {
        fireEvent(getUnTrustEvent(user, level, untrusted, claim, claimWorld), callback);
    }

    default void fireTransferClaimEvent(@NotNull OnlineUser user, @NotNull Claim claim, @NotNull ClaimWorld claimWorld,
                                        @NotNull User newOwner, @NotNull Consumer<TransferClaimEvent> callback) {
        fireEvent(getTransferClaimEvent(user, claim, claimWorld, newOwner), callback);
    }

    default void fireClaimBanEvent(@NotNull OnlineUser user, @NotNull Claim claim, @NotNull ClaimWorld claimWorld,
                                   @NotNull User bannedUser, @NotNull Consumer<ClaimBanEvent> callback) {
        fireEvent(getClaimBanEvent(user, claim, claimWorld, bannedUser), callback);
    }

    default void fireClaimUnBanEvent(@NotNull OnlineUser user, @NotNull Claim claim, @NotNull ClaimWorld claimWorld,
                                     @NotNull User bannedUser, @NotNull Consumer<ClaimUnBanEvent> callback) {
        fireEvent(getClaimUnBanEvent(user, claim, claimWorld, bannedUser), callback);
    }

    default void fireClaimMakePrivateEvent(@NotNull OnlineUser user, @NotNull Claim claim, 
                                           @NotNull ClaimWorld claimWorld, 
                                           @NotNull Consumer<ClaimMakePrivateEvent> callback) {
        fireEvent(getClaimMakePrivateEvent(user, claim, claimWorld), callback);
    }

    default void fireClaimMakePublicEvent(@NotNull OnlineUser user, @NotNull Claim claim,
                                           @NotNull ClaimWorld claimWorld,
                                           @NotNull Consumer<ClaimMakePublicEvent> callback) {
        fireEvent(getClaimMakePublicEvent(user, claim, claimWorld), callback);
    }

    default Optional<ClaimWorldPruneEvent> fireIsCancelledClaimWorldPruneEvent(@NotNull ClaimWorld world,
                                                                               @NotNull Map<User, Long> userMap) {
        final ClaimWorldPruneEvent event = getClaimWorldPruneEvent(world, userMap);
        return !fireIsCancelled(event) ? Optional.of(event) : Optional.empty();
    }

    @NotNull
    HuskClaims getPlugin();

}
