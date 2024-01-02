package net.william278.huskclaims.hook;

import net.william278.huskclaims.HuskClaims;
import net.william278.huskclaims.position.Position;
import net.william278.huskclaims.trust.TrustLevel;
import net.william278.huskclaims.user.OnlineUser;
import org.jetbrains.annotations.NotNull;

import java.util.logging.Level;

public abstract class HuskHomesHook extends Hook {

    private TrustLevel requiredLevel;

    protected HuskHomesHook(@NotNull HuskClaims plugin) {
        super("HuskHomes", plugin);
    }

    @Override
    public void load() {
        requiredLevel = plugin.getTrustLevel(plugin.getSettings().getHooks().getHuskHomes().getSetHomeTrustLevel())
                .orElseGet(() -> {
                    plugin.log(Level.WARNING, "Invalid home trust level specified for HuskHomes hook.");
                    return plugin.getLowestTrustLevel();
                });
    }

    protected boolean cancelHomeAt(@NotNull OnlineUser user, @NotNull Position position) {
        return plugin.getClaimAt(position)
                .flatMap(claim -> claim.getTrustLevel(user, plugin).map(level -> level.compareTo(requiredLevel) < 0))
                .orElse(false);
    }

    public abstract void teleport(@NotNull OnlineUser user, @NotNull Position position, @NotNull String server);

}
