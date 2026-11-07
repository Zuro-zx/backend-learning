package top.zsj.doc.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import top.zsj.doc.common.BusinessException;
import top.zsj.doc.common.ErrorCode;
import top.zsj.doc.common.PageResult;
import top.zsj.doc.common.Result;
import top.zsj.doc.dto.BookCreateRequest;
import top.zsj.doc.dto.BookQuery;
import top.zsj.doc.entity.Book;
import top.zsj.doc.entity.Category;
import top.zsj.doc.service.BookService;

import java.util.List;

/**
 * 图书接口：注解写多细，文档就多细
 *
 * @author zsj
 */
@Tag(name = "图书管理", description = "图书的增删改查、分页与分类查询")
@RestController
@RequestMapping("/books")
public class BookController {

    private final BookService bookService;

    public BookController(BookService bookService) {
        this.bookService = bookService;
    }

    @Operation(
            summary = "分页查询图书",
            description = "支持按关键字（书名或作者）与分类过滤。两个条件都可省略，不传即查全部。"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "查询成功"),
            @ApiResponse(responseCode = "400", description = "分页参数非法",
                    content = @Content(schema = @Schema(implementation = Result.class)))
    })
    @GetMapping
    public Result<PageResult<Book>> page(@ParameterObject BookQuery query) {
        return Result.ok(bookService.page(query));
    }

    @Operation(summary = "查询图书详情")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "查询成功"),
            @ApiResponse(responseCode = "404", description = "图书不存在",
                    content = @Content(schema = @Schema(implementation = Result.class)))
    })
    @GetMapping("/{id}")
    public Result<Book> getById(
            @Parameter(description = "图书 ID", example = "1", required = true)
            @PathVariable Long id) {
        return Result.ok(bookService.getById(id));
    }

    @Operation(summary = "新增图书", description = "ISBN 全局唯一，重复时返回 409")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "新增成功"),
            @ApiResponse(responseCode = "400", description = "参数校验失败",
                    content = @Content(schema = @Schema(implementation = Result.class))),
            @ApiResponse(responseCode = "409", description = "ISBN 已存在",
                    content = @Content(schema = @Schema(implementation = Result.class)))
    })
    @PostMapping
    public Result<Book> create(
            // 注意：这里用的是 swagger 的 @RequestBody，不是 Spring 的。
            // 两个类同名，写错 import 不报错，只是文档里没有示例。
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "图书信息，除库存与出版日期外都是必填",
                    required = true,
                    content = @Content(examples = @ExampleObject(name = "标准示例", value = """
                            {
                              "title": "重构：改善既有代码的设计",
                              "author": "Martin Fowler",
                              "isbn": "9787115508645",
                              "category": "TECH",
                              "price": 118.00,
                              "stock": 15,
                              "publishDate": "2019-05-01"
                            }""")))
            @Valid @org.springframework.web.bind.annotation.RequestBody BookCreateRequest request) {
        return Result.ok(bookService.create(request));
    }

    @Operation(summary = "删除图书")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "删除成功"),
            @ApiResponse(responseCode = "404", description = "图书不存在",
                    content = @Content(schema = @Schema(implementation = Result.class)))
    })
    @DeleteMapping("/{id}")
    public Result<Void> delete(
            @Parameter(description = "图书 ID", example = "6", required = true)
            @PathVariable Long id) {
        bookService.delete(id);
        return Result.ok();
    }

    @Operation(summary = "查询全部分类", description = "枚举值，前端下拉框直接用这个接口的返回渲染")
    @GetMapping("/categories")
    public Result<List<Category>> categories() {
        return Result.ok(bookService.categories());
    }

    @Operation(summary = "触发未预期异常",
            description = "演示 500 响应的结构，生产环境不会保留这种接口",
            deprecated = true)
    @GetMapping("/boom")
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Result<Void> boom() {
        throw new BusinessException(ErrorCode.INTERNAL_ERROR);
    }
}
