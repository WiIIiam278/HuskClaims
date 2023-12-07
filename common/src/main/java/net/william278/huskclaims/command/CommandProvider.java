package net.william278.huskclaims.command;

import com.google.common.collect.Lists;
import lombok.AllArgsConstructor;
import net.william278.huskclaims.HuskClaims;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.function.Function;

/**
 * Provider for HuskClaims commands
 */
public interface CommandProvider {

    @NotNull
    List<BaseCommand> getCommands();

    /**
     * Registers all plugin commands: built-in commands, trust level commands, then operation group commands
     *
     * @since 1.0
     */
    default void loadCommands() {
        // Register built-in commands
        this.getCommands().addAll(BuiltinCommands.registerAll(getPlugin()));

        // Register trust level commands todo

        // Register operation group commands todo
    }

    @NotNull
    HuskClaims getPlugin();

    @AllArgsConstructor
    enum BuiltinCommands {
        HUSKCLAIMS(HuskClaimsCommand::new);

        private final Function<HuskClaims, ? extends BaseCommand> creator;

        @NotNull
        public static List<BaseCommand> registerAll(@NotNull HuskClaims plugin) {
            final List<BaseCommand> commands = Lists.newArrayList();
            for (BuiltinCommands commandType : values()) {
                commandType.creator.apply(plugin);
            }
            return commands;
        }
    }

}
