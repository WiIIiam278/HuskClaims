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

import com.google.common.collect.Maps;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import lombok.*;
import org.apache.commons.text.WordUtils;
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
    private TreeMap<String, Entry> logEntries = Maps.newTreeMap();

    public void log(@Nullable User user, @NotNull AuditLog.Action action, @Nullable String message) {
        logEntries.putIfAbsent(
                OffsetDateTime.now().toString(),
                Entry.builder().action(action).user(user).message(message).build()
        );
    }

    public void log(@NotNull UserManager.ClaimBlockSource blockSource, long newBalance) {
        log(
                null, Action.CLAIM_BLOCKS,
                "%s (%s)".formatted(WordUtils.capitalizeFully(blockSource.name()), newBalance)
        );
    }

    public void log(@NotNull AuditLog.Action action) {
        log(null, action, null);
    }

    @Builder
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    @NoArgsConstructor(access = AccessLevel.PRIVATE)
    private static final class Entry {
        @Nullable
        @Expose
        private User user;
        @Expose
        private Action action;
        @Nullable
        @Expose
        private String message;
    }

    public enum Action {
        CLAIM_BLOCKS
    }

}
