package com.team.lms.librarian.service.impl;

import com.team.lms.common.enums.BorrowRecordStatus;
import com.team.lms.common.enums.FineStatus;
import com.team.lms.common.enums.ReservationStatus;
import com.team.lms.common.enums.RoleType;
import com.team.lms.common.support.CurrentUserSupport;
import com.team.lms.entity.BorrowRecord;
import com.team.lms.entity.Fine;
import com.team.lms.entity.Inventory;
import com.team.lms.entity.Reservation;
import com.team.lms.entity.User;
import com.team.lms.exception.BusinessException;
import com.team.lms.librarian.dto.FineStatusUpdateRequest;
import com.team.lms.librarian.dto.ReservationProcessRequest;
import com.team.lms.librarian.dto.ReturnProcessRequest;
import com.team.lms.librarian.service.LibrarianOperationsService;
import com.team.lms.librarian.vo.FineManageVo;
import com.team.lms.librarian.vo.LibrarianStatsDetailVo;
import com.team.lms.librarian.vo.LibrarianStatsVo;
import com.team.lms.librarian.vo.ReservationManageVo;
import com.team.lms.librarian.vo.ReturnManageVo;
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
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class LibrarianOperationsServiceImpl implements LibrarianOperationsService {

    private final BorrowRecordMapper borrowRecordMapper;
    private final BorrowRequestMapper borrowRequestMapper;
    private final ReservationMapper reservationMapper;
    private final FineMapper fineMapper;
    private final InventoryMapper inventoryMapper;
    private final BookMapper bookMapper;
    private final CurrentUserSupport currentUserSupport;

    @Override
    public List<ReturnManageVo> listPendingReturns() {
        return borrowRecordMapper.selectReturnPendingRecords().stream()
                .map(record -> toReturnManageVo(record, findFineByRecordId(record.getId()), null))
                .toList();
    }

    @Override
    @Transactional
    public ReturnManageVo processReturn(String authorizationHeader, Long recordId, ReturnProcessRequest request) {
        requireLibrarian(authorizationHeader);
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
        if (fineAmount != null && fineAmount.compareTo(BigDecimal.ZERO) > 0) {
            fine = upsertFine(record, fineAmount, FineStatus.UNPAID);
        }

        return toReturnManageVo(record, fine, "return processed successfully");
    }

    @Override
    public List<ReservationManageVo> listReservations() {
        return reservationMapper.selectAll().stream()
                .map(item -> toReservationVo(item, null))
                .toList();
    }

    @Override
    @Transactional
    public ReservationManageVo processReservation(String authorizationHeader, Long reservationId, ReservationProcessRequest request) {
        requireLibrarian(authorizationHeader);
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
    public List<FineManageVo> listFines() {
        return fineMapper.selectAll().stream().map(this::toFineVo).toList();
    }

    @Override
    @Transactional
    public FineManageVo updateFineStatus(String authorizationHeader, Long fineId, FineStatusUpdateRequest request) {
        requireLibrarian(authorizationHeader);
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
    public LibrarianStatsVo getStatistics() {
        int totalBooks = bookMapper.selectAll().size();
        int offShelfBooks = (int) bookMapper.selectAll().stream()
                .filter(book -> book.getShelfStatus() != null && !"ON_SHELF".equals(book.getShelfStatus().name()))
                .count();
        int pendingBorrowRequests = borrowRequestMapper.selectPendingRequests().size();
        int pendingReturnRequests = borrowRecordMapper.selectReturnPendingRecords().size();
        int pendingReservations = reservationMapper.selectPending().size();
        int unpaidFines = fineMapper.selectUnpaid().size();
        int activeBorrows = (int) borrowRecordMapper.selectAll().stream()
                .filter(record -> record.getStatus() == BorrowRecordStatus.BORROWED || record.getStatus() == BorrowRecordStatus.RETURN_PENDING)
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

    private void requireLibrarian(String authorizationHeader) {
        User user = currentUserSupport.requireUser(authorizationHeader);
        if (user.getRole() != RoleType.LIBRARIAN) {
            throw new BusinessException(403, "current user is not a librarian");
        }
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
        return fineMapper.selectAll().stream()
                .filter(fine -> fine.getBorrowRecord() != null && recordId.equals(fine.getBorrowRecord().getId()))
                .findFirst()
                .orElse(null);
    }

    private ReturnManageVo toReturnManageVo(BorrowRecord record, Fine fine, String message) {
        return ReturnManageVo.builder()
                .recordId(record.getId())
                .bookId(record.getBook().getId())
                .bookTitle(record.getBook().getTitle())
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
                .readerId(fine.getReader() == null ? null : fine.getReader().getId())
                .readerUsername(fine.getReader() == null ? null : fine.getReader().getUsername())
                .amount(fine.getAmount())
                .status(fine.getStatus() == null ? null : fine.getStatus().name())
                .build();
    }
    @Override
    public LibrarianStatsDetailVo getDetailedStatistics(String periodType) {
        LibrarianStatsVo basicStats = getStatistics();

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

        String groupBy = "month".equalsIgnoreCase(periodType) ? "month" : "week";
        List<LibrarianStatsDetailVo.BorrowTrendVo> borrowTrend =
                borrowRecordMapper.selectBorrowTrend(groupBy).stream()
                        .map(stat -> LibrarianStatsDetailVo.BorrowTrendVo.builder()
                                .period(stat.getPeriod())
                                .borrowCount(stat.getBorrowCount())
                                .returnCount(stat.getReturnCount())
                                .build())
                        .toList();

        return LibrarianStatsDetailVo.builder()
                .basicStats(basicStats)
                .popularBooks(popularBooks)
                .borrowTrend(borrowTrend)
                .build();
    }
}
