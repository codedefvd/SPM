package com.team.lms.librarian.controller;

import com.team.lms.common.api.ApiResponse;
import com.team.lms.common.api.BaseController;
import com.team.lms.librarian.dto.BookCopyLocationUpdateRequest;
import com.team.lms.librarian.dto.BookCreateRequest;
import com.team.lms.librarian.dto.BookUpdateRequest;
import com.team.lms.librarian.dto.InventoryUpdateRequest;
import com.team.lms.librarian.dto.ShelfStatusUpdateRequest;
import com.team.lms.librarian.service.LibrarianBookService;
import com.team.lms.librarian.vo.BookBarcodeVo;
import com.team.lms.librarian.vo.BookCopyVo;
import com.team.lms.librarian.vo.BookManageVo;
import com.team.lms.librarian.vo.IsbnLookupVo;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/librarian/books")
public class LibrarianBookController extends BaseController {

    private final LibrarianBookService librarianBookService;

    @GetMapping("/isbn-lookup")
    public ApiResponse<IsbnLookupVo> lookupBookByIsbn(
            @RequestHeader("Authorization") String authorizationHeader,
            @RequestParam("isbn") String isbn
    ) {
        return success(librarianBookService.lookupBookByIsbn(authorizationHeader, isbn));
    }

    @GetMapping
    public ApiResponse<List<BookManageVo>> listBooks(@RequestHeader("Authorization") String authorizationHeader) {
        return success(librarianBookService.listBooks(authorizationHeader));
    }

    @GetMapping("/{bookId}/barcode")
    public ApiResponse<BookBarcodeVo> getBookBarcode(
            @RequestHeader("Authorization") String authorizationHeader,
            @PathVariable Long bookId
    ) {
        return success(librarianBookService.getBookBarcode(authorizationHeader, bookId));
    }

    @GetMapping("/{bookId}/copies")
    public ApiResponse<List<BookCopyVo>> listBookCopies(
            @RequestHeader("Authorization") String authorizationHeader,
            @PathVariable Long bookId
    ) {
        return success(librarianBookService.listBookCopies(authorizationHeader, bookId));
    }

    @PostMapping
    public ApiResponse<BookManageVo> createBook(
            @RequestHeader("Authorization") String authorizationHeader,
            @Valid @RequestBody BookCreateRequest request
    ) {
        return success(librarianBookService.createBook(authorizationHeader, request));
    }

    @PutMapping("/{bookId}")
    public ApiResponse<BookManageVo> updateBook(
            @RequestHeader("Authorization") String authorizationHeader,
            @PathVariable Long bookId,
            @Valid @RequestBody BookUpdateRequest request
    ) {
        return success(librarianBookService.updateBook(authorizationHeader, bookId, request));
    }

    @PatchMapping("/{bookId}/inventory")
    public ApiResponse<BookManageVo> updateInventory(
            @RequestHeader("Authorization") String authorizationHeader,
            @PathVariable Long bookId,
            @Valid @RequestBody InventoryUpdateRequest request
    ) {
        return success(librarianBookService.updateInventory(authorizationHeader, bookId, request));
    }

    @PatchMapping("/{bookId}/shelf-status")
    public ApiResponse<BookManageVo> updateShelfStatus(
            @RequestHeader("Authorization") String authorizationHeader,
            @PathVariable Long bookId,
            @Valid @RequestBody ShelfStatusUpdateRequest request
    ) {
        return success(librarianBookService.updateShelfStatus(authorizationHeader, bookId, request));
    }

    @PatchMapping("/copies/{copyId}/location")
    public ApiResponse<BookCopyVo> updateCopyLocation(
            @RequestHeader("Authorization") String authorizationHeader,
            @PathVariable Long copyId,
            @Valid @RequestBody BookCopyLocationUpdateRequest request
    ) {
        return success(librarianBookService.updateCopyLocation(authorizationHeader, copyId, request));
    }

    @DeleteMapping("/{bookId}")
    public ApiResponse<Void> deleteBook(
            @RequestHeader("Authorization") String authorizationHeader,
            @PathVariable Long bookId
    ) {
        librarianBookService.deleteBook(authorizationHeader, bookId);
        return success(null);
    }
}
