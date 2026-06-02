package com.team.lms.admin.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.team.lms.admin.dto.AdminBackupRestoreRequest;
import com.team.lms.admin.service.AdminMonitoringService;
import com.team.lms.admin.vo.AdminBackupRecordVo;
import com.team.lms.admin.vo.AdminBusinessReportVo;
import com.team.lms.admin.vo.AdminFineStatisticsVo;
import com.team.lms.admin.vo.AdminMetricItemVo;
import com.team.lms.admin.vo.AdminMonitoringOverviewVo;
import com.team.lms.admin.vo.AdminOperationLogVo;
import com.team.lms.admin.vo.AdminRestoreResultVo;
import com.team.lms.admin.vo.AdminRuntimeStatusVo;
import com.team.lms.common.enums.BorrowRecordStatus;
import com.team.lms.common.enums.FineStatus;
import com.team.lms.common.enums.RoleType;
import com.team.lms.common.support.CurrentUserSupport;
import com.team.lms.common.support.OperationLogSupport;
import com.team.lms.entity.BackupRecord;
import com.team.lms.entity.Book;
import com.team.lms.entity.BorrowRecord;
import com.team.lms.entity.BorrowRequest;
import com.team.lms.entity.Fine;
import com.team.lms.entity.Inventory;
import com.team.lms.entity.OperationLog;
import com.team.lms.entity.Reservation;
import com.team.lms.entity.User;
import com.team.lms.exception.BusinessException;
import com.team.lms.mapper.BackupRecordMapper;
import com.team.lms.mapper.BookMapper;
import com.team.lms.mapper.BorrowRecordMapper;
import com.team.lms.mapper.BorrowRequestMapper;
import com.team.lms.mapper.FineMapper;
import com.team.lms.mapper.InventoryMapper;
import com.team.lms.mapper.OperationLogMapper;
import com.team.lms.mapper.ReservationMapper;
import com.team.lms.mapper.SystemConfigMapper;
import com.team.lms.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminMonitoringServiceImpl implements AdminMonitoringService {

    private static final DateTimeFormatter BACKUP_STAMP_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");
    private static final Set<String> RESTORABLE_TABLES = Set.of(
            "users",
            "categories",
            "books",
            "inventory",
            "borrow_requests",
            "borrow_records",
            "reservations",
            "fines",
            "role_permissions",
            "system_configs",
            "status_codes",
            "book_favorites",
            "book_reviews",
            "book_copies"
    );
    private static final List<String> RESTORE_DELETE_ORDER = List.of(
            "book_reviews",
            "book_favorites",
            "fines",
            "reservations",
            "borrow_records",
            "borrow_requests",
            "book_copies",
            "inventory",
            "books",
            "status_codes",
            "system_configs",
            "role_permissions",
            "categories",
            "users"
    );
    private static final List<String> BACKUP_TABLES = List.of(
            "users",
            "categories",
            "books",
            "inventory",
            "borrow_requests",
            "borrow_records",
            "reservations",
            "fines",
            "role_permissions",
            "system_configs",
            "status_codes",
            "book_favorites",
            "book_reviews",
            "book_copies"
    );

    private final UserMapper userMapper;
    private final BookMapper bookMapper;
    private final BorrowRecordMapper borrowRecordMapper;
    private final BorrowRequestMapper borrowRequestMapper;
    private final ReservationMapper reservationMapper;
    private final FineMapper fineMapper;
    private final InventoryMapper inventoryMapper;
    private final OperationLogMapper operationLogMapper;
    private final BackupRecordMapper backupRecordMapper;
    private final SystemConfigMapper systemConfigMapper;
    private final CurrentUserSupport currentUserSupport;
    private final OperationLogSupport operationLogSupport;
    private final ObjectMapper objectMapper;
    private final JdbcTemplate jdbcTemplate;

    @Override
    public AdminMonitoringOverviewVo getOverview(String authorizationHeader) {
        requireAdmin(authorizationHeader);
        List<User> users = userMapper.selectAllActive();
        List<Book> books = bookMapper.selectAll();
        List<BorrowRecord> borrowRecords = borrowRecordMapper.selectAll();
        List<Reservation> reservations = reservationMapper.selectAll();
        List<Fine> fines = fineMapper.selectAll();
        BackupRecord latestBackup = backupRecordMapper.selectLatest();
        return AdminMonitoringOverviewVo.builder()
                .totalUsers(users.size())
                .totalBooks(books.size())
                .activeBorrows((int) borrowRecords.stream().filter(this::isActiveBorrow).count())
                .pendingReservations((int) reservations.stream().filter(item -> "PENDING".equalsIgnoreCase(String.valueOf(item.getStatus()))).count())
                .unpaidFines((int) fines.stream().filter(item -> item.getStatus() == FineStatus.UNPAID).count())
                .systemStatus(buildSystemStatusLabel(latestBackup, fines, borrowRecords))
                .latestBackup(latestBackup == null ? "No backup yet" : latestBackup.getBackupName())
                .build();
    }

    @Override
    public AdminBusinessReportVo getBusinessReport(String authorizationHeader) {
        requireAdmin(authorizationHeader);
        List<User> users = userMapper.selectAllActive();
        List<Book> books = bookMapper.selectAll();
        List<BorrowRequest> borrowRequests = getAllBorrowRequests();
        List<BorrowRecord> borrowRecords = borrowRecordMapper.selectAll();
        List<Reservation> reservations = reservationMapper.selectAll();
        List<Fine> fines = fineMapper.selectAll();
        List<Inventory> inventories = getAllInventories(books);

        BigDecimal unpaidFineAmount = sumFinesByStatus(fines, FineStatus.UNPAID);
        BigDecimal paidFineAmount = sumFinesByStatus(fines, FineStatus.PAID);

        return AdminBusinessReportVo.builder()
                .totalReaders((int) users.stream().filter(item -> item.getRole() == RoleType.READER).count())
                .totalLibrarians((int) users.stream().filter(item -> item.getRole() == RoleType.LIBRARIAN).count())
                .totalAdmins((int) users.stream().filter(item -> item.getRole() == RoleType.ADMIN).count())
                .totalBorrowRequests(borrowRequests.size())
                .pendingBorrowRequests((int) borrowRequests.stream().filter(item -> "PENDING".equalsIgnoreCase(String.valueOf(item.getStatus()))).count())
                .overdueBorrows((int) borrowRecords.stream().filter(item -> item.getStatus() == BorrowRecordStatus.OVERDUE).count())
                .activeReservations((int) reservations.stream().filter(item -> "PENDING".equalsIgnoreCase(String.valueOf(item.getStatus()))).count())
                .unavailableBooks((int) inventories.stream().filter(item -> item.getAvailableCopies() == null || item.getAvailableCopies() <= 0).count())
                .totalInventoryCopies(inventories.stream().map(Inventory::getTotalCopies).filter(Objects::nonNull).mapToInt(Integer::intValue).sum())
                .availableInventoryCopies(inventories.stream().map(Inventory::getAvailableCopies).filter(Objects::nonNull).mapToInt(Integer::intValue).sum())
                .unpaidFineAmount(unpaidFineAmount)
                .paidFineAmount(paidFineAmount)
                .borrowStatusBreakdown(toMetricItems(groupByLabel(borrowRecords.stream().map(item -> String.valueOf(item.getStatus())).toList())))
                .requestStatusBreakdown(toMetricItems(groupByLabel(borrowRequests.stream().map(item -> String.valueOf(item.getStatus())).toList())))
                .reservationStatusBreakdown(toMetricItems(groupByLabel(reservations.stream().map(item -> String.valueOf(item.getStatus())).toList())))
                .build();
    }

    @Override
    public AdminFineStatisticsVo getFineStatistics(String authorizationHeader) {
        requireAdmin(authorizationHeader);

        BigDecimal unpaidFineAmount = fineMapper.sumAmountByStatusAndDateRange("UNPAID", null, null);
        BigDecimal paidFineTotal = fineMapper.sumAmountByStatusAndDateRange("PAID", null, null);

        LocalDateTime now = LocalDateTime.now();

        LocalDateTime todayStart = now.toLocalDate().atStartOfDay();
        BigDecimal paidFineToday = fineMapper.sumAmountByStatusAndDateRange("PAID", todayStart, null);

        LocalDateTime yesterdayStart = todayStart.minusDays(1);
        BigDecimal paidFineYesterday = fineMapper.sumAmountByStatusAndDateRange("PAID", yesterdayStart, todayStart);

        LocalDateTime thisWeekStart = now.toLocalDate().with(java.time.DayOfWeek.MONDAY).atStartOfDay();
        LocalDateTime lastWeekStart = thisWeekStart.minusDays(7);
        BigDecimal paidFineLastWeek = fineMapper.sumAmountByStatusAndDateRange("PAID", lastWeekStart, thisWeekStart);

        LocalDateTime thisMonthStart = now.toLocalDate().withDayOfMonth(1).atStartOfDay();
        LocalDateTime lastMonthStart = thisMonthStart.minusMonths(1);
        BigDecimal paidFineLastMonth = fineMapper.sumAmountByStatusAndDateRange("PAID", lastMonthStart, thisMonthStart);

        return AdminFineStatisticsVo.builder()
                .unpaidFineAmount(unpaidFineAmount)
                .paidFineTotal(paidFineTotal)
                .paidFineToday(paidFineToday)
                .paidFineYesterday(paidFineYesterday)
                .paidFineLastWeek(paidFineLastWeek)
                .paidFineLastMonth(paidFineLastMonth)
                .build();
    }

    @Override
    public AdminRuntimeStatusVo getRuntimeStatus(String authorizationHeader) {
        requireAdmin(authorizationHeader);
        List<User> users = userMapper.selectAllActive();
        List<Book> books = bookMapper.selectAll();
        List<BorrowRecord> borrowRecords = borrowRecordMapper.selectAll();
        List<OperationLog> logs = operationLogMapper.selectAll();
        BackupRecord latestBackup = backupRecordMapper.selectLatest();
        List<Inventory> inventories = getAllInventories(books);

        int recentFailureLogs = (int) logs.stream()
                .limit(20)
                .filter(this::isFailureLog)
                .count();
        int disabledUsers = (int) users.stream().filter(item -> !Boolean.TRUE.equals(item.getEnabled())).count();
        int booksWithoutAvailableCopies = (int) inventories.stream()
                .filter(item -> item.getAvailableCopies() == null || item.getAvailableCopies() <= 0)
                .count();
        int activeUsersInLast24Hours = (int) logs.stream()
                .filter(item -> item.getCreatedAt() != null && item.getCreatedAt().isAfter(LocalDateTime.now().minusHours(24)))
                .map(OperationLog::getOperatorName)
                .filter(Objects::nonNull)
                .filter(name -> !name.isBlank())
                .collect(Collectors.toSet())
                .size();
        String systemStatus = buildSystemStatusLabel(latestBackup, List.of(), borrowRecords);

        List<AdminMetricItemVo> alerts = new ArrayList<>();
        if (recentFailureLogs > 0) {
            alerts.add(metric("Recent failed operations", recentFailureLogs));
        }
        if (booksWithoutAvailableCopies > 0) {
            alerts.add(metric("Books out of stock", booksWithoutAvailableCopies));
        }
        if (latestBackup == null) {
            alerts.add(metric("Missing backups", 1L));
        }
        if (alerts.isEmpty()) {
            alerts.add(metric("Active alerts", 0L));
        }

        return AdminRuntimeStatusVo.builder()
                .systemStatus(systemStatus)
                .databaseStatus(checkDatabaseStatus())
                .backupStatus(latestBackup == null ? "No backup snapshot available" : ("Latest backup: " + latestBackup.getBackupName()))
                .recentFailureLogs(recentFailureLogs)
                .disabledUsers(disabledUsers)
                .booksWithoutAvailableCopies(booksWithoutAvailableCopies)
                .activeUsersInLast24Hours(activeUsersInLast24Hours)
                .latestOperationLogId(logs.isEmpty() ? null : logs.get(0).getId().intValue())
                .alerts(alerts)
                .build();
    }

    @Override
    public List<AdminOperationLogVo> listOperationLogs(String authorizationHeader) {
        requireAdmin(authorizationHeader);
        return operationLogMapper.selectAll().stream().map(this::toOperationLogVo).toList();
    }

    @Override
    public List<AdminOperationLogVo> listAbnormalBehaviors(String authorizationHeader) {
        requireAdmin(authorizationHeader);
        return operationLogMapper.selectAll().stream()
                .filter(this::isFailureLog)
                .map(this::toOperationLogVo)
                .toList();
    }

    @Override
    public AdminBackupRecordVo executeBackup(String authorizationHeader) {
        User admin = requireAdmin(authorizationHeader);
        try {
            Files.createDirectories(Path.of("backups"));
            String stamp = LocalDateTime.now().format(BACKUP_STAMP_FORMATTER);
            String backupName = "backup-" + stamp;
            Path file = Path.of("backups", backupName + ".json");

            Map<String, Object> snapshot = buildBackupSnapshot();
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(file.toFile(), snapshot);

            BackupRecord backupRecord = new BackupRecord();
            backupRecord.setBackupName(backupName);
            backupRecord.setFilePath(file.toAbsolutePath().toString());
            backupRecord.setStatus("SUCCESS");
            backupRecord.setSummary(buildBackupSummary(snapshot));
            backupRecordMapper.insert(backupRecord);

            operationLogSupport.record("SYSTEM_BACKUP", "BACKUP_EXECUTE", admin.getUsername(), "Backup created at " + backupRecord.getFilePath());
            return toBackupVo(backupRecordMapper.selectLatest());
        } catch (IOException | SQLException exception) {
            operationLogSupport.record("SYSTEM_BACKUP", "BACKUP_FAILED", admin.getUsername(), exception.getMessage());
            throw new BusinessException(500, "failed to execute backup");
        }
    }

    @Override
    public List<AdminBackupRecordVo> listBackupRecords(String authorizationHeader) {
        requireAdmin(authorizationHeader);
        return backupRecordMapper.selectAll().stream().map(this::toBackupVo).toList();
    }

    @Override
    @Transactional
    public AdminRestoreResultVo restoreBackup(String authorizationHeader, AdminBackupRestoreRequest request) {
        User admin = requireAdmin(authorizationHeader);
        BackupRecord backupRecord = backupRecordMapper.selectById(request.getBackupId());
        if (backupRecord == null) {
            throw new BusinessException(404, "backup record not found");
        }
        Path backupPath = Path.of(backupRecord.getFilePath());
        if (!Files.exists(backupPath)) {
            operationLogSupport.record("SYSTEM_BACKUP", "RESTORE_FAILED", admin.getUsername(), "backup file not found: " + backupRecord.getFilePath());
            throw new BusinessException(404, "backup file not found");
        }

        try {
            JsonNode root = objectMapper.readTree(backupPath.toFile());
            JsonNode tablesNode = root.get("tables");
            if (tablesNode == null || !tablesNode.isObject()) {
                throw new BusinessException(400, "backup file does not support restore");
            }

            Map<String, List<Map<String, Object>>> tables = objectMapper.convertValue(
                    tablesNode,
                    new TypeReference<>() {}
            );

            restoreSnapshotTables(tables);
            operationLogSupport.record("SYSTEM_BACKUP", "RESTORE_EXECUTE", admin.getUsername(), "Restored backup " + backupRecord.getBackupName());
            return AdminRestoreResultVo.builder()
                    .backupId(backupRecord.getId())
                    .backupName(backupRecord.getBackupName())
                    .restoredAt(LocalDateTime.now().toString())
                    .restoredBy(admin.getUsername())
                    .restoredTables(RESTORE_DELETE_ORDER.stream()
                            .filter(tables::containsKey)
                            .map(table -> metric(table, (long) tables.get(table).size()))
                            .toList())
                    .build();
        } catch (IOException exception) {
            operationLogSupport.record("SYSTEM_BACKUP", "RESTORE_FAILED", admin.getUsername(), exception.getMessage());
            throw new BusinessException(500, "failed to read backup file");
        }
    }

    private Map<String, Object> buildBackupSnapshot() throws SQLException {
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("version", "2");
        snapshot.put("createdAt", LocalDateTime.now().toString());
        snapshot.put("summary", buildSnapshotSummaryMap());
        Map<String, List<String>> columns = new LinkedHashMap<>();
        Map<String, List<Map<String, Object>>> tables = new LinkedHashMap<>();
        for (String table : BACKUP_TABLES) {
            columns.put(table, resolveTableColumns(table));
            tables.put(table, jdbcTemplate.queryForList("select * from " + table + " order by id asc"));
        }
        snapshot.put("columns", columns);
        snapshot.put("tables", tables);
        return snapshot;
    }

    private Map<String, Object> buildSnapshotSummaryMap() {
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("users", userMapper.selectAllActive().size());
        summary.put("books", bookMapper.selectAll().size());
        summary.put("borrowRecords", borrowRecordMapper.selectAll().size());
        summary.put("pendingReservations", reservationMapper.selectPending().size());
        summary.put("configs", systemConfigMapper.selectAll().size());
        return summary;
    }

    private String buildBackupSummary(Map<String, Object> snapshot) {
        Object summaryNode = snapshot.get("summary");
        if (!(summaryNode instanceof Map<?, ?> summary)) {
            return "Backup snapshot created";
        }
        return "Users=" + summary.get("users")
                + ", Books=" + summary.get("books")
                + ", BorrowRecords=" + summary.get("borrowRecords")
                + ", PendingReservations=" + summary.get("pendingReservations");
    }

    private List<String> resolveTableColumns(String tableName) throws SQLException {
        try (Connection connection = Objects.requireNonNull(jdbcTemplate.getDataSource()).getConnection()) {
            DatabaseMetaData metaData = connection.getMetaData();
            try (ResultSet resultSet = metaData.getColumns(connection.getCatalog(), null, tableName, null)) {
                List<String> columns = new ArrayList<>();
                while (resultSet.next()) {
                    columns.add(resultSet.getString("COLUMN_NAME"));
                }
                return columns;
            }
        }
    }

    private void restoreSnapshotTables(Map<String, List<Map<String, Object>>> tables) {
        for (String table : tables.keySet()) {
            if (!RESTORABLE_TABLES.contains(table)) {
                throw new BusinessException(400, "backup file contains unsupported table: " + table);
            }
        }

        jdbcTemplate.execute("SET FOREIGN_KEY_CHECKS = 0");
        try {
            for (String table : RESTORE_DELETE_ORDER) {
                if (tables.containsKey(table)) {
                    jdbcTemplate.update("delete from " + table);
                }
            }

            for (String table : RESTORE_DELETE_ORDER) {
                List<Map<String, Object>> rows = tables.get(table);
                if (rows == null || rows.isEmpty()) {
                    continue;
                }
                insertRows(table, rows);
            }
        } finally {
            jdbcTemplate.execute("SET FOREIGN_KEY_CHECKS = 1");
        }
    }

    private void insertRows(String table, List<Map<String, Object>> rows) {
        Set<String> columnSet = new LinkedHashSet<>();
        rows.forEach(row -> columnSet.addAll(row.keySet()));
        List<String> columns = columnSet.stream()
                .sorted(Comparator.comparingInt(column -> "id".equals(column) ? 0 : 1))
                .toList();
        String placeholders = columns.stream().map(ignored -> "?").collect(Collectors.joining(", "));
        String sql = "insert into " + table + " (" + String.join(", ", columns) + ") values (" + placeholders + ")";
        List<Object[]> batchArgs = rows.stream()
                .map(row -> columns.stream().map(row::get).toArray())
                .toList();
        jdbcTemplate.batchUpdate(sql, batchArgs);
    }

    private String buildSystemStatusLabel(BackupRecord latestBackup, List<Fine> fines, List<BorrowRecord> borrowRecords) {
        boolean hasBackup = latestBackup != null;
        boolean hasOverdue = borrowRecords.stream().anyMatch(item -> item.getStatus() == BorrowRecordStatus.OVERDUE);
        boolean hasUnpaidFines = fines.stream().anyMatch(item -> item.getStatus() == FineStatus.UNPAID);
        if (hasBackup && !hasOverdue && !hasUnpaidFines) {
            return "Healthy";
        }
        if (!hasBackup) {
            return "Attention";
        }
        return "Warning";
    }

    private String checkDatabaseStatus() {
        try {
            Integer result = jdbcTemplate.queryForObject("select 1", Integer.class);
            return Integer.valueOf(1).equals(result) ? "Connected" : "Unknown";
        } catch (Exception exception) {
            return "Unavailable";
        }
    }

    private List<Inventory> getAllInventories(List<Book> books) {
        return books.stream()
                .map(Book::getId)
                .filter(Objects::nonNull)
                .map(inventoryMapper::selectByBookId)
                .filter(Objects::nonNull)
                .toList();
    }

    private List<BorrowRequest> getAllBorrowRequests() {
        List<BorrowRequest> requests = new ArrayList<>();
        requests.addAll(borrowRequestMapper.selectPendingRequests());
        requests.addAll(borrowRequestMapper.selectByStatus("APPROVED"));
        requests.addAll(borrowRequestMapper.selectByStatus("REJECTED"));
        return requests.stream()
                .collect(Collectors.toMap(BorrowRequest::getId, item -> item, (left, right) -> left, LinkedHashMap::new))
                .values()
                .stream()
                .sorted(Comparator.comparing(BorrowRequest::getId))
                .toList();
    }

    private BigDecimal sumFinesByStatus(List<Fine> fines, FineStatus status) {
        return fines.stream()
                .filter(item -> item.getStatus() == status)
                .map(Fine::getAmount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private Map<String, Long> groupByLabel(List<String> labels) {
        return labels.stream()
                .filter(Objects::nonNull)
                .collect(Collectors.groupingBy(label -> label, LinkedHashMap::new, Collectors.counting()));
    }

    private List<AdminMetricItemVo> toMetricItems(Map<String, Long> counters) {
        return counters.entrySet().stream()
                .map(entry -> metric(entry.getKey(), entry.getValue()))
                .toList();
    }

    private boolean isActiveBorrow(BorrowRecord item) {
        return item.getStatus() == BorrowRecordStatus.BORROWED
                || item.getStatus() == BorrowRecordStatus.RETURN_PENDING
                || item.getStatus() == BorrowRecordStatus.OVERDUE;
    }

    private boolean isFailureLog(OperationLog item) {
        String action = item.getActionName() == null ? "" : item.getActionName().toUpperCase();
        String message = item.getResultMessage() == null ? "" : item.getResultMessage().toLowerCase();
        return action.contains("FAILED")
                || action.contains("DENIED")
                || action.contains("RESTORE_FAILED")
                || message.contains("invalid")
                || message.contains("denied")
                || message.contains("disabled")
                || message.contains("not found")
                || message.contains("failed");
    }

    private User requireAdmin(String authorizationHeader) {
        User user = currentUserSupport.requireUser(authorizationHeader);
        if (user.getRole() != RoleType.ADMIN) {
            throw new BusinessException(403, "current user is not an admin");
        }
        return user;
    }

    private AdminMetricItemVo metric(String label, long value) {
        return AdminMetricItemVo.builder()
                .label(label)
                .value(value)
                .build();
    }

    private AdminOperationLogVo toOperationLogVo(OperationLog operationLog) {
        return AdminOperationLogVo.builder()
                .logId(operationLog.getId())
                .moduleName(operationLog.getModuleName())
                .actionName(operationLog.getActionName())
                .operatorName(operationLog.getOperatorName())
                .resultMessage(operationLog.getResultMessage())
                .createdAt(operationLog.getCreatedAt() == null ? null : operationLog.getCreatedAt().toString())
                .build();
    }

    private AdminBackupRecordVo toBackupVo(BackupRecord backupRecord) {
        boolean restorable = false;
        if (backupRecord.getFilePath() != null && !backupRecord.getFilePath().isBlank()) {
            Path path = Path.of(backupRecord.getFilePath());
            if (Files.exists(path)) {
                try {
                    JsonNode root = objectMapper.readTree(path.toFile());
                    restorable = root.has("tables");
                } catch (IOException ignored) {
                    restorable = false;
                }
            }
        }

        return AdminBackupRecordVo.builder()
                .backupId(backupRecord.getId())
                .backupName(backupRecord.getBackupName())
                .filePath(backupRecord.getFilePath())
                .status(backupRecord.getStatus())
                .summary(backupRecord.getSummary())
                .createdAt(backupRecord.getCreatedAt() == null ? null : backupRecord.getCreatedAt().toString())
                .restorable(restorable)
                .build();
    }
}
