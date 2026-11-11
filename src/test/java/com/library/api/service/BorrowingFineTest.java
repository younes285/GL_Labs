package com.library.api.service;

import com.library.api.repository.BookRepository;
import com.library.api.repository.BorrowingRepository;
import com.library.api.repository.MemberRepository;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

class BorrowingFineTest {
    private final BorrowingService service = new BorrowingService(mock(BookRepository.class),
            mock(MemberRepository.class), mock(BorrowingRepository.class));

    @ParameterizedTest(name = "Return {0} gives fine {1}")
    @CsvSource({
            "2026-11-08, 0.00",
            "2026-11-11, 0.00",
            "2026-11-12, 0.50",
            "2026-11-18, 3.50"
    })
    void calculateFine_coversEarlyExactAndOverdueReturns(String returned, String expected) {
        BigDecimal fine = service.calculateFine(LocalDate.of(2026, 11, 11), LocalDate.parse(returned));

        assertEquals(new BigDecimal(expected), fine);
    }
}
