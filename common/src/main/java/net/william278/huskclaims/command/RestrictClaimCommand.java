package net.william278.huskclaims.command;

import net.william278.huskclaims.HuskClaims;
import net.william278.huskclaims.claim.Claim;
import net.william278.huskclaims.claim.ClaimWorld;
import net.william278.huskclaims.claim.ClaimingMode;
import net.william278.huskclaims.user.OnlineUser;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class RestrictClaimCommand extends InClaimCommand {

    protected RestrictClaimCommand(@NotNull HuskClaims plugin) {
        super(List.of("restrictclaim"), "", null, plugin);
    }

    @Override
    public void execute(@NotNull OnlineUser executor, @NotNull ClaimWorld world,
                        @NotNull Claim claim, @NotNull String[] args) {
        // Only the owner can restrict claims as they bypass trust list requirements
        if ((claim.getOwner().isEmpty() && !ClaimingMode.ADMIN_CLAIMS.canUse(executor))
                || claim.getOwner().map(owner -> !owner.equals(executor.getUuid())).orElse(true)) {
            plugin.getLocales().getLocale("no_resizing_permission")
                    .ifPresent(executor::sendMessage);
            return;
        }
        restrictChildClaim(claim, world, executor);
    }

    private void restrictChildClaim(@NotNull Claim claim, @NotNull ClaimWorld world, @NotNull OnlineUser user) {
        if (!claim.isChildClaim(world)) {
            plugin.getLocales().getLocale("error_restrict_not_child")
                    .ifPresent(user::sendMessage);
            return;
        }
        claim.setInheritParent(!claim.isInheritParent());
        plugin.getDatabase().updateClaimWorld(world);
        plugin.getLocales().getLocale(claim.isInheritParent() ? "child_claims_inherit" : "child_claims_do_not_inherit")
                .ifPresent(user::sendMessage);
    }

}
