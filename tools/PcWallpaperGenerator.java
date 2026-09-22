import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

/**
 * Gera o papel de parede do PC com a logo do ShifuMon, no tamanho que o Cobblemon usa (174x155).
 *
 * A logo fica escurecida e por baixo dos Pokémon, para não atrapalhar a leitura dos sprites.
 *
 * Uso, na raiz do projeto:
 *   java tools/PcWallpaperGenerator.java "<caminho da logo.png>"
 */
public final class PcWallpaperGenerator {
    private static final String OUT = "src/main/resources/assets/shifumon/textures/gui/pc/wallpaper_shifumon.png";
    private static final int WIDTH = 174;
    private static final int HEIGHT = 155;
    private static final int LOGO_SIZE = 132;
    private static final float LOGO_ALPHA = 0.7f;

    private static final Color TOP = new Color(0x1C1C2E);
    private static final Color BOTTOM = new Color(0x0F0F1A);
    private static final Color BORDER = new Color(0x7A4FB0);

    public static void main(String[] args) throws IOException {
        if (args.length < 1) {
            System.err.println("Uso: java tools/PcWallpaperGenerator.java <logo.png>");
            System.exit(1);
        }
        BufferedImage logo = ImageIO.read(new File(args[0]));
        BufferedImage out = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = out.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

        g.setPaint(new GradientPaint(0, 0, TOP, 0, HEIGHT, BOTTOM));
        g.fillRect(0, 0, WIDTH, HEIGHT);

        // Brilho roxo atrás da logo
        for (int i = 34; i > 0; i--) {
            int alpha = 3 + (34 - i) / 3;
            g.setColor(new Color(BORDER.getRed(), BORDER.getGreen(), BORDER.getBlue(), alpha));
            g.fillOval(WIDTH / 2 - LOGO_SIZE / 2 - i, HEIGHT / 2 - LOGO_SIZE / 2 - i, LOGO_SIZE + i * 2, LOGO_SIZE + i * 2);
        }

        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, LOGO_ALPHA));
        g.drawImage(logo, (WIDTH - LOGO_SIZE) / 2, (HEIGHT - LOGO_SIZE) / 2, LOGO_SIZE, LOGO_SIZE, null);
        g.setComposite(AlphaComposite.SrcOver);

        // Moldura fina, como as telas do mod
        g.setColor(new Color(BORDER.getRed(), BORDER.getGreen(), BORDER.getBlue(), 150));
        g.drawRect(0, 0, WIDTH - 1, HEIGHT - 1);
        g.dispose();

        File file = new File(OUT);
        file.getParentFile().mkdirs();
        ImageIO.write(out, "png", file);
        System.out.println("Papel de parede gerado: " + file.getAbsolutePath() + " (" + WIDTH + "x" + HEIGHT + ")");

        // Textura vazia: entra no lugar da camada de brilho do papel de parede original
        File empty = new File(file.getParentFile(), "empty.png");
        ImageIO.write(new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB), "png", empty);
        System.out.println("Textura vazia gerada: " + empty.getAbsolutePath());
    }
}
