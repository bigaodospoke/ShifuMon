import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

/**
 * Gera o ícone do mod (o que aparece na lista de mods) a partir da logo do ShifuMon, em 128x128.
 *
 * O fundo fica transparente de propósito: a lista de mods desenha o ícone sobre a própria tela, e
 * uma moldura quadrada ficaria pior do que a logo recortada.
 *
 * Uso, na raiz do projeto:
 *   java tools/IconGenerator.java "<caminho da logo.png>"
 */
public final class IconGenerator {
    private static final String OUT = "src/main/resources/assets/shifumon/icon.png";
    private static final int SIZE = 128;

    public static void main(String[] args) throws IOException {
        if (args.length < 1) {
            System.err.println("Uso: java tools/IconGenerator.java <logo.png>");
            System.exit(1);
        }
        BufferedImage logo = ImageIO.read(new File(args[0]));
        BufferedImage out = new BufferedImage(SIZE, SIZE, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = out.createGraphics();
        // A logo vem grande (mais de 1000px). Reduzir em uma etapa só com interpolação bilinear
        // deixa a arte quebradiça; o passo a passo, pela metade de cada vez, preserva os traços.
        BufferedImage atual = logo;
        while (atual.getWidth() / 2 > SIZE) {
            atual = reduzirMetade(atual);
        }
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.drawImage(atual, 0, 0, SIZE, SIZE, null);
        g.dispose();

        File file = new File(OUT);
        file.getParentFile().mkdirs();
        ImageIO.write(out, "png", file);
        System.out.println("Ícone gerado: " + file.getAbsolutePath() + " (" + SIZE + "x" + SIZE + ")");
    }

    private static BufferedImage reduzirMetade(BufferedImage origem) {
        int largura = Math.max(1, origem.getWidth() / 2);
        int altura = Math.max(1, origem.getHeight() / 2);
        BufferedImage menor = new BufferedImage(largura, altura, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = menor.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g.drawImage(origem, 0, 0, largura, altura, null);
        g.dispose();
        return menor;
    }
}
