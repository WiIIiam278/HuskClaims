package net.william278.huskclaims.user;

import com.google.gson.annotations.Expose;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public class User {

    @Expose
    private String username;
    @Expose
    private UUID uuid;

    @SuppressWarnings("unused")
    private User() {
    }

    private User(String username, UUID uuid) {
        this.username = username;
        this.uuid = uuid;
    }

    @NotNull
    public static User of(String username, UUID uuid) {
        return new User(username, uuid);
    }

    @NotNull
    public String getUsername() {
        return username;
    }

    @NotNull
    public UUID getUuid() {
        return uuid;
    }
}
