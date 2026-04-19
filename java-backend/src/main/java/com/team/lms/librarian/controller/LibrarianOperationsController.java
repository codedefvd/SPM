package com.team.lms.librarian.controller;

import com.team.lms.common.api.ApiResponse;
import com.team.lms.common.api.BaseController;
import com.team.lms.librarian.dto.FineStatusUpdateRequest;
import com.team.lms.librarian.dto.ReservationProcessRequest;
import com.team.lms.librarian.dto.ReturnProcessRequest;
import com.team.lms.librarian.service.LibrarianOperationsService;
import com.team.lms.librarian.vo.FineManageVo;
import com.team.lms.librarian.vo.LibrarianStatsDetailVo;
import com.team.lms.librarian.vo.LibrarianStatsVo;
import com.team.lms.librarian.vo.ReservationManageVo;
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

    @GetMapping("/return-requests")
    public ApiResponse<List<ReturnManageVo>> listPendingReturns() {
        return success(librarianOperationsService.listPendingReturns());
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
    public ApiResponse<List<ReservationManageVo>> listReservations() {
        return success(librarianOperationsService.listReservations());
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
    public ApiResponse<List<FineManageVo>> listFines() {
        return success(librarianOperationsService.listFines());
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
    public ApiResponse<LibrarianStatsVo> getStatistics() {
        return success(librarianOperationsService.getStatistics());
    }
    @GetMapping("/statistics/detailed")
    public ApiResponse<LibrarianStatsDetailVo> getDetailedStatistics(
            @RequestParam(value = "periodType", defaultValue = "month") String periodType) {
        return success(librarianOperationsService.getDetailedStatistics(periodType));
    }
}
