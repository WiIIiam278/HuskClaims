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

import com.djrapitops.plan.capability.CapabilityService;
import com.djrapitops.plan.extension.CallEvents;
import com.djrapitops.plan.extension.DataExtension;
import com.djrapitops.plan.extension.ExtensionService;
import com.djrapitops.plan.extension.annotation.*;
import com.djrapitops.plan.extension.icon.Color;
import com.djrapitops.plan.extension.icon.Family;
import com.djrapitops.plan.extension.icon.Icon;
import com.djrapitops.plan.extension.table.Table;
import lombok.NonNull;
import net.william278.huskclaims.HuskClaims;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;
import java.util.logging.Level;

@PluginHook(
        name = "Plan",
        register = PluginHook.Register.ON_ENABLE
)
public class PlanHook extends Hook {

    public PlanHook(@NonNull HuskClaims plugin) {
        super(plugin);
    }

    @Override
    public void load() {
        if (!areAllCapabilitiesAvailable()) {
            return;
        }
        registerDataExtension();
        handlePlanReload();
    }

    @Override
    public void unload() {
    }

    private boolean areAllCapabilitiesAvailable() {
        CapabilityService capabilities = CapabilityService.getInstance();
        return capabilities.hasCapability("DATA_EXTENSION_VALUES");
    }

    private void registerDataExtension() {
        try {
            ExtensionService.getInstance().register(new PlanDataExtension(plugin));
        } catch (IllegalStateException | IllegalArgumentException e) {
            plugin.log(Level.SEVERE, "Failed to register HuskHomes Plan extension", e);
        }
    }

    // Re-register the extension when plan enables
    private void handlePlanReload() {
        CapabilityService.getInstance().registerEnableListener(isPlanEnabled -> {
            if (isPlanEnabled) {
                registerDataExtension();
            }
        });
    }

    @PluginInfo(
            name = "HuskClaims",
            iconFamily = Family.SOLID,
            color = Color.LIGHT_BLUE
    )
    @SuppressWarnings("unused")
    protected static class PlanDataExtension implements DataExtension {

        private static final String UNKNOWN_STRING = "N/A";
        private HuskClaims plugin;

        protected PlanDataExtension(@NotNull HuskClaims plugin) {
            this.plugin = plugin;
        }

        protected PlanDataExtension() {
        }

        @Override
        @NotNull
        public CallEvents[] callExtensionMethodsOn() {
            return new CallEvents[]{
                    CallEvents.PLAYER_JOIN,
                    CallEvents.PLAYER_LEAVE
            };
        }

        @TableProvider
        @NotNull
        public Table getClaimWorldsTable() {
            Table.Factory table = Table.builder()
                    .columnOne("World", Icon.called("globe").build())
                    .columnTwo("Claims", Icon.called("map-marker-alt").build());
            plugin.getClaimWorlds().forEach((worldName, world) -> table.addRow(
                    worldName, world.getClaimCount()
            ));
            return table.build();
        }

        @BooleanProvider(
                text = "Has Data",
                description = "Whether this user has HuskClaims data.",
                iconFamily = Family.SOLID,
                conditionName = "hasData",
                hidden = true
        )
        public boolean getHasUserData(@NotNull UUID uuid) {
            return plugin.getDatabase().getUser(uuid).isPresent();
        }

        @NumberProvider(
                text = "Claims",
                description = "Number of claims this user has created on this server.",
                iconName = "map-marker",
                iconFamily = Family.SOLID,
                priority = 1
        )
        @Conditional("hasData")
        public long getClaimsCreated(@NotNull UUID uuid) {
            return plugin.getClaimWorlds().values().stream()
                    .map(w -> w.getClaimsByUser(uuid).size())
                    .reduce(0, Integer::sum);
        }

        @NumberProvider(
                text = "Claim Blocks",
                description = "The number of claim blocks this user has.",
                iconName = "square",
                iconFamily = Family.SOLID,
                priority = 2
        )
        @Conditional("hasData")
        public long getClaimBlocks(@NotNull UUID uuid) {
            return plugin.getClaimBlocks(uuid);
        }

    }
}
