package net.william278.huskclaims.command;

import lombok.Getter;
import lombok.Setter;
import net.william278.huskclaims.HuskClaims;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Optional;
import java.util.StringJoiner;

public abstract class Node implements Executable {

    protected static final String PERMISSION_PREFIX = "huskclaims.command";

    protected final HuskClaims plugin;
    @Getter
    private final List<String> aliases;
    @Getter
    @Setter
    private boolean operatorCommand = false;

    protected Node(@NotNull List<String> aliases, @NotNull HuskClaims plugin) {
        if (aliases.isEmpty()) {
            throw new IllegalArgumentException("Command name cannot be blank");
        }
        this.aliases = aliases;
        this.plugin = plugin;
    }

    @NotNull
    public String getPermission(@NotNull String... child) {
        final StringJoiner joiner = new StringJoiner(".")
                .add(PERMISSION_PREFIX)
                .add(getName());
        for (final String node : child) {
            joiner.add(node);
        }
        return joiner.toString().trim();
    }

    @NotNull
    public String getName() {
        return aliases.get(0);
    }

    protected Optional<String> parseStringArg(@NotNull String[] args, int index) {
        if (args.length > index) {
            return Optional.of(args[index]);
        }
        return Optional.empty();
    }

    protected Optional<String> parseGreedyArguments(@NotNull String[] args) {
        if (args.length > 1) {
            final StringJoiner sentence = new StringJoiner(" ");
            for (int i = 1; i < args.length; i++) {
                sentence.add(args[i]);
            }
            return Optional.of(sentence.toString().trim());
        }
        return Optional.empty();
    }

}
