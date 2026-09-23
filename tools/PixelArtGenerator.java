import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Map;

/**
 * Gerador dos sprites pixel art do ShifuMon.
 *
 * Cada ícone é desenhado aqui como uma grade ASCII e exportado como PNG sem suavização para
 * src/main/resources/assets/shifumon/. Os PNGs podem ser retocados em qualquer editor de pixel art
 * (Aseprite, LibreSprite, Piskel...) ou sobrescritos por resource packs.
 *
 * Uso, na raiz do projeto (JDK 21):
 *   java tools/PixelArtGenerator.java
 *
 * Legenda das grades:
 *   '.'  pixel transparente
 *   '#'  corpo com contorno, luz (cima/esquerda) e sombra (baixo/direita) automáticos, estilo GBA
 *   outras letras: cor explícita definida na paleta do sprite
 */
public final class PixelArtGenerator {
    private static final String OUT = "src/main/resources/assets/shifumon/";

    /** Cores do corpo '#': contorno, luz, base e sombra. */
    private record Body(int outline, int light, int base, int shade) {}

    public static void main(String[] args) throws IOException {
        shinyIcons();
        typeIcons();
        hudIcons();
        modIcon();
        System.out.println("Sprites gerados em " + new File(OUT).getAbsolutePath());
    }

    // ---------------------------------------------------------------- Icones de tipo (8x8)

    /**
     * Silhuetas dos 18 tipos, em branco: o mod desenha uma sombra escura atras e usa a cor do
     * tipo como fundo da etiqueta. As formas sao bem diferentes entre si porque as cores de Terra,
     * Pedra e Eletrico se parecem demais.
     */
    private static void typeIcons() throws IOException {
        save("textures/gui/types/normal.png", render(new String[]{
                "..wwww..",
                ".ww..ww.",
                "ww....ww",
                "w......w",
                "w......w",
                "ww....ww",
                ".ww..ww.",
                "..wwww..",
        }, null, Map.of('w', 0xFFFFFFFF), false), 1);

        save("textures/gui/types/fire.png", render(new String[]{
                "...w....",
                "..ww....",
                ".www....",
                ".wwww...",
                "wwwwww..",
                "ww.www..",
                ".w..ww..",
                "........",
        }, null, Map.of('w', 0xFFFFFFFF), false), 1);

        save("textures/gui/types/water.png", render(new String[]{
                "...w....",
                "...ww...",
                "..wwww..",
                "..wwww..",
                ".wwwwww.",
                "wwwwwwww",
                ".wwwwww.",
                "..wwww..",
        }, null, Map.of('w', 0xFFFFFFFF), false), 1);

        save("textures/gui/types/electric.png", render(new String[]{
                "....www.",
                "...ww...",
                "..ww....",
                ".wwwwww.",
                "....ww..",
                "...ww...",
                "..ww....",
                ".w......",
        }, null, Map.of('w', 0xFFFFFFFF), false), 1);

        save("textures/gui/types/grass.png", render(new String[]{
                "......ww",
                "....wwww",
                "..wwwww.",
                ".wwwwww.",
                ".wwwww..",
                ".wwww...",
                "ww......",
                "w.......",
        }, null, Map.of('w', 0xFFFFFFFF), false), 1);

        save("textures/gui/types/ice.png", render(new String[]{
                "...w....",
                ".w.w.w..",
                "..www...",
                "wwwwwww.",
                "..www...",
                ".w.w.w..",
                "...w....",
                "........",
        }, null, Map.of('w', 0xFFFFFFFF), false), 1);

        save("textures/gui/types/fighting.png", render(new String[]{
                "........",
                ".wwww...",
                "wwwwww..",
                "wwwwwww.",
                "ww.w.ww.",
                "wwwwwww.",
                ".wwwww..",
                "........",
        }, null, Map.of('w', 0xFFFFFFFF), false), 1);

        save("textures/gui/types/poison.png", render(new String[]{
                "..wwww..",
                ".wwwwww.",
                "ww.ww.ww",
                "wwwwwwww",
                "wwwwwwww",
                ".ww..ww.",
                "..wwww..",
                "........",
        }, null, Map.of('w', 0xFFFFFFFF), false), 1);

        save("textures/gui/types/ground.png", render(new String[]{
                "........",
                "wwwwwwww",
                "........",
                "www.wwww",
                "........",
                "wwwww.ww",
                "........",
                "wwwwwwww",
        }, null, Map.of('w', 0xFFFFFFFF), false), 1);

        save("textures/gui/types/flying.png", render(new String[]{
                "........",
                ".ww.....",
                "wwwww...",
                "wwwwwww.",
                ".wwwwww.",
                "..wwww..",
                "...ww...",
                "........",
        }, null, Map.of('w', 0xFFFFFFFF), false), 1);

        save("textures/gui/types/psychic.png", render(new String[]{
                "..wwww..",
                ".w....w.",
                "w..ww..w",
                "w.wwww.w",
                "w..ww..w",
                ".w....w.",
                "..wwww..",
                "........",
        }, null, Map.of('w', 0xFFFFFFFF), false), 1);

        save("textures/gui/types/bug.png", render(new String[]{
                ".w....w.",
                "..w..w..",
                ".wwwwww.",
                "ww.ww.ww",
                "wwwwwwww",
                ".w.ww.w.",
                ".w....w.",
                "........",
        }, null, Map.of('w', 0xFFFFFFFF), false), 1);

        save("textures/gui/types/rock.png", render(new String[]{
                "....w...",
                "...www..",
                "..wwwww.",
                ".wwwwwww",
                "wwwwwwww",
                "wwwwwwww",
                "........",
                "........",
        }, null, Map.of('w', 0xFFFFFFFF), false), 1);

        save("textures/gui/types/ghost.png", render(new String[]{
                "..wwww..",
                ".wwwwww.",
                "ww.ww.ww",
                "wwwwwwww",
                "wwwwwwww",
                "wwwwwwww",
                "w.ww.w.w",
                "........",
        }, null, Map.of('w', 0xFFFFFFFF), false), 1);

        save("textures/gui/types/dragon.png", render(new String[]{
                "w.......",
                "ww......",
                "wwww....",
                ".wwwww..",
                "..wwwwww",
                "...wwww.",
                "....ww..",
                "........",
        }, null, Map.of('w', 0xFFFFFFFF), false), 1);

        save("textures/gui/types/dark.png", render(new String[]{
                "..www...",
                ".wwww...",
                "wwww....",
                "wwww....",
                "wwww....",
                ".wwww...",
                "..www...",
                "........",
        }, null, Map.of('w', 0xFFFFFFFF), false), 1);

        save("textures/gui/types/steel.png", render(new String[]{
                "..w..w..",
                ".wwwwww.",
                ".ww..ww.",
                "ww....ww",
                "ww....ww",
                ".ww..ww.",
                ".wwwwww.",
                "..w..w..",
        }, null, Map.of('w', 0xFFFFFFFF), false), 1);

        save("textures/gui/types/fairy.png", render(new String[]{
                "...w....",
                "...w....",
                ".wwwww..",
                "wwwwwww.",
                ".wwwww..",
                "...w....",
                "...w....",
                "........",
        }, null, Map.of('w', 0xFFFFFFFF), false), 1);

    }

    // ------------------------------------------------------------------ Ícones de shiny (16x16)

    private static void shinyIcons() throws IOException {
        save("textures/gui/shiny/star.png", render(new String[]{
                "................",
                ".......##.......",
                "......####......",
                "......#W##......",
                ".....######.....",
                "################",
                "################",
                ".##############.",
                "..############..",
                "...##########...",
                "...##########...",
                "..############..",
                ".######..######.",
                ".#####....#####.",
                ".###........###.",
                "................",
        }, new Body(0xFF5C4200, 0xFFFFFBC2, 0xFFFFE14D, 0xFFE0A800), Map.of('W', 0xFFFFFFFF), false), 1);

        save("textures/gui/shiny/square.png", render(new String[]{
                "................",
                "................",
                "..############..",
                "..############..",
                "..#WW#########..",
                "..#W##########..",
                "..####DDDD####..",
                "..####D##D####..",
                "..####D##D####..",
                "..####DDDD####..",
                "..############..",
                "..############..",
                "..############..",
                "..############..",
                "................",
                "................",
        }, new Body(0xFF4A0930, 0xFFFFB3E0, 0xFFF25CB5, 0xFFB02A7C),
                Map.of('W', 0xFFFFFFFF, 'D', 0xFFC93D93), false), 1);

        // Diamante com facetas desenhadas à mão (diagonais não ficam boas com sombreamento automático)
        save("textures/gui/shiny/diamond.png", render(new String[]{
                "................",
                ".......OO.......",
                "......OLMO......",
                ".....OLLMMO.....",
                "....OLWLMMMO....",
                "...OLLLLMMMMO...",
                "..OLLLLLMMMMDO..",
                ".OLLLLLLMMMMDDO.",
                ".OMMMMMMDDDDDDO.",
                "..OMMMMMDDDDDO..",
                "...OMMMMDDDDO...",
                "....OMMMDDDO....",
                ".....OMMDDO.....",
                "......OMDO......",
                ".......OO.......",
                "................",
        }, null, Map.of('O', 0xFF0A3550, 'L', 0xFFBFF6FF, 'M', 0xFF5ED8F5, 'D', 0xFF2A8FC4, 'W', 0xFFFFFFFF), false), 1);

        save("textures/gui/shiny/crown.png", render(new String[]{
                "................",
                "................",
                "................",
                "..#....##....#..",
                ".###..####..###.",
                ".###..####..###.",
                ".####.####.####.",
                ".##############.",
                ".##############.",
                ".###rR#bB#rR###.",
                ".###RR#BB#RR###.",
                ".##############.",
                ".##############.",
                "................",
                "................",
                "................",
        }, new Body(0xFF4A2600, 0xFFFFD98A, 0xFFF5A623, 0xFFB86B00),
                Map.of('R', 0xFFE8283C, 'r', 0xFFFF8A94, 'B', 0xFF2F7BFF, 'b', 0xFF9CC4FF), false), 1);
    }

    // ------------------------------------------------------------------ Ícones da HUD (9x9, como os corações do Minecraft)

    private static void hudIcons() throws IOException {
        save("textures/gui/hud/turn.png", glyph(new String[]{
                ".........",
                ".kkkkkkk.",
                "..ksssk..",
                "...ksk...",
                "....k....",
                "...ksk...",
                "..ksssk..",
                ".kkkkkkk.",
                ".........",
        }, Map.of('k', 0xFFC8955A, 's', 0xFFFFE08A)), 1);

        save("textures/gui/hud/gender_male.png", glyph(new String[]{
                ".........",
                "....bbbb.",
                "......bb.",
                ".bbb.b.b.",
                "b...b....",
                "b...b....",
                "b...b....",
                ".bbb.....",
                ".........",
        }, Map.of('b', 0xFF4AA8FF)), 1);

        save("textures/gui/hud/gender_female.png", glyph(new String[]{
                "...rrr...",
                "..r...r..",
                "..r...r..",
                "..r...r..",
                "...rrr...",
                "....r....",
                "...rrr...",
                "....r....",
                ".........",
        }, Map.of('r', 0xFFFF6FA8)), 1);

        save("textures/gui/hud/weather_sun.png", glyph(new String[]{
                "....Y....",
                ".Y.....Y.",
                "...ooo...",
                "..oyyyo..",
                "Y.oyyyo.Y",
                "..oyyyo..",
                "...ooo...",
                ".Y.....Y.",
                "....Y....",
        }, Map.of('Y', 0xFFFFD23F, 'o', 0xFFF08A1C, 'y', 0xFFFFF3A0)), 1);

        save("textures/gui/hud/weather_rain.png", glyph(new String[]{
                ".........",
                "...ww....",
                ".wwwwwww.",
                "wwwwwwwww",
                ".ggggggg.",
                ".........",
                "..b..b..b",
                ".b..b..b.",
                ".........",
        }, Map.of('w', 0xFFF0F4FF, 'g', 0xFF9AA6C0, 'b', 0xFF3C8CFF)), 1);

        save("textures/gui/hud/weather_sand.png", glyph(new String[]{
                ".........",
                "sssssss..",
                ".......s.",
                ".....ss..",
                ".........",
                "ssssssss.",
                "........s",
                "dddd..ss.",
                ".........",
        }, Map.of('s', 0xFFE6C27A, 'd', 0xFFB08A48)), 1);

        save("textures/gui/hud/weather_snow.png", glyph(new String[]{
                "....c....",
                "..c.c.c..",
                "...ccc...",
                ".c..w..c.",
                "cccwwwccc",
                ".c..w..c.",
                "...ccc...",
                "..c.c.c..",
                "....c....",
        }, Map.of('c', 0xFF9EE7FF, 'w', 0xFFFFFFFF)), 1);

        save("textures/gui/hud/terrain_electric.png", glyph(new String[]{
                ".....yy..",
                "....yy...",
                "...yy....",
                "..yyyyy..",
                "....yy...",
                "...yy....",
                "..yy.....",
                ".yy......",
                ".........",
        }, Map.of('y', 0xFFFFE030)), 1);

        save("textures/gui/hud/terrain_grassy.png", glyph(new String[]{
                ".........",
                "....g....",
                ".g..g..g.",
                ".g..g..g.",
                ".gg.g.gg.",
                "..g.g.g..",
                "..ggggg..",
                "GGGGGGGGG",
                ".........",
        }, Map.of('g', 0xFF6EE05A, 'G', 0xFF2F9E3A)), 1);

        save("textures/gui/hud/terrain_misty.png", glyph(new String[]{
                ".........",
                "..pppp...",
                ".p....p..",
                ".p.pp.p..",
                ".p..p.p..",
                "..ppp.p..",
                "......p..",
                ".ppppp...",
                ".........",
        }, Map.of('p', 0xFFFF9AD5)), 1);

        save("textures/gui/hud/terrain_psychic.png", glyph(new String[]{
                ".........",
                ".........",
                "..uuuuu..",
                ".u..P..u.",
                "u..PPP..u",
                ".u..P..u.",
                "..uuuuu..",
                ".........",
                ".........",
        }, Map.of('u', 0xFFE45BFF, 'P', 0xFFFF4FA0)), 1);
    }

    // ------------------------------------------------------------------ Ícone do mod (Poké Bola com brilho, 16x16 ampliado 8x)

    private static void modIcon() throws IOException {
        save("icon.png", render(new String[]{
                ".....OOOOOO..Y..",
                "...OORRRRRROOY..",
                "..ORrrRRRRRYYSYY",
                ".ORrRRRRRRRRRYO.",
                ".ORRRRRRRRRRRYO.",
                "ORRRRRRRRRRRRRRO",
                "ORRRRRROORRRRRRO",
                "OOOOOOOwwOOOOOOO",
                "OWWWWWOwwOWWWWWO",
                "OWWWWWWOOWWWWWWO",
                "OWWWWWWWWWWWWWgO",
                ".OWWWWWWWWWWWgO.",
                ".OWWWWWWWWWWggO.",
                "..OWWWWWWWWggO..",
                "...OOWWWWggOO...",
                ".....OOOOOO.....",
        }, null, Map.of(
                'O', 0xFF1A1A1A, 'R', 0xFFE83838, 'r', 0xFFFF8A8A, 'W', 0xFFF4F4F4, 'g', 0xFFB8B8C8,
                'w', 0xFFFFFFFF, 'Y', 0xFFFFE14D, 'S', 0xFFFFFFFF), false), 8);
    }

    // ------------------------------------------------------------------ Renderização

    private static BufferedImage glyph(String[] rows, Map<Character, Integer> colors) {
        return render(rows, null, colors, true);
    }

    private static BufferedImage render(String[] rows, Body body, Map<Character, Integer> colors, boolean dropShadow) {
        int height = rows.length;
        int width = rows[0].length();
        for (String row : rows) {
            if (row.length() != width) throw new IllegalArgumentException("Linha com largura diferente: '" + row + "'");
        }

        boolean[][] outline = new boolean[height][width];
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                outline[y][x] = rows[y].charAt(x) == '#' && touchesEmpty(rows, x, y);
            }
        }

        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                char c = rows[y].charAt(x);
                if (c == '.') continue;
                int color;
                if (c == '#') {
                    if (body == null) throw new IllegalArgumentException("Sprite sem paleta de corpo usa '#'");
                    if (outline[y][x]) {
                        color = body.outline();
                    } else {
                        boolean lit = isOutline(outline, x, y - 1) || isOutline(outline, x - 1, y);
                        boolean dark = isOutline(outline, x, y + 1) || isOutline(outline, x + 1, y);
                        color = lit && !dark ? body.light() : dark && !lit ? body.shade() : body.base();
                    }
                } else {
                    Integer explicit = colors.get(c);
                    if (explicit == null) throw new IllegalArgumentException("Cor não definida para '" + c + "'");
                    color = explicit;
                }
                image.setRGB(x, y, color);
            }
        }

        if (dropShadow) {
            // Sombra de 1px para baixo/direita com 25% do brilho, igual à sombra da fonte do Minecraft
            BufferedImage shadowed = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    int argb = image.getRGB(x, y);
                    if ((argb >>> 24) == 0) continue;
                    if (x + 1 < width && y + 1 < height && (image.getRGB(x + 1, y + 1) >>> 24) == 0) {
                        shadowed.setRGB(x + 1, y + 1, darken(argb));
                    }
                }
            }
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    int argb = image.getRGB(x, y);
                    if ((argb >>> 24) != 0) shadowed.setRGB(x, y, argb);
                }
            }
            image = shadowed;
        }
        return image;
    }

    private static boolean touchesEmpty(String[] rows, int x, int y) {
        return isEmpty(rows, x - 1, y) || isEmpty(rows, x + 1, y) || isEmpty(rows, x, y - 1) || isEmpty(rows, x, y + 1);
    }

    private static boolean isEmpty(String[] rows, int x, int y) {
        return y < 0 || y >= rows.length || x < 0 || x >= rows[y].length() || rows[y].charAt(x) == '.';
    }

    private static boolean isOutline(boolean[][] outline, int x, int y) {
        return y >= 0 && y < outline.length && x >= 0 && x < outline[y].length && outline[y][x];
    }

    private static int darken(int argb) {
        int r = ((argb >> 16) & 0xFF) / 4;
        int g = ((argb >> 8) & 0xFF) / 4;
        int b = (argb & 0xFF) / 4;
        return 0xFF000000 | (r << 16) | (g << 8) | b;
    }

    private static void save(String path, BufferedImage image, int scale) throws IOException {
        BufferedImage output = image;
        if (scale > 1) {
            output = new BufferedImage(image.getWidth() * scale, image.getHeight() * scale, BufferedImage.TYPE_INT_ARGB);
            for (int y = 0; y < output.getHeight(); y++) {
                for (int x = 0; x < output.getWidth(); x++) {
                    output.setRGB(x, y, image.getRGB(x / scale, y / scale));
                }
            }
        }
        File file = new File(OUT + path);
        file.getParentFile().mkdirs();
        ImageIO.write(output, "png", file);
        System.out.println("  " + path + " (" + output.getWidth() + "x" + output.getHeight() + ")");
    }
}
