package net.william278.huskclaims.hook;

import net.william278.huskclaims.BukkitHuskClaims;
import net.william278.huskclaims.config.Settings;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public interface BukkitHookProvider extends HookProvider {

    @Override
    @NotNull
    default List<Hook> getAvailableHooks() {
        final List<Hook> hooks = HookProvider.super.getAvailableHooks();
        final Settings.HookSettings settings = getPlugin().getSettings().getHooks();

        // Add bukkit hooks
        if (isDependencyAvailable("HuskHomes") && settings.getHuskHomes().isEnabled()) {
            hooks.add(new BukkitHuskHomesHook(getPlugin()));
        }

        return hooks;
    }

    @Override
    default boolean isDependencyAvailable(@NotNull String name) {
        return ((BukkitHuskClaims) getPlugin()).getServer().getPluginManager().isPluginEnabled(name);
    }

}
