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
import net.william278.huskclaims.user.OnlineUser;
import net.william278.huskclaims.user.User;
import org.jetbrains.annotations.NotNull;

public interface FabricClaimMakePrivateEvent extends FabricEventCallback<ClaimMakePrivateEvent> {

    @NotNull
    Event<FabricClaimMakePrivateEvent> EVENT = EventFactory.createArrayBacked(FabricClaimMakePrivateEvent.class,
            (listeners) -> (event) -> {
                for (FabricClaimMakePrivateEvent listener : listeners) {
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
    final class Callback implements ClaimMakePrivateEvent {

        private final OnlineUser onlineUser;
        private final Claim claim;
        private final ClaimWorld claimWorld;
        private final HuskClaims plugin;

        @Setter
        private boolean cancelled = false;

        @NotNull
        public Event<FabricClaimMakePrivateEvent> getEvent() {
            return EVENT;
        }

    }

}
