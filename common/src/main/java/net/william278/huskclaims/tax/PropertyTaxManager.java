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

import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.logging.Level;

/**
 * Manager for handling property tax calculations and payments
 * <p>
 * <b>When Tax Calculations Are Updated:</b>
 * <ul>
 *   <li><b>Tax Owed (calculateTaxOwed):</b> Calculated dynamically on-demand every time it's called.
 *       It's based on: days since lastTaxCalculation * blocks * taxRate. This is NOT stored,
 *       it's calculated fresh each time. Tax accrues continuously based on time, even when players are offline.</li>
 *   <li><b>Tax Balance:</b> Stored in database, only changes when:
 *       <ul>
 *         <li>User pays tax via /paytax command (adds to balance)</li>
 *         <li>autoPayTaxFromBalance() is called (deducts from balance when sufficient)</li>
 *         <li>Scheduled tax processing task runs (every hour, processes all users)</li>
 *       </ul>
 *   </li>
 *   <li><b>lastTaxCalculation Date:</b> Updated when:
 *       <ul>
 *         <li>Claim is created (set to creation time)</li>
 *         <li>Claim is expanded (set to current time)</li>
 *         <li>autoPayTaxFromBalance() runs and balance covers all tax (set to current time)</li>
 *         <li>Scheduled tax processing task auto-pays from balance (set to current time)</li>
 *       </ul>
 *   </li>
 * </ul>
 * <p>
 * <b>When autoPayTaxFromBalance() is Called:</b>
 * <ul>
 *   <li>When user runs /taxinfo command</li>
 *   <li>When user enters their own claim</li>
 *   <li>When scheduled tax processing task runs (every hour, for all users including offline)</li>
 *   <li>On server startup (pruneOverdueTaxClaims checks all claims)</li>
 * </ul>
 * <p>
 * <b>What Happens When Player is Offline:</b>
 * <ul>
 *   <li><b>With Prepaid Balance:</b>
 *       <ul>
 *         <li>Tax continues to accrue based on time (days since lastTaxCalculation)</li>
 *         <li>Scheduled task (every hour) automatically deducts tax from balance when sufficient</li>
 *         <li>If balance is exhausted, tax continues to accrue and claim becomes overdue</li>
 *         <li>After grace period (dueDays), claim is automatically unclaimed on server startup or scheduled task</li>
 *       </ul>
 *   </li>
 *   <li><b>Without Prepaid Balance:</b>
 *       <ul>
 *         <li>Tax continues to accrue based on time</li>
 *         <li>No automatic payment (no balance to use)</li>
 *         <li>After grace period (dueDays), claim is automatically unclaimed on server startup or scheduled task</li>
 *         <li>Example: If dueDays=30, player offline for 34 days with no balance = claim unclaimed</li>
 *       </ul>
 *   </li>
 *   <li><b>Important: Tax Accumulation During Grace Period:</b>
 *       <ul>
 *         <li>Tax CONTINUES to accumulate during the grace period</li>
 *         <li>The grace period (dueDays) only delays when the claim gets unclaimed</li>
 *         <li>Tax keeps accruing every day: days × blocks × rate</li>
 *         <li>Example: If dueDays=30 and you owe $10 on day 1, you'll owe $40 on day 30 (if rate is $1/day)</li>
 *         <li>The grace period is a buffer before unclaiming, NOT a tax-free period</li>
 *       </ul>
 *   </li>
 *   <li><b>Server Startup:</b>
 *       <ul>
 *         <li>pruneOverdueTaxClaims() runs automatically</li>
 *         <li>Checks all claims for overdue tax</li>
 *         <li>Unclaims claims that are overdue beyond grace period</li>
 *       </ul>
 *   </li>
 * </ul>
 * <p>
 * <b>Tax Balance vs Net Balance:</b>
 * <ul>
 *   <li><b>Tax Balance:</b> The prepaid amount stored in database (always >= 0)</li>
 *   <li><b>Tax Owed:</b> Calculated dynamically based on time since lastTaxCalculation</li>
 *   <li><b>Net Balance:</b> Tax Balance - Tax Owed
 *       <ul>
 *         <li>If positive: You have prepaid tax</li>
 *         <li>If negative: You owe tax (shown as negative balance)</li>
 *         <li>If zero: Balanced</li>
 *       </ul>
 *   </li>
 * </ul>
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

        final double defaultRate = settings.getDefaultTaxRatePerDayPerBlock();

        // Check for permission-based tax rate
        if (user instanceof OnlineUser onlineUser) {
            final Optional<Double> permissionRate = getTaxRateFromPermissions(onlineUser);
            if (permissionRate.isPresent()) {
                final double rate = permissionRate.get();
                // Validate the permission rate is reasonable (within 1000x of default)
                // This prevents wildcard permissions from causing issues
                final double maxReasonableRate = Math.max(defaultRate * 1000, 10.0);
                if (rate > 0 && rate <= maxReasonableRate) {
                    return rate;
                } else {
                    // Log warning if unreasonable rate found
                    getPlugin().log(java.util.logging.Level.WARNING, 
                        String.format("User %s has unreasonable tax rate permission: %.2f (default: %.2f). Using default rate.",
                            user.getName(), rate, defaultRate));
                }
            }
        }

        return defaultRate;
    }

    /**
     * Get tax rate from user permissions
     * Checks permissions like huskclaims.tax.rate.0.01, huskclaims.tax.rate.0.05, etc.
     * <p>
     * Note: Only explicit permission nodes are checked. Wildcard permissions that might
     * grant high values (like huskclaims.tax.rate.*) are ignored to prevent incorrect rates.
     *
     * @param user the online user
     * @return the tax rate if found in permissions
     */
    default Optional<Double> getTaxRateFromPermissions(@NotNull OnlineUser user) {
        final double defaultRate = getTaxSettings().getDefaultTaxRatePerDayPerBlock();
        
        // Check common decimal rates by testing explicit permission nodes
        // We check common values: 0.001, 0.01, 0.05, 0.1, 0.5, 1.0, etc.
        // Only check rates that are reasonable (within 100x of default rate)
        final double maxReasonableRate = Math.max(defaultRate * 100, 1.0);
        final double[] commonRates = {0.001, 0.002, 0.005, 0.01, 0.02, 0.05, 0.1, 0.2, 0.5, 1.0, 2.0, 5.0, 10.0};
        double highestRate = -1.0;

        for (double rate : commonRates) {
            // Skip rates that are unreasonably high compared to default
            if (rate > maxReasonableRate) {
                continue;
            }
            
            // Try different formats: 0.01, 0_01, 001
            final String[] formats = {
                    String.format("huskclaims.tax.rate.%.3f", rate),
                    String.format("huskclaims.tax.rate.%.2f", rate),
                    String.format("huskclaims.tax.rate.%.1f", rate),
                    String.format("huskclaims.tax.rate.%d", (int) rate)
            };

            for (String permission : formats) {
                // Only check if user has the explicit permission (not wildcard)
                if (user.hasPermission(permission, false)) {
                    // Verify this is an explicit permission, not a wildcard match
                    // by checking if the permission node exists explicitly
                    if (rate > highestRate) {
                        highestRate = rate;
                    }
                    break; // Found a match for this rate, move to next
                }
            }
        }
        
        // Also check numerical permission, but validate it's reasonable
        final Optional<Long> numericalRate = user.getNumericalPermission("huskclaims.tax.rate.");
        if (numericalRate.isPresent()) {
            final double numRate = numericalRate.get().doubleValue();
            // Only use if it's reasonable (within 100x of default)
            if (numRate <= maxReasonableRate && numRate > 0) {
                if (numRate > highestRate) {
                    highestRate = numRate;
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

    /**
     * Load the scheduled task for automatic tax processing
     * <p>
     * This task runs periodically (every hour) to:
     * 1. Automatically pay tax from balance for all users (online and offline)
     * 2. Process overdue tax claims
     * <p>
     * This ensures tax is processed even when players are offline.
     */
    default void loadTaxScheduler() {
        final Settings.ClaimSettings.PropertyTaxSettings settings = getTaxSettings();
        if (!settings.isEnabled()) {
            return;
        }

        // Run every hour to process tax
        getPlugin().getRepeatingTask(
                () -> {
                    try {
                        processTaxForAllUsers();
                    } catch (Exception e) {
                        getPlugin().log(Level.SEVERE, "Error processing tax for all users", e);
                    }
                },
                Duration.ofHours(1),
                Duration.ofMinutes(5) // Start 5 minutes after server start
        ).run();
    }

    /**
     * Process tax for all users (both online and offline)
     * <p>
     * This method:
     * 1. Automatically pays tax from balance when sufficient
     * 2. Checks for overdue claims and processes them
     * <p>
     * This ensures tax is processed even when players don't log in.
     */
    default void processTaxForAllUsers() {
        final Settings.ClaimSettings.PropertyTaxSettings settings = getTaxSettings();
        if (!settings.isEnabled()) {
            return;
        }

        // Get all unique claim owners
        final Set<UUID> claimOwners = new java.util.HashSet<>();
        for (ClaimWorld world : getPlugin().getClaimWorlds().values()) {
            for (Claim claim : world.getClaims()) {
                if (claim.getOwner().isPresent() && !claim.isAdminClaim()) {
                    claimOwners.add(claim.getOwner().get());
                }
            }
        }

        int processedUsers = 0;
        int autoPaidUsers = 0;
        
        // Process tax for each owner
        for (UUID ownerUuid : claimOwners) {
            try {
                final Optional<SavedUser> savedUser = getPlugin().getDatabase().getUser(ownerUuid);
                if (savedUser.isEmpty()) {
                    continue;
                }

                final User owner = savedUser.get().getUser();
                
                // Auto-pay tax from balance if sufficient
                if (autoPayTaxFromBalance(owner)) {
                    autoPaidUsers++;
                }
                
                processedUsers++;
            } catch (Exception e) {
                getPlugin().log(Level.WARNING, 
                    String.format("Error processing tax for user %s", ownerUuid), e);
            }
        }

        // Process overdue claims (this also runs on server startup)
        getPlugin().runAsync(() -> {
            try {
                getPlugin().pruneOverdueTaxClaims();
            } catch (Exception e) {
                getPlugin().log(Level.SEVERE, "Error pruning overdue tax claims", e);
            }
        });

        if (processedUsers > 0) {
            getPlugin().log(Level.INFO, String.format(
                "Tax processing cycle completed: %d users processed, %d users had tax auto-paid from balance",
                processedUsers, autoPaidUsers));
        }
    }

}
