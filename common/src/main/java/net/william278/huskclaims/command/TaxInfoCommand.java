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

package net.william278.huskclaims.command;

import com.google.common.collect.Lists;
import de.themoep.minedown.adventure.MineDown;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.william278.huskclaims.HuskClaims;
import net.william278.huskclaims.claim.Claim;
import net.william278.huskclaims.claim.ClaimWorld;
import net.william278.huskclaims.config.Locales;
import net.william278.huskclaims.config.Settings;
import net.william278.huskclaims.hook.EconomyHook;
import net.william278.huskclaims.tax.PropertyTaxManager;
import net.william278.huskclaims.user.OnlineUser;
import net.william278.huskclaims.user.SavedUser;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Optional;

/**
 * Command for viewing property tax information
 *
 * @since 1.5
 */
public class TaxInfoCommand extends OnlineUserCommand implements PropertyTaxManager {

    public TaxInfoCommand(@NotNull HuskClaims plugin) {
        super(
                List.of("taxinfo", "propertytax"),
                "",
                plugin
        );
    }

    @Override
    public void execute(@NotNull OnlineUser executor, @NotNull String[] args) {
        final Settings.ClaimSettings.PropertyTaxSettings settings = getPlugin().getSettings().getClaims().getPropertyTax();
        if (!settings.isEnabled()) {
            plugin.getLocales().getLocale("error_property_tax_disabled")
                    .ifPresent(executor::sendMessage);
            return;
        }

        // Get economy hook
        final Optional<EconomyHook> hook = plugin.getHook(EconomyHook.class);
        if (hook.isEmpty()) {
            plugin.getLocales().getLocale("error_economy_not_found")
                    .ifPresent(executor::sendMessage);
            return;
        }

        // Get user's saved data
        final Optional<SavedUser> savedUser = plugin.getDatabase().getUser(executor.getUuid());
        if (savedUser.isEmpty()) {
            plugin.getLocales().getLocale("error_invalid_user", executor.getName())
                    .ifPresent(executor::sendMessage);
            return;
        }

        final SavedUser userData = savedUser.get();
        
        // Automatically pay tax from balance if sufficient
        autoPayTaxFromBalance(executor);
        
        // Refresh user data after auto-payment
        final Optional<SavedUser> updatedUserData = plugin.getDatabase().getUser(executor.getUuid());
        final SavedUser finalUserData = updatedUserData.orElse(userData);
        
        final double taxBalance = finalUserData.getTaxBalance();
        final double totalTaxOwed = getTotalTaxOwed(executor);
        final double netBalance = taxBalance - totalTaxOwed;
        final double taxRate = getTaxRate(executor);

        // Calculate daily tax rate across all claims
        double totalDailyTax = 0.0;
        long totalClaimBlocks = 0;
        for (ClaimWorld world : plugin.getClaimWorlds().values()) {
            for (Claim claim : world.getClaims()) {
                if (claim.getOwner().isPresent() && claim.getOwner().get().equals(executor.getUuid())) {
                    final long claimBlocks = claim.getRegion().getSurfaceArea();
                    totalClaimBlocks += claimBlocks;
                    totalDailyTax += claimBlocks * taxRate;
                }
            }
        }

        // Calculate next payment due date
        // This is when the balance will be exhausted (if positive) or when payment is needed (if negative)
        long daysUntilDue = 0;
        java.time.OffsetDateTime nextDueDate = null;
        if (netBalance > 0.01 && totalDailyTax > 0.01) {
            // Positive balance - calculate when it will be exhausted
            daysUntilDue = (long) Math.floor(netBalance / totalDailyTax);
            nextDueDate = java.time.OffsetDateTime.now().plusDays(daysUntilDue);
        } else if (netBalance < -0.01) {
            // Negative balance - payment is due now
            nextDueDate = java.time.OffsetDateTime.now();
            daysUntilDue = 0;
        } else if (netBalance <= 0.01 && totalDailyTax > 0.01) {
            // Balance is zero or very small - next payment due in 1 day
            nextDueDate = java.time.OffsetDateTime.now().plusDays(1);
            daysUntilDue = 1;
        }

        // Prepare messages
        final Locales locales = plugin.getLocales();
        final List<MineDown> lines = Lists.newArrayList();
        
        locales.getLocale("tax_info_header", executor.getName()).ifPresent(lines::add);
        
        // Always show the raw tax balance (prepaid amount)
        locales.getLocale("tax_info_balance", hook.get().format(taxBalance)).ifPresent(lines::add);
        
        // Always show total tax owed
        locales.getLocale("tax_info_total_owed", hook.get().format(Math.max(0.0, totalTaxOwed))).ifPresent(lines::add);
        
        // Show daily tax charge
        if (totalDailyTax > 0.01) {
            locales.getLocale("tax_info_daily_charge", hook.get().format(totalDailyTax), Long.toString(totalClaimBlocks))
                    .ifPresent(lines::add);
        }
        
        // Show net balance - this is the key metric (balance - owed)
        // If negative, you owe money; if positive, you have prepaid
        if (netBalance > 0.01) {
            // Positive net balance = prepaid
            locales.getLocale("tax_info_net_balance", hook.get().format(netBalance)).ifPresent(lines::add);
            if (daysUntilDue > 0 && nextDueDate != null) {
                locales.getLocale("tax_info_days_until_due", Long.toString(daysUntilDue)).ifPresent(lines::add);
            }
        } else if (netBalance < -0.01) {
            // Negative net balance = you owe money
            final double amountOwed = -netBalance;
            locales.getLocale("tax_info_net_balance_negative", hook.get().format(amountOwed)).ifPresent(lines::add);
            locales.getLocale("tax_info_amount_owed", hook.get().format(amountOwed)).ifPresent(lines::add);
        } else {
            // Balanced (net balance is 0)
            locales.getLocale("tax_info_balanced").ifPresent(lines::add);
        }
        
        // Always show next payment due date if we have one
        if (nextDueDate != null && totalDailyTax > 0.01) {
            locales.getLocale("tax_info_next_due_date", nextDueDate.format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")))
                    .ifPresent(lines::add);
        }
        
        locales.getLocale("tax_info_rate", hook.get().format(taxRate)).ifPresent(lines::add);
        locales.getLocale("tax_info_due_days", Long.toString(settings.getDueDays())).ifPresent(lines::add);

        // Build component & dispatch
        final TextComponent.Builder builder = Component.text();
        lines.stream().map(MineDown::toComponent).forEach(c -> builder.append(c).append(Component.newline()));
        executor.sendMessage(builder.build());
    }

}
