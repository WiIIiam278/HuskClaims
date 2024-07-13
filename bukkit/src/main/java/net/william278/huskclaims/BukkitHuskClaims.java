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
import com.google.common.collect.Sets;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import net.kyori.adventure.platform.AudienceProvider;
import net.kyori.adventure.platform.bukkit.BukkitAudiences;
import net.william278.cloplib.operation.OperationType;
import net.william278.desertwell.util.Version;
import net.william278.huskclaims.api.BukkitHuskClaimsAPI;
import net.william278.huskclaims.claim.ClaimWorld;
import net.william278.huskclaims.command.BukkitCommand;
import net.william278.huskclaims.command.Command;
import net.william278.huskclaims.config.Locales;
import net.william278.huskclaims.config.Server;
import net.william278.huskclaims.config.Settings;
import net.william278.huskclaims.config.TrustLevels;
import net.william278.huskclaims.database.Database;
import net.william278.huskclaims.event.BukkitEventDispatcher;
import net.william278.huskclaims.highlighter.BlockUpdateHighlighter;
import net.william278.huskclaims.highlighter.Highlighter;
import net.william278.huskclaims.hook.BukkitHookProvider;
import net.william278.huskclaims.hook.Hook;
import net.william278.huskclaims.listener.BukkitListener;
import net.william278.huskclaims.listener.ClaimsListener;
import net.william278.huskclaims.network.Broker;
import net.william278.huskclaims.network.PluginMessageBroker;
import net.william278.huskclaims.pet.BukkitPetHandler;
import net.william278.huskclaims.position.Position;
import net.william278.huskclaims.position.World;
import net.william278.huskclaims.trust.TrustLevel;
import net.william278.huskclaims.trust.TrustTag;
import net.william278.huskclaims.trust.UserGroup;
import net.william278.huskclaims.user.*;
import net.william278.huskclaims.util.BlockProvider;
import net.william278.huskclaims.util.BukkitBlockProvider;
import net.william278.huskclaims.util.BukkitSafeTeleportProvider;
import net.william278.huskclaims.util.BukkitTask;
import org.bstats.bukkit.Metrics;
import org.bstats.charts.SimplePie;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.plugin.messaging.PluginMessageListener;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import space.arim.morepaperlib.MorePaperLib;

import java.nio.file.Path;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.logging.Level;

@NoArgsConstructor
@Getter
public class BukkitHuskClaims extends JavaPlugin implements HuskClaims, BukkitTask.Supplier, BukkitBlockProvider,
        BukkitSafeTeleportProvider, BukkitPetHandler, BukkitEventDispatcher, BukkitHookProvider, PluginMessageListener {

    private MorePaperLib morePaperLib;
    private AudienceProvider audiences;
    private final Set<TrustTag> trustTags = ConcurrentHashMap.newKeySet();
    private final Map<UUID, List<DroppedItem>> markedDrops = Maps.newHashMap();
    private final Map<UUID, Set<GroundStack>> trackedItems = Maps.newHashMap();
    private final ConcurrentMap<String, List<User>> globalUserList = Maps.newConcurrentMap();
    private final ConcurrentMap<UUID, ClaimSelection> claimSelections = Maps.newConcurrentMap();
    private final ConcurrentMap<UUID, SavedUser> userCache = Maps.newConcurrentMap();
    private final List<Command> commands = Lists.newArrayList();
    private final HashMap<String, ClaimWorld> claimWorlds = Maps.newHashMap();
    @Setter
    private Map<UUID, Set<UserGroup>> userGroups = Maps.newConcurrentMap();
    @Setter
    private Set<Hook> hooks = Sets.newHashSet();
    @Setter
    private Highlighter highlighter;
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
    public void onLoad() {
        this.load();
    }

    @Override
    public void onEnable() {
        this.audiences = BukkitAudiences.create(this);
        this.morePaperLib = new MorePaperLib(this);
        this.enable();
    }

    @Override
    public void onDisable() {
        this.shutdown();
        if (this.audiences != null) {
            this.audiences.close();
        }
    }

    @Override
    public void loadAPI() {
        BukkitHuskClaimsAPI.register(this);
    }

    @Override
    public void loadMetrics() {
        try {
            final Metrics metrics = new Metrics(this, BSTATS_PLUGIN_ID);
            metrics.addCustomChart(new SimplePie("is_cross_server",
                    () -> settings.getCrossServer().isEnabled() ? "true" : "false")
            );
            metrics.addCustomChart(new SimplePie("language",
                    () -> settings.getLanguage().toLowerCase(Locale.ENGLISH))
            );
            metrics.addCustomChart(new SimplePie("database_type",
                    () -> settings.getDatabase().getType().getDisplayName())
            );
            metrics.addCustomChart(new SimplePie("highlighter_type",
                    () -> getHighlighter() instanceof BlockUpdateHighlighter ? "Block Updates" : "Display Entities")
            );
            getBroker().ifPresent(broker -> metrics.addCustomChart(new SimplePie("broker_type",
                    () -> settings.getCrossServer().getBrokerType().getDisplayName()
            )));
        } catch (Exception e) {
            log(Level.WARNING, "Failed to initialize plugin metrics", e);
        }
    }

    @Override
    public void disablePlugin() {
        log(Level.INFO, "Disabling HuskClaims...");
        getServer().getPluginManager().disablePlugin(this);
    }

    @NotNull
    @Override
    public Version getPluginVersion() {
        return Version.fromString(getDescription().getVersion());
    }

    @Override
    @NotNull
    public String getServerType() {
        return String.format("%s/%s", getServer().getName(), getServer().getVersion());
    }

    @Override
    @NotNull
    public Version getMinecraftVersion() {
        return Version.fromString(getServer().getBukkitVersion());
    }

    @Override
    public List<? extends OnlineUser> getOnlineUsers() {
        return getServer().getOnlinePlayers().stream()
                .map(player -> BukkitUser.adapt(player, this))
                .toList();
    }

    @NotNull
    @Override
    public List<World> getWorlds() {
        return getServer().getWorlds().stream().map(Adapter::adapt).toList();
    }

    @Override
    public void log(@NotNull Level level, @NotNull String message, @NotNull Throwable... exceptions) {
        if (exceptions.length > 0) {
            getLogger().log(level, message, exceptions[0]);
            return;
        }
        getLogger().log(level, message);
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

    @Override
    public @NotNull Set<OperationType> allowedOwnerOperations() {
        return trustLevels.getAllowedOwnerOperations();
    }

    @NotNull
    @Override
    public Path getConfigDirectory() {
        return getDataFolder().toPath();
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
    public void registerCommands(@NotNull List<Command> commands) {
        commands.forEach(command -> {
            new BukkitCommand(command, this).register();
            this.commands.add(command);
        });
    }

    @Override
    @NotNull
    public ConsoleUser getConsole() {
        return ConsoleUser.wrap(audiences.console());
    }


    @Override
    public void setupPluginMessagingChannels() {
        final String channelId = PluginMessageBroker.BUNGEE_CHANNEL_ID;
        getServer().getMessenger().registerIncomingPluginChannel(this, channelId, this);
        getServer().getMessenger().registerOutgoingPluginChannel(this, channelId);
    }

    @Override
    public void onPluginMessageReceived(@NotNull String channel, @NotNull Player player, byte[] message) {
        if (broker != null && broker instanceof PluginMessageBroker pluginMessenger
                && getSettings().getCrossServer().getBrokerType() == Broker.Type.PLUGIN_MESSAGE) {
            pluginMessenger.onReceive(channel, BukkitUser.adapt(player, this), message);
        }
    }

    @Override
    public void setClaimWorlds(@NotNull HashMap<World, ClaimWorld> claimWorlds) {
        claimWorlds.forEach((world, claimWorld) -> this.claimWorlds.put(world.getName(), claimWorld));
    }

    @NotNull
    @Override
    public ClaimsListener createListener() {
        return new BukkitListener(this);
    }

    @Override
    public void closeDatabase() {
        if (database != null) {
            database.close();
        }
    }

    @Override
    public void sendBlockUpdates(@NotNull OnlineUser user, @NotNull Map<Position, MaterialBlock> blocks) {
        final Player player = ((BukkitUser) user).getBukkitPlayer();
        blocks.forEach((position, materialBlock) -> player.sendBlockChange(
                Adapter.adapt(position),
                Objects.requireNonNull(Material.matchMaterial(materialBlock.getMaterialKey()),
                        "Invalid material: " + materialBlock.getMaterialKey()
                ).createBlockData()
        ));
    }

    @Override
    public void startQueuePoller() {
        getRepeatingTask(()->{
            Runnable task = claimBlocksEditQueue.poll();
            if (task != null) {
                task.run();
            }
        }, Duration.of(100, ChronoUnit.MILLIS), Duration.of(100, ChronoUnit.MILLIS)).run();
    }

    @Override
    @NotNull
    public BukkitHuskClaims getPlugin() {
        return this;
    }

    public static class Adapter {
        @NotNull
        public static Position adapt(@NotNull Location location) {
            return Position.at(
                    location.getX(), location.getY(), location.getZ(),
                    location.getYaw(), location.getPitch(),
                    adapt(Objects.requireNonNull(location.getWorld(), "Location world is null"))
            );
        }

        @NotNull
        public static Location adapt(@NotNull Position position) {
            return new Location(
                    adapt(position.getWorld()),
                    position.getX(), position.getY(), position.getZ(),
                    position.getYaw(), position.getPitch()
            );
        }

        @NotNull
        public static org.bukkit.World adapt(@NotNull World world) {
            return Optional.ofNullable(Bukkit.getWorld(world.getUuid()))
                    .or(() -> Optional.ofNullable(Bukkit.getWorld(world.getName())))
                    .orElseThrow(() -> new IllegalArgumentException("World not found"));
        }

        @NotNull
        public static World adapt(@NotNull org.bukkit.World world) {
            return World.of(world.getName(), world.getUID(), world.getEnvironment().name().toLowerCase(Locale.ENGLISH));
        }

        @NotNull
        public static Map<Location, BlockData> adapt(@NotNull Map<Position, BlockProvider.MaterialBlock> blocks) {
            final Map<Location, BlockData> blockData = Maps.newHashMap();
            blocks.forEach((position, materialBlock) -> blockData.put(
                    adapt(position),
                    Objects.requireNonNull(Material.matchMaterial(materialBlock.getMaterialKey()),
                            "Invalid material: " + materialBlock.getMaterialKey()
                    ).createBlockData()
            ));
            return blockData;
        }
    }

}
