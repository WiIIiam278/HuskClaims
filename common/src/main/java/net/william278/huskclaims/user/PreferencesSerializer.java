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
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import lombok.AllArgsConstructor;
import net.william278.huskclaims.claim.ClaimingMode;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.Map;

@AllArgsConstructor
public class PreferencesSerializer extends TypeAdapter<Preferences> {

    // Max number of log entries to persist. More can be in "hot-data".
    int maxPersistedLogEntries;
    private final UserSerializer userSerializer;

    @Override
    public void write(@NotNull JsonWriter out, @NotNull Preferences prefs) throws IOException {
        out.beginObject();
        out.name("ignoring_claims").value(prefs.isIgnoringClaims());
        out.name("sign_notifications").value(prefs.isSignNotifications());
        out.name("claiming_mode").value(prefs.getClaimingMode().name());
        out.name("audit_log");
        writeAuditLog(out, prefs.getLogEntries());
        out.endObject();
    }

    private void writeAuditLog(@NotNull JsonWriter out, @NotNull Map<String, AuditLogger.Entry> auditLog) throws IOException {
        out.beginObject();
        int i = 0;
        for (Map.Entry<String, AuditLogger.Entry> entry : auditLog.entrySet()) {
            if (i >= maxPersistedLogEntries) {
                break;
            }
            out.name(entry.getKey());
            writeAuditLogEntry(out, entry.getValue());
            i++;
        }
        out.endObject();
    }

    private void writeAuditLogEntry(@NotNull JsonWriter out, @NotNull AuditLogger.Entry entry) throws IOException {
        out.beginObject();
        out.name("action").value(entry.getAction().name());
        if (entry.getMessage() != null) {
            out.name("message").value(entry.getMessage());
        }
        if (entry.getUser() != null) {
            out.name("user");
            userSerializer.write(out, entry.getUser());
        }
        out.endObject();
    }

    @Override
    @NotNull
    public Preferences read(@NotNull JsonReader in) throws IOException {
        in.beginObject();
        final Preferences prefs = new Preferences();
        while (in.hasNext()) {
            switch (in.nextName()) {
                case "ignoring_claims":
                    prefs.setIgnoringClaims(in.nextBoolean());
                    break;
                case "sign_notifications":
                    prefs.setSignNotifications(in.nextBoolean());
                    break;
                case "claiming_mode":
                    prefs.setClaimingMode(ClaimingMode.valueOf(in.nextString()));
                    break;
                case "audit_log":
                    prefs.setLogEntries(readAuditLog(in));
                    break;
            }
        }
        in.endObject();
        return prefs;
    }

    @NotNull
    private Map<String, AuditLogger.Entry> readAuditLog(@NotNull JsonReader in) throws IOException {
        final Map<String, AuditLogger.Entry> auditLog = Maps.newHashMap();
        in.beginObject();
        while (in.hasNext()) {
            auditLog.put(in.nextName(), readAuditLogEntry(in));
        }
        in.endObject();
        return auditLog;
    }

    @NotNull
    private AuditLogger.Entry readAuditLogEntry(@NotNull JsonReader in) throws IOException {
        in.beginObject();
        final AuditLogger.Entry entry = new AuditLogger.Entry();
        while (in.hasNext()) {
            switch (in.nextName()) {
                case "action":
                    entry.setAction(AuditLogger.Action.valueOf(in.nextString()));
                    break;
                case "user":
                    entry.setUser(userSerializer.read(in));
                    break;
                case "message":
                    entry.setMessage(in.nextString());
                    break;
            }
        }
        in.endObject();
        return entry;
    }

}
