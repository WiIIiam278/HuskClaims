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

import lombok.AllArgsConstructor;
import net.william278.huskclaims.HuskClaims;
import net.william278.huskclaims.config.Settings;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Set;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.stream.Collectors;

public interface HookProvider {

    @NotNull
    Set<Hook> getHooks();

    void setHooks(@NotNull Set<Hook> hooks);

    default void loadHooks() {
        setHooks(HookBootstrap.getAvailable(getPlugin()));
        getHooks().removeIf(hook -> {
            try {
                hook.load();
                return false;
            } catch (Throwable e) {
                getPlugin().log(Level.SEVERE, "Failed to load the " + hook.getName() + " hook", e);
            }
            return true;
        });
    }

    default void unloadHooks() {
        getHooks().removeIf(hook -> {
            try {
                hook.unload();
            } catch (Throwable e) {
                getPlugin().log(Level.SEVERE, "Failed to unload the " + hook.getName() + " hook", e);
            }
            return true;
        });
    }

    boolean isDependencyAvailable(@NotNull String name);

    @NotNull
    HuskClaims getPlugin();

    @AllArgsConstructor
    enum HookBootstrap {
        LUCKPERMS("LuckPerms", (settings -> settings.getLuckPerms().isEnabled()), LuckPermsHook::new);

        private final String dependency;
        private final Function<Settings.HookSettings, Boolean> shouldEnable;
        private final Function<HuskClaims, Hook> supplier;

        @NotNull
        private static Set<Hook> getAvailable(@NotNull HuskClaims plugin) {
            return Arrays.stream(values())
                    .filter(hook -> plugin.isDependencyAvailable(hook.dependency))
                    .filter(hook -> hook.shouldEnable.apply(plugin.getSettings().getHooks()))
                    .map(hook -> hook.supplier.apply(plugin))
                    .peek(hook -> plugin.log(Level.INFO, "Loaded " + hook.getName() + " hook"))
                    .collect(Collectors.toSet());
        }

    }

}
