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

package net.william278.huskclaims.network;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import net.william278.huskclaims.HuskClaims;
import net.william278.huskclaims.user.OnlineUser;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.logging.Level;

/**
 * A broker for dispatching {@link Message}s across the proxy network
 */
@AllArgsConstructor(access = AccessLevel.PROTECTED)
public abstract class Broker implements MessageHandler {

    protected final HuskClaims plugin;

    /**
     * Handle an inbound {@link Message}
     *
     * @param receiver The user who received the message, if a receiver exists
     * @param message  The message
     */
    protected void handle(@Nullable OnlineUser receiver, @NotNull Message message) {
        if (message.getSourceServer().equals(getServer())) {
            return;
        }
        switch (message.getType()) {
            case REQUEST_USER_LIST -> handleRequestUserList(message, receiver);
            case UPDATE_USER_LIST -> handleUpdateUserList(message);
            default -> plugin.log(Level.SEVERE, "Received unknown message type: " + message.getType());
        }
    }

    /**
     * Initialize the message broker
     *
     * @throws RuntimeException if the broker fails to initialize
     */
    public abstract void initialize() throws RuntimeException;

    /**
     * Send a message to the broker
     *
     * @param message the message to send
     * @param sender  the sender of the message
     */
    protected abstract void send(@NotNull Message message, @NotNull OnlineUser sender);

    /**
     * Move an {@link OnlineUser} to a new server on the proxy network
     *
     * @param user   the user to move
     * @param server the server to move the user to
     */
    public abstract void changeServer(@NotNull OnlineUser user, @NotNull String server);

    /**
     * Terminate the broker
     */
    public abstract void close();

    /**
     * Get the sub-channel ID for broker communications
     *
     * @return the sub-channel ID
     * @since 1.0
     */
    @NotNull
    protected String getSubChannelId() {
        return plugin.getKey(plugin.getSettings().getCrossServer().getClusterId(), getFormattedVersion()).asString();
    }

    /**
     * Return the server name
     *
     * @return the server name
     * @since 1.0
     */
    protected String getServer() {
        return plugin.getServerName();
    }

    // Returns the formatted version of the plugin (format: x.x)
    @NotNull
    private String getFormattedVersion() {
        return String.format("%s.%s", plugin.getPluginVersion().getMajor(), plugin.getPluginVersion().getMinor());
    }

    // Return this broker instance
    @NotNull
    @Override
    public Broker getBroker() {
        return this;
    }

    // Return the plugin instance
    @NotNull
    @Override
    public HuskClaims getPlugin() {
        return plugin;
    }

    /**
     * Identifies types of message brokers
     */
    public enum Type {
        PLUGIN_MESSAGE("Plugin Messages"),
        REDIS("Redis");
        @NotNull
        private final String displayName;

        Type(@NotNull String displayName) {
            this.displayName = displayName;
        }

        @NotNull
        public String getDisplayName() {
            return displayName;
        }
    }

}
