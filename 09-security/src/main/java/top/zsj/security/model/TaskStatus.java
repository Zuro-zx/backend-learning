package top.zsj.security.model;

import lombok.Getter;

/**
 * 任务状态枚举：只有待批改与已批改两种
 *
 * @author zsj
 */
@Getter
public enum TaskStatus {

    /** 待批改：学生刚提交，或批改后重交 */
    SUBMITTED("待批改"),

    /** 已批改：分数一旦给学生看就是结论（BR-4），要改分数得走另外的流程 */
    REVIEWED("已批改");

    /** 状态中文文案，出参里的 statusText */
    private final String text;

    TaskStatus(String text) {
        this.text = text;
    }

    /** 按数据库里的状态字符串查枚举，查不到返回 null */
    public static TaskStatus fromCode(String code) {
        try {
            return valueOf(code);
        } catch (IllegalArgumentException | NullPointerException e) {
            return null;
        }
    }
}
