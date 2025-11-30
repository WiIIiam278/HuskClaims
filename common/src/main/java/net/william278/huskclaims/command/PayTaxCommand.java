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

import net.william278.huskclaims.HuskClaims;
import net.william278.huskclaims.config.Settings;
import net.william278.huskclaims.hook.EconomyHook;
import net.william278.huskclaims.tax.PropertyTaxManager;
import net.william278.huskclaims.user.OnlineUser;
import net.william278.huskclaims.user.SavedUser;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Optional;

/**
 * Command for paying property tax
 *
 * @since 1.5
 */
public class PayTaxCommand extends OnlineUserCommand implements PropertyTaxManager {

    public PayTaxCommand(@NotNull HuskClaims plugin) {
        super(
                List.of("paytax"),
                "[<amount>]",
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

        // Get user's saved data asynchronously for Folia compatibility
        plugin.runAsync(() -> {
            final Optional<SavedUser> savedUser = plugin.getDatabase().getUser(executor.getUuid());
            if (savedUser.isEmpty()) {
                plugin.runSync(executor, () -> {
                    plugin.getLocales().getLocale("error_invalid_user", executor.getName())
                            .ifPresent(executor::sendMessage);
                });
                return;
            }

            final SavedUser userData = savedUser.get();
            final double currentBalance = userData.getTaxBalance();
            final double totalOwed = getTotalTaxOwed(executor);
            final double amountToPay = totalOwed - currentBalance;

            // If no amount specified, show tax info
            if (args.length == 0) {
                plugin.runSync(executor, () -> {
                    showTaxInfo(executor, currentBalance, totalOwed, amountToPay, hook.get());
                    
                    // If there's an amount to pay, offer to pay it
                    if (amountToPay > 0) {
                        plugin.getLocales().getLocale("tax_info_pay_prompt",
                                hook.get().format(amountToPay))
                                .ifPresent(executor::sendMessage);
                    }
                });
                return;
            }

            // Parse amount
            final Optional<Double> amount = parseDouble(args[0]);
            if (amount.isEmpty() || amount.get() <= 0) {
                plugin.runSync(executor, () -> {
                    plugin.getLocales().getLocale("error_invalid_syntax", getUsage())
                            .ifPresent(executor::sendMessage);
                });
                return;
            }

            // Pay tax amount (this will handle async internally)
            plugin.runSync(executor, () -> {
                payTaxAmount(executor, amount.get(), hook.get());
            });
        });
    }

    private void payTaxAmount(@NotNull OnlineUser executor, double amount, @NotNull EconomyHook hook) {
        // Check if user has enough money
        if (!hook.takeMoney(executor, amount, EconomyHook.EconomyReason.PAY_TAX)) {
            plugin.getLocales().getLocale("error_insufficient_funds")
                    .ifPresent(executor::sendMessage);
            return;
        }

        // Add to tax balance (payTax handles async internally)
        plugin.runAsync(() -> {
            if (payTax(executor, amount)) {
                final Optional<SavedUser> savedUser = plugin.getDatabase().getUser(executor.getUuid());
                final double newBalance = savedUser.map(SavedUser::getTaxBalance).orElse(0.0);
                plugin.runSync(executor, () -> {
                    plugin.getLocales().getLocale("tax_paid", hook.format(amount), hook.format(newBalance))
                            .ifPresent(executor::sendMessage);
                });
            } else {
                // Refund if payment failed
                plugin.runSync(executor, () -> {
                    hook.takeMoney(executor, -amount, EconomyHook.EconomyReason.PAY_TAX);
                    plugin.getLocales().getLocale("error_tax_payment_failed")
                            .ifPresent(executor::sendMessage);
                });
            }
        });
    }

    private void showTaxInfo(@NotNull OnlineUser executor, double currentBalance, double totalOwed, double amountToPay, @NotNull EconomyHook hook) {
        // Show current tax balance
        if (currentBalance > 0) {
            plugin.getLocales().getLocale("tax_info_balance",
                    hook.format(currentBalance))
                    .ifPresent(executor::sendMessage);
        } else {
            plugin.getLocales().getLocale("tax_info_balance_zero")
                    .ifPresent(executor::sendMessage);
        }

        // Show total tax owed across all claims
        plugin.getLocales().getLocale("tax_info_total_owed",
                hook.format(totalOwed))
                .ifPresent(executor::sendMessage);

        // Show amount to pay (if any)
        if (amountToPay > 0) {
            plugin.getLocales().getLocale("tax_info_amount_to_pay",
                    hook.format(amountToPay))
                    .ifPresent(executor::sendMessage);
        } else if (currentBalance > totalOwed) {
            // Show prepaid amount
            final double prepaid = currentBalance - totalOwed;
            plugin.getLocales().getLocale("tax_info_prepaid",
                    hook.format(prepaid))
                    .ifPresent(executor::sendMessage);
        }
    }

    private Optional<Double> parseDouble(@NotNull String value) {
        try {
            return Optional.of(Double.parseDouble(value));
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }

}
