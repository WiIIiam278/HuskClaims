/*
 * This file is part of HuskClaims, licensed under the Apache License 2.0.
 *
 *  Copyright (c) William278 <will27528@gmail.com>
 *  Copyright (c) contributors
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

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
