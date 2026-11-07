package top.zsj.doc.service;

import org.springframework.stereotype.Service;
import top.zsj.doc.common.BusinessException;
import top.zsj.doc.common.ErrorCode;
import top.zsj.doc.common.PageResult;
import top.zsj.doc.dto.BookCreateRequest;
import top.zsj.doc.dto.BookQuery;
import top.zsj.doc.entity.Book;
import top.zsj.doc.entity.Category;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 图书服务：数据存内存，重启恢复
 * <p>
 * 这一篇的重点在注解，不该被数据层分散，所以不接数据库。
 *
 * @author zsj
 */
@Service
public class BookService {

    private final Map<Long, Book> store = new ConcurrentHashMap<>();

    private final AtomicLong idGenerator = new AtomicLong();

    public BookService() {
        seed();
    }

    /** 分页 + 关键字 + 分类过滤，两个条件都可省略 */
    public PageResult<Book> page(BookQuery query) {
        int pageNum = query.getPageNum() == null || query.getPageNum() < 1 ? 1 : query.getPageNum();
        int pageSize = query.getPageSize() == null || query.getPageSize() < 1 ? 10 : query.getPageSize();
        if (pageSize > 100) {
            throw new BusinessException(ErrorCode.BAD_REQUEST);
        }

        List<Book> filtered = store.values().stream()
                .filter(book -> matchKeyword(book, query.getKeyword()))
                .filter(book -> query.getCategory() == null || query.getCategory() == book.getCategory())
                .sorted(Comparator.comparing(Book::getId))
                .toList();

        int from = Math.min((pageNum - 1) * pageSize, filtered.size());
        int to = Math.min(from + pageSize, filtered.size());
        return new PageResult<>(filtered.subList(from, to), filtered.size(), pageNum, pageSize);
    }

    public Book getById(Long id) {
        Book book = store.get(id);
        if (book == null) {
            throw new BusinessException(ErrorCode.BOOK_NOT_FOUND);
        }
        return book;
    }

    public Book create(BookCreateRequest request) {
        boolean duplicated = store.values().stream()
                .anyMatch(book -> book.getIsbn().equals(request.getIsbn()));
        if (duplicated) {
            throw new BusinessException(ErrorCode.BOOK_ISBN_DUPLICATED);
        }

        Book book = new Book();
        book.setId(idGenerator.incrementAndGet());
        book.setTitle(request.getTitle());
        book.setAuthor(request.getAuthor());
        book.setIsbn(request.getIsbn());
        book.setCategory(request.getCategory());
        book.setPrice(request.getPrice());
        book.setStock(request.getStock() == null ? 0 : request.getStock());
        book.setPublishDate(request.getPublishDate());
        book.setCreateTime(LocalDateTime.now());
        store.put(book.getId(), book);
        return book;
    }

    public void delete(Long id) {
        if (store.remove(id) == null) {
            throw new BusinessException(ErrorCode.BOOK_NOT_FOUND);
        }
    }

    public List<Category> categories() {
        return Arrays.asList(Category.values());
    }

    private boolean matchKeyword(Book book, String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return true;
        }
        String lower = keyword.toLowerCase();
        return book.getTitle().toLowerCase().contains(lower)
                || book.getAuthor().toLowerCase().contains(lower);
    }

    /** 五本起始数据 */
    private void seed() {
        create(seedRequest("深入理解 Java 虚拟机", "周志明", "9787111641247",
                Category.TECH, "129.00", 20, LocalDate.of(2019, 12, 1)));
        create(seedRequest("Java 编程思想", "Bruce Eckel", "9787111213826",
                Category.TECH, "108.00", 12, LocalDate.of(2007, 6, 1)));
        create(seedRequest("活着", "余华", "9787506365437",
                Category.LITERATURE, "28.00", 50, LocalDate.of(2012, 8, 1)));
        create(seedRequest("万历十五年", "黄仁宇", "9787108009821",
                Category.HISTORY, "26.00", 8, LocalDate.of(1997, 5, 1)));
        create(seedRequest("时间简史", "史蒂芬·霍金", "9787535732309",
                Category.SCIENCE, "45.00", 30, LocalDate.of(2010, 4, 1)));
    }

    private BookCreateRequest seedRequest(String title, String author, String isbn,
                                          Category category, String price, int stock, LocalDate publishDate) {
        BookCreateRequest request = new BookCreateRequest();
        request.setTitle(title);
        request.setAuthor(author);
        request.setIsbn(isbn);
        request.setCategory(category);
        request.setPrice(new BigDecimal(price));
        request.setStock(stock);
        request.setPublishDate(publishDate);
        return request;
    }
}
