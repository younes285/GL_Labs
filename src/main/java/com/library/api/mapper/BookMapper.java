package com.library.api.mapper;

import com.library.api.dto.BookDTO;
import com.library.api.entity.Book;
import org.springframework.stereotype.Component;

@Component
public class BookMapper {
    public BookDTO toDTO(Book book) {
        return new BookDTO(book.getId(), book.getIsbn(), book.getTitle(), book.getAuthor(),
                book.getGenre(), book.getPublishedYear(), book.isAvailable());
    }

    public Book toEntity(BookDTO dto) {
        return new Book(dto.id(), dto.isbn(), dto.title(), dto.author(), dto.genre(),
                dto.publishedYear(), dto.available());
    }
}
