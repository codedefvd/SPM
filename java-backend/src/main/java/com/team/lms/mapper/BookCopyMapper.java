package com.team.lms.mapper;

import com.team.lms.entity.BookCopy;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface BookCopyMapper {
    List<BookCopy> selectByBookId(Long bookId);

    List<BookCopy> selectActiveByBookId(Long bookId);

    BookCopy selectByBarcode(String barcode);

    Integer selectMaxCopyNoByBookId(Long bookId);

    void insert(BookCopy bookCopy);

    void softDeleteByIds(@Param("ids") List<Long> ids);

    void softDeleteByBookId(Long bookId);

    BookCopy selectById(Long id);

    void updateStorageLocation(@Param("id") Long id, @Param("storageLocation") String storageLocation);
}
