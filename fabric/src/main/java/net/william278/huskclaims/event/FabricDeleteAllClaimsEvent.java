package net.william278.huskclaims.event;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.util.ActionResult;
import net.william278.huskclaims.HuskClaims;
import net.william278.huskclaims.claim.ServerWorldClaim;
import net.william278.huskclaims.user.OnlineUser;
import net.william278.huskclaims.user.User;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Optional;

public interface FabricDeleteAllClaimsEvent extends FabricEventCallback<DeleteAllClaimsEvent> {

    @NotNull
    Event<FabricDeleteAllClaimsEvent> EVENT = EventFactory.createArrayBacked(FabricDeleteAllClaimsEvent.class,
            (listeners) -> (event) -> {
                for (FabricDeleteAllClaimsEvent listener : listeners) {
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
    final class Callback implements DeleteAllClaimsEvent {

        private final OnlineUser onlineUser;
        @Nullable
        private final User claimOwner;
        private final Collection<ServerWorldClaim> claims;
        private final HuskClaims plugin;

        @Setter
        private boolean cancelled = false;

        public Optional<User> getClaimOwner() {
            return Optional.ofNullable(claimOwner);
        }

        @NotNull
        public Event<FabricDeleteAllClaimsEvent> getEvent() {
            return EVENT;
        }

    }

}
