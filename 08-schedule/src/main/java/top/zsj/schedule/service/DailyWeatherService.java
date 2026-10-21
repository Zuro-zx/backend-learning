package top.zsj.schedule.service;

import com.alibaba.fastjson2.JSONObject;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * 每日天气早报：定时调用和风天气 API 获取近 3 天天气，组装邮件正文后群发。
 * <p>
 * 配置来源 application.yml：weather.api.*（密钥 / 城市 / 接口地址）、
 * spring.mail.*（QQ 邮箱 SMTP）、email.recipients（收件人列表）。
 *
 * @author zsj
 * @date 2026/10/21
 * @description DailyWeatherService
 */
@Slf4j
@Service
public class DailyWeatherService {

    private final OkHttpClient okHttpClient = new OkHttpClient();

    @Resource
    private JavaMailSender mailSender;

    // 从配置文件注入参数
    @Value("${weather.api.key:}")
    private String weatherApiKey;

    @Value("${weather.api.city}")
    private String cityId;

    @Value("${weather.api.city-name}")
    private String cityName;

    @Value("${weather.api.url:}")
    private String weatherApiUrl;

    @Value("${spring.mail.username:}")
    private String fromEmail;

    @Value("${email.recipients:}")
    private String[] toEmails;

    /**
     * 定时任务：每天 9:22 执行
     */
    @Scheduled(cron = "0 22 9 * * ?")
    public void sendDailyWeather() {
        try {
            // 0. 校验本地私有配置是否就绪（application-local.yml 未创建时给出明确提示）
            if (weatherApiUrl.isBlank() || weatherApiKey.isBlank()
                    || fromEmail.isBlank() || toEmails.length == 0 || toEmails[0].isBlank()) {
                log.warn("【天气早报】私有配置缺失，请在 src/main/resources/application-local.yml 中配置 "
                        + "weather.api.key/url、spring.mail.username/password、email.recipients");
                return;
            }
            // 1. 调用和风天气 API 获取天气数据
            String weatherResponse = getWeatherData();
            if (weatherResponse == null) {
                log.error("【天气早报】获取天气数据失败");
                return;
            }
            // 2. 解析 API 响应（取到当天天气）
            JSONObject weatherJson = JSONObject.parseObject(weatherResponse);
            JSONObject todayWeather = weatherJson.getJSONArray("daily").getJSONObject(0);
            // 日期
            String date = todayWeather.getString("fxDate");
            // 最高温
            String tempMax = todayWeather.getString("tempMax");
            // 最低温
            String tempMin = todayWeather.getString("tempMin");
            // 白天天气（如「晴」）
            String textDay = todayWeather.getString("textDay");
            // 风向 / 风力
            String windDirDay = todayWeather.getString("windDirDay");
            String windScaleDay = todayWeather.getString("windScaleDay");

            // 3. 组装邮件内容
            String subject = String.format("【每日天气早报】%s %s天气",
                    LocalDate.parse(date, DateTimeFormatter.ofPattern("yyyy-MM-dd")), cityName);
            String content = String.format("""
                    📅 日期: %s
                    🌤 天气: %s
                    🌡 温度: %s℃ ~ %s℃
                    💨 风向: %s
                    🍃 风力: %s级

                    今天请根据天气增减衣物，注意交通安全！""", date, textDay, tempMin, tempMax, windDirDay, windScaleDay);

            // 4. 发送邮件
            log.info("【天气早报】{} {} 天气：{}，{}~{}℃，{} {}级", cityName, date, textDay, tempMin, tempMax, windDirDay, windScaleDay);
            sendEmail(subject, content);
            log.info("【天气早报】邮件发送成功，收件人: {}", String.join(",", toEmails));
        } catch (Exception e) {
            log.error("【天气早报】执行失败", e);
        }
    }

    /**
     * 调用和风天气 API，获取 3 天天气数据
     *
     * @return API 响应（JSON 格式）
     */
    private String getWeatherData() throws IOException {
        // 拼 API 请求 URL（含城市 ID 和 API 密钥）
        String requestUrl = String.format("%s?location=%s&key=%s", weatherApiUrl, cityId, weatherApiKey);
        Request request = new Request.Builder().url(requestUrl).build();
        try (Response response = okHttpClient.newCall(request).execute()) {
            if (response.isSuccessful() && response.body() != null) {
                return response.body().string();
            }
            return null;
        }
    }

    /**
     * 发送邮件方法
     *
     * @param subject 邮件主题
     * @param content 邮件内容
     */
    private void sendEmail(String subject, String content) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromEmail);
        message.setTo(toEmails);
        message.setSubject(subject);
        message.setText(content);
        mailSender.send(message);
    }
}
