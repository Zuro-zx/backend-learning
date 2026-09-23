package top.mqxu.config.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * @Value 四种常见形态演示服务
 *
 * <p>1. 占位符引用：{@code ${app.author}} —— 直接引用配置文件中的属性值；</p>
 * <p>2. 默认值：{@code ${app.remark:暂无备注}} —— 冒号后是默认值，
 * 属性不存在时使用默认值，避免启动报错；</p>
 * <p>3. 随机值：{@code ${random.uuid}}、{@code ${random.int(1,100)}} ——
 * Spring 内置 RandomValuePropertySource，每次应用启动生成一次，
 * 重启后才会变化；</p>
 * <p>4. SpEL 表达式：{@code #{${student.age} >= 18 ? '成年' : '未成年'}} ——
 * 先由占位符解析出 18，再由 SpEL 动态计算表达式结果。</p>
 *
 * @author zsj
 * @date 2026/9/17
 * @description @Value 四种形态的注入与演示
 */
@Service
public class ValueService {

    /** 形态一：占位符引用，直接读取 app.author */
    @Value("${app.author}")
    private String author;

    /** 形态二：默认值，app.remark 未配置时取冒号后的“暂无备注” */
    @Value("${app.remark:暂无备注}")
    private String remark;

    /** 形态三：随机值，生成一个随机 UUID（每次启动重新生成） */
    @Value("${random.uuid}")
    private String randomUuid;

    /** 形态三：随机值，生成 1~100 之间的随机整数（每次启动重新生成） */
    @Value("${random.int(1,100)}")
    private Integer randomNumber;

    /** 形态四：SpEL 表达式，根据 student.age 动态判断成年与否 */
    @Value("#{${student.age} >= 18 ? '成年' : '未成年'}")
    private String ageStage;

    public String getAuthor() {
        return author;
    }

    public String getRemark() {
        return remark;
    }

    public String getRandomUuid() {
        return randomUuid;
    }

    public Integer getRandomNumber() {
        return randomNumber;
    }

    public String getAgeStage() {
        return ageStage;
    }

    /**
     * 把四种形态的注入结果组装成 Map，供接口直接返回
     *
     * @return 形态 -> 注入结果
     */
    public Map<String, Object> toValueMap() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("占位符引用 ${app.author}", author);
        result.put("默认值 ${app.remark:暂无备注}", remark);
        result.put("随机值 ${random.uuid}", randomUuid);
        result.put("随机值 ${random.int(1,100)}", randomNumber);
        result.put("SpEL #{${student.age} >= 18 ? '成年' : '未成年'}", ageStage);
        return result;
    }

    @Override
    public String toString() {
        return "ValueService{" +
                "author='" + author + '\'' +
                ", remark='" + remark + '\'' +
                ", randomUuid=" + randomUuid +
                ", randomNumber=" + randomNumber +
                ", ageStage='" + ageStage + '\'' +
                '}';
    }
}
