package net.william278.huskclaims.claim;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import net.william278.cloplib.operation.OperationType;
import net.william278.huskclaims.user.User;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;

public class ClaimWorld {

    @Expose
    private ConcurrentMap<UUID, ConcurrentLinkedQueue<Claim>> claims;
    @Expose
    @SerializedName("user_cache")
    private ConcurrentMap<UUID, String> userCache;
    @Expose
    @SerializedName("wilderness_flags")
    private List<OperationType> wildernessFlags;

    public Optional<User> getUser(@NotNull UUID uuid) {
        return Optional.ofNullable(userCache.get(uuid)).map(name -> User.of(name, uuid));
    }

    @NotNull
    public ConcurrentMap<UUID, ConcurrentLinkedQueue<Claim>> getClaimMap() {
        return claims;
    }

    @NotNull
    public ConcurrentLinkedQueue<Claim> getClaims() {
        return claims.values().stream()
                .flatMap(Collection::stream)
                .collect(Collectors.toCollection(ConcurrentLinkedQueue::new));
    }

    @NotNull
    public ConcurrentMap<UUID, String> getUserCache() {
        return userCache;
    }

    @NotNull
    public List<OperationType> getWildernessFlags() {
        return wildernessFlags;
    }

}
