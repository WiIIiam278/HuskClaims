package net.william278.huskclaims.user;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import lombok.*;

/**
 * Represents user preferences
 *
 * @since 1.0
 */
@Getter
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = lombok.AccessLevel.PRIVATE)
public class Preferences {

    @Expose
    @SerializedName("ignoring_claims")
    private boolean isIgnoringClaims = false;

    @Expose
    @SerializedName("audit_log")
    private AuditLog auditLog = new AuditLog();

}
