package net.william278.huskclaims.command;

import net.william278.huskclaims.user.CommandUser;
import org.jetbrains.annotations.NotNull;

public interface Executable {

    void onExecuted(@NotNull CommandUser executor, @NotNull String[] args);

}