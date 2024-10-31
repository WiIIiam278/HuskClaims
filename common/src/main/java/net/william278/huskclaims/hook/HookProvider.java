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

import java.util.*;
import java.util.logging.Level;
import java.util.stream.Collectors;

public interface HookProvider extends MapHookProvider {

    @NotNull
    Set<Hook> getHooks();

    default <H extends Hook> Optional<H> getHook(@NotNull Class<H> hookClass) {
        return getHooks().stream()
                .filter(hook -> hookClass.isAssignableFrom(hook.getClass()))
                .map(hookClass::cast)
                .findFirst();
    }

    @NotNull
    default List<RestrictedRegionHook> getRestrictedRegionHooks() {
        return getHooks().stream()
                .filter(hook -> hook instanceof RestrictedRegionHook)
                .map(hook -> (RestrictedRegionHook) hook)
                .toList();
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

    default void loadHooks(@NotNull PluginHook.Register... register) {
        final Set<PluginHook.Register> registers = Arrays.stream(register).collect(Collectors.toSet());
        final List<Hook> load = getAvailableHooks().stream().filter(h -> registers.contains(h.getRegister())).toList();
        setHooks(Sets.newHashSet(load));
    }

    default void registerHooks(@NotNull PluginHook.Register... register) {
        final Set<PluginHook.Register> registers = Arrays.stream(register).collect(Collectors.toSet());
        getHooks().removeIf(hook -> {
            if (!registers.contains(hook.getRegister())) {
                return false;
            }
            try {
                hook.load();
                return false;
            } catch (Throwable e) {
                getPlugin().log(Level.SEVERE, "Failed to register the %s hook".formatted(hook.getName()), e);
            }
            return true;
        });
        getPlugin().log(Level.INFO, "Registered '%s' hooks".formatted(registers.stream()
                .map(h -> h.name().toLowerCase(Locale.ENGLISH)).collect(Collectors.joining(" & "))));
    }

    default void unloadHooks(@NotNull PluginHook.Register... register) {
        final Set<PluginHook.Register> registers = Arrays.stream(register).collect(Collectors.toSet());
        getHooks().removeIf(hook -> {
            if (!registers.contains(hook.getRegister())) {
                return false;
            }
            try {
                hook.unload();
            } catch (Throwable e) {
                getPlugin().log(Level.SEVERE, "Failed to unload the %s hook".formatted(hook.getName()), e);
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
        if (isDependencyAvailable("Plan") && settings.getPlan().isEnabled()) {
            hooks.add(new PlanHook(getPlugin()));
        }
        if (isDependencyAvailable("Geyser") && settings.getPlan().isEnabled()) {
            hooks.add(new GeyserHook(getPlugin()));
        }

        // Add map hooks
        if (settings.getMap().isEnabled()) {
            if (isDependencyAvailable("dynmap")) {
                hooks.add(new DynmapHook(getPlugin()));
            }
            if (isDependencyAvailable("BlueMap")) {
                hooks.add(new BlueMapHook(getPlugin()));
            }
            if (isDependencyAvailable("Pl3xMap")) {
                hooks.add(new Pl3xMapHook(getPlugin()));
            }
        }

        return hooks;
    }

    boolean isDependencyAvailable(@NotNull String name);

    @NotNull
    HuskClaims getPlugin();

}
