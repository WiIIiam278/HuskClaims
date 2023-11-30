package net.william278.huskclaims.util;

import com.fatboyindustrial.gsonjavatime.Converters;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;
import net.william278.huskclaims.claim.ClaimWorld;
import net.william278.huskclaims.user.Preferences;
import net.william278.huskclaims.user.User;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Interface for providing JSON adaptation utilities via Gson
 *
 * @since 1.0
 */
public interface GsonProvider {

    TypeToken<List<User>> USER_LIST_TOKEN = new TypeToken<>() {
    };

    @NotNull
    default GsonBuilder getGsonBuilder() {
        return Converters.registerOffsetDateTime(new GsonBuilder().excludeFieldsWithoutExposeAnnotation());
    }

    @NotNull
    default Gson getGson() {
        return getGsonBuilder().create();
    }

    @NotNull
    default ClaimWorld getClaimWorldFromJson(@NotNull String json) throws JsonSyntaxException {
        return getGson().fromJson(json, ClaimWorld.class);
    }

    @NotNull
    default List<User> getUserListFromJson(@NotNull String json) throws JsonSyntaxException {
        return getGson().fromJson(json, USER_LIST_TOKEN);
    }

    @NotNull
    default Preferences getPreferencesFromJson(@NotNull String json) throws JsonSyntaxException {
        return getGson().fromJson(json, Preferences.class);
    }

}
