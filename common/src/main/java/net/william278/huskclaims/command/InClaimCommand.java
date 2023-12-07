package net.william278.huskclaims.command;

import net.william278.huskclaims.HuskClaims;
import net.william278.huskclaims.claim.Claim;
import net.william278.huskclaims.claim.ClaimWorld;
import net.william278.huskclaims.claim.TrustLevel;
import net.william278.huskclaims.claim.Trustable;
import net.william278.huskclaims.config.Settings;
import net.william278.huskclaims.user.CommandUser;
import net.william278.huskclaims.user.OnlineUser;
import net.william278.huskclaims.user.SavedUser;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Optional;

public abstract class InClaimCommand extends Command {

    protected InClaimCommand(@NotNull List<String> aliases, @NotNull HuskClaims plugin) {
        super(aliases, getUsageText(plugin.getSettings().getUserGroups()), plugin);
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

    /**
     * Returns if an {@link OnlineUser} has access to manage the trust level of a {@link Trustable}
     *
     * @param executor  The executor
     * @param trustable The trustable
     * @param world     The world
     * @param claim     The claim
     * @return if the executor has access
     * @since 1.0
     */
    protected boolean checkUserHasAccess(@NotNull OnlineUser executor, @NotNull Trustable trustable,
                                         @NotNull ClaimWorld world, @NotNull Claim claim) {
        if (!claim.isPrivilegeAllowed(TrustLevel.Privilege.MANAGE_TRUSTEES, executor, world, plugin)) {
            plugin.getLocales().getLocale("no_managing_permission")
                    .ifPresent(executor::sendMessage);
            return false;
        }

        // Check if the executor is allowed to set the trust level of the trustable
        final Optional<Integer> trustableWeight = claim.getEffectiveTrustLevel(trustable, world, plugin)
                .map(TrustLevel::getWeight);
        final int executorWeight = claim.getEffectiveTrustLevel(executor, world, plugin)
                .map(TrustLevel::getWeight).orElse(Integer.MAX_VALUE);
        if (trustableWeight.isPresent() && executorWeight <= trustableWeight.get()) {
            plugin.getLocales().getLocale("error_trust_level_rank")
                    .ifPresent(executor::sendMessage);
            return false;
        }
        return true;
    }

    protected Optional<Trustable> resolveTrustable(@NotNull String name, @NotNull Claim claim) {
        // Resolve group
        final Settings.UserGroupSettings groups = plugin.getSettings().getUserGroups();
        if (groups.isEnabled() && name.startsWith(groups.getGroupSpecifierPrefix()) && claim.getOwner().isPresent()) {
            return claim.getOwner().flatMap(uuid -> plugin.getUserGroup(uuid,
                    name.substring(groups.getGroupSpecifierPrefix().length()))
            );
        }

        // Resolve user
        return plugin.getDatabase().getUser(name).map(SavedUser::user);
    }

    @NotNull
    protected static String getUsageText(@NotNull Settings.UserGroupSettings groupSettings) {
        return String.format(
                "[<player%s>]",
                groupSettings.isEnabled() ? String.format("|%s<group>", groupSettings.getGroupSpecifierPrefix()) : ""
        );
    }

    public abstract void execute(@NotNull OnlineUser executor, @NotNull ClaimWorld world, @NotNull Claim claim,
                                 @NotNull String[] args);

}
