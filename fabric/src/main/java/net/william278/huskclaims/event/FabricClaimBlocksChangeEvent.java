package net.william278.huskclaims.event;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.util.ActionResult;
import net.william278.huskclaims.HuskClaims;
import net.william278.huskclaims.user.ClaimBlocksManager;
import net.william278.huskclaims.user.User;
import org.jetbrains.annotations.NotNull;

public interface FabricClaimBlocksChangeEvent extends FabricEventCallback<ClaimBlocksChangeEvent> {

    @NotNull
    Event<FabricClaimBlocksChangeEvent> EVENT = EventFactory.createArrayBacked(FabricClaimBlocksChangeEvent.class,
            (listeners) -> (event) -> {
                for (FabricClaimBlocksChangeEvent listener : listeners) {
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
    final class Callback implements ClaimBlocksChangeEvent {

        private final User user;
        private final long oldBlocks;
        private final long newBlocks;
        private final ClaimBlocksManager.ClaimBlockSource reason;
        private final HuskClaims plugin;

        @Setter
        private boolean cancelled = false;

        @NotNull
        public Event<FabricClaimBlocksChangeEvent> getEvent() {
            return EVENT;
        }

    }

}
