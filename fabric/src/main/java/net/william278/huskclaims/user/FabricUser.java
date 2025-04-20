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

package net.william278.huskclaims.user;

import com.google.common.collect.Lists;
import com.pokeskies.fabricpluginmessaging.PluginMessagePacket;
import me.lucko.fabric.api.permissions.v0.Permissions;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.CustomModelDataComponent;
import net.minecraft.entity.Entity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import net.william278.cloplib.listener.InspectorCallbackProvider;
import net.william278.huskclaims.FabricHuskClaims;
import net.william278.huskclaims.HuskClaims;
import net.william278.huskclaims.hook.HuskHomesHook;
import net.william278.huskclaims.position.Position;
import net.william278.huskclaims.util.Location;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;

public class FabricUser extends OnlineUser {

    private final ServerPlayerEntity fabricPlayer;

    private FabricUser(@NotNull ServerPlayerEntity fabricPlayer, @NotNull HuskClaims plugin) {
        super(fabricPlayer.getGameProfile().getName(), fabricPlayer.getUuid(), plugin);
        this.fabricPlayer = fabricPlayer;
    }

    @NotNull
    @ApiStatus.Internal
    public static FabricUser adapt(@NotNull ServerPlayerEntity player, @NotNull HuskClaims plugin) {
        return new FabricUser(player, plugin);
    }

    @NotNull
    @Override
    public Position getPosition() {
        return FabricHuskClaims.Adapter.adapt(new Location(fabricPlayer));
    }

    @Override
    public void sendPluginMessage(@NotNull String channel, byte[] message) {
        ServerPlayNetworking.send(fabricPlayer, new PluginMessagePacket(message));
    }

    @Override
    public boolean hasPermission(@NotNull String node) {
        final Map<String, Boolean> permissionDefaultsMap = ((FabricHuskClaims) plugin).getPermissions();
        return hasPermission(node, !permissionDefaultsMap.getOrDefault(node, true));
    }

    @Override
    public boolean hasPermission(@NotNull String node, boolean isDefault) {
        return Permissions.check(fabricPlayer, node, fabricPlayer.hasPermissionLevel(!isDefault ? 3 : 0));
    }

    @Override
    public boolean isHolding(@NotNull InspectorCallbackProvider.InspectionTool tool) {
        final Predicate<ItemStack> TYPE_MATCH = (i) -> i.getRegistryEntry().matchesId(Identifier.of(tool.material()));
        final Predicate<ItemStack> MODEL_MATCH = (i) -> !tool.useCustomModelData() || getCustomModelData(i)
                .map(m -> m == tool.customModelData()).orElse(false);
        return TYPE_MATCH.and(MODEL_MATCH).test(fabricPlayer.getMainHandStack());
    }

    private Optional<Integer> getCustomModelData(@NotNull ItemStack i) {
        final CustomModelDataComponent modelData = i.getComponents().get(DataComponentTypes.CUSTOM_MODEL_DATA);
        //#if MC==12104
        if (modelData != null && !modelData.floats().isEmpty()) {
            return Optional.of(modelData.floats().getFirst().intValue());
        //#else
        //$$ if (modelData != null) {
        //$$     return Optional.of(modelData.value());
        //#endif
        }
        return Optional.empty();
    }

    @Override
    @NotNull
    public Optional<Long> getNumericalPermission(@NotNull String prefix) {
        final List<Long> permissions = Lists.newArrayList();
        for (long l = 0; l < 100; l++) {
            if (hasPermission(prefix + l)) {
                permissions.add(l);
            }
        }
        return permissions.stream().sorted().findFirst();
    }

    @Override
    public boolean isSneaking() {
        return fabricPlayer.isSneaking();
    }

    @Override
    public void teleport(@NotNull Position position, boolean instant) {
        if (instant) {
            teleportInstant(position);
            return;
        }
        plugin.getHook(HuskHomesHook.class).ifPresentOrElse(
                homes -> homes.teleport(this, position, plugin.getServerName()),
                () -> teleportInstant(position)
        );
    }

    private void teleportInstant(@NotNull Position position) {
        fabricPlayer.stopRiding();
        fabricPlayer.getPassengerList().forEach(Entity::stopRiding);
        fabricPlayer.teleportTo(FabricHuskClaims.Adapter.adapt(
                position, ((FabricHuskClaims) plugin).getMinecraftServer()
        ).teleportTarget());
    }

    @NotNull
    public ServerPlayerEntity getFabricPlayer() {
        return fabricPlayer;
    }

}
