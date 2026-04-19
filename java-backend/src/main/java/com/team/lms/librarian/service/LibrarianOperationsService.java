package com.team.lms.librarian.service;

import com.team.lms.librarian.dto.FineStatusUpdateRequest;
import com.team.lms.librarian.dto.ReservationProcessRequest;
import com.team.lms.librarian.dto.ReturnProcessRequest;
import com.team.lms.librarian.vo.FineManageVo;
import com.team.lms.librarian.vo.LibrarianStatsDetailVo;
import com.team.lms.librarian.vo.LibrarianStatsVo;
import com.team.lms.librarian.vo.ReservationManageVo;
import com.team.lms.librarian.vo.ReturnManageVo;

import java.util.List;

public interface LibrarianOperationsService {
    List<ReturnManageVo> listPendingReturns();
    ReturnManageVo processReturn(String authorizationHeader, Long recordId, ReturnProcessRequest request);

    List<ReservationManageVo> listReservations();
    ReservationManageVo processReservation(String authorizationHeader, Long reservationId, ReservationProcessRequest request);

    List<FineManageVo> listFines();
    FineManageVo updateFineStatus(String authorizationHeader, Long fineId, FineStatusUpdateRequest request);

    LibrarianStatsVo getStatistics();
    LibrarianStatsDetailVo getDetailedStatistics(String periodType);
}
