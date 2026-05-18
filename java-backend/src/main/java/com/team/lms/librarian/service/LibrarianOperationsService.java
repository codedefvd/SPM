package com.team.lms.librarian.service;

import com.team.lms.librarian.dto.FineStatusUpdateRequest;
import com.team.lms.librarian.dto.ReservationProcessRequest;
import com.team.lms.librarian.dto.ReturnProcessRequest;
import com.team.lms.librarian.vo.BorrowingRecordManageVo;
import com.team.lms.librarian.vo.FineManageVo;
import com.team.lms.librarian.vo.LibrarianStatsDetailVo;
import com.team.lms.librarian.vo.LibrarianStatsVo;
import com.team.lms.librarian.vo.ReservationManageVo;
import com.team.lms.librarian.vo.ReturnReminderSummaryVo;
import com.team.lms.librarian.vo.ReturnReminderVo;
import com.team.lms.librarian.vo.ReturnManageVo;

import java.util.List;

public interface LibrarianOperationsService {
    List<BorrowingRecordManageVo> listCurrentBorrowings(String authorizationHeader);
    List<BorrowingRecordManageVo> listOverdueBorrowings(String authorizationHeader);
    List<ReturnReminderVo> listReturnReminders(String authorizationHeader);
    ReturnReminderSummaryVo getReturnReminderSummary(String authorizationHeader);
    List<ReturnManageVo> listPendingReturns(String authorizationHeader);
    ReturnManageVo getActiveBorrowByBarcode(String authorizationHeader, String barcode);
    ReturnManageVo processReturn(String authorizationHeader, Long recordId, ReturnProcessRequest request);

    List<ReservationManageVo> listReservations(String authorizationHeader);
    ReservationManageVo processReservation(String authorizationHeader, Long reservationId, ReservationProcessRequest request);

    List<FineManageVo> listFines(String authorizationHeader);
    FineManageVo updateFineStatus(String authorizationHeader, Long fineId, FineStatusUpdateRequest request);

    LibrarianStatsVo getStatistics(String authorizationHeader);
    LibrarianStatsDetailVo getDetailedStatistics(String authorizationHeader, String periodType);
}
