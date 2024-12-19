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

package net.william278.huskclaims.user;

import lombok.*;
import org.apache.commons.text.WordUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

public interface AuditLogger {

    @NotNull
    Map<String, Entry> getLogEntries();

    @NotNull
    @Unmodifiable
    default List<TimestampedEntry> getTimestampedLogEntries() {
        return getLogEntries().entrySet().stream().map(TimestampedEntry::from).sorted().toList();
    }

    default void log(@Nullable User user, @NotNull AuditLogger.Action action, @Nullable String message) {
        getLogEntries().putIfAbsent(
                OffsetDateTime.now().toString(),
                Entry.builder().action(action).user(user).message(message).build()
        );
    }

    default void log(@NotNull SavedUserProvider.ClaimBlockSource blockSource, long newBalance) {
        log(
                null, Action.CLAIM_BLOCKS,
                "%s (%s)".formatted(blockSource.getFormattedName(), newBalance)
        );
    }

    default void log(@NotNull AuditLogger.Action action) {
        log(null, action, null);
    }

    @Setter
    @Builder
    @Getter
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    @NoArgsConstructor(access = AccessLevel.PACKAGE)
    final class Entry {
        private Action action;
        @Nullable
        private User user;
        @Nullable
        private String message;
    }

    record TimestampedEntry(@NotNull OffsetDateTime timestamp, @NotNull Entry entry) implements Comparable<TimestampedEntry> {
        @NotNull
        private static AuditLogger.TimestampedEntry from(@NotNull Map.Entry<String, Entry> entry) {
            return new TimestampedEntry(OffsetDateTime.parse(entry.getKey()), entry.getValue());
        }

        @Override
        public int compareTo(@NotNull AuditLogger.TimestampedEntry o) {
            return timestamp.compareTo(o.timestamp);
        }
    }

    enum Action {
        REGISTERED,
        USER_IMPORTED,
        CLAIM_BLOCKS;

        @NotNull
        public String getFormattedName() {
            return WordUtils.capitalizeFully(name().replace("_", " "));
        }
    }

}
