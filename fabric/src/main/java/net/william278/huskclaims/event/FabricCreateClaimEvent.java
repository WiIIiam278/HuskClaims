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
import net.william278.huskclaims.user.User;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

public interface FabricCreateClaimEvent extends FabricEventCallback<CreateClaimEvent> {

    @NotNull
    Event<FabricCreateClaimEvent> EVENT = EventFactory.createArrayBacked(FabricCreateClaimEvent.class,
            (listeners) -> (event) -> {
                for (FabricCreateClaimEvent listener : listeners) {
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
    final class Callback implements CreateClaimEvent {

        private final OnlineUser onlineUser;
        @Getter(value = AccessLevel.NONE)
        @Nullable
        private final User claimOwner;
        private final Region region;
        private final ClaimWorld claimWorld;
        private final HuskClaims plugin;

        @Setter
        private boolean cancelled = false;

        public Optional<User> getClaimOwner() {
            return Optional.ofNullable(claimOwner);
        }

        @NotNull
        public Event<FabricCreateClaimEvent> getEvent() {
            return EVENT;
        }

    }

}
