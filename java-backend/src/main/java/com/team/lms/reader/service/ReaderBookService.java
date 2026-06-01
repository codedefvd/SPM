package com.team.lms.reader.service;

import com.team.lms.reader.dto.ReaderBorrowRequestCreateRequest;
import com.team.lms.reader.dto.ReaderBookReviewCreateRequest;
import com.team.lms.reader.dto.ReaderReviewReplyCreateRequest;
import com.team.lms.reader.vo.ReaderBookDetailVo;
import com.team.lms.reader.vo.ReaderBookVo;
import com.team.lms.reader.vo.ReaderBorrowRecordVo;
import com.team.lms.reader.vo.ReaderBorrowRequestVo;
import com.team.lms.reader.vo.ReaderBookReviewVo;
import com.team.lms.reader.vo.ReaderReviewLikeVo;
import com.team.lms.reader.vo.ReaderReviewReplyVo;
import com.team.lms.reader.vo.ReaderFavoriteToggleVo;
import com.team.lms.reader.vo.ReaderFinePaymentOrderVo;
import com.team.lms.reader.vo.ReaderFineVo;
import com.team.lms.reader.vo.ReaderReservationVo;

import java.util.List;

public interface ReaderBookService {
    List<ReaderBookVo> listVisibleBooks(String authorizationHeader, String keyword);

    List<ReaderBookVo> listFavoriteBooks(String authorizationHeader);

    ReaderBookDetailVo getBookDetail(String authorizationHeader, Long bookId);

    ReaderFavoriteToggleVo toggleFavorite(String authorizationHeader, Long bookId);

    ReaderBorrowRequestVo submitBorrowRequest(String authorizationHeader, ReaderBorrowRequestCreateRequest request);

    ReaderBookReviewVo submitReview(String authorizationHeader, Long bookId, ReaderBookReviewCreateRequest request);

    ReaderReviewLikeVo toggleReviewLike(String authorizationHeader, Long reviewId);

    ReaderReviewReplyVo submitReviewReply(String authorizationHeader, Long reviewId, ReaderReviewReplyCreateRequest request);

    List<ReaderBorrowRecordVo> listBorrowRecords(String authorizationHeader);

    List<ReaderFineVo> listFines(String authorizationHeader);

    ReaderFinePaymentOrderVo createFinePaymentOrder(String authorizationHeader, Long fineId);

    ReaderFineVo confirmFinePayment(String authorizationHeader, Long fineId);

    ReaderBorrowRecordVo submitReturnRequest(String authorizationHeader, Long recordId);

    List<ReaderReservationVo> listReservations(String authorizationHeader);

    ReaderReservationVo createReservation(String authorizationHeader, Long bookId);
}
