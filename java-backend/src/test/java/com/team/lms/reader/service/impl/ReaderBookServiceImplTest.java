package com.team.lms.reader.service.impl;

import com.team.lms.common.enums.BorrowRecordStatus;
import com.team.lms.common.enums.FineStatus;
import com.team.lms.common.enums.RoleType;
import com.team.lms.common.support.CurrentUserSupport;
import com.team.lms.common.support.PermissionScopeSupport;
import com.team.lms.common.support.SystemConfigSupport;
import com.team.lms.entity.Book;
import com.team.lms.entity.BorrowRecord;
import com.team.lms.entity.Fine;
import com.team.lms.entity.User;
import com.team.lms.mapper.BookFavoriteMapper;
import com.team.lms.mapper.BookMapper;
import com.team.lms.mapper.BookReviewMapper;
import com.team.lms.mapper.BorrowRecordMapper;
import com.team.lms.mapper.BorrowRequestMapper;
import com.team.lms.mapper.FineMapper;
import com.team.lms.mapper.InventoryMapper;
import com.team.lms.mapper.ReservationMapper;
import com.team.lms.payment.AlipaySandboxPaymentService;
import com.team.lms.reader.vo.ReaderBorrowRecordVo;
import com.team.lms.reader.vo.ReaderFinePaymentOrderVo;
import com.team.lms.reader.vo.ReaderFineVo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReaderBookServiceImplTest {

    private static final String AUTHORIZATION = "Bearer mock-token-for-reader";

    @Mock
    private BookMapper bookMapper;
    @Mock
    private BookFavoriteMapper bookFavoriteMapper;
    @Mock
    private BookReviewMapper bookReviewMapper;
    @Mock
    private InventoryMapper inventoryMapper;
    @Mock
    private BorrowRequestMapper borrowRequestMapper;
    @Mock
    private BorrowRecordMapper borrowRecordMapper;
    @Mock
    private ReservationMapper reservationMapper;
    @Mock
    private FineMapper fineMapper;
    @Mock
    private CurrentUserSupport currentUserSupport;
    @Mock
    private PermissionScopeSupport permissionScopeSupport;
    @Mock
    private SystemConfigSupport systemConfigSupport;
    @Mock
    private AlipaySandboxPaymentService alipaySandboxPaymentService;

    private ReaderBookServiceImpl service;
    private User reader;

    @BeforeEach
    void setUp() {
        service = new ReaderBookServiceImpl(
                bookMapper,
                bookFavoriteMapper,
                bookReviewMapper,
                inventoryMapper,
                borrowRequestMapper,
                borrowRecordMapper,
                reservationMapper,
                fineMapper,
                currentUserSupport,
                permissionScopeSupport,
                systemConfigSupport,
                alipaySandboxPaymentService
        );
        reader = reader(1L, "reader");
    }

    @Test
    void listBorrowRecordsCreatesUnpaidFineForOverdueBorrow() {
        BorrowRecord overdueRecord = borrowRecord(10L, reader, "Clean Architecture", BorrowRecordStatus.BORROWED, 3);
        when(currentUserSupport.requireUser(AUTHORIZATION)).thenReturn(reader);
        when(borrowRecordMapper.selectByReaderId(reader.getId())).thenReturn(List.of(overdueRecord));
        when(fineMapper.selectByBorrowRecordId(overdueRecord.getId())).thenReturn(null);
        when(systemConfigSupport.getOverdueFinePerDay()).thenReturn(new BigDecimal("2.00"));

        List<ReaderBorrowRecordVo> records = service.listBorrowRecords(AUTHORIZATION);

        assertThat(records).hasSize(1);
        assertThat(records.get(0).getStatus()).isEqualTo("OVERDUE");
        assertThat(records.get(0).getOverdueDays()).isEqualTo(3);
        assertThat(records.get(0).getFineAmount()).isEqualByComparingTo("6.00");

        ArgumentCaptor<Fine> fineCaptor = ArgumentCaptor.forClass(Fine.class);
        verify(fineMapper).insert(fineCaptor.capture());
        Fine createdFine = fineCaptor.getValue();
        assertThat(createdFine.getReader().getId()).isEqualTo(reader.getId());
        assertThat(createdFine.getBorrowRecord().getId()).isEqualTo(overdueRecord.getId());
        assertThat(createdFine.getAmount()).isEqualByComparingTo("6.00");
        assertThat(createdFine.getStatus()).isEqualTo(FineStatus.UNPAID);
    }

    @Test
    void listFinesReturnsOnlyCurrentReadersFines() {
        User otherReader = reader(2L, "other");
        BorrowRecord ownRecord = borrowRecord(11L, reader, "Clean Architecture", BorrowRecordStatus.BORROWED, 4);
        BorrowRecord otherRecord = borrowRecord(12L, otherReader, "Other Book", BorrowRecordStatus.BORROWED, 5);
        Fine ownFine = fine(101L, reader, ownRecord, "4.00", FineStatus.UNPAID);
        Fine otherFine = fine(102L, otherReader, otherRecord, "5.00", FineStatus.UNPAID);

        when(currentUserSupport.requireUser(AUTHORIZATION)).thenReturn(reader);
        when(borrowRecordMapper.selectByReaderId(reader.getId())).thenReturn(List.of(ownRecord));
        when(fineMapper.selectByBorrowRecordId(ownRecord.getId())).thenReturn(ownFine);
        when(systemConfigSupport.getOverdueFinePerDay()).thenReturn(new BigDecimal("1.00"));
        when(fineMapper.selectAll()).thenReturn(List.of(otherFine, ownFine));

        List<ReaderFineVo> fines = service.listFines(AUTHORIZATION);

        assertThat(fines).hasSize(1);
        ReaderFineVo fine = fines.get(0);
        assertThat(fine.getFineId()).isEqualTo(101L);
        assertThat(fine.getRecordId()).isEqualTo(11L);
        assertThat(fine.getBookTitle()).isEqualTo("Clean Architecture");
        assertThat(fine.getAmount()).isEqualByComparingTo("4.00");
        assertThat(fine.getStatus()).isEqualTo("UNPAID");
        assertThat(fine.getOverdueDays()).isEqualTo(4);
    }

    @Test
    void submitReturnRequestCreatesUnpaidFineForOverdueBorrow() {
        BorrowRecord overdueRecord = borrowRecord(13L, reader, "Refactoring", BorrowRecordStatus.BORROWED, 2);
        when(currentUserSupport.requireUser(AUTHORIZATION)).thenReturn(reader);
        when(borrowRecordMapper.selectById(overdueRecord.getId())).thenReturn(overdueRecord);
        when(fineMapper.selectByBorrowRecordId(overdueRecord.getId())).thenReturn(null);
        when(systemConfigSupport.getOverdueFinePerDay()).thenReturn(new BigDecimal("1.50"));

        ReaderBorrowRecordVo result = service.submitReturnRequest(AUTHORIZATION, overdueRecord.getId());

        assertThat(result.getStatus()).isEqualTo("RETURN_PENDING");
        assertThat(result.getFineAmount()).isEqualByComparingTo("3.00");

        ArgumentCaptor<Fine> fineCaptor = ArgumentCaptor.forClass(Fine.class);
        verify(fineMapper).insert(fineCaptor.capture());
        assertThat(fineCaptor.getValue().getAmount()).isEqualByComparingTo("3.00");
        assertThat(fineCaptor.getValue().getStatus()).isEqualTo(FineStatus.UNPAID);
    }

    @Test
    void confirmFinePaymentMarksOwnUnpaidFineAsPaid() {
        BorrowRecord record = borrowRecord(14L, reader, "Domain-Driven Design", BorrowRecordStatus.BORROWED, 6);
        Fine unpaidFine = fine(103L, reader, record, "6.00", FineStatus.UNPAID);
        Fine paidFine = fine(103L, reader, record, "6.00", FineStatus.PAID);

        when(currentUserSupport.requireUser(AUTHORIZATION)).thenReturn(reader);
        when(fineMapper.selectById(unpaidFine.getId())).thenReturn(unpaidFine, paidFine);

        ReaderFineVo result = service.confirmFinePayment(AUTHORIZATION, unpaidFine.getId());

        assertThat(result.getFineId()).isEqualTo(103L);
        assertThat(result.getStatus()).isEqualTo("PAID");

        ArgumentCaptor<Fine> fineCaptor = ArgumentCaptor.forClass(Fine.class);
        verify(fineMapper).update(fineCaptor.capture());
        assertThat(fineCaptor.getValue().getStatus()).isEqualTo(FineStatus.PAID);
    }

    @Test
    void createFinePaymentOrderUsesOwnUnpaidFine() {
        BorrowRecord record = borrowRecord(15L, reader, "Patterns of Enterprise Application Architecture", BorrowRecordStatus.BORROWED, 8);
        Fine unpaidFine = fine(104L, reader, record, "8.00", FineStatus.UNPAID);
        ReaderFinePaymentOrderVo order = ReaderFinePaymentOrderVo.builder()
                .fineId(104L)
                .outTradeNo("FINE_104_20260529233000")
                .amount(new BigDecimal("8.00"))
                .payUrl("https://openapi-sandbox.dl.alipaydev.com/gateway.do?mock=1")
                .qrCode("https://qr.alipay.com/fake-fine-order")
                .build();

        when(currentUserSupport.requireUser(AUTHORIZATION)).thenReturn(reader);
        when(fineMapper.selectById(unpaidFine.getId())).thenReturn(unpaidFine);
        when(alipaySandboxPaymentService.createFinePrecreate(unpaidFine)).thenReturn(order);

        ReaderFinePaymentOrderVo result = service.createFinePaymentOrder(AUTHORIZATION, unpaidFine.getId());

        assertThat(result.getFineId()).isEqualTo(104L);
        assertThat(result.getPayUrl()).contains("openapi-sandbox");
        assertThat(result.getQrCode()).isEqualTo("https://qr.alipay.com/fake-fine-order");
        verify(alipaySandboxPaymentService).createFinePrecreate(unpaidFine);
    }

    private User reader(Long id, String username) {
        User user = new User();
        user.setId(id);
        user.setUsername(username);
        user.setRole(RoleType.READER);
        return user;
    }

    private BorrowRecord borrowRecord(Long id, User reader, String bookTitle, BorrowRecordStatus status, int overdueDays) {
        Book book = new Book();
        book.setId(100L + id);
        book.setTitle(bookTitle);

        BorrowRecord record = new BorrowRecord();
        record.setId(id);
        record.setReader(reader);
        record.setBook(book);
        record.setStatus(status);
        record.setBorrowDate(LocalDate.now().minusDays(30 + overdueDays));
        record.setDueDate(LocalDate.now().minusDays(overdueDays));
        return record;
    }

    private Fine fine(Long id, User reader, BorrowRecord record, String amount, FineStatus status) {
        Fine fine = new Fine();
        fine.setId(id);
        fine.setReader(reader);
        fine.setBorrowRecord(record);
        fine.setAmount(new BigDecimal(amount));
        fine.setStatus(status);
        return fine;
    }
}
