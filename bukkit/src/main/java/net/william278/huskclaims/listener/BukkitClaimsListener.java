package net.william278.huskclaims.listener;

import net.william278.cloplib.listener.BukkitOperationListener;
import net.william278.cloplib.operation.OperationPosition;
import net.william278.cloplib.operation.OperationUser;
import net.william278.huskclaims.BukkitHuskClaims;
import net.william278.huskclaims.HuskClaims;
import net.william278.huskclaims.user.BukkitUser;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class BukkitClaimsListener extends BukkitOperationListener implements ClaimsListener {

    private final HuskClaims plugin;

    public BukkitClaimsListener(@NotNull BukkitHuskClaims plugin) {
        super(plugin, plugin);
        this.plugin = plugin;
    }

    @Override
    @NotNull
    public OperationPosition getPosition(@NotNull Location location) {
        return BukkitHuskClaims.adapt(location);
    }

    @Override
    @NotNull
    public OperationUser getUser(@NotNull Player player) {
        return BukkitUser.adapt(player, plugin);
    }

    @NotNull
    @Override
    public HuskClaims getPlugin() {
        return plugin;
    }

    @Override
    @SuppressWarnings("unused")
    public void setInspectionDistance(int i) {
    }

}
