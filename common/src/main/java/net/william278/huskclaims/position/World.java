package net.william278.huskclaims.position;

import com.google.gson.annotations.Expose;
import net.william278.cloplib.operation.OperationWorld;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public class World implements OperationWorld {

    @Expose
    private String name;
    @Expose
    private UUID uuid;

    private World(@NotNull String name, @NotNull UUID uuid) {
        this.name = name;
        this.uuid = uuid;
    }

    @SuppressWarnings("unused")
    private World() {
    }

    @NotNull
    public static World of(@NotNull String name, @NotNull UUID uuid) {
        return new World(name, uuid);
    }

    @NotNull
    @Override
    public String getName() {
        return name;
    }

    @NotNull
    @Override
    public UUID getUuid() {
        return uuid;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof World world) {
            return world.uuid.equals(uuid) || world.name.equals(name);
        }
        return false;
    }
}
