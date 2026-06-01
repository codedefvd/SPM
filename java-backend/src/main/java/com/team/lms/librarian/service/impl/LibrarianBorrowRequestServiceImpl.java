package com.team.lms.librarian.service.impl;

import com.team.lms.common.enums.BorrowRecordStatus;
import com.team.lms.common.enums.BorrowRequestStatus;
import com.team.lms.common.enums.RoleType;
import com.team.lms.common.support.CurrentUserSupport;
import com.team.lms.common.support.PermissionScopeSupport;
import com.team.lms.common.support.SystemConfigSupport;
import com.team.lms.entity.BookCopy;
import com.team.lms.entity.BorrowRecord;
import com.team.lms.entity.BorrowRequest;
import com.team.lms.entity.Inventory;
import com.team.lms.entity.User;
import com.team.lms.exception.BusinessException;
import com.team.lms.librarian.dto.BorrowRequestProcessRequest;
import com.team.lms.librarian.service.LibrarianBorrowRequestService;
import com.team.lms.librarian.vo.BorrowRequestManageVo;
import com.team.lms.mapper.BookCopyMapper;
import com.team.lms.mapper.BorrowRecordMapper;
import com.team.lms.mapper.BorrowRequestMapper;
import com.team.lms.mapper.InventoryMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class LibrarianBorrowRequestServiceImpl implements LibrarianBorrowRequestService {

    private final BorrowRequestMapper borrowRequestMapper;
    private final BorrowRecordMapper borrowRecordMapper;
    private final BookCopyMapper bookCopyMapper;
    private final InventoryMapper inventoryMapper;
    private final CurrentUserSupport currentUserSupport;
    private final PermissionScopeSupport permissionScopeSupport;
    private final SystemConfigSupport systemConfigSupport;

    @Override
    public List<BorrowRequestManageVo> listRequests(String authorizationHeader, String statusFilter) {
        permissionScopeSupport.requirePermission(authorizationHeader, RoleType.LIBRARIAN, "REQUEST_PROCESS");
        List<BorrowRequest> requests;
        if (statusFilter == null || statusFilter.isBlank()) {
            requests = borrowRequestMapper.selectPendingRequests();
        } else {
            String normalizedStatus = statusFilter.trim().toUpperCase();
            try {
                BorrowRequestStatus.valueOf(normalizedStatus);
            } catch (IllegalArgumentException exception) {
                throw new BusinessException(400, "invalid borrow request status");
            }
            requests = borrowRequestMapper.selectByStatus(normalizedStatus);
        }
        return requests.stream().map(this::toManageVo).toList();
    }

    @Override
    @Transactional
    public BorrowRequestManageVo processRequest(String authorizationHeader, Long requestId, BorrowRequestProcessRequest request) {
        permissionScopeSupport.requirePermission(authorizationHeader, RoleType.LIBRARIAN, "REQUEST_PROCESS");
        User librarian = currentUserSupport.requireUser(authorizationHeader);

        BorrowRequest borrowRequest = borrowRequestMapper.selectById(requestId);
        if (borrowRequest == null) {
            throw new BusinessException(404, "borrow request not found");
        }
        if (borrowRequest.getStatus() != BorrowRequestStatus.PENDING) {
            throw new BusinessException(400, "borrow request has already been processed");
        }

        Inventory inventory = inventoryMapper.selectByBookId(borrowRequest.getBook().getId());
        if (inventory == null) {
            throw new BusinessException(500, "inventory record not found");
        }

        borrowRequest.setProcessedBy(librarian);
        borrowRequest.setProcessedAt(LocalDateTime.now());

        boolean approveAction = request.isApprove();
        String message;
        if (approveAction) {
            if (inventory.getAvailableCopies() <= 0) {
                throw new BusinessException(400, "no available copies left");
            }
            BookCopy selectedCopy = resolveAvailableCopy(
                    borrowRequest.getBook().getId(),
                    request.getCopyBarcode()
            );
            inventory.setAvailableCopies(inventory.getAvailableCopies() - 1);
            inventoryMapper.update(inventory);

            BorrowRecord borrowRecord = new BorrowRecord();
            borrowRecord.setReader(borrowRequest.getReader());
            borrowRecord.setBook(borrowRequest.getBook());
            borrowRecord.setBorrowRequest(borrowRequest);
            borrowRecord.setBookCopy(selectedCopy);
            borrowRecord.setStatus(BorrowRecordStatus.BORROWED);
            borrowRecord.setBorrowDate(LocalDate.now());
            borrowRecord.setDueDate(LocalDate.now().plusDays(systemConfigSupport.getBorrowPeriodDays()));
            borrowRecord.setRenewalCount(0);
            borrowRecordMapper.insert(borrowRecord);

            borrowRequest.setStatus(BorrowRequestStatus.APPROVED);
            borrowRequest.setRejectReason(null);
            message = "borrow request approved and inventory updated";
        } else {
            borrowRequest.setStatus(BorrowRequestStatus.REJECTED);
            String rejectReason = request.effectiveRejectReason();
            borrowRequest.setRejectReason(rejectReason == null || rejectReason.isBlank()
                    ? "rejected by librarian"
                    : rejectReason.trim());
            message = borrowRequest.getRejectReason();
        }

        borrowRequestMapper.update(borrowRequest);
        Inventory latestInventory = inventoryMapper.selectByBookId(borrowRequest.getBook().getId());

        return BorrowRequestManageVo.builder()
                .requestId(borrowRequest.getId())
                .bookId(borrowRequest.getBook().getId())
                .bookTitle(borrowRequest.getBook().getTitle())
                .copyBarcode(findCopyBarcodeByRequestId(borrowRequest.getId()))
                .readerId(borrowRequest.getReader().getId())
                .readerUsername(borrowRequest.getReader().getUsername())
                .status(borrowRequest.getStatus().name())
                .requestedAt(borrowRequest.getCreatedAt() == null ? null : borrowRequest.getCreatedAt().toString())
                .remainingCopies(latestInventory == null ? 0 : latestInventory.getAvailableCopies())
                .message(message)
                .build();
    }

    private BorrowRequestManageVo toManageVo(BorrowRequest request) {
        Inventory inventory = inventoryMapper.selectByBookId(request.getBook().getId());
        return BorrowRequestManageVo.builder()
                .requestId(request.getId())
                .bookId(request.getBook().getId())
                .bookTitle(request.getBook().getTitle())
                .copyBarcode(findCopyBarcodeByRequestId(request.getId()))
                .readerId(request.getReader().getId())
                .readerUsername(request.getReader().getUsername())
                .status(request.getStatus().name())
                .requestedAt(request.getCreatedAt() == null ? null : request.getCreatedAt().toString())
                .remainingCopies(inventory == null ? 0 : inventory.getAvailableCopies())
                .message(request.getRejectReason())
                .build();
    }

    private BookCopy resolveAvailableCopy(Long bookId, String requestedBarcode) {
        List<BookCopy> activeCopies = bookCopyMapper.selectActiveByBookId(bookId);
        Set<Long> occupiedCopyIds = borrowRecordMapper.selectAll().stream()
                .filter(record -> record.getBookCopy() != null && record.getBookCopy().getId() != null)
                .filter(record -> record.getStatus() == BorrowRecordStatus.BORROWED
                        || record.getStatus() == BorrowRecordStatus.RETURN_PENDING
                        || record.getStatus() == BorrowRecordStatus.OVERDUE)
                .map(record -> record.getBookCopy().getId())
                .collect(Collectors.toSet());

        List<BookCopy> availableCopies = activeCopies.stream()
                .filter(copy -> !occupiedCopyIds.contains(copy.getId()))
                .toList();

        if (availableCopies.isEmpty()) {
            throw new BusinessException(400, "no available book copy barcode left");
        }

        if (requestedBarcode == null || requestedBarcode.isBlank()) {
            return availableCopies.get(0);
        }

        String normalized = requestedBarcode.trim();
        return availableCopies.stream()
                .filter(copy -> normalized.equalsIgnoreCase(copy.getBarcode()))
                .findFirst()
                .orElseThrow(() -> new BusinessException(400, "specified copy barcode is unavailable"));
    }

    private String findCopyBarcodeByRequestId(Long requestId) {
        BorrowRecord record = borrowRecordMapper.selectByBorrowRequestId(requestId);
        return record == null || record.getBookCopy() == null ? null : record.getBookCopy().getBarcode();
    }
}
