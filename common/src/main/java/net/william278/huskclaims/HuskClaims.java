package net.william278.huskclaims;

import net.william278.huskclaims.claim.TrustLevel;
import net.william278.huskclaims.config.Locales;
import net.william278.huskclaims.config.Settings;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Optional;

public interface HuskClaims {

    @NotNull
    Settings getSettings();

    @NotNull
    Locales getLocales();

    @NotNull
    List<TrustLevel> getTrustLevels();

    default Optional<TrustLevel> getTrustLevel(@NotNull String id) {
        return getTrustLevels().stream().filter(trustLevel -> trustLevel.getId().equalsIgnoreCase(id)).findFirst();
    }

}
