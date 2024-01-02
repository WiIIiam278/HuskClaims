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

import net.william278.huskclaims.BukkitHuskClaims;
import net.william278.huskclaims.config.Settings;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public interface BukkitHookProvider extends HookProvider {

    @Override
    @NotNull
    default List<Hook> getAvailableHooks() {
        final List<Hook> hooks = HookProvider.super.getAvailableHooks();
        final Settings.HookSettings settings = getPlugin().getSettings().getHooks();

        // Add bukkit hooks
        if (isDependencyAvailable("HuskHomes") && settings.getHuskHomes().isEnabled()) {
            hooks.add(new BukkitHuskHomesHook(getPlugin()));
        }
        if (isDependencyAvailable("Vault") && settings.getEconomy().isEnabled()) {
            hooks.add(new BukkitVaultEconomyHook(getPlugin()));
        }

        return hooks;
    }

    @Override
    default boolean isDependencyAvailable(@NotNull String name) {
        return ((BukkitHuskClaims) getPlugin()).getServer().getPluginManager().isPluginEnabled(name);
    }

}
