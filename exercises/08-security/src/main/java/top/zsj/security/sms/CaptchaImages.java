package top.zsj.security.sms;

import javax.imageio.ImageIO;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Base64;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 图形验证码绘制（练习 5）
 * <p>
 * 纯 JDK 画图，不引入第三方验证码库。图片给前端看，答案存服务端，两者不在一处。
 *
 * @author zsj
 */
public final class CaptchaImages {

    /** 去掉容易看错的 0/O、1/I/L，减少"明明对了却被判错"的投诉 */
    private static final char[] POOL = "ABCDEFGHJKMNPQRSTUVWXYZ23456789".toCharArray();

    private static final int WIDTH = 120;

    private static final int HEIGHT = 40;

    private static final int LENGTH = 4;

    private CaptchaImages() {
    }

    /** 随机答案，4 位 */
    public static String randomText() {
        StringBuilder sb = new StringBuilder(LENGTH);
        for (int i = 0; i < LENGTH; i++) {
            sb.append(POOL[ThreadLocalRandom.current().nextInt(POOL.length)]);
        }
        return sb.toString();
    }

    /**
     * 画一张 PNG，返回 data URI
     *
     * @param text 答案
     * @return {@code data:image/png;base64,...}
     */
    public static String toDataUri(String text) {
        BufferedImage image = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setColor(new Color(245, 247, 250));
            g.fillRect(0, 0, WIDTH, HEIGHT);

            // 干扰线：画几条浅色斜线，够挡住机器识别，又不至于人眼认不出
            for (int i = 0; i < 6; i++) {
                g.setColor(randomColor(180, 230));
                g.setStroke(new BasicStroke(1.2f));
                g.drawLine(ThreadLocalRandom.current().nextInt(WIDTH),
                        ThreadLocalRandom.current().nextInt(HEIGHT),
                        ThreadLocalRandom.current().nextInt(WIDTH),
                        ThreadLocalRandom.current().nextInt(HEIGHT));
            }

            g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 26));
            for (int i = 0; i < text.length(); i++) {
                g.setColor(randomColor(20, 120));
                double angle = (ThreadLocalRandom.current().nextDouble() - 0.5) * 0.5;
                int x = 12 + i * 26;
                int y = 30;
                g.rotate(angle, x, y);
                g.drawString(String.valueOf(text.charAt(i)), x, y);
                g.rotate(-angle, x, y);
            }
        } finally {
            g.dispose();
        }

        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            ImageIO.write(image, "png", out);
            return "data:image/png;base64," + Base64.getEncoder().encodeToString(out.toByteArray());
        } catch (IOException e) {
            throw new UncheckedIOException("图形验证码生成失败", e);
        }
    }

    private static Color randomColor(int min, int max) {
        int bound = max - min;
        return new Color(min + ThreadLocalRandom.current().nextInt(bound),
                min + ThreadLocalRandom.current().nextInt(bound),
                min + ThreadLocalRandom.current().nextInt(bound));
    }
}
