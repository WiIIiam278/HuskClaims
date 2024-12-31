package net.william278.huskclaims.hook;

import net.fabricmc.loader.api.FabricLoader;
import net.william278.huskclaims.FabricHuskClaims;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public interface FabricHookProvider extends HookProvider {

    @Override
    @NotNull
    default List<Hook> getAvailableHooks() {
        return HookProvider.super.getAvailableHooks();
    }

    @Override
    default boolean isDependencyAvailable(@NotNull String name) {
        return FabricLoader.getInstance().isModLoaded(name);
    }

    @NotNull
    FabricHuskClaims getPlugin();

}
