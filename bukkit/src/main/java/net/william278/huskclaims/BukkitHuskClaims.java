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
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import net.kyori.adventure.platform.bukkit.BukkitAudiences;
import net.william278.cloplib.operation.OperationWorld;
import net.william278.desertwell.util.Version;
import net.william278.huskclaims.claim.ClaimHighlighter;
import net.william278.huskclaims.claim.ClaimWorld;
import net.william278.huskclaims.claim.TrustLevel;
import net.william278.huskclaims.command.BukkitCommand;
import net.william278.huskclaims.command.Command;
import net.william278.huskclaims.config.Locales;
import net.william278.huskclaims.config.Server;
import net.william278.huskclaims.config.Settings;
import net.william278.huskclaims.config.TrustLevels;
import net.william278.huskclaims.database.Database;
import net.william278.huskclaims.group.UserGroup;
import net.william278.huskclaims.listener.BukkitClaimsListener;
import net.william278.huskclaims.listener.ClaimsListener;
import net.william278.huskclaims.network.Broker;
import net.william278.huskclaims.network.PluginMessageBroker;
import net.william278.huskclaims.position.BlockPosition;
import net.william278.huskclaims.position.Position;
import net.william278.huskclaims.position.World;
import net.william278.huskclaims.user.*;
import net.william278.huskclaims.util.BukkitTask;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.plugin.messaging.PluginMessageListener;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import space.arim.morepaperlib.MorePaperLib;

import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentMap;
import java.util.logging.Level;

@NoArgsConstructor
public class BukkitHuskClaims extends JavaPlugin implements HuskClaims, BukkitTask.Supplier, PluginMessageListener {

    @Getter
    @Setter
    private ConcurrentLinkedQueue<UserGroup> userGroups = Queues.newConcurrentLinkedQueue();
    @Getter
    private ConcurrentMap<String, List<User>> globalUserList = Maps.newConcurrentMap();
    @Getter
    private ConcurrentMap<UUID, ClaimSelection> claimSelections = Maps.newConcurrentMap();
    @Getter
    private ConcurrentMap<UUID, Preferences> userPreferences = Maps.newConcurrentMap();
    @Getter
    private ConcurrentMap<UUID, Long> claimBlocks = Maps.newConcurrentMap();
    @Getter
    @Setter
    private ConcurrentMap<OperationWorld, ClaimWorld> claimWorlds = Maps.newConcurrentMap();
    @Getter
    private List<Command> commands = Lists.newArrayList();
    @Getter
    @Setter
    private ClaimHighlighter claimHighlighter;
    @Getter
    @Setter
    private Database database;
    @Nullable
    @Setter
    private Broker broker;
    @Getter
    @Setter
    private Settings settings;
    @Setter
    private TrustLevels trustLevels;
    @Getter
    @Setter
    private Locales locales;
    @Nullable
    @Setter
    private Server server;
    @Getter
    private MorePaperLib morePaperLib;
    private BukkitAudiences audiences;

    @Override
    public void onEnable() {
        this.audiences = BukkitAudiences.create(this);
        this.morePaperLib = new MorePaperLib(this);
        this.initialize();
    }

    @Override
    public void onDisable() {
        this.shutdown();
    }

    @Override
    public void disablePlugin() {
        getServer().getPluginManager().disablePlugin(this);
    }

    @NotNull
    @Override
    public Version getPluginVersion() {
        return Version.fromString(getDescription().getVersion());
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
        return server != null ? server.getName() : "server";
    }

    @NotNull
    @Override
    public List<TrustLevel> getTrustLevels() {
        return trustLevels.getTrustLevels();
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
    public @NotNull ConsoleUser getConsole() {
        return ConsoleUser.wrap(audiences.console());
    }

    @Override
    @NotNull
    public BukkitAudiences getAudiences() {
        return audiences;
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

    @NotNull
    @Override
    public List<Integer> getHighestBlockYAt(@NotNull List<BlockPosition> positions, @NotNull World world) {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @NotNull
    @Override
    public ClaimsListener createClaimsListener() {
        return new BukkitClaimsListener(this);
    }

    @Override
    public void closeDatabase() {
        if (database != null) {
            database.close();
        }
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
        public static org.bukkit.World adapt(@NotNull World position) {
            return Optional.ofNullable(Bukkit.getWorld(position.getUuid()))
                    .or(() -> Optional.ofNullable(Bukkit.getWorld(position.getName())))
                    .orElseThrow(() -> new IllegalArgumentException("World not found"));
        }

        @NotNull
        public static World adapt(@NotNull org.bukkit.World world) {
            return World.of(world.getName(), world.getUID());
        }
    }

}
