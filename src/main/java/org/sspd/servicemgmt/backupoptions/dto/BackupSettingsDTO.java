package org.sspd.servicemgmt.backupoptions.dto;

import lombok.Data;
import org.sspd.servicemgmt.backupoptions.model.BackupFrequency;

@Data
public class BackupSettingsDTO {
    private Integer id;
    private BackupFrequency frequency;
    private Integer dayValue;
    private Integer monthValue;
    private String backupTime;
    private String backupDir;
    private boolean enabled;
    private Integer keepDays;
    private String mysqldumpPath;
}
