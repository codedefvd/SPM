package com.team.lms.librarian.controller;

import com.team.lms.common.api.ApiResponse;
import com.team.lms.common.api.BaseController;
import com.team.lms.librarian.dto.FineStatusUpdateRequest;
import com.team.lms.librarian.dto.ReservationProcessRequest;
import com.team.lms.librarian.dto.ReturnProcessRequest;
import com.team.lms.librarian.vo.BorrowingRecordManageVo;
import com.team.lms.librarian.service.LibrarianOperationsService;
import com.team.lms.librarian.vo.FineManageVo;
import com.team.lms.librarian.vo.LibrarianStatsDetailVo;
import com.team.lms.librarian.vo.LibrarianStatsVo;
import com.team.lms.librarian.vo.ReservationManageVo;
import com.team.lms.librarian.vo.ReturnReminderSummaryVo;
import com.team.lms.librarian.vo.ReturnReminderVo;
import com.team.lms.librarian.vo.ReturnManageVo;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/librarian")
public class LibrarianOperationsController extends BaseController {

    private final LibrarianOperationsService librarianOperationsService;

    @GetMapping("/borrow-records/current")
    public ApiResponse<List<BorrowingRecordManageVo>> listCurrentBorrowings(
            @RequestHeader("Authorization") String authorizationHeader
    ) {
        return success(librarianOperationsService.listCurrentBorrowings(authorizationHeader));
    }

    @GetMapping("/borrow-records/overdue")
    public ApiResponse<List<BorrowingRecordManageVo>> listOverdueBorrowings(
            @RequestHeader("Authorization") String authorizationHeader
    ) {
        return success(librarianOperationsService.listOverdueBorrowings(authorizationHeader));
    }

    @GetMapping("/borrow-records/reminders")
    public ApiResponse<List<ReturnReminderVo>> listReturnReminders(
            @RequestHeader("Authorization") String authorizationHeader
    ) {
        return success(librarianOperationsService.listReturnReminders(authorizationHeader));
    }

    @GetMapping("/borrow-records/reminders/summary")
    public ApiResponse<ReturnReminderSummaryVo> getReturnReminderSummary(
            @RequestHeader("Authorization") String authorizationHeader
    ) {
        return success(librarianOperationsService.getReturnReminderSummary(authorizationHeader));
    }

    @GetMapping("/borrow-records/by-barcode")
    public ApiResponse<ReturnManageVo> getActiveBorrowByBarcode(
            @RequestHeader("Authorization") String authorizationHeader,
            @RequestParam("barcode") String barcode
    ) {
        return success(librarianOperationsService.getActiveBorrowByBarcode(authorizationHeader, barcode));
    }

    @GetMapping("/return-requests")
    public ApiResponse<List<ReturnManageVo>> listPendingReturns(
            @RequestHeader("Authorization") String authorizationHeader
    ) {
        return success(librarianOperationsService.listPendingReturns(authorizationHeader));
    }

    @PostMapping("/return-requests/{recordId}/process")
    public ApiResponse<ReturnManageVo> processReturn(
            @RequestHeader("Authorization") String authorizationHeader,
            @PathVariable Long recordId,
            @Valid @RequestBody ReturnProcessRequest request
    ) {
        return success(librarianOperationsService.processReturn(authorizationHeader, recordId, request));
    }

    @GetMapping("/reservations")
    public ApiResponse<List<ReservationManageVo>> listReservations(
            @RequestHeader("Authorization") String authorizationHeader
    ) {
        return success(librarianOperationsService.listReservations(authorizationHeader));
    }

    @PostMapping("/reservations/{reservationId}/process")
    public ApiResponse<ReservationManageVo> processReservation(
            @RequestHeader("Authorization") String authorizationHeader,
            @PathVariable Long reservationId,
            @Valid @RequestBody ReservationProcessRequest request
    ) {
        return success(librarianOperationsService.processReservation(authorizationHeader, reservationId, request));
    }

    @GetMapping("/fines")
    public ApiResponse<List<FineManageVo>> listFines(
            @RequestHeader("Authorization") String authorizationHeader
    ) {
        return success(librarianOperationsService.listFines(authorizationHeader));
    }

    @PatchMapping("/fines/{fineId}/status")
    public ApiResponse<FineManageVo> updateFineStatus(
            @RequestHeader("Authorization") String authorizationHeader,
            @PathVariable Long fineId,
            @Valid @RequestBody FineStatusUpdateRequest request
    ) {
        return success(librarianOperationsService.updateFineStatus(authorizationHeader, fineId, request));
    }

    @GetMapping("/statistics")
    public ApiResponse<LibrarianStatsVo> getStatistics(
            @RequestHeader("Authorization") String authorizationHeader
    ) {
        return success(librarianOperationsService.getStatistics(authorizationHeader));
    }
    @GetMapping("/statistics/detailed")
    public ApiResponse<LibrarianStatsDetailVo> getDetailedStatistics(
            @RequestHeader("Authorization") String authorizationHeader,
            @RequestParam(value = "periodType", defaultValue = "month") String periodType) {
        return success(librarianOperationsService.getDetailedStatistics(authorizationHeader, periodType));
    }
}
