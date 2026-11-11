package com.library.api.repository;

import com.library.api.entity.Borrowing;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BorrowingRepository extends JpaRepository<Borrowing, Long> {
    List<Borrowing> findByMemberIdAndReturnDateIsNullOrderByIdAsc(Long memberId);
}
