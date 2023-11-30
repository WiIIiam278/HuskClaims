package net.william278.huskclaims.claim;

import net.william278.huskclaims.HuskClaims;
import net.william278.huskclaims.config.Settings;
import net.william278.huskclaims.user.OnlineUser;
import org.jetbrains.annotations.NotNull;

public class DefaultHighlighter implements ClaimHighlighter {

    private final Settings.HighlighterSettings settings;

    public DefaultHighlighter(@NotNull HuskClaims plugin) {
        this.settings = plugin.getSettings().getClaimHighlighting();
    }

    @Override
    public void highlightClaim(@NotNull OnlineUser user, @NotNull ClaimWorld world, @NotNull Claim claim) {
        //todo task tracking and such
    }

}
