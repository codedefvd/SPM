package com.team.lms.reader.controller;

import com.team.lms.common.api.ApiResponse;
import com.team.lms.common.api.BaseController;
import com.team.lms.reader.dto.ReaderBorrowRequestCreateRequest;
import com.team.lms.reader.dto.ReaderBookReviewCreateRequest;
import com.team.lms.reader.dto.ReaderReviewReplyCreateRequest;
import com.team.lms.reader.service.ReaderBookService;
import com.team.lms.reader.vo.ReaderBookDetailVo;
import com.team.lms.reader.vo.ReaderBookVo;
import com.team.lms.reader.vo.ReaderBorrowRecordVo;
import com.team.lms.reader.vo.ReaderBookReviewVo;
import com.team.lms.reader.vo.ReaderReviewLikeVo;
import com.team.lms.reader.vo.ReaderReviewReplyVo;
import com.team.lms.reader.vo.ReaderBorrowRequestVo;
import com.team.lms.reader.vo.ReaderFavoriteToggleVo;
import com.team.lms.reader.vo.ReaderReservationVo;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/reader/books")
public class ReaderBookController extends BaseController {

    private final ReaderBookService readerBookService;

    @GetMapping
    public ApiResponse<List<ReaderBookVo>> listVisibleBooks(
            @RequestHeader("Authorization") String authorizationHeader,
            @RequestParam(value = "q", required = false) String keyword
    ) {
        return success(readerBookService.listVisibleBooks(authorizationHeader, keyword));
    }

    @GetMapping("/favorites")
    public ApiResponse<List<ReaderBookVo>> listFavoriteBooks(
            @RequestHeader("Authorization") String authorizationHeader
    ) {
        return success(readerBookService.listFavoriteBooks(authorizationHeader));
    }

    @GetMapping("/{bookId}")
    public ApiResponse<ReaderBookDetailVo> getBookDetail(
            @RequestHeader("Authorization") String authorizationHeader,
            @PathVariable Long bookId
    ) {
        return success(readerBookService.getBookDetail(authorizationHeader, bookId));
    }

    @PostMapping("/{bookId}/favorite")
    public ApiResponse<ReaderFavoriteToggleVo> toggleFavorite(
            @RequestHeader("Authorization") String authorizationHeader,
            @PathVariable Long bookId
    ) {
        return success(readerBookService.toggleFavorite(authorizationHeader, bookId));
    }

    @PostMapping("/borrow-requests")
    public ApiResponse<ReaderBorrowRequestVo> submitBorrowRequest(
            @RequestHeader("Authorization") String authorizationHeader,
            @Valid @RequestBody ReaderBorrowRequestCreateRequest request
    ) {
        return success(readerBookService.submitBorrowRequest(authorizationHeader, request));
    }

    @PostMapping("/{bookId}/reviews")
    public ApiResponse<ReaderBookReviewVo> submitReview(
            @RequestHeader("Authorization") String authorizationHeader,
            @PathVariable Long bookId,
            @Valid @RequestBody ReaderBookReviewCreateRequest request
    ) {
        return success(readerBookService.submitReview(authorizationHeader, bookId, request));
    }

    @PostMapping("/reviews/{reviewId}/like")
    public ApiResponse<ReaderReviewLikeVo> toggleReviewLike(
            @RequestHeader("Authorization") String authorizationHeader,
            @PathVariable Long reviewId
    ) {
        return success(readerBookService.toggleReviewLike(authorizationHeader, reviewId));
    }

    @PostMapping("/reviews/{reviewId}/replies")
    public ApiResponse<ReaderReviewReplyVo> submitReviewReply(
            @RequestHeader("Authorization") String authorizationHeader,
            @PathVariable Long reviewId,
            @Valid @RequestBody ReaderReviewReplyCreateRequest request
    ) {
        return success(readerBookService.submitReviewReply(authorizationHeader, reviewId, request));
    }

    @GetMapping("/records")
    public ApiResponse<List<ReaderBorrowRecordVo>> listBorrowRecords(
            @RequestHeader("Authorization") String authorizationHeader
    ) {
        return success(readerBookService.listBorrowRecords(authorizationHeader));
    }

    @PostMapping("/records/{recordId}/renew")
    public ApiResponse<ReaderBorrowRecordVo> renewBorrowRecord(
            @RequestHeader("Authorization") String authorizationHeader,
            @PathVariable Long recordId
    ) {
        return success(readerBookService.renewBorrowRecord(authorizationHeader, recordId));
    }

    @PostMapping("/records/{recordId}/return-request")
    public ApiResponse<ReaderBorrowRecordVo> submitReturnRequest(
            @RequestHeader("Authorization") String authorizationHeader,
            @PathVariable Long recordId
    ) {
        return success(readerBookService.submitReturnRequest(authorizationHeader, recordId));
    }

    @GetMapping("/reservations")
    public ApiResponse<List<ReaderReservationVo>> listReservations(
            @RequestHeader("Authorization") String authorizationHeader
    ) {
        return success(readerBookService.listReservations(authorizationHeader));
    }

    @PostMapping("/reservations")
    public ApiResponse<ReaderReservationVo> createReservation(
            @RequestHeader("Authorization") String authorizationHeader,
            @Valid @RequestBody ReservationCreateBody body
    ) {
        return success(readerBookService.createReservation(authorizationHeader, body.bookId));
    }

    public static class ReservationCreateBody {
        @NotNull(message = "bookId is required")
        public Long bookId;
    }
}
