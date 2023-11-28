package net.william278.huskclaims.claim;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import net.william278.cloplib.operation.OperationType;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class TrustLevel implements Comparable<TrustLevel> {

    @Expose
    private String id;
    @Expose
    @SerializedName("display_name")
    private String displayName;
    @Expose
    @SerializedName("command_aliases")
    private List<String> commandAliases;
    @Expose
    @SerializedName("flags")
    private List<OperationType> flags;
    @Expose
    private List<Privilege> privileges;
    @Expose
    private int weight;

    @NotNull
    public String getId() {
        return id;
    }

    @NotNull
    public String getDisplayName() {
        return displayName;
    }

    @NotNull
    public List<String> getCommandAliases() {
        return commandAliases;
    }

    @NotNull
    public List<OperationType> getFlags() {
        return flags;
    }

    @NotNull
    public List<Privilege> getPrivileges() {
        return privileges;
    }

    public int getWeight() {
        return weight;
    }

    @Override
    public int compareTo(@NotNull TrustLevel o) {
        return Integer.compare(weight, o.weight);
    }

    public enum Privilege {
        MANAGE_TRUSTEES,
        MANAGE_SUBDIVISIONS,
        MANAGE_EXPLOSIONS,
    }

}
