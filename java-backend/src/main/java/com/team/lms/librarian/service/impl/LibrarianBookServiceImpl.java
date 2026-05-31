package com.team.lms.librarian.service.impl;

import com.team.lms.common.enums.RoleType;
import com.team.lms.common.enums.ShelfStatus;
import com.team.lms.common.support.PermissionScopeSupport;
import com.team.lms.entity.Book;
import com.team.lms.entity.BookCopy;
import com.team.lms.entity.Category;
import com.team.lms.entity.Inventory;
import com.team.lms.exception.BusinessException;
import com.team.lms.librarian.dto.BookCopyLocationUpdateRequest;
import com.team.lms.librarian.dto.BookCreateRequest;
import com.team.lms.librarian.dto.BookUpdateRequest;
import com.team.lms.librarian.dto.InventoryUpdateRequest;
import com.team.lms.librarian.dto.ShelfStatusUpdateRequest;
import com.team.lms.librarian.service.LocalIsbnRepository;
import com.team.lms.librarian.service.GoogleBooksIsbnClient;
import com.team.lms.librarian.service.OpenLibraryIsbnClient;
import com.team.lms.librarian.service.LibrarianBookService;
import com.team.lms.librarian.vo.BookBarcodeVo;
import com.team.lms.librarian.vo.BookCopyVo;
import com.team.lms.librarian.vo.BookManageVo;
import com.team.lms.librarian.vo.IsbnLookupVo;
import com.team.lms.mapper.BookMapper;
import com.team.lms.mapper.BookCopyMapper;
import com.team.lms.mapper.CategoryMapper;
import com.team.lms.mapper.InventoryMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.stream.IntStream;

@Service
@RequiredArgsConstructor
public class LibrarianBookServiceImpl implements LibrarianBookService {

    private final BookMapper bookMapper;
    private final BookCopyMapper bookCopyMapper;
    private final CategoryMapper categoryMapper;
    private final InventoryMapper inventoryMapper;
    private final PermissionScopeSupport permissionScopeSupport;
    private final LocalIsbnRepository localIsbnRepository;
    private final OpenLibraryIsbnClient openLibraryIsbnClient;
    private final GoogleBooksIsbnClient googleBooksIsbnClient;

    @Override
    public IsbnLookupVo lookupBookByIsbn(String authorizationHeader, String isbn) {
        permissionScopeSupport.requirePermission(authorizationHeader, RoleType.LIBRARIAN, "BOOK_MANAGE");
        if (isbn == null || isbn.trim().isEmpty()) {
            throw new BusinessException(400, "isbn is required");
        }
        String normalizedIsbn = isbn.trim();
        IsbnLookupVo local = localIsbnRepository.find(normalizedIsbn);
        if (local != null) {
            return local;
        }
        try {
            return openLibraryIsbnClient.lookup(normalizedIsbn);
        } catch (Exception openLibraryError) {
            try {
                return googleBooksIsbnClient.lookup(normalizedIsbn);
            } catch (BusinessException googleBooksError) {
                throw new BusinessException(404, "no book found for isbn: " + normalizedIsbn);
            }
        }
    }

    @Override
    @Transactional
    public BookManageVo createBook(String authorizationHeader, BookCreateRequest request) {
        permissionScopeSupport.requirePermission(authorizationHeader, RoleType.LIBRARIAN, "BOOK_MANAGE");
        Category category = requireCategory(request.getCategoryId());
        if (bookMapper.selectByIsbn(request.getIsbn().trim()) != null) {
            throw new BusinessException(400, "isbn already exists");
        }
        if (request.getAvailableCopies() > request.getTotalCopies()) {
            throw new BusinessException(400, "available copies cannot be greater than total copies");
        }

        Book book = new Book();
        book.setTitle(request.getTitle().trim());
        book.setAuthor(request.getAuthor().trim());
        book.setIsbn(request.getIsbn().trim());
        book.setBarcode(generateUniqueBarcode());
        book.setPublisher(normalizeOptional(request.getPublisher()));
        book.setDescription(normalizeOptional(request.getDescription()));
        book.setThumbnailUrl(normalizeOptional(request.getThumbnailUrl()));
        book.setPublishedDate(normalizeOptional(request.getPublishedDate()));
        book.setCategory(category);
        book.setShelfStatus(parseShelfStatus(request.getShelfStatus()));
        book.setStorageLocation(normalizeOptional(request.getStorageLocation()));
        bookMapper.insert(book);

        Inventory inventory = new Inventory();
        inventory.setBook(book);
        inventory.setTotalCopies(request.getTotalCopies());
        inventory.setAvailableCopies(request.getAvailableCopies());
        inventoryMapper.insert(inventory);
        syncBookCopies(book, request.getTotalCopies());

        return toManageVo(book, inventory);
    }

    @Override
    @Transactional
    public BookManageVo updateBook(String authorizationHeader, Long bookId, BookUpdateRequest request) {
        permissionScopeSupport.requirePermission(authorizationHeader, RoleType.LIBRARIAN, "BOOK_MANAGE");
        Book existingBook = requireBook(bookId);
        Category category = requireCategory(request.getCategoryId());

        String nextIsbn = request.getIsbn().trim();
        Book duplicated = bookMapper.selectByIsbn(nextIsbn);
        if (duplicated != null && !duplicated.getId().equals(bookId)) {
            throw new BusinessException(400, "isbn already exists");
        }

        existingBook.setTitle(request.getTitle().trim());
        existingBook.setAuthor(request.getAuthor().trim());
        existingBook.setIsbn(nextIsbn);
        if (existingBook.getBarcode() == null || existingBook.getBarcode().isBlank()) {
            existingBook.setBarcode(generateUniqueBarcode());
        }
        existingBook.setPublisher(normalizeOptional(request.getPublisher()));
        existingBook.setDescription(normalizeOptional(request.getDescription()));
        existingBook.setThumbnailUrl(normalizeOptional(request.getThumbnailUrl()));
        existingBook.setPublishedDate(normalizeOptional(request.getPublishedDate()));
        existingBook.setStorageLocation(normalizeOptional(request.getStorageLocation()));
        existingBook.setCategory(category);
        bookMapper.update(existingBook);

        return toManageVo(existingBook, inventoryMapper.selectByBookId(existingBook.getId()));
    }

    @Override
    @Transactional
    public BookManageVo updateInventory(String authorizationHeader, Long bookId, InventoryUpdateRequest request) {
        permissionScopeSupport.requirePermission(authorizationHeader, RoleType.LIBRARIAN, "INVENTORY_MANAGE");
        Book book = requireBook(bookId);
        if (request.getAvailableCopies() > request.getTotalCopies()) {
            throw new BusinessException(400, "available copies cannot be greater than total copies");
        }

        Inventory inventory = inventoryMapper.selectByBookId(bookId);
        if (inventory == null) {
            inventory = new Inventory();
            inventory.setBook(book);
            inventory.setTotalCopies(request.getTotalCopies());
            inventory.setAvailableCopies(request.getAvailableCopies());
            inventoryMapper.insert(inventory);
        } else {
            inventory.setTotalCopies(request.getTotalCopies());
            inventory.setAvailableCopies(request.getAvailableCopies());
            inventoryMapper.update(inventory);
        }
        syncBookCopies(book, request.getTotalCopies());

        return toManageVo(book, inventoryMapper.selectByBookId(bookId));
    }

    @Override
    @Transactional
    public BookManageVo updateShelfStatus(String authorizationHeader, Long bookId, ShelfStatusUpdateRequest request) {
        permissionScopeSupport.requirePermission(authorizationHeader, RoleType.LIBRARIAN, "INVENTORY_MANAGE");
        Book book = requireBook(bookId);
        book.setShelfStatus(parseShelfStatus(request.getShelfStatus()));
        bookMapper.update(book);
        return toManageVo(book, inventoryMapper.selectByBookId(bookId));
    }

    @Override
    @Transactional
    public void deleteBook(String authorizationHeader, Long bookId) {
        permissionScopeSupport.requirePermission(authorizationHeader, RoleType.LIBRARIAN, "BOOK_MANAGE");
        requireBook(bookId);
        bookMapper.softDeleteById(bookId);
        bookCopyMapper.softDeleteByBookId(bookId);

        Inventory inventory = inventoryMapper.selectByBookId(bookId);
        if (inventory != null) {
            inventory.setTotalCopies(0);
            inventory.setAvailableCopies(0);
            inventoryMapper.update(inventory);
        }
    }

    @Override
    public List<BookManageVo> listBooks(String authorizationHeader) {
        permissionScopeSupport.requireAnyPermission(
                authorizationHeader,
                RoleType.LIBRARIAN,
                List.of("BOOK_MANAGE", "INVENTORY_MANAGE")
        );
        return bookMapper.selectAll().stream()
                .map(book -> toManageVo(book, inventoryMapper.selectByBookId(book.getId())))
                .toList();
    }

    @Override
    public BookBarcodeVo getBookBarcode(String authorizationHeader, Long bookId) {
        permissionScopeSupport.requireAnyPermission(
                authorizationHeader,
                RoleType.LIBRARIAN,
                List.of("BOOK_MANAGE", "INVENTORY_MANAGE")
        );
        Book book = requireBook(bookId);
        List<BookCopy> copies = bookCopyMapper.selectActiveByBookId(bookId);
        return BookBarcodeVo.builder()
                .bookId(book.getId())
                .isbn(book.getIsbn())
                .copyCount(copies.size())
                .barcodes(copies.stream().map(BookCopy::getBarcode).toList())
                .build();
    }

    @Override
    public List<BookCopyVo> listBookCopies(String authorizationHeader, Long bookId) {
        permissionScopeSupport.requireAnyPermission(
                authorizationHeader,
                RoleType.LIBRARIAN,
                List.of("BOOK_MANAGE", "INVENTORY_MANAGE")
        );
        requireBook(bookId);
        return bookCopyMapper.selectActiveByBookId(bookId).stream()
                .map(copy -> BookCopyVo.builder()
                        .copyId(copy.getId())
                        .copyNo(copy.getCopyNo())
                        .barcode(copy.getBarcode())
                        .storageLocation(copy.getStorageLocation())
                        .build())
                .toList();
    }

    @Override
    @Transactional
    public BookCopyVo updateCopyLocation(String authorizationHeader, Long copyId, BookCopyLocationUpdateRequest request) {
        permissionScopeSupport.requirePermission(authorizationHeader, RoleType.LIBRARIAN, "INVENTORY_MANAGE");
        BookCopy copy = bookCopyMapper.selectById(copyId);
        if (copy == null) {
            throw new BusinessException(404, "book copy not found");
        }
        String location = normalizeOptional(request.getStorageLocation());
        if (location == null) {
            throw new BusinessException(400, "storageLocation is required");
        }
        bookCopyMapper.updateStorageLocation(copyId, location);
        copy.setStorageLocation(location);
        return BookCopyVo.builder()
                .copyId(copy.getId())
                .copyNo(copy.getCopyNo())
                .barcode(copy.getBarcode())
                .storageLocation(copy.getStorageLocation())
                .build();
    }

    private Book requireBook(Long bookId) {
        Book book = bookMapper.selectById(bookId);
        if (book == null) {
            throw new BusinessException(404, "book not found");
        }
        return book;
    }

    private Category requireCategory(Long categoryId) {
        Category category = categoryMapper.selectById(categoryId);
        if (category == null) {
            throw new BusinessException(404, "category not found");
        }
        if (!Boolean.TRUE.equals(category.getEnabled())) {
            throw new BusinessException(400, "category is disabled");
        }
        return category;
    }

    private ShelfStatus parseShelfStatus(String rawStatus) {
        try {
            return ShelfStatus.valueOf(rawStatus.trim().toUpperCase());
        } catch (Exception ignored) {
            throw new BusinessException(400, "invalid shelf status");
        }
    }

    private String normalizeOptional(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }

    private void syncBookCopies(Book book, int targetTotalCopies) {
        List<BookCopy> activeCopies = bookCopyMapper.selectActiveByBookId(book.getId());
        if (activeCopies.size() < targetTotalCopies) {
            int nextCopyNo = bookCopyMapper.selectMaxCopyNoByBookId(book.getId()) == null
                    ? 1
                    : bookCopyMapper.selectMaxCopyNoByBookId(book.getId()) + 1;
            IntStream.range(0, targetTotalCopies - activeCopies.size()).forEach(offset -> {
                BookCopy copy = new BookCopy();
                int copyNo = nextCopyNo + offset;
                copy.setBook(book);
                copy.setCopyNo(copyNo);
                copy.setBarcode(generateCopyBarcode(book.getId(), copyNo));
                copy.setStorageLocation(book.getStorageLocation());
                bookCopyMapper.insert(copy);
            });
            return;
        }

        if (activeCopies.size() > targetTotalCopies) {
            List<Long> idsToDelete = activeCopies.stream()
                    .sorted((left, right) -> Integer.compare(right.getCopyNo(), left.getCopyNo()))
                    .limit(activeCopies.size() - targetTotalCopies)
                    .map(BookCopy::getId)
                    .toList();
            if (!idsToDelete.isEmpty()) {
                bookCopyMapper.softDeleteByIds(idsToDelete);
            }
        }
    }

    private String generateCopyBarcode(Long bookId, int copyNo) {
        String candidate = String.format(Locale.ROOT, "LIB-%06d-%03d", bookId, copyNo);
        if (bookCopyMapper.selectByBarcode(candidate) != null) {
            throw new BusinessException(500, "generated duplicate copy barcode");
        }
        return candidate;
    }

    private String generateUniqueBarcode() {
        String candidate;
        do {
            candidate = "BOOK-" + UUID.randomUUID().toString().replace("-", "")
                    .substring(0, 12)
                    .toUpperCase(Locale.ROOT);
        } while (bookMapper.selectByBarcode(candidate) != null);
        return candidate;
    }

    private BookManageVo toManageVo(Book book, Inventory inventory) {
        return BookManageVo.builder()
                .bookId(book.getId())
                .title(book.getTitle())
                .author(book.getAuthor())
                .isbn(book.getIsbn())
                .barcode(book.getBarcode())
                .categoryId(book.getCategory() == null ? null : book.getCategory().getId())
                .categoryName(book.getCategory() == null ? null : book.getCategory().getName())
                .publisher(book.getPublisher())
                .description(book.getDescription())
                .thumbnailUrl(book.getThumbnailUrl())
                .publishedDate(book.getPublishedDate())
                .totalCopies(inventory == null ? 0 : inventory.getTotalCopies())
                .availableCopies(inventory == null ? 0 : inventory.getAvailableCopies())
                .shelfStatus(book.getShelfStatus() == null ? null : book.getShelfStatus().name())
                .storageLocation(book.getStorageLocation())
                .build();
    }
}
