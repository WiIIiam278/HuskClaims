package net.william278.huskclaims.listener;

import net.kyori.adventure.text.Component;
import net.william278.huskclaims.BukkitHuskClaims;
import net.william278.huskclaims.moderation.SignWrite;
import net.william278.huskclaims.user.BukkitUser;
import org.bukkit.block.sign.Side;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.SignChangeEvent;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public class PaperListener extends BukkitListener {

    public PaperListener(@NotNull BukkitHuskClaims plugin) {
        super(plugin);
    }

    @Override
    public void register() {
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        setInspectorCallbacks();
    }

    @EventHandler
    public void onSignEdit(@NotNull SignChangeEvent e) {
        filterSign(
                handleSignEdit(SignWrite.create(
                        BukkitUser.adapt(e.getPlayer(), plugin),
                        BukkitHuskClaims.Adapter.adapt(e.getBlock().getLocation()),
                        e.getSide() == Side.FRONT ? SignWrite.Type.SIGN_EDIT_FRONT : SignWrite.Type.SIGN_EDIT_BACK,
                        e.lines(),
                        plugin.getServerName()
                )), e
        );
    }

    // Apply filter edits to a sign if needed
    private void filterSign(@NotNull SignWrite write, @NotNull SignChangeEvent e) {
        if (!write.isFiltered()) {
            return;
        }
        for (int l = 0; l < e.lines().size(); l++) {
            e.line(l, Component.text(
                    write.getText().get(l),
                    Objects.requireNonNull(e.line(l)).style()
            ));
        }
    }

}
