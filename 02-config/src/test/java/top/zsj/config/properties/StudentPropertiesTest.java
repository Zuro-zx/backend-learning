package top.zsj.config.properties;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author zsj
 * @date 2026/9/16
 * @description 验证 @ConfigurationProperties 对各类配置的绑定结果
 */
@SpringBootTest
@Slf4j
public class StudentPropertiesTest {

    @Autowired
    private StudentProperties studentProperties;

    @Test
    public void printStudentInfo() {
        log.info("学生完整信息：{}", studentProperties);
        log.info("学生姓名：{}", studentProperties.getName());
        log.info("语文成绩：{}", studentProperties.getScores().get("chinese"));
    }

    /**
     * 简单字段：student.name、student.age
     */
    @Test
    public void testSimpleField() {
        assertEquals("张三", studentProperties.getName());
        assertEquals(18, studentProperties.getAge());
        log.info("简单字段绑定成功：name={}, age={}", studentProperties.getName(), studentProperties.getAge());
    }

    /**
     * List：student.hobbies
     */
    @Test
    public void testList() {
        List<String> hobbies = studentProperties.getHobbies();
        assertNotNull(hobbies);
        assertEquals(3, hobbies.size());
        assertTrue(hobbies.contains("篮球"));
        assertTrue(hobbies.contains("编程"));
        assertTrue(hobbies.contains("阅读"));
        log.info("List 绑定成功：hobbies={}", hobbies);
    }

    /**
     * Map：student.scores
     */
    @Test
    public void testMap() {
        Map<String, Integer> scores = studentProperties.getScores();
        assertNotNull(scores);
        assertEquals(90, scores.get("chinese"));
        assertEquals(95, scores.get("math"));
        assertEquals(88, scores.get("english"));
        log.info("Map 绑定成功：scores={}", scores);
    }

    /**
     * 嵌套对象：student.address
     */
    @Test
    public void testNestedObject() {
        StudentProperties.Address address = studentProperties.getAddress();
        assertNotNull(address);
        assertEquals("江苏省", address.getProvince());
        assertEquals("南京市", address.getCity());
        log.info("嵌套对象绑定成功：address={}", address);
    }

    /**
     * List 嵌套对象：student.courses
     */
    @Test
    public void testListNestedObject() {
        List<StudentProperties.Course> courses = studentProperties.getCourses();
        assertNotNull(courses);
        assertEquals(2, courses.size());

        StudentProperties.Course math = courses.get(0);
        assertEquals("高等数学", math.getName());
        assertEquals(4, math.getCredit());

        StudentProperties.Course english = courses.get(1);
        assertEquals("大学英语", english.getName());
        assertEquals(3, english.getCredit());

        log.info("List 嵌套对象绑定成功：courses={}", courses);
    }
}
