package net.william278.huskclaims.hook;

import net.kyori.adventure.text.format.TextColor;
import net.william278.huskclaims.HuskClaims;
import net.william278.huskclaims.claim.Claim;
import net.william278.huskclaims.claim.ClaimWorld;
import net.william278.huskclaims.claim.ClaimingMode;
import net.william278.huskclaims.config.Settings;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

public abstract class MapHook extends Hook {

    protected MapHook(@NotNull String name, @NotNull HuskClaims plugin) {
        super(name, plugin);
    }

    public abstract void markClaim(@NotNull Claim claim, @NotNull ClaimWorld claimWorld);

    public abstract void markClaims(@NotNull Iterable<Claim> claims, @NotNull ClaimWorld claimWorld);

    public abstract void unMarkClaim(@NotNull Claim claim, @NotNull ClaimWorld claimWorld);

    public abstract void unMarkClaimsBy(@Nullable UUID owner);

    public final void unMarkAdminClaims() {
        unMarkClaimsBy(null);
    }

    public abstract void unMarkAllClaims();

    @NotNull
    protected final String getMarkerSetKey() {
        return plugin.getKey(getName().toLowerCase(Locale.ENGLISH), "markers").toString();
    }

    @NotNull
    protected Settings.HookSettings.MapHookSettings getSettings() {
        return plugin.getSettings().getHooks().getMap();
    }

    protected Optional<TextColor> getClaimColor(@NotNull Claim claim, @NotNull ClaimWorld claimWorld) {
        final ClaimingMode type = claim.isChildClaim(claimWorld) ? ClaimingMode.CHILD_CLAIMS
                : claim.getOwner().isPresent() ? ClaimingMode.CLAIMS : ClaimingMode.ADMIN_CLAIMS;
        return Optional.ofNullable(getSettings().getColors().get(type)).map(TextColor::fromHexString);
    }

}
