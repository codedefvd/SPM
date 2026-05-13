package com.team.lms.admin.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AdminBackupRestoreRequest {
    @NotNull(message = "backupId is required")
    private Long backupId;
}
