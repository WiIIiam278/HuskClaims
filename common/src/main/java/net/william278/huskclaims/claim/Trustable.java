package net.william278.huskclaims.claim;

import net.william278.huskclaims.HuskClaims;
import org.jetbrains.annotations.NotNull;

public interface Trustable {

    @NotNull
    String getTrustIdentifier(@NotNull HuskClaims plugin);

}
