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

import net.william278.huskclaims.HuskClaims;
import net.william278.huskclaims.api.HuskClaimsAPI;
import net.william278.huskclaims.user.OnlineUser;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

/**
 * Represents a hook for handling economy functions within the plugin
 *
 * @since 1.0.4
 */
public abstract class EconomyHook extends Hook {

    @ApiStatus.Internal
    protected EconomyHook(@NotNull String name, @NotNull HuskClaims plugin) {
        super(name, plugin);
    }

    /**
     * Create an economy hook
     *
     * @param name The name of the hook
     * @param api  the {@link HuskClaimsAPI API instance}
     * @since 1.0.4
     */
    @SuppressWarnings("unused")
    public EconomyHook(@NotNull String name, @NotNull HuskClaimsAPI api) {
        this(name, api.getPlugin());
    }

    /**
     * Take money from a user's account
     *
     * @param user   the user
     * @param amount the amount
     * @return whether the transaction was successful
     * @since 1.0.4
     */
    public abstract boolean takeMoney(@NotNull OnlineUser user, double amount, @NotNull EconomyReason reason);

    /**
     * Format a double with the currency symbol
     *
     * @param amount the amount
     * @return the formatted string
     * @since 1.0
     */
    @NotNull
    public abstract String format(double amount);

    /**
     * Represents reasons for an economy transaction
     */
    public enum EconomyReason {
        /**
         * A transaction to purchase additional claim blocks
         */
        BUY_CLAIM_BLOCKS
    }

}
