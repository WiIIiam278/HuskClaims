package net.william278.huskclaims.command;

import net.william278.huskclaims.HuskClaims;
import net.william278.huskclaims.user.CommandUser;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class HuskClaimsCommand extends Command {

    protected HuskClaimsCommand(@NotNull HuskClaims plugin) {
        super(List.of("huskclaims"), "[test|test|test]", plugin);
    }

    @Override
    public void execute(@NotNull CommandUser executor, @NotNull String[] args) {

    }

}
