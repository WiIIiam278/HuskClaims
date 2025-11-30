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

package net.william278.huskclaims.tax;

import net.william278.huskclaims.HuskClaims;
import net.william278.huskclaims.claim.Claim;
import net.william278.huskclaims.claim.ClaimWorld;
import net.william278.huskclaims.config.Settings;
import net.william278.huskclaims.position.World;
import net.william278.huskclaims.user.OnlineUser;
import net.william278.huskclaims.user.SavedUser;
import net.william278.huskclaims.user.User;
import org.jetbrains.annotations.NotNull;

import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Manager for handling property tax calculations and payments
 *
 * @since 1.5
 */
public interface PropertyTaxManager {

    @NotNull
    HuskClaims getPlugin();

    /**
     * Get the tax rate for a user (from permissions or default)
     *
     * @param user the user to get the tax rate for
     * @return the tax rate per day per block
     */
    default double getTaxRate(@NotNull User user) {
        final Settings.ClaimSettings.PropertyTaxSettings settings = getTaxSettings();
        if (!settings.isEnabled()) {
            return 0.0;
        }

        // Check for permission-based tax rate
        if (user instanceof OnlineUser onlineUser) {
            final Optional<Double> permissionRate = getTaxRateFromPermissions(onlineUser);
            if (permissionRate.isPresent()) {
                return permissionRate.get();
            }
        }

        return settings.getDefaultTaxRatePerDayPerBlock();
    }

    /**
     * Get tax rate from user permissions
     * Checks permissions like huskclaims.tax.rate.0.01, huskclaims.tax.rate.0.05, etc.
     *
     * @param user the online user
     * @return the tax rate if found in permissions
     */
    default Optional<Double> getTaxRateFromPermissions(@NotNull OnlineUser user) {
        // Try to get numerical permission first (for integer rates)
        final Optional<Long> numericalRate = user.getNumericalPermission("huskclaims.tax.rate.");
        if (numericalRate.isPresent()) {
            return Optional.of(numericalRate.get().doubleValue());
        }

        // Check common decimal rates by testing permission nodes
        // We check common values: 0.001, 0.01, 0.05, 0.1, 0.5, 1.0, etc.
        final double[] commonRates = {0.001, 0.002, 0.005, 0.01, 0.02, 0.05, 0.1, 0.2, 0.5, 1.0, 2.0, 5.0, 10.0};
        double highestRate = -1.0;

        for (double rate : commonRates) {
            // Try different formats: 0.01, 0_01, 001
            final String[] formats = {
                    String.format("huskclaims.tax.rate.%.3f", rate),
                    String.format("huskclaims.tax.rate.%.2f", rate),
                    String.format("huskclaims.tax.rate.%.1f", rate),
                    String.format("huskclaims.tax.rate.%d", (int) rate)
            };

            for (String permission : formats) {
                if (user.hasPermission(permission, false)) {
                    if (rate > highestRate) {
                        highestRate = rate;
                    }
                    break; // Found a match for this rate, move to next
                }
            }
        }

        return highestRate >= 0 ? Optional.of(highestRate) : Optional.empty();
    }

    /**
     * Calculate the tax owed for a claim
     *
     * @param claim the claim to calculate tax for
     * @param world the claim world
     * @return the tax amount owed
     */
    default double calculateTaxOwed(@NotNull Claim claim, @NotNull ClaimWorld world) {
        final Settings.ClaimSettings.PropertyTaxSettings settings = getTaxSettings();
        if (!settings.isEnabled() || claim.isAdminClaim() || claim.getOwner().isEmpty()) {
            return 0.0;
        }

        final UUID owner = claim.getOwner().get();
        if (settings.getExcludedUsers().contains(owner.toString())) {
            return 0.0;
        }

        final String worldName = world.getName(getPlugin());
        if (settings.getExcludedWorlds().contains(worldName)) {
            return 0.0;
        }

        // Get tax rate for owner
        final Optional<SavedUser> savedUser = getPlugin().getDatabase().getUser(owner);
        if (savedUser.isEmpty()) {
            return 0.0;
        }
        final double taxRate = getTaxRate(savedUser.get().getUser());

        if (taxRate <= 0.0) {
            return 0.0;
        }

        // Calculate days since last tax calculation (or creation)
        final OffsetDateTime now = OffsetDateTime.now();
        final OffsetDateTime lastCalculation = claim.getLastTaxCalculation()
                .orElse(claim.getCreationTime().orElse(now));
        
        // Calculate time difference - use hours for more precision, then convert to days
        final long hours = ChronoUnit.HOURS.between(lastCalculation, now);
        final double days = hours / 24.0;

        if (days <= 0.0) {
            return 0.0;
        }

        // Calculate tax: days * blocks * rate
        final long claimBlocks = claim.getRegion().getSurfaceArea();
        return days * claimBlocks * taxRate;
    }

    /**
     * Get the number of days a claim is overdue
     *
     * @param claim the claim to check
     * @param world the claim world
     * @param userTaxBalance the user's current tax balance
     * @return the number of days overdue (0 if not overdue)
     */
    default long getDaysOverdue(@NotNull Claim claim, @NotNull ClaimWorld world, double userTaxBalance) {
        final Settings.ClaimSettings.PropertyTaxSettings settings = getTaxSettings();
        if (!settings.isEnabled()) {
            return 0;
        }

        final double taxOwed = calculateTaxOwed(claim, world);
        final double totalOwed = taxOwed - userTaxBalance;

        if (totalOwed <= 0) {
            return 0; // Not overdue
        }

        // Calculate how many days of tax this represents
        final Optional<UUID> owner = claim.getOwner();
        if (owner.isEmpty()) {
            return 0;
        }

        final Optional<SavedUser> savedUser = getPlugin().getDatabase().getUser(owner.get());
        if (savedUser.isEmpty()) {
            return 0;
        }

        final double taxRate = getTaxRate(savedUser.get().getUser());
        if (taxRate <= 0.0) {
            return 0;
        }

        final long claimBlocks = claim.getRegion().getSurfaceArea();
        final double dailyTax = claimBlocks * taxRate;
        if (dailyTax <= 0.0) {
            return 0;
        }

        // Calculate days overdue based on total owed
        // This represents how many days of tax are unpaid
        // Use precise calculation to handle partial days
        final double daysOverdueDouble = totalOwed / dailyTax;
        final long daysOverdue = (long) Math.ceil(daysOverdueDouble);
        return Math.max(0, daysOverdue);
    }

    /**
     * Check if a claim is overdue and should be unclaimed
     *
     * @param claim the claim to check
     * @param world the claim world
     * @param userTaxBalance the user's current tax balance
     * @return true if the claim should be unclaimed
     */
    default boolean isClaimOverdue(@NotNull Claim claim, @NotNull ClaimWorld world, double userTaxBalance) {
        final Settings.ClaimSettings.PropertyTaxSettings settings = getTaxSettings();
        if (!settings.isEnabled()) {
            return false;
        }

        final long daysOverdue = getDaysOverdue(claim, world, userTaxBalance);
        return daysOverdue >= settings.getDueDays();
    }

    /**
     * Get the total tax owed across all claims for a user
     *
     * @param user the user
     * @return the total tax owed
     */
    default double getTotalTaxOwed(@NotNull User user) {
        final Settings.ClaimSettings.PropertyTaxSettings settings = getTaxSettings();
        if (!settings.isEnabled()) {
            return 0.0;
        }

        double total = 0.0;
        for (ClaimWorld world : getPlugin().getClaimWorlds().values()) {
            for (Claim claim : world.getClaims()) {
                if (claim.getOwner().isPresent() && claim.getOwner().get().equals(user.getUuid())) {
                    total += calculateTaxOwed(claim, world);
                }
            }
        }
        return total;
    }

    /**
     * Process tax payment for a user
     * <p>
     * When tax is paid:
     * 1. The payment is added to the user's tax balance
     * 2. The balance automatically reduces what's owed (tax is calculated dynamically)
     * <p>
     * The tax balance is shared across all claims, so multiple claims use the same balance.
     * Tax is calculated based on days since lastTaxCalculation, and the balance reduces
     * the net amount owed.
     *
     * @param user the user paying
     * @param amount the amount to pay
     * @return true if payment was successful
     */
    default boolean payTax(@NotNull OnlineUser user, double amount) {
        if (amount <= 0.0) {
            return false;
        }

        final Optional<SavedUser> savedUser = getPlugin().getDatabase().getUser(user.getUuid());
        if (savedUser.isEmpty()) {
            return false;
        }

        final SavedUser userData = savedUser.get();
        final double currentBalance = userData.getTaxBalance();
        
        // Simply add to balance - the balance will automatically reduce what's owed
        // Tax is calculated dynamically based on days since lastTaxCalculation
        userData.setTaxBalance(currentBalance + amount);
        
        getPlugin().getDatabase().updateUser(userData);
        return true;
    }

    /**
     * Automatically deduct tax from balance and update lastTaxCalculation dates
     * when the balance is sufficient to cover tax owed.
     * <p>
     * This method should be called periodically or when checking tax info to ensure
     * that tax is automatically paid from the balance when sufficient funds are available.
     *
     * @param user the user to process tax for
     * @return true if any tax was automatically paid from balance
     */
    default boolean autoPayTaxFromBalance(@NotNull User user) {
        final Optional<SavedUser> savedUser = getPlugin().getDatabase().getUser(user.getUuid());
        if (savedUser.isEmpty()) {
            return false;
        }

        final SavedUser userData = savedUser.get();
        final double currentBalance = userData.getTaxBalance();
        final double totalTaxOwed = getTotalTaxOwed(user);
        final double netOwed = totalTaxOwed - currentBalance;

        // If balance is sufficient to cover all tax, automatically pay it
        if (netOwed <= 0.01 && totalTaxOwed > 0.01) {
            // Balance covers all tax - update lastTaxCalculation dates to now
            final OffsetDateTime now = OffsetDateTime.now();
            boolean updated = false;
            
            for (ClaimWorld world : getPlugin().getClaimWorlds().values()) {
                for (Claim claim : world.getClaims()) {
                    if (claim.getOwner().isPresent() && claim.getOwner().get().equals(user.getUuid())) {
                        final double claimTaxOwed = calculateTaxOwed(claim, world);
                        if (claimTaxOwed > 0.01) {
                            claim.setLastTaxCalculation(now);
                            updated = true;
                        }
                    }
                }
            }
            
            if (updated) {
                // Deduct the tax from balance
                userData.setTaxBalance(currentBalance - totalTaxOwed);
                getPlugin().getDatabase().updateUser(userData);
                return true;
            }
        }
        
        return false;
    }

    /**
     * Update tax calculation date for a claim after expansion
     * <p>
     * When a claim is expanded, we update the lastTaxCalculation to the current time.
     * This ensures that tax is only calculated on the new size from the expansion point forward.
     * The tax balance is NOT affected - it remains unchanged and can be used by all claims.
     *
     * @param claim the claim that was expanded
     */
    default void onClaimExpanded(@NotNull Claim claim) {
        // Update lastTaxCalculation to now so tax is calculated on new size from this point
        // The tax balance is NOT reset - it remains available for all claims
        claim.setLastTaxCalculation(OffsetDateTime.now());
    }

    /**
     * Adjust tax balance when a claim is unclaimed
     * <p>
     * When a claim is unclaimed, the tax that was owed for that claim should no longer be owed.
     * Since taxBalance represents prepaid tax (positive) that reduces what's owed, and tax is
     * calculated dynamically based on days since last calculation, we reset the last tax calculation
     * date for the claim before it's removed. However, since the claim is being deleted, we
     * don't need to do anything - the tax will simply not be calculated for it anymore.
     * <p>
     * The prepaid balance stays unchanged so other claims can use it.
     *
     * @param claim the claim being unclaimed
     * @param world the claim world
     * @param owner the owner of the claim
     */
    default void onClaimUnclaimed(@NotNull Claim claim, @NotNull ClaimWorld world, @NotNull User owner) {
        // When a claim is unclaimed, it will no longer accrue tax, so we don't need to adjust
        // the tax balance. The prepaid balance stays for other claims to use.
        // Tax is calculated dynamically, so removing the claim automatically removes its tax burden.
    }

    @NotNull
    default Settings.ClaimSettings.PropertyTaxSettings getTaxSettings() {
        return getPlugin().getSettings().getClaims().getPropertyTax();
    }

}
