package com.team.lms.librarian.service;

import com.team.lms.librarian.dto.BookCopyLocationUpdateRequest;
import com.team.lms.librarian.dto.BookCreateRequest;
import com.team.lms.librarian.dto.BookUpdateRequest;
import com.team.lms.librarian.dto.InventoryUpdateRequest;
import com.team.lms.librarian.dto.ShelfStatusUpdateRequest;
import com.team.lms.librarian.vo.BookBarcodeVo;
import com.team.lms.librarian.vo.BookCopyVo;
import com.team.lms.librarian.vo.BookManageVo;
import com.team.lms.librarian.vo.IsbnLookupVo;

import java.util.List;

public interface LibrarianBookService {
    BookManageVo createBook(String authorizationHeader, BookCreateRequest request);
    BookManageVo updateBook(String authorizationHeader, Long bookId, BookUpdateRequest request);
    BookManageVo updateInventory(String authorizationHeader, Long bookId, InventoryUpdateRequest request);
    BookManageVo updateShelfStatus(String authorizationHeader, Long bookId, ShelfStatusUpdateRequest request);
    void deleteBook(String authorizationHeader, Long bookId);

    List<BookManageVo> listBooks(String authorizationHeader);

    IsbnLookupVo lookupBookByIsbn(String authorizationHeader, String isbn);

    BookBarcodeVo getBookBarcode(String authorizationHeader, Long bookId);

    List<BookCopyVo> listBookCopies(String authorizationHeader, Long bookId);

    BookCopyVo updateCopyLocation(String authorizationHeader, Long copyId, BookCopyLocationUpdateRequest request);
}
