package net.william278.huskclaims.api;

import net.william278.huskclaims.HuskClaims;
import net.william278.huskclaims.claim.ClaimWorld;
import net.william278.huskclaims.position.World;
import net.william278.huskclaims.user.OnlineUser;
import net.william278.huskclaims.user.SavedUser;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class HuskClaimsAPI {

    // Singleton API instance
    private static HuskClaimsAPI instance;
    // Plugin instance
    private final HuskClaims plugin;

    /**
     * <b>(Internal use only)</b> - Constructor, instantiating the API.
     *
     * @since 1.0
     */
    @ApiStatus.Internal
    private HuskClaimsAPI(@NotNull HuskClaims plugin) {
        this.plugin = plugin;
    }

    /**
     * Get a {@link SavedUser} instance for a player, by UUID.
     *
     * @param uuid the UUID of the player
     * @return a {@link CompletableFuture} containing an {@link Optional} of the {@link SavedUser} instance, if found.
     * This contains persisted (offline) player data pertinent to HuskClaims.
     * @since 1.0
     */
    public CompletableFuture<Optional<SavedUser>> getUser(@NotNull UUID uuid) {
        return plugin.supplyAsync(() -> plugin.getDatabase().getUser(uuid));
    }

    /**
     * Get a {@link SavedUser} instance for a player, by username.
     *
     * @param name the name of the player
     * @return a {@link CompletableFuture} containing an {@link Optional} of the {@link SavedUser} instance, if found.
     * This contains persisted (offline) player data pertinent to HuskClaims.
     * @since 1.0
     */
    public CompletableFuture<Optional<SavedUser>> getUser(@NotNull String name) {
        return plugin.supplyAsync(() -> plugin.getDatabase().getUser(name));
    }

    /**
     * Get the {@link OnlineUser} instance representing an online player.
     *
     * @param uuid the UUID of the player
     * @return the {@link OnlineUser} instance
     * @throws IllegalArgumentException if the player is not online
     * @since 1.0
     */
    @NotNull
    public OnlineUser getOnlineUser(@NotNull UUID uuid) {
        return plugin.getOnlineUsers().stream()
                .filter(user -> user.getUuid().equals(uuid)).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("No user online with UUID " + uuid));
    }

    /**
     * Get the {@link World} instance representing a server world.
     *
     * @param worldName the name of the world
     * @return the {@link World} instance
     * @throws IllegalArgumentException if the world does not exist
     * @since 1.0
     */
    @NotNull
    public World getWorld(@NotNull String worldName) {
        return plugin.getWorlds().stream()
                .filter(world -> world.getName().equals(worldName)).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("No world with name " + worldName));
    }

    /**
     * Get the {@link ClaimWorld} instance representing a world.
     *
     * @param world the world
     * @return the {@link ClaimWorld} instance
     * @since 1.0
     */
    public Optional<ClaimWorld> getClaimWorld(@NotNull World world) {
        return plugin.getClaimWorld(world);
    }

    /**
     * Get the {@link ClaimWorld} instance representing a world.
     *
     * @param worldName the name of the world
     * @return the {@link ClaimWorld} instance
     * @since 1.0
     */
    public Optional<ClaimWorld> getClaimWorld(@NotNull String worldName) {
        return plugin.getClaimWorld(worldName);
    }

    /**
     * Get an instance of the HuskClaims API.
     *
     * @return instance of the HuskClaims API
     * @throws NotRegisteredException if the API has not yet been registered.
     * @since 1.0
     */
    @NotNull
    public static HuskClaimsAPI getInstance() throws NotRegisteredException {
        if (instance == null) {
            throw new NotRegisteredException();
        }
        return instance;
    }

    /**
     * <b>(Internal use only)</b> - Register the API instance.
     *
     * @param plugin the plugin instance
     * @since 1.0
     */
    @ApiStatus.Internal
    public static void register(@NotNull HuskClaims plugin) {
        instance = new HuskClaimsAPI(plugin);
    }

    /**
     * <b>(Internal use only)</b> - Unregister the API instance.
     *
     * @since 1.0
     */
    @ApiStatus.Internal
    public static void unregister() {
        instance = null;
    }

    /**
     * An exception indicating the plugin has been accessed before it has been registered.
     *
     * @since 1.0
     */
    public static final class NotRegisteredException extends IllegalStateException {

        private static final String MESSAGE = """
                Could not access the HuskClaims API as it has not yet been registered. This could be because:
                1) HuskClaims has failed to enable successfully
                2) Your plugin isn't set to load after HuskClaims has
                   (Check if it set as a (soft)depend in plugin.yml or to load: BEFORE in paper-plugin.yml?)
                3) You are attempting to access HuskClaims on plugin construction/before your plugin has enabled.""";

        NotRegisteredException() {
            super(MESSAGE);
        }

    }

}