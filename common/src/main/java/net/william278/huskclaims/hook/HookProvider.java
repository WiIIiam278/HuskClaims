package net.william278.huskclaims.hook;

import org.jetbrains.annotations.NotNull;

import java.util.Set;

public interface HookProvider {

    @NotNull
    Set<Hook> getHooks();

    void setHooks(@NotNull Set<Hook> hooks);

    default void loadHooks() {

    }

}
