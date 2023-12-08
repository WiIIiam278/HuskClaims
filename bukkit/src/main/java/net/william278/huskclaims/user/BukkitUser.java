package net.william278.huskclaims.user;

import net.kyori.adventure.audience.Audience;
import net.william278.huskclaims.BukkitHuskClaims;
import net.william278.huskclaims.HuskClaims;
import net.william278.huskclaims.position.Position;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class BukkitUser extends OnlineUser {

    private final HuskClaims plugin;
    private final Player player;

    private BukkitUser(@NotNull Player player, @NotNull HuskClaims plugin) {
        super(player.getName(), player.getUniqueId());
        this.plugin = plugin;
        this.player = player;
    }

    @NotNull
    public static BukkitUser adapt(@NotNull Player player, @NotNull HuskClaims plugin) {
        return new BukkitUser(player, plugin);
    }

    @NotNull
    @Override
    public Position getPosition() {
        return BukkitHuskClaims.adapt(player.getLocation());
    }

    @NotNull
    @Override
    protected Audience getAudience() {
        return null;
    }

    @Override
    public void sendPluginMessage(@NotNull String channel, byte[] message) {

    }

    @Override
    public void sendBlockChange(@NotNull Position position, @NotNull String blockId) {

    }

    @Override
    public boolean hasPermission(@NotNull String permission) {
        return false;
    }
}
