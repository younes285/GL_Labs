package com.library.api.mapper;

import com.library.api.dto.BorrowingDTO;
import com.library.api.entity.Borrowing;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class BorrowingMapper {
    public BorrowingDTO toDTO(Borrowing borrowing) {
        return new BorrowingDTO(borrowing.getId(), borrowing.getBook().getId(), borrowing.getMember().getId(),
                borrowing.getBorrowDate(), borrowing.getDueDate(), borrowing.getReturnDate(), borrowing.getFineAmount());
    }

    public List<BorrowingDTO> toDTOs(List<Borrowing> borrowings) {
        return borrowings.stream().map(this::toDTO).toList();
    }
}
