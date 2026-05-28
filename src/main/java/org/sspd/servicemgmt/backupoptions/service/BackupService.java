package org.sspd.servicemgmt.backupoptions.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.sspd.servicemgmt.backupoptions.dto.BackupSettingsDTO;
import org.sspd.servicemgmt.backupoptions.model.BackupFrequency;
import org.sspd.servicemgmt.backupoptions.model.BackupSettings;
import org.sspd.servicemgmt.backupoptions.repository.BackupSettingsRepository;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class BackupService {

    private final BackupSettingsRepository repository;
    private final BackupSchedulerService schedulerService;

    @Value("${spring.datasource.url}")
    private String datasourceUrl;

    @Value("${spring.datasource.username}")
    private String dbUsername;

    @Value("${spring.datasource.password}")
    private String dbPassword;

    @Transactional(readOnly = true)
    public BackupSettingsDTO getSettings() {
        return toDto(getOrCreate());
    }

    @Transactional
    public BackupSettingsDTO saveSettings(BackupSettingsDTO dto) {
        BackupSettings s = getOrCreate();
        s.setFrequency(dto.getFrequency());
        s.setDayValue(dto.getDayValue());
        s.setMonthValue(dto.getMonthValue());
        s.setBackupTime(LocalTime.parse(dto.getBackupTime()));
        s.setBackupDir(dto.getBackupDir() != null ? dto.getBackupDir() : "./backups");
        s.setEnabled(dto.isEnabled());
        s.setKeepDays(dto.getKeepDays() != null ? dto.getKeepDays() : 30);
        s.setMysqldumpPath(dto.getMysqldumpPath());
        BackupSettings saved = repository.save(s);
        schedulerService.reschedule(saved);
        return toDto(saved);
    }

    public String runNow() {
        return executeBackup(getOrCreate());
    }

    public String executeBackup(BackupSettings settings) {
        try {
            String dbName = extractDbName(datasourceUrl);
            String timestamp = LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss"));
            Path dir = Paths.get(settings.getBackupDir());
            Files.createDirectories(dir);

            String fileName = dbName + "_" + timestamp + ".sql";
            Path outFile = dir.resolve(fileName);

            String mysqldump = (settings.getMysqldumpPath() != null && !settings.getMysqldumpPath().isBlank())
                ? settings.getMysqldumpPath()
                : findMysqldump();
            log.info("Using mysqldump at: {}", mysqldump);

            ProcessBuilder pb = new ProcessBuilder(
                mysqldump,
                "-u", dbUsername,
                "-p" + dbPassword,
                "--single-transaction",
                "--routines",
                "--triggers",
                dbName
            );
            pb.redirectOutput(outFile.toFile());
            pb.redirectErrorStream(false);

            Process process = pb.start();
            String errorOutput = new String(process.getErrorStream().readAllBytes());
            int exit = process.waitFor();

            if (exit == 0) {
                cleanOldBackups(settings);
                log.info("Backup success: {}", fileName);
                return fileName;
            }
            log.error("mysqldump exit {} — stderr: {}", exit, errorOutput);
            return null;
        } catch (Exception e) {
            log.error("Backup error: {}", e.getMessage(), e);
            return null;
        }
    }

    public List<String> listBackups() {
        try {
            File dir = new File(getOrCreate().getBackupDir());
            if (!dir.exists()) return List.of();
            File[] files = dir.listFiles((d, n) -> n.endsWith(".sql"));
            if (files == null) return List.of();
            return Arrays.stream(files)
                .sorted(Comparator.comparingLong(File::lastModified).reversed())
                .map(File::getName)
                .toList();
        } catch (Exception e) {
            return List.of();
        }
    }

    public void importBackup(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new RuntimeException("Backup file is required");
        }
        String name = file.getOriginalFilename() != null ? file.getOriginalFilename().toLowerCase() : "";
        if (!name.endsWith(".sql")) {
            throw new RuntimeException("Only .sql file is supported");
        }

        Path tempFile = null;
        try {
            tempFile = Files.createTempFile("backup-restore-", ".sql");
            try (InputStream in = file.getInputStream()) {
                Files.copy(in, tempFile, StandardCopyOption.REPLACE_EXISTING);
            }

            String dbName = extractDbName(datasourceUrl);
            String mysql = resolveMysqlCommand(getOrCreate().getMysqldumpPath());

            ProcessBuilder pb = new ProcessBuilder(
                mysql,
                "-u", dbUsername,
                "-p" + dbPassword,
                dbName
            );
            pb.redirectInput(tempFile.toFile());
            pb.redirectErrorStream(true);

            Process process = pb.start();
            String output = new String(process.getInputStream().readAllBytes());
            int exit = process.waitFor();
            if (exit != 0) {
                throw new RuntimeException("Import failed: " + output);
            }
            log.info("Backup import success: {}", file.getOriginalFilename());
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Import failed: " + e.getMessage(), e);
        } finally {
            if (tempFile != null) {
                try {
                    Files.deleteIfExists(tempFile);
                } catch (Exception ignored) {
                }
            }
        }
    }

    private String findMysqldump() {
        // Check PATH first
        String[] candidates = System.getProperty("os.name", "").toLowerCase().contains("win")
            ? new String[]{
                "mysqldump",
                "C:\\Program Files\\MySQL\\MySQL Server 8.4\\bin\\mysqldump.exe",
                "C:\\Program Files\\MySQL\\MySQL Server 8.0\\bin\\mysqldump.exe",
                "C:\\Program Files\\MySQL\\MySQL Server 5.7\\bin\\mysqldump.exe",
                "C:\\xampp\\mysql\\bin\\mysqldump.exe",
                "C:\\wamp64\\bin\\mysql\\mysql8.0\\bin\\mysqldump.exe",
              }
            : new String[]{
                "mysqldump",
                "/usr/bin/mysqldump",
                "/usr/local/bin/mysqldump",
              };

        for (String candidate : candidates) {
            if (candidate.equals("mysqldump")) return candidate; // trust PATH first
            if (new File(candidate).exists()) return candidate;
        }
        return "mysqldump"; // fallback — let OS resolve
    }

    private String resolveMysqlCommand(String configuredMysqldumpPath) {
        if (configuredMysqldumpPath != null && !configuredMysqldumpPath.isBlank()) {
            String trimmed = configuredMysqldumpPath.trim();
            if (trimmed.toLowerCase().endsWith("mysqldump.exe")) {
                return trimmed.substring(0, trimmed.length() - "mysqldump.exe".length()) + "mysql.exe";
            }
            if (trimmed.toLowerCase().endsWith("mysqldump")) {
                return trimmed.substring(0, trimmed.length() - "mysqldump".length()) + "mysql";
            }
        }

        String[] candidates = System.getProperty("os.name", "").toLowerCase().contains("win")
            ? new String[]{
                "mysql",
                "C:\\Program Files\\MySQL\\MySQL Server 8.4\\bin\\mysql.exe",
                "C:\\Program Files\\MySQL\\MySQL Server 8.0\\bin\\mysql.exe",
                "C:\\Program Files\\MySQL\\MySQL Server 5.7\\bin\\mysql.exe",
                "C:\\xampp\\mysql\\bin\\mysql.exe",
                "C:\\wamp64\\bin\\mysql\\mysql8.0\\bin\\mysql.exe",
            }
            : new String[]{
                "mysql",
                "/usr/bin/mysql",
                "/usr/local/bin/mysql",
            };

        for (String candidate : candidates) {
            if (candidate.equals("mysql")) return candidate;
            if (new File(candidate).exists()) return candidate;
        }
        return "mysql";
    }

    private void cleanOldBackups(BackupSettings s) {
        try {
            File dir = new File(s.getBackupDir());
            if (!dir.exists()) return;
            File[] files = dir.listFiles((d, n) -> n.endsWith(".sql"));
            if (files == null) return;
            LocalDate cutoff = LocalDate.now().minusDays(s.getKeepDays());
            for (File f : files) {
                if (LocalDate.ofEpochDay(f.lastModified() / 86400000).isBefore(cutoff))
                    f.delete();
            }
        } catch (Exception e) {
            log.warn("Cleanup error", e);
        }
    }

    private String extractDbName(String url) {
        String[] parts = url.split("/");
        String last = parts[parts.length - 1];
        return last.contains("?") ? last.substring(0, last.indexOf("?")) : last;
    }

    @SuppressWarnings("null")
    private BackupSettings getOrCreate() {
        List<BackupSettings> all = repository.findAll();
        if (!all.isEmpty()) return all.get(0);
        return repository.save(BackupSettings.builder()
            .frequency(BackupFrequency.DAILY)
            .backupTime(LocalTime.of(2, 0))
            .backupDir("./backups")
            .enabled(true)
            .keepDays(30)
            .build());
    }

    private BackupSettingsDTO toDto(BackupSettings s) {
        BackupSettingsDTO dto = new BackupSettingsDTO();
        dto.setId(s.getId());
        dto.setFrequency(s.getFrequency());
        dto.setDayValue(s.getDayValue());
        dto.setMonthValue(s.getMonthValue());
        dto.setBackupTime(s.getBackupTime().toString());
        dto.setBackupDir(s.getBackupDir());
        dto.setEnabled(s.isEnabled());
        dto.setKeepDays(s.getKeepDays());
        dto.setMysqldumpPath(s.getMysqldumpPath());
        return dto;
    }
}
