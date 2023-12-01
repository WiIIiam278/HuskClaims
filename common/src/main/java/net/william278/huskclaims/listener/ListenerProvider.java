package net.william278.huskclaims.listener;

import org.jetbrains.annotations.NotNull;

public interface ListenerProvider {

    @NotNull
    ClaimsListener getClaimsListener();

    default void loadListeners() {
        getClaimsListener().register();
    }

}
