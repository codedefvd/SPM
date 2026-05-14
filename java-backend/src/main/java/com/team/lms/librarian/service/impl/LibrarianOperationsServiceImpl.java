package com.team.lms.librarian.service.impl;

import com.team.lms.common.enums.BorrowRecordStatus;
import com.team.lms.common.enums.FineStatus;
import com.team.lms.common.enums.ReservationStatus;
import com.team.lms.common.enums.RoleType;
import com.team.lms.common.support.CurrentUserSupport;
import com.team.lms.common.support.PermissionScopeSupport;
import com.team.lms.common.support.SystemConfigSupport;
import com.team.lms.entity.BorrowRecord;
import com.team.lms.entity.BookCopy;
import com.team.lms.entity.Fine;
import com.team.lms.entity.Inventory;
import com.team.lms.entity.Reservation;
import com.team.lms.exception.BusinessException;
import com.team.lms.librarian.dto.FineStatusUpdateRequest;
import com.team.lms.librarian.dto.ReservationProcessRequest;
import com.team.lms.librarian.dto.ReturnProcessRequest;
import com.team.lms.librarian.service.LibrarianOperationsService;
import com.team.lms.librarian.vo.BorrowingRecordManageVo;
import com.team.lms.librarian.vo.FineManageVo;
import com.team.lms.librarian.vo.LibrarianStatsDetailVo;
import com.team.lms.librarian.vo.LibrarianStatsVo;
import com.team.lms.librarian.vo.ReservationManageVo;
import com.team.lms.librarian.vo.ReturnManageVo;
import com.team.lms.mapper.BookCopyMapper;
import com.team.lms.mapper.BookMapper;
import com.team.lms.mapper.BorrowRecordMapper;
import com.team.lms.mapper.BorrowRequestMapper;
import com.team.lms.mapper.FineMapper;
import com.team.lms.mapper.InventoryMapper;
import com.team.lms.mapper.ReservationMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class LibrarianOperationsServiceImpl implements LibrarianOperationsService {

    private final BorrowRecordMapper borrowRecordMapper;
    private final BorrowRequestMapper borrowRequestMapper;
    private final ReservationMapper reservationMapper;
    private final FineMapper fineMapper;
    private final InventoryMapper inventoryMapper;
    private final BookMapper bookMapper;
    private final BookCopyMapper bookCopyMapper;
    private final CurrentUserSupport currentUserSupport;
    private final PermissionScopeSupport permissionScopeSupport;
    private final SystemConfigSupport systemConfigSupport;

    @Override
    public List<BorrowingRecordManageVo> listCurrentBorrowings(String authorizationHeader) {
        permissionScopeSupport.requirePermission(authorizationHeader, RoleType.LIBRARIAN, "REQUEST_PROCESS");
        return borrowRecordMapper.selectAll().stream()
                .filter(record -> record.getStatus() == BorrowRecordStatus.BORROWED
                        || record.getStatus() == BorrowRecordStatus.RETURN_PENDING
                        || record.getStatus() == BorrowRecordStatus.OVERDUE)
                .map(this::toBorrowingRecordVo)
                .toList();
    }

    @Override
    public List<BorrowingRecordManageVo> listOverdueBorrowings(String authorizationHeader) {
        permissionScopeSupport.requirePermission(authorizationHeader, RoleType.LIBRARIAN, "FINE_MANAGE");
        return borrowRecordMapper.selectAll().stream()
                .filter(this::isOverdueRecord)
                .map(this::ensureOverdueFine)
                .map(this::toBorrowingRecordVo)
                .toList();
    }

    @Override
    public List<ReturnManageVo> listPendingReturns(String authorizationHeader) {
        permissionScopeSupport.requirePermission(authorizationHeader, RoleType.LIBRARIAN, "REQUEST_PROCESS");
        return borrowRecordMapper.selectReturnPendingRecords().stream()
                .map(record -> toReturnManageVo(record, findFineByRecordId(record.getId()), null))
                .toList();
    }

    @Override
    public ReturnManageVo getActiveBorrowByBarcode(String authorizationHeader, String barcode) {
        permissionScopeSupport.requirePermission(authorizationHeader, RoleType.LIBRARIAN, "REQUEST_PROCESS");
        if (barcode == null || barcode.trim().isEmpty()) {
            throw new BusinessException(400, "barcode is required");
        }
        BookCopy copy = bookCopyMapper.selectByBarcode(barcode.trim());
        if (copy == null) {
            throw new BusinessException(404, "no book copy found for the given barcode");
        }
        BorrowRecord record = borrowRecordMapper.selectActiveByBookCopyBarcode(barcode.trim());
        if (record == null) {
            throw new BusinessException(404, "no active borrow record found for this barcode");
        }
        Fine fine = findFineByRecordId(record.getId());
        return toReturnManageVo(record, fine, "active borrow record found");
    }

    @Override
    @Transactional
    public ReturnManageVo processReturn(String authorizationHeader, Long recordId, ReturnProcessRequest request) {
        permissionScopeSupport.requirePermission(authorizationHeader, RoleType.LIBRARIAN, "REQUEST_PROCESS");
        BorrowRecord record = borrowRecordMapper.selectById(recordId);
        if (record == null) {
            throw new BusinessException(404, "borrow record not found");
        }
        if (record.getStatus() != BorrowRecordStatus.RETURN_PENDING) {
            throw new BusinessException(400, "only RETURN_PENDING records can be processed");
        }

        if (!request.getApprove()) {
            record.setStatus(BorrowRecordStatus.BORROWED);
            borrowRecordMapper.update(record);
            return toReturnManageVo(record, findFineByRecordId(record.getId()),
                    request.getRejectReason() == null || request.getRejectReason().isBlank()
                            ? "return request rejected"
                            : request.getRejectReason().trim());
        }

        record.setStatus(BorrowRecordStatus.RETURNED);
        record.setReturnDate(LocalDate.now());
        borrowRecordMapper.update(record);

        Inventory inventory = inventoryMapper.selectByBookId(record.getBook().getId());
        if (inventory == null) {
            throw new BusinessException(500, "inventory record not found");
        }
        inventory.setAvailableCopies(inventory.getAvailableCopies() + 1);
        inventoryMapper.update(inventory);

        Fine fine = null;
        BigDecimal fineAmount = request.getFineAmount();
        if ((fineAmount == null || fineAmount.compareTo(BigDecimal.ZERO) <= 0)
                && record.getDueDate() != null
                && LocalDate.now().isAfter(record.getDueDate())) {
            long overdueDays = record.getDueDate().until(LocalDate.now()).getDays();
            fineAmount = systemConfigSupport.getOverdueFinePerDay().multiply(BigDecimal.valueOf(overdueDays));
        }
        if (fineAmount != null && fineAmount.compareTo(BigDecimal.ZERO) > 0) {
            fine = upsertFine(record, fineAmount, FineStatus.UNPAID);
        }

        return toReturnManageVo(record, fine, "return processed successfully");
    }

    @Override
    public List<ReservationManageVo> listReservations(String authorizationHeader) {
        permissionScopeSupport.requirePermission(authorizationHeader, RoleType.LIBRARIAN, "RESERVATION_PROCESS");
        return reservationMapper.selectAll().stream()
                .map(item -> toReservationVo(item, null))
                .toList();
    }

    @Override
    @Transactional
    public ReservationManageVo processReservation(String authorizationHeader, Long reservationId, ReservationProcessRequest request) {
        permissionScopeSupport.requirePermission(authorizationHeader, RoleType.LIBRARIAN, "RESERVATION_PROCESS");
        Reservation reservation = reservationMapper.selectById(reservationId);
        if (reservation == null) {
            throw new BusinessException(404, "reservation not found");
        }
        if (reservation.getStatus() != ReservationStatus.PENDING) {
            throw new BusinessException(400, "reservation is already processed");
        }

        String action = request.getAction().trim().toUpperCase(Locale.ROOT);
        switch (action) {
            case "FULFILL" -> {
                List<Reservation> pendingQueue = reservationMapper.selectPendingByBookId(reservation.getBook().getId());
                Reservation nextReservation = pendingQueue.isEmpty() ? null : pendingQueue.get(0);
                if (nextReservation == null || !nextReservation.getId().equals(reservation.getId())) {
                    throw new BusinessException(400, "reservation must be fulfilled according to queue order");
                }
                Inventory inventory = inventoryMapper.selectByBookId(reservation.getBook().getId());
                if (inventory == null || inventory.getAvailableCopies() <= 0) {
                    throw new BusinessException(400, "no copies available for reservation fulfillment");
                }
                inventory.setAvailableCopies(inventory.getAvailableCopies() - 1);
                inventoryMapper.update(inventory);
                reservation.setStatus(ReservationStatus.FULFILLED);
                reservationMapper.update(reservation);
                return toReservationVo(reservation, "reservation fulfilled and inventory deducted");
            }
            case "CANCEL" -> {
                reservation.setStatus(ReservationStatus.CANCELED);
                reservationMapper.update(reservation);
                return toReservationVo(reservation, "reservation canceled");
            }
            case "EXPIRE" -> {
                reservation.setStatus(ReservationStatus.EXPIRED);
                reservationMapper.update(reservation);
                return toReservationVo(reservation, "reservation expired");
            }
            default -> throw new BusinessException(400, "action must be one of FULFILL/CANCEL/EXPIRE");
        }
    }

    @Override
    public List<FineManageVo> listFines(String authorizationHeader) {
        permissionScopeSupport.requirePermission(authorizationHeader, RoleType.LIBRARIAN, "FINE_MANAGE");
        return fineMapper.selectAll().stream().map(this::toFineVo).toList();
    }

    @Override
    @Transactional
    public FineManageVo updateFineStatus(String authorizationHeader, Long fineId, FineStatusUpdateRequest request) {
        permissionScopeSupport.requirePermission(authorizationHeader, RoleType.LIBRARIAN, "FINE_MANAGE");
        Fine fine = fineMapper.selectById(fineId);
        if (fine == null) {
            throw new BusinessException(404, "fine not found");
        }
        try {
            fine.setStatus(FineStatus.valueOf(request.getStatus().trim().toUpperCase(Locale.ROOT)));
        } catch (IllegalArgumentException ex) {
            throw new BusinessException(400, "fine status must be UNPAID/PAID/WAIVED");
        }
        fineMapper.update(fine);
        return toFineVo(fineMapper.selectById(fineId));
    }

    @Override
    public LibrarianStatsVo getStatistics(String authorizationHeader) {
        permissionScopeSupport.requireAnyPermission(
                authorizationHeader,
                RoleType.LIBRARIAN,
                List.of("REQUEST_PROCESS", "RESERVATION_PROCESS", "FINE_MANAGE")
        );
        int totalBooks = bookMapper.selectAll().size();
        int offShelfBooks = (int) bookMapper.selectAll().stream()
                .filter(book -> book.getShelfStatus() != null && !"ON_SHELF".equals(book.getShelfStatus().name()))
                .count();
        int pendingBorrowRequests = borrowRequestMapper.selectPendingRequests().size();
        int pendingReturnRequests = borrowRecordMapper.selectReturnPendingRecords().size();
        int pendingReservations = reservationMapper.selectPending().size();
        int unpaidFines = fineMapper.selectUnpaid().size();
        int activeBorrows = (int) borrowRecordMapper.selectAll().stream()
                .filter(record -> record.getStatus() == BorrowRecordStatus.BORROWED
                        || record.getStatus() == BorrowRecordStatus.RETURN_PENDING
                        || record.getStatus() == BorrowRecordStatus.OVERDUE)
                .count();

        return LibrarianStatsVo.builder()
                .totalBooks(totalBooks)
                .offShelfBooks(offShelfBooks)
                .pendingBorrowRequests(pendingBorrowRequests)
                .pendingReturnRequests(pendingReturnRequests)
                .pendingReservations(pendingReservations)
                .unpaidFines(unpaidFines)
                .activeBorrows(activeBorrows)
                .build();
    }

    private Fine upsertFine(BorrowRecord record, BigDecimal amount, FineStatus status) {
        Fine existing = findFineByRecordId(record.getId());
        if (existing == null) {
            Fine fine = new Fine();
            fine.setReader(record.getReader());
            fine.setBorrowRecord(record);
            fine.setAmount(amount);
            fine.setStatus(status);
            fineMapper.insert(fine);
            return fineMapper.selectById(fine.getId());
        }
        existing.setAmount(amount);
        existing.setStatus(status);
        fineMapper.update(existing);
        return fineMapper.selectById(existing.getId());
    }

    private Fine findFineByRecordId(Long recordId) {
        return fineMapper.selectByBorrowRecordId(recordId);
    }

    private ReturnManageVo toReturnManageVo(BorrowRecord record, Fine fine, String message) {
        return ReturnManageVo.builder()
                .recordId(record.getId())
                .bookId(record.getBook().getId())
                .bookTitle(record.getBook().getTitle())
                .copyBarcode(record.getBookCopy() == null ? null : record.getBookCopy().getBarcode())
                .readerId(record.getReader().getId())
                .readerUsername(record.getReader().getUsername())
                .status(record.getStatus().name())
                .dueDate(record.getDueDate() == null ? null : record.getDueDate().toString())
                .returnDate(record.getReturnDate() == null ? null : record.getReturnDate().toString())
                .fineAmount(fine == null ? BigDecimal.ZERO : fine.getAmount())
                .message(message)
                .build();
    }

    private ReservationManageVo toReservationVo(Reservation reservation, String message) {
        return ReservationManageVo.builder()
                .reservationId(reservation.getId())
                .bookId(reservation.getBook().getId())
                .bookTitle(reservation.getBook().getTitle())
                .readerId(reservation.getReader().getId())
                .readerUsername(reservation.getReader().getUsername())
                .status(reservation.getStatus().name())
                .queueNo(reservation.getQueueNo())
                .message(message)
                .build();
    }

    private FineManageVo toFineVo(Fine fine) {
        return FineManageVo.builder()
                .fineId(fine.getId())
                .recordId(fine.getBorrowRecord() == null ? null : fine.getBorrowRecord().getId())
                .bookTitle(fine.getBorrowRecord() == null || fine.getBorrowRecord().getBook() == null ? null : fine.getBorrowRecord().getBook().getTitle())
                .copyBarcode(fine.getBorrowRecord() == null || fine.getBorrowRecord().getBookCopy() == null ? null : fine.getBorrowRecord().getBookCopy().getBarcode())
                .readerId(fine.getReader() == null ? null : fine.getReader().getId())
                .readerUsername(fine.getReader() == null ? null : fine.getReader().getUsername())
                .amount(fine.getAmount())
                .status(fine.getStatus() == null ? null : fine.getStatus().name())
                .build();
    }

    private BorrowRecord ensureOverdueFine(BorrowRecord record) {
        long overdueDays = calculateOverdueDays(record);
        if (overdueDays <= 0) {
            return record;
        }
        BigDecimal amount = systemConfigSupport.getOverdueFinePerDay().multiply(BigDecimal.valueOf(overdueDays));
        upsertFine(record, amount, resolveFineStatus(record.getId()));
        return record;
    }

    private FineStatus resolveFineStatus(Long recordId) {
        Fine existing = findFineByRecordId(recordId);
        return existing == null || existing.getStatus() == null ? FineStatus.UNPAID : existing.getStatus();
    }

    private BorrowingRecordManageVo toBorrowingRecordVo(BorrowRecord record) {
        long overdueDays = calculateOverdueDays(record);
        Fine fine = overdueDays > 0 ? ensureFineLoaded(record) : findFineByRecordId(record.getId());
        String displayStatus = overdueDays > 0 ? BorrowRecordStatus.OVERDUE.name() : record.getStatus().name();

        return BorrowingRecordManageVo.builder()
                .recordId(record.getId())
                .bookId(record.getBook() == null ? null : record.getBook().getId())
                .bookTitle(record.getBook() == null ? null : record.getBook().getTitle())
                .copyBarcode(record.getBookCopy() == null ? null : record.getBookCopy().getBarcode())
                .readerId(record.getReader() == null ? null : record.getReader().getId())
                .readerUsername(record.getReader() == null ? null : record.getReader().getUsername())
                .status(displayStatus)
                .borrowDate(record.getBorrowDate() == null ? null : record.getBorrowDate().toString())
                .dueDate(record.getDueDate() == null ? null : record.getDueDate().toString())
                .returnDate(record.getReturnDate() == null ? null : record.getReturnDate().toString())
                .overdueDays(overdueDays)
                .fineAmount(fine == null ? BigDecimal.ZERO : fine.getAmount())
                .fineStatus(fine == null || fine.getStatus() == null ? FineStatus.UNPAID.name() : fine.getStatus().name())
                .reminderInfo(buildReminderInfo(record, overdueDays, fine))
                .build();
    }

    private Fine ensureFineLoaded(BorrowRecord record) {
        long overdueDays = calculateOverdueDays(record);
        if (overdueDays <= 0) {
            return findFineByRecordId(record.getId());
        }
        BigDecimal amount = systemConfigSupport.getOverdueFinePerDay().multiply(BigDecimal.valueOf(overdueDays));
        return upsertFine(record, amount, resolveFineStatus(record.getId()));
    }

    private boolean isOverdueRecord(BorrowRecord record) {
        if (record.getDueDate() == null) {
            return false;
        }
        if (record.getStatus() == BorrowRecordStatus.RETURNED) {
            return false;
        }
        return LocalDate.now().isAfter(record.getDueDate());
    }

    private long calculateOverdueDays(BorrowRecord record) {
        if (!isOverdueRecord(record)) {
            return 0;
        }
        return Math.max(0, ChronoUnit.DAYS.between(record.getDueDate(), LocalDate.now()));
    }

    private String buildReminderInfo(BorrowRecord record, long overdueDays, Fine fine) {
        if (overdueDays <= 0) {
            return "No reminder needed";
        }
        String reader = record.getReader() == null ? "Reader" : record.getReader().getUsername();
        BigDecimal amount = fine == null ? BigDecimal.ZERO : fine.getAmount();
        return String.format("%s is overdue by %d day(s). Current fine: %s", reader, overdueDays, amount);
    }

    @Override
    public LibrarianStatsDetailVo getDetailedStatistics(String authorizationHeader, String periodType) {
        String normalizedPeriodType = normalizePeriodType(periodType);
        LibrarianStatsVo basicStats = getStatistics(authorizationHeader);
        List<BorrowRecord> allRecords = borrowRecordMapper.selectAll();

        List<LibrarianStatsDetailVo.PopularBookVo> popularBooks =
                borrowRecordMapper.selectPopularBooks(10).stream()
                        .map(stat -> LibrarianStatsDetailVo.PopularBookVo.builder()
                                .bookId(stat.getBookId())
                                .title(stat.getTitle())
                                .author(stat.getAuthor())
                                .borrowCount(stat.getBorrowCount())
                                .categoryName(stat.getCategoryName())
                                .build())
                        .toList();

        List<LibrarianStatsDetailVo.CategoryBorrowVo> popularCategories =
                borrowRecordMapper.selectPopularCategories(5).stream()
                        .map(stat -> LibrarianStatsDetailVo.CategoryBorrowVo.builder()
                                .categoryName(stat.getCategoryName())
                                .borrowCount(stat.getBorrowCount())
                                .build())
                        .toList();

        List<LibrarianStatsDetailVo.BorrowTrendVo> borrowTrend =
                borrowRecordMapper.selectBorrowTrend(normalizedPeriodType).stream()
                        .map(stat -> LibrarianStatsDetailVo.BorrowTrendVo.builder()
                                .period(stat.getPeriod())
                                .borrowCount(stat.getBorrowCount())
                                .returnCount(stat.getReturnCount())
                                .build())
                        .toList();

        return LibrarianStatsDetailVo.builder()
                .basicStats(basicStats)
                .periodSummary(buildPeriodSummary(allRecords, normalizedPeriodType))
                .popularBooks(popularBooks)
                .popularCategories(popularCategories)
                .borrowTrend(borrowTrend)
                .build();
    }

    private String normalizePeriodType(String periodType) {
        return "week".equalsIgnoreCase(periodType) ? "week" : "month";
    }

    private LibrarianStatsDetailVo.PeriodSummaryVo buildPeriodSummary(List<BorrowRecord> records, String periodType) {
        LocalDate today = LocalDate.now();
        LocalDate start = "week".equals(periodType)
                ? today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
                : today.withDayOfMonth(1);
        LocalDate end = "week".equals(periodType)
                ? start.plusDays(6)
                : today.with(TemporalAdjusters.lastDayOfMonth());

        long borrowCount = records.stream()
                .filter(record -> isDateWithinPeriod(record.getBorrowDate(), start, end))
                .count();
        long returnCount = records.stream()
                .filter(record -> isDateWithinPeriod(record.getReturnDate(), start, end))
                .count();
        long overdueCount = records.stream()
                .filter(record -> isOverdueWithinPeriod(record, start, end))
                .count();
        long activeReaderCount = records.stream()
                .filter(record -> isDateWithinPeriod(record.getBorrowDate(), start, end))
                .map(record -> record.getReader() == null ? null : record.getReader().getId())
                .filter(Objects::nonNull)
                .distinct()
                .count();

        return LibrarianStatsDetailVo.PeriodSummaryVo.builder()
                .label("week".equals(periodType) ? "This Week" : "This Month")
                .borrowCount(borrowCount)
                .returnCount(returnCount)
                .overdueCount(overdueCount)
                .activeReaderCount(activeReaderCount)
                .returnRate(calculateRate(returnCount, borrowCount))
                .overdueRate(calculateRate(overdueCount, borrowCount))
                .build();
    }

    private boolean isDateWithinPeriod(LocalDate value, LocalDate start, LocalDate end) {
        if (value == null) {
            return false;
        }
        return !value.isBefore(start) && !value.isAfter(end);
    }

    private boolean isOverdueWithinPeriod(BorrowRecord record, LocalDate start, LocalDate end) {
        if (record.getDueDate() == null || record.getDueDate().isBefore(start) || record.getDueDate().isAfter(end)) {
            return false;
        }
        if (record.getReturnDate() != null) {
            return record.getReturnDate().isAfter(record.getDueDate());
        }
        return LocalDate.now().isAfter(record.getDueDate());
    }

    private double calculateRate(long numerator, long denominator) {
        if (denominator <= 0) {
            return 0D;
        }
        return Math.round((numerator * 10000D) / denominator) / 100D;
    }
}
