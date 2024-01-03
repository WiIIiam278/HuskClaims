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

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import net.william278.huskclaims.HuskClaims;
import net.william278.huskclaims.config.Settings;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Unmodifiable;

import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Level;
import java.util.stream.Collectors;

public interface HookProvider {

    @NotNull
    Set<Hook> getHooks();

    default <H extends Hook> Optional<H> getHook(@NotNull Class<H> hookClass) {
        return getHooks().stream()
                .filter(hook -> hookClass.isAssignableFrom(hook.getClass()))
                .map(hookClass::cast)
                .findFirst();
    }

    @NotNull
    @Unmodifiable
    default Set<Importer> getImporters() {
        return getHooks().stream()
                .filter(hook -> hook instanceof Importer)
                .map(hook -> (Importer) hook)
                .collect(Collectors.toSet());
    }

    default Optional<Importer> getImporterByName(@NotNull String name) {
        return getImporters().stream()
                .filter(i -> i.getName().toLowerCase(Locale.ENGLISH).equals(name.toLowerCase(Locale.ENGLISH)))
                .findFirst();
    }

    void setHooks(@NotNull Set<Hook> hooks);

    default void loadHooks() {
        setHooks(Sets.newHashSet(getAvailableHooks()));
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

    @NotNull
    default List<Hook> getAvailableHooks() {
        final List<Hook> hooks = Lists.newArrayList();
        final Settings.HookSettings settings = getPlugin().getSettings().getHooks();

        // Add common hooks
        if (isDependencyAvailable("LuckPerms") && settings.getLuckPerms().isEnabled()) {
            hooks.add(new LuckPermsHook(getPlugin()));
        }

        return hooks;
    }

    boolean isDependencyAvailable(@NotNull String name);

    @NotNull
    HuskClaims getPlugin();

}
