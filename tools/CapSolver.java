import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * FlyCms 后台登录验证码识别（e2e 自动化用）。
 *
 * 为什么不用通用 OCR：本项目的 GifCaptcha 用 Java2D + `Verdana ITALIC|BOLD 28`
 * 逐帧把同一串字符按不同 Alpha 画在 146x33 白底上，字符集是
 * {@code Randoms.ALPHA}（A-Z 去掉 I/J/L/O，a-z 去掉 l/o，再加 2-9，共 58 个）。
 * 通用 OCR 训练集里没有这套「斜体粗体 + 随机椭圆噪声」，因此这里改为
 * **同字体渲染模板 + 逐槽位 IoU 匹配**：同一套 Java2D 字体栈渲染出的字形与线上
 * 完全一致，匹配天然不受抗锯齿差异影响。
 *
 * 用法：
 *   java CapSolver.java <captcha.gif>               # 打印识别结果
 *   java CapSolver.java <captcha.gif> --dump <dir>  # 额外导出二值图便于肉眼核对
 */
public class CapSolver {

    /** 与 Randoms.ALPHA 一致 */
    private static final char[] ALPHA =
            "ABCDEFGHGKMNPQRSTUVWXYZabcdefghijkmnpqrstuvwxyz23456789".toCharArray();

    private static final int W = 146;
    private static final int H = 33;
    private static final int LEN = 4;
    private static final int SLOT = W / LEN;          // 36
    private static final int X0 = 11;                 // (W-LEN*SLOT) + (SLOT-28) + 1
    /**
     * 基线 y。<b>必须照抄 GifCaptcha.graphicsImage 的算法</b>：
     * {@code h = height - ((height - fontSize) >> 1)} = 33 - (5>>1) = 31，
     * 绘制时用 {@code h - 4} = 27。
     *
     * <p>坑：早期这里图省事写成 {@code H - 4} = 29，比真实基线低了 <b>2 像素</b>，
     * 字形与模板整体错位，'6' 会被判成 'b'、'8' 判成 'R'（识别率约 20%）。
     * 2px 在 33px 高的画布上是 6% 的偏移，足以让笔画错开半个像素格。
     */
    private static final int BASELINE = H - ((H - 28) >> 1) - 4;

    private static final Font FONT = new Font("Verdana", Font.ITALIC | Font.BOLD, 28);

    /**
     * 二值化阈值。合并帧里字形最深 20~130，白底 255，中间 160~180 几乎是空的
     * （实测直方图），取 190 安全地把字形与白底分开；后续开运算负责去掉椭圆环噪声。
     */
    private static final int TH = 190;

    public static void main(String[] args) throws Exception {
        if (args.length == 0) {
            System.err.println("usage: java CapSolver.java <captcha.gif> [--dump <dir>]");
            System.exit(1);
        }
        File gif = new File(args[0]);
        String dumpDir = null;
        if (args.length >= 3 && "--dump".equals(args[1])) {
            dumpDir = args[2];
        }

        List<BufferedImage> frames = readFrames(gif);
        BufferedImage merged = mergeFrames(frames);
        boolean[][] img = opening(binarize(merged, TH));

        if (dumpDir != null) {
            ImageIO.write(toImage(img), "png", new File(dumpDir, "cap-bin.png"));
        }

        // 逐槽位匹配：每槽只画一个字符，槽内像素独立，因此可以独立分类
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < LEN; i++) {
            int from = i * SLOT;
            int to = Math.min(W, from + SLOT);
            char best = '?';
            double bestScore = -1;
            StringBuilder top = new StringBuilder();
            List<double[]> ranked = new ArrayList<>();
            for (char c : ALPHA) {
                boolean[][] tpl = renderChar(c, i);
                double s = iou(img, tpl, from, to);
                ranked.add(new double[]{s, c});
                if (s > bestScore) {
                    bestScore = s;
                    best = c;
                }
            }
            ranked.sort((a, b) -> Double.compare(b[0], a[0]));
            for (int k = 0; k < Math.min(3, ranked.size()); k++) {
                top.append(String.format("%c:%.3f ", (char) ranked.get(k)[1], ranked.get(k)[0]));
            }
            if (dumpDir != null) {
                System.out.printf("slot%d -> %c  (%s)%n", i, best, top.toString().trim());
            }
            sb.append(best);
        }
        System.out.println(sb.toString());
    }

    private static List<BufferedImage> readFrames(File f) throws Exception {
        List<BufferedImage> out = new ArrayList<>();
        try (ImageInputStream iis = ImageIO.createImageInputStream(f)) {
            ImageReader r = ImageIO.getImageReaders(iis).next();
            r.setInput(iis);
            int n = r.getNumImages(true);
            for (int i = 0; i < n; i++) {
                BufferedImage bi = r.read(i);
                BufferedImage rgb = new BufferedImage(W, H, BufferedImage.TYPE_INT_RGB);
                Graphics2D g = rgb.createGraphics();
                g.setColor(Color.WHITE);
                g.fillRect(0, 0, W, H);
                g.drawImage(bi, 0, 0, null);
                g.dispose();
                out.add(rgb);
            }
            r.dispose();
        }
        return out;
    }

    /** 逐帧取暗（min）：字符在任一帧达到最深，噪声椭圆也会叠加，靠后面的 IoU 匹配抗噪 */
    private static BufferedImage mergeFrames(List<BufferedImage> frames) {
        BufferedImage acc = frames.get(0);
        for (int k = 1; k < frames.size(); k++) {
            BufferedImage f = frames.get(k);
            for (int y = 0; y < H; y++) {
                for (int x = 0; x < W; x++) {
                    int a = acc.getRGB(x, y) & 0xFF;
                    int b = f.getRGB(x, y) & 0xFF;
                    int v = Math.min(a, b);
                    acc.setRGB(x, y, (v << 16) | (v << 8) | v);
                }
            }
        }
        return acc;
    }

    /**
     * 形态学开运算 = 腐蚀 + 膨胀。
     *
     * <p>为什么必须去噪：GifCaptcha 每帧额外画 4 个**同色同透明度**的空心椭圆
     * （`drawOval`，描边 1~2px），4 帧合并后图上会留 10+ 个随机位置的椭圆环，
     * 直接用 IoU 匹配会把轮廓近似的字符（8/9、B/R、U/V）判错。
     * 椭圆环是「细结构」，字形笔画是「粗结构」：腐蚀先抹掉细环，膨胀再把笔画厚度补回来，
     * 于是噪声消失而字形几乎无损。
     */
    private static boolean[][] opening(boolean[][] src) {
        return dilate(erode(src));
    }

    private static boolean[][] erode(boolean[][] src) {
        boolean[][] out = new boolean[W][H];
        for (int x = 0; x < W; x++) {
            for (int y = 0; y < H; y++) {
                if (!src[x][y]) continue;
                boolean keep = true;
                for (int dx = -1; dx <= 1 && keep; dx++) {
                    for (int dy = -1; dy <= 1; dy++) {
                        int nx = x + dx;
                        int ny = y + dy;
                        if (nx < 0 || ny < 0 || nx >= W || ny >= H || !src[nx][ny]) {
                            keep = false;
                            break;
                        }
                    }
                }
                out[x][y] = keep;
            }
        }
        return out;
    }

    private static boolean[][] dilate(boolean[][] src) {
        boolean[][] out = new boolean[W][H];
        for (int x = 0; x < W; x++) {
            for (int y = 0; y < H; y++) {
                boolean any = false;
                for (int dx = -1; dx <= 1 && !any; dx++) {
                    for (int dy = -1; dy <= 1; dy++) {
                        int nx = x + dx;
                        int ny = y + dy;
                        if (nx >= 0 && ny >= 0 && nx < W && ny < H && src[nx][ny]) {
                            any = true;
                            break;
                        }
                    }
                }
                out[x][y] = any;
            }
        }
        return out;
    }

    private static boolean[][] binarize(BufferedImage img, int th) {
        boolean[][] m = new boolean[W][H];
        for (int x = 0; x < W; x++) {
            for (int y = 0; y < H; y++) {
                m[x][y] = (img.getRGB(x, y) & 0xFF) < th;
            }
        }
        return m;
    }

    private static BufferedImage toImage(boolean[][] m) {
        BufferedImage bi = new BufferedImage(W, H, BufferedImage.TYPE_INT_RGB);
        for (int x = 0; x < W; x++) {
            for (int y = 0; y < H; y++) {
                bi.setRGB(x, y, m[x][y] ? 0x000000 : 0xFFFFFF);
            }
        }
        return bi;
    }

    /** 把单个字符按线上完全相同的坐标/字体画在空白画布上 */
    private static boolean[][] renderChar(char c, int slot) {
        BufferedImage img = new BufferedImage(W, H, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, W, H);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setFont(FONT);
        g.setColor(Color.BLACK);
        // 与线上在同一画布、同一字体、同一基线绘制，字形像素方能一一对应（见 BASELINE 注释）
        g.drawString(String.valueOf(c), X0 + slot * SLOT, BASELINE);
        g.dispose();
        return binarize(img, TH);
    }

    /**
     * 非对称覆盖率评分：{@code inter / (|T| + 0.5*extra)}。
     *
     * <p>为什么不用 IoU：实测图像里必然叠着随机空心椭圆噪声，IoU 把「图像多出的墨」
     * 与「模板漏掉的墨」同等惩罚，噪声会整体压低所有候选分数、把 9 误判成 8。
     * 这里改为「先保证模板笔画都被图像覆盖」（分子/|T|），只对额外墨给一半权重的惩罚——
     * 噪声对所有候选是近似常数项，真字符因其笔画完整覆盖仍然胜出。
     */
    private static double iou(boolean[][] img, boolean[][] tpl, int from, int to) {
        int inter = 0;
        int tplInk = 0;
        int imgInk = 0;
        for (int x = from; x < to; x++) {
            for (int y = 0; y < H; y++) {
                boolean a = img[x][y];
                boolean b = tpl[x][y];
                if (a) imgInk++;
                if (b) tplInk++;
                if (a && b) inter++;
            }
        }
        if (tplInk == 0) return 0;
        int extra = imgInk - inter;
        return inter / (tplInk + 0.5 * Math.max(0, extra));
    }
}
