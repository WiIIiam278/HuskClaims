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
