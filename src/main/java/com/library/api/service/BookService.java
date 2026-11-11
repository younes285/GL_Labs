package com.library.api.service;

import com.library.api.entity.Book;
import com.library.api.repository.BookRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class BookService {
    private final BookRepository books;

    public BookService(BookRepository books) {
        this.books = books;
    }

    public List<Book> getAll() {
        return books.findAll();
    }

    public Book getById(Long id) {
        return books.findById(id).orElseThrow(() -> new EntityNotFoundException("Book not found"));
    }

    @Transactional
    public Book create(Book book) {
        book.setId(null);
        return books.save(book);
    }

    @Transactional
    public Book update(Long id, Book updated) {
        Book existing = getById(id);
        existing.setIsbn(updated.getIsbn());
        existing.setTitle(updated.getTitle());
        existing.setAuthor(updated.getAuthor());
        existing.setGenre(updated.getGenre());
        existing.setPublishedYear(updated.getPublishedYear());
        existing.setAvailable(updated.isAvailable());
        return books.save(existing);
    }

    @Transactional
    public void delete(Long id) {
        books.delete(getById(id));
    }
}
