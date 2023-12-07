package net.william278.huskclaims.command;

import net.william278.huskclaims.HuskClaims;
import net.william278.huskclaims.claim.Claim;
import net.william278.huskclaims.claim.ClaimWorld;
import net.william278.huskclaims.user.CommandUser;
import net.william278.huskclaims.user.OnlineUser;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Optional;

public abstract class InClaimCommand extends Command {

    protected InClaimCommand(@NotNull List<String> aliases, @NotNull String usage, @NotNull HuskClaims plugin) {
        super(aliases, usage, plugin);
    }

    @Override
    public void execute(@NotNull CommandUser executor, @NotNull String[] args) {
        if (!(executor instanceof OnlineUser user)) {
            return;
        }
        final Optional<ClaimWorld> world = plugin.getClaimWorld(((OnlineUser) executor).getWorld());
        if (world.isEmpty()) {
            plugin.getLocales().getLocale("world_not_claimable")
                    .ifPresent(user::sendMessage);
            return;
        }

        final Optional<Claim> claim = world.get().getClaimAt(user.getPosition());
        if (claim.isEmpty()) {
            plugin.getLocales().getLocale("error_not_in_claim")
                    .ifPresent(user::sendMessage);
            return;
        }

        this.execute(user, world.get(), claim.get(), args);
    }

    public abstract void execute(@NotNull OnlineUser executor, @NotNull ClaimWorld world, @NotNull Claim claim,
                                 @NotNull String[] args);

}
