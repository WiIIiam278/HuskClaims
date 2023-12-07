package net.william278.huskclaims.command;

import com.google.common.collect.Lists;
import net.william278.huskclaims.HuskClaims;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Provider for HuskClaims commands
 */
public interface CommandProvider {

    @NotNull
    List<Command> getCommands();

    void setCommands(@NotNull List<Command> commands);

    /**
     * Registers all plugin commands: built-in commands, trust level commands, then operation group commands
     *
     * @since 1.0
     */
    default void loadCommands() {
        final List<Command> commands = Lists.newArrayList();

        // Register built-in commands
        commands.add(new HuskClaimsCommand(getPlugin()));

        // Register trust level commands todo

        // Register operation group commands todo

        setCommands(commands);
    }

    @NotNull
    HuskClaims getPlugin();

}
