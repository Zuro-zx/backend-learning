package top.zsj.config.properties;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * 学生配置类
 *
 * <p>使用 {@code @Component} 注册为 Spring 容器中的 Bean，
 * 再通过 {@code @ConfigurationProperties} 把 application.yml 中
 * {@code student} 前缀下的一组配置一次性绑定到本类的成员变量上。</p>
 *
 * <p>支持的类型：简单字段、List、Map、嵌套对象、List 嵌套对象。</p>
 *
 * @author zsj
 * @date 2026/9/16
 * @description student 前缀配置的绑定对象
 */
@Component
@Validated
@ConfigurationProperties(prefix = "student")
public class StudentProperties {

    /** 简单字段：student.name */
    @NotBlank(message = "学生姓名name不能为空")
    private String name;

    /** 简单字段：student.age */
    @Min(value = 1, message = "年龄age必须≥1")
    private Integer age;

    /** 邮箱字段：student.email，不能为空且必须符合邮箱格式 */
    @NotBlank(message = "邮箱email不能为空")
    @Email(message = "邮箱email格式不合法")
    private String email;

    /** 日期字段：student.birthday，不能为空且必须是过去的日期 */
    @NotNull(message = "生日birthday不能为空")
    @Past(message = "生日birthday必须是过去的日期")
    private LocalDate birthday;

    /** List：student.hobbies */
    private List<String> hobbies;

    /** Map：student.scores */
    private Map<String, Integer> scores;

    /** 嵌套对象：student.address */
    private Address address;

    /** List 嵌套对象：student.courses */
    private List<Course> courses;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Integer getAge() {
        return age;
    }

    public void setAge(Integer age) {
        this.age = age;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public LocalDate getBirthday() {
        return birthday;
    }

    public void setBirthday(LocalDate birthday) {
        this.birthday = birthday;
    }

    public List<String> getHobbies() {
        return hobbies;
    }

    public void setHobbies(List<String> hobbies) {
        this.hobbies = hobbies;
    }

    public Map<String, Integer> getScores() {
        return scores;
    }

    public void setScores(Map<String, Integer> scores) {
        this.scores = scores;
    }

    public Address getAddress() {
        return address;
    }

    public void setAddress(Address address) {
        this.address = address;
    }

    public List<Course> getCourses() {
        return courses;
    }

    public void setCourses(List<Course> courses) {
        this.courses = courses;
    }

    @Override
    public String toString() {
        return "StudentProperties{" +
                "name='" + name + '\'' +
                ", age=" + age +
                ", email='" + email + '\'' +
                ", birthday=" + birthday +
                ", hobbies=" + hobbies +
                ", scores=" + scores +
                ", address=" + address +
                ", courses=" + courses +
                '}';
    }

    /**
     * 嵌套对象：对应 student.address 下的 province、city
     */
    public static class Address {

        private String province;

        private String city;

        public String getProvince() {
            return province;
        }

        public void setProvince(String province) {
            this.province = province;
        }

        public String getCity() {
            return city;
        }

        public void setCity(String city) {
            this.city = city;
        }

        @Override
        public String toString() {
            return "Address{" +
                    "province='" + province + '\'' +
                    ", city='" + city + '\'' +
                    '}';
        }
    }

    /**
     * List 嵌套对象中的元素：对应 student.courses 数组里的 name、credit
     */
    public static class Course {

        private String name;

        private Integer credit;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public Integer getCredit() {
            return credit;
        }

        public void setCredit(Integer credit) {
            this.credit = credit;
        }

        @Override
        public String toString() {
            return "Course{" +
                    "name='" + name + '\'' +
                    ", credit=" + credit +
                    '}';
        }
    }
}
