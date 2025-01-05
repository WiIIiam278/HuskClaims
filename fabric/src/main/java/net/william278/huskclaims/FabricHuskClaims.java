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

package net.william278.huskclaims;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Queues;
import com.google.common.collect.Sets;
import com.google.gson.Gson;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import net.fabricmc.api.DedicatedServerModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.platform.modcommon.MinecraftServerAudiences;
import net.minecraft.block.Block;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;
import net.william278.cloplib.operation.OperationType;
import net.william278.desertwell.util.Version;
import net.william278.huskclaims.api.FabricHuskClaimsAPI;
import net.william278.huskclaims.claim.ClaimWorld;
import net.william278.huskclaims.command.Command;
import net.william278.huskclaims.command.FabricCommand;
import net.william278.huskclaims.config.Locales;
import net.william278.huskclaims.config.Server;
import net.william278.huskclaims.config.Settings;
import net.william278.huskclaims.config.TrustLevels;
import net.william278.huskclaims.database.Database;
import net.william278.huskclaims.event.FabricEventDispatcher;
import net.william278.huskclaims.highlighter.FabricBlockDisplayHighlighter;
import net.william278.huskclaims.highlighter.Highlighter;
import net.william278.huskclaims.hook.FabricHookProvider;
import net.william278.huskclaims.hook.Hook;
import net.william278.huskclaims.listener.ClaimsListener;
import net.william278.huskclaims.listener.FabricListener;
import net.william278.huskclaims.network.Broker;
import net.william278.huskclaims.network.FabricPluginMessage;
import net.william278.huskclaims.network.PluginMessageBroker;
import net.william278.huskclaims.pet.FabricPetHandler;
import net.william278.huskclaims.position.Position;
import net.william278.huskclaims.position.World;
import net.william278.huskclaims.trust.TrustLevel;
import net.william278.huskclaims.trust.TrustTag;
import net.william278.huskclaims.trust.UserGroup;
import net.william278.huskclaims.user.*;
import net.william278.huskclaims.util.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.spi.LoggingEventBuilder;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.logging.Level;

@NoArgsConstructor
@Getter
public class FabricHuskClaims implements DedicatedServerModInitializer, HuskClaims, FabricTask.Supplier,
        FabricUserProvider, FabricBlockProvider, FabricSafeTeleportProvider, FabricPetHandler, FabricEventDispatcher,
        FabricHookProvider, ServerPlayNetworking.PlayPayloadHandler<FabricPluginMessage> {

    public static final Logger LOGGER = LoggerFactory.getLogger("HuskClaims");
    private final ModContainer mod = FabricLoader.getInstance().getModContainer("huskclaims")
            .orElseThrow(() -> new RuntimeException("Failed to get Mod Container"));
    private final Map<String, Boolean> permissions = Maps.newHashMap();

    private MinecraftServerAudiences audiences;
    private MinecraftServer minecraftServer;

    private final Gson gson = getGsonBuilder().create();
    private final Set<TrustTag> trustTags = ConcurrentHashMap.newKeySet();
    private final Map<UUID, List<DroppedItem>> markedDrops = Maps.newHashMap();
    private final Map<UUID, Set<GroundStack>> trackedItems = Maps.newHashMap();
    private final ConcurrentMap<String, List<User>> globalUserList = Maps.newConcurrentMap();
    private final ConcurrentMap<UUID, ClaimSelection> claimSelections = Maps.newConcurrentMap();
    private final ConcurrentMap<UUID, OnlineUser> onlineUserMap = Maps.newConcurrentMap();
    private final ConcurrentMap<UUID, SavedUser> userCache = Maps.newConcurrentMap();
    private final ConcurrentMap<UUID, Highlighter> highlighterCache = Maps.newConcurrentMap();
    private final List<Command> commands = Lists.newArrayList();
    private final List<Highlighter> highlighters = Lists.newArrayList();
    private final HashMap<String, ClaimWorld> claimWorlds = Maps.newHashMap();
    private final Queue<Task.Async> taskQueue = Queues.newConcurrentLinkedQueue();

    @Setter
    private Map<UUID, Set<UserGroup>> userGroups = Maps.newConcurrentMap();
    @Setter
    private Set<Hook> hooks = Sets.newHashSet();
    @Setter
    private ClaimsListener operationListener;
    @Setter
    private Database database;
    @Setter
    private Settings settings;
    @Setter
    private TrustLevels trustLevels;
    @Setter
    private Locales locales;
    @Setter
    @Nullable
    private Broker broker;
    @Setter
    @Nullable
    private Server serverName;

    @Override
    public void onInitializeServer() {
        this.load();
        this.loadCommands();

        ServerLifecycleEvents.SERVER_STARTING.register(this::onEnable);
        ServerLifecycleEvents.SERVER_STOPPING.register(this::onDisable);
    }

    private void onEnable(@NotNull MinecraftServer server) {
        this.minecraftServer = server;
        this.audiences = MinecraftServerAudiences.of(minecraftServer);
        this.enable();
    }

    private void onDisable(@NotNull MinecraftServer server) {
        this.shutdown();
        if (audiences != null) {
            audiences.close();
            audiences = null;
        }
    }

    @Override
    public void loadAPI() {
        FabricHuskClaimsAPI.register(this);
    }

    @Override
    public void loadMetrics() {
        // No metrics on Fabric
    }

    @Override
    public void disablePlugin() {
        onDisable(minecraftServer);
    }

    @NotNull
    @Override
    public Audience getAudience(@NotNull UUID user) {
        return audiences.player(user);
    }

    @NotNull
    @Override
    public Version getPluginVersion() {
        return Version.fromString(mod.getMetadata().getVersion().getFriendlyString(), "-");
    }

    @Override
    @NotNull
    public Version getMinecraftVersion() {
        return Version.fromString(minecraftServer.getVersion());
    }

    @Override
    @NotNull
    public String getServerType() {
        return "%s %s/%s".formatted("Fabric", FabricLoader.getInstance()
                .getModContainer("fabricloader").map(l -> l.getMetadata().getVersion().getFriendlyString())
                .orElse("unknown"), minecraftServer.getVersion());
    }

    @NotNull
    @Override
    public List<World> getWorlds() {
        final List<World> worlds = Lists.newArrayList();
        minecraftServer.getWorlds().forEach(world -> worlds.add(Adapter.adapt(world)));
        return worlds;
    }

    @Override
    public void log(@NotNull Level level, @NotNull String message, @NotNull Throwable... exceptions) {
        LoggingEventBuilder logEvent = LOGGER.makeLoggingEventBuilder(
                switch (level.getName()) {
                    case "WARNING" -> org.slf4j.event.Level.WARN;
                    case "SEVERE" -> org.slf4j.event.Level.ERROR;
                    default -> org.slf4j.event.Level.INFO;
                }
        );
        if (exceptions.length >= 1) {
            logEvent = logEvent.setCause(exceptions[0]);
        }
        logEvent.log(message);
    }

    @NotNull
    @Override
    public String getServerName() {
        return serverName != null ? serverName.getName() : "server";
    }

    @NotNull
    @Override
    public List<TrustLevel> getTrustLevels() {
        return trustLevels.getTrustLevels();
    }

    @NotNull
    @Override
    public Set<OperationType> allowedOwnerOperations() {
        return trustLevels.getAllowedOwnerOperations();
    }

    @Override
    public InputStream getResource(@NotNull String name) {
        return this.mod.findPath(name)
                .map(path -> {
                    try {
                        return Files.newInputStream(path);
                    } catch (IOException e) {
                        log(Level.WARNING, "Failed to load resource: " + name, e);
                    }
                    return null;
                })
                .orElse(this.getClass().getClassLoader().getResourceAsStream(name));
    }

    @NotNull
    @Override
    public Path getConfigDirectory() {
        return FabricLoader.getInstance().getConfigDir().resolve("huskclaims");
    }

    @Override
    public Optional<Broker> getBroker() {
        return Optional.ofNullable(broker);
    }

    @Override
    public void closeBroker() {
        if (broker != null) {
            broker.close();
        }
    }

    @Override
    public void registerCommands(@NotNull List<Command> toRegister) {
        CommandRegistrationCallback.EVENT.register((dispatcher, i1, i2) -> toRegister.stream().peek(commands::add)
                .forEach((command) -> new FabricCommand(command, this).register(dispatcher)));
    }

    @Override
    @NotNull
    public ConsoleUser getConsole() {
        return ConsoleUser.wrap(audiences.console());
    }


    @Override
    public void setupPluginMessagingChannels() {
        PayloadTypeRegistry.playC2S().register(FabricPluginMessage.CHANNEL_ID, FabricPluginMessage.CODEC);
        PayloadTypeRegistry.playS2C().register(FabricPluginMessage.CHANNEL_ID, FabricPluginMessage.CODEC);
        ServerPlayNetworking.registerGlobalReceiver(FabricPluginMessage.CHANNEL_ID, this);
    }

    // When the server receives a plugin message
    @Override
    public void receive(@NotNull FabricPluginMessage payload, @NotNull ServerPlayNetworking.Context context) {
        if (broker instanceof PluginMessageBroker messenger &&
            getSettings().getCrossServer().getBrokerType() == Broker.Type.PLUGIN_MESSAGE) {
            messenger.onReceive(
                    PluginMessageBroker.BUNGEE_CHANNEL_ID,
                    getOnlineUser(context.player()),
                    payload.getData()
            );
        }
    }

    @Override
    public void setClaimWorlds(@NotNull HashMap<World, ClaimWorld> claimWorlds) {
        claimWorlds.forEach((world, claimWorld) -> this.claimWorlds.put(world.getName(), claimWorld));
    }

    @NotNull
    @Override
    public ClaimsListener createListener() {
        return new FabricListener(this);
    }

    @Override
    public void closeDatabase() {
        if (database != null) {
            database.close();
        }
    }

    @Override
    public void loadHighlighters() {
//        HuskClaims.super.loadHighlighters(); // todo - not yet implemented

        if (!getSettings().getHighlighter().isBlockDisplays()) {
            log(Level.WARNING, "Block display highlighting is disabled, but this is the only builtin " +
                               "highlighter supported on Fabric currently. Enabling anyway.");
        }
        registerHighlighter(new FabricBlockDisplayHighlighter(this));
    }

    // Pack change data into chunk sections for the ChunkDeltaUpdate packet
    @Override
    public void sendBlockUpdates(@NotNull OnlineUser user, @NotNull Map<Position, MaterialBlock> blocks) {
        throw new UnsupportedOperationException("Block updates not yet implemented in Fabric HuskClaims");
        /* todo

        // Get biome registry
        final net.minecraft.world.World world = Adapter.adapt(user.getWorld(), minecraftServer);
        final Registry<Biome> biomes = world.getRegistryManager().getOrThrow(RegistryKeys.BIOME);

        // Calculate chunk sectors to update
        final Map<ChunkSectionPos, Pair<ShortSet, ChunkSection>> sections = Maps.newHashMap();
        for (Map.Entry<Position, MaterialBlock> entry : blocks.entrySet()) {
            final Position pos = entry.getKey();
            final BlockPos block = BlockPos.ofFloored(pos.getBlockX(), pos.getY(), pos.getBlockZ());
            final Pair<ShortSet, ChunkSection> sectionData = sections.computeIfAbsent(
                    ChunkSectionPos.from(block), ignored -> new Pair<>(new ShortArraySet(), new ChunkSection(biomes))
            );

            // Set data
            final short packed = ChunkSectionPos.packLocal(block);
            sectionData.getLeft().add(packed);
            sectionData.getRight().setBlockState(
                    ChunkSectionPos.unpackLocalX(packed),
                    ChunkSectionPos.unpackLocalY(packed),
                    ChunkSectionPos.unpackLocalZ(packed),
                    ((BlockMaterialBlock) entry.getValue()).getData().getDefaultState()
            );
        }

        // Send packets for each pos
        final ServerPlayerEntity player = ((FabricUser) user).getFabricPlayer();
        sections.forEach((section, data) -> player.networkHandler.sendPacket(
                new ChunkDeltaUpdateS2CPacket(section, data.getLeft(), data.getRight())));*/
    }

    @Override
    @NotNull
    public FabricHuskClaims getPlugin() {
        return this;
    }


    public static class Adapter {
        @NotNull
        public static Position adapt(@NotNull net.minecraft.world.World world, @NotNull Vec3d pos,
                                     float yaw, float pitch) {
            return Position.at(pos.getX(), pos.getY(), pos.getZ(), yaw, pitch, adapt(world));
        }

        @NotNull
        public static Position adapt(@NotNull Location location) {
            return adapt(location.world(), location.pos(), location.yaw(), location.pitch());
        }

        @NotNull
        public static Location adapt(@NotNull Position position, @NotNull MinecraftServer server) {
            return new Location(
                    adapt(position.getWorld(), server),
                    position.getX(), position.getY(), position.getZ(),
                    position.getYaw(), position.getPitch()
            );
        }

        @NotNull
        public static ServerWorld adapt(@NotNull World world, @NotNull MinecraftServer server) {
            final Identifier id = Identifier.tryParse(world.getName());
            final ServerWorld found = server.getWorld(RegistryKey.of(RegistryKeys.WORLD, id));
            if (found == null) {
                throw new IllegalArgumentException("World not found: %s".formatted(world.getName()));
            }
            return found;
        }

        @NotNull
        public static World adapt(@NotNull net.minecraft.world.World world) {
            final String id = world.getRegistryKey().getValue().asString();
            return World.of(
                    id, UUID.nameUUIDFromBytes(id.getBytes(StandardCharsets.UTF_8)),
                    world.getDimensionEntry().getIdAsString().toLowerCase(Locale.ENGLISH)
            );
        }

        @NotNull
        public static Map<Location, Block> adapt(@NotNull Map<Position, BlockProvider.MaterialBlock> blocks,
                                                 @NotNull MinecraftServer server) {
            final Map<Location, Block> blockData = Maps.newHashMap();
            blocks.forEach((position, materialBlock) -> blockData.put(
                    adapt(position, server),
                    Registries.BLOCK.get(Identifier.tryParse(materialBlock.getMaterialKey()))
            ));
            return blockData;
        }

    }

}
