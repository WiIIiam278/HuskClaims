package net.william278.huskclaims.user;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import lombok.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.OffsetDateTime;
import java.util.TreeMap;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public final class AuditLog {

    @Expose
    @SerializedName("log_entries")
    private TreeMap<OffsetDateTime, LogEntry> logEntries = new TreeMap<>();

    public void log(@Nullable User user, @NotNull LogAction action) {
        if (logEntries.get(OffsetDateTime.now()) == null) {
            logEntries.put(OffsetDateTime.now(), LogEntry.builder().action(action).user(user).build());
        }
    }

    public void log(@NotNull LogAction action) {
        log(null, action);
    }

    @Builder
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    @NoArgsConstructor(access = AccessLevel.PRIVATE)
    private static final class LogEntry {
        @Nullable
        @Expose
        private User user;
        @Expose
        private LogAction action;
    }

    public enum LogAction {
        SET_BONUS_CLAIM_BLOCKS,
        CREATE_CLAIM,
        RESIZE_CLAIM,
        MANAGE_CLAIM_MEMBERS
    }

}
