package top.zsj.zhihu.common;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 统一返回体
 * <p>
 * 与 06-redis 的 Result 不同，这里按教程约定用 code / msg / data 三个字段，
 * 前端读到的是 msg 而不是 message，两边必须对齐。
 *
 * @author zsj
 * @date 2026/10/19
 * @description 统一返回体
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ResponseResult {

    /** 业务码，200 表示成功 */
    private Integer code;

    /** 提示语 */
    private String msg;

    /** 数据 */
    private Object data;
}
