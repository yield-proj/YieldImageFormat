package com.xebisco.yif;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.zip.*;

public class YIF {
    public static String toUncompressedYifFile(BufferedImage image) {
        StringBuilder pixelsPart = new StringBuilder(), colorsPart = new StringBuilder(), resolutionPart = new StringBuilder();
        Map<Integer, Integer> colors = new HashMap<>();
        MathContext mathContext = new MathContext(9, RoundingMode.DOWN);
        int width = image.getWidth(), height = image.getHeight();
        resolutionPart.append(width).append('x').append(height);
        int lastRgb = 0;
        for (int i = 0; i < width * height; i++) {
            int y = (new BigDecimal(i / width, mathContext)).intValue(), x = i - width * y, rgb = image.getRGB(x, y);
            if (!colors.containsKey(rgb)) {
                int color = colors.size();
                colors.put(rgb, color);
                colorsPart.append(color).append("\6").append(rgb).append(" ");
            }
            if (lastRgb != rgb)
                pixelsPart.append(colors.get(rgb)).append('\3').append(i).append('\2');
            lastRgb = rgb;
        }
        return resolutionPart.toString() + '\n' + colorsPart + '\n' + '\1' + pixelsPart;
    }

    public static String toYifFile(BufferedImage image) {
        Deflater def = new Deflater(9);
        byte[] sbytes = toUncompressedYifFile(image).getBytes(StandardCharsets.UTF_8);
        def.setInput(sbytes);
        def.finish();
        byte[] buffer = new byte[sbytes.length];
        int n = def.deflate(buffer);
        return new String(buffer, 0, n, StandardCharsets.ISO_8859_1)
                + "*" + sbytes.length;
    }

    public static void toYifFile(BufferedImage image, OutputStream outputStream) throws IOException {
        DeflaterOutputStream dos = new DeflaterOutputStream(outputStream);
        String f = toUncompressedYifFile(image);
        for (char c : f.toCharArray()) {
            dos.write(c);
        }
        dos.close();
        dos.flush();
    }

    public static void toYifFile(BufferedImage image, File file) throws IOException {
        toYifFile(image, new FileOutputStream(file));
    }

    public static BufferedImage fromYifFile(InputStream inputStream) throws IOException {
        StringBuilder out = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(new InflaterInputStream(inputStream)))) {
            String line;
            while ((line = reader.readLine()) != null) {
                out.append(line).append('\n');
            }
        }
        out.setLength(out.length() - 1);
        return fromUncompressedYifFile(out.toString());
    }

    public static BufferedImage fromYifFile(File file) throws IOException {
        return fromYifFile(new FileInputStream(file));
    }

    public static BufferedImage fromYifFile(String yifContents) {
        int pos = yifContents.lastIndexOf('*');
        int len = Integer.parseInt(yifContents.substring(pos + 1));
        yifContents = yifContents.substring(0, pos);

        Inflater inf = new Inflater();
        byte[] buffer = yifContents.getBytes(StandardCharsets.ISO_8859_1);
        byte[] decomp = new byte[len];
        inf.setInput(buffer);
        try {
            inf.inflate(decomp, 0, len);
            inf.end();
        } catch (DataFormatException e) {
            throw new IllegalArgumentException(e);
        }
        return fromUncompressedYifFile(new String(decomp, StandardCharsets.UTF_8));
    }

    public static BufferedImage fromUncompressedYifFile(String yifContents) {
        String[] firstSections = yifContents.split("\n");
        Map<Integer, Color> colors = new HashMap<>();
        String[] colorsP = firstSections[1].split(" ");
        for (String c : colorsP) {
            String[] pcs = c.split("\6");
            colors.put(Integer.parseInt(pcs[0]), new Color(Integer.parseInt(pcs[1])));
        }
        String[] res = firstSections[0].split("x");
        int width = Integer.parseInt(res[0]), height = Integer.parseInt(res[1]), pixelSectionStart = yifContents.indexOf('\1');
        String pixelSection = yifContents.substring(pixelSectionStart + 1);
        String[] pixels = pixelSection.split("\2");
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics g = image.getGraphics();
        class Coordinate {
            final int x, y;

            Coordinate(int x, int y) {
                this.x = x;
                this.y = y;
            }

            @Override
            public boolean equals(Object o) {
                if (this == o) return true;
                if (o == null || getClass() != o.getClass()) return false;
                Coordinate that = (Coordinate) o;
                return x == that.x && y == that.y;
            }

            @Override
            public int hashCode() {
                return Objects.hash(x, y);
            }
        }
        Map<Coordinate, Color> coordinateColorMap = new HashMap<>();
        MathContext mathContext = new MathContext(9, RoundingMode.DOWN);
        for (String s : pixels) {
            String[] pixel = s.split("\3");
            int color = Integer.parseInt(pixel[0]);
            int pos = Integer.parseInt(pixel[1]);
            int y = (new BigDecimal(pos / width, mathContext)).intValue(), x = pos - width * y;
            coordinateColorMap.put(new Coordinate(x, y), colors.get(color));
        }
        for (int y = 0; y < height; y++)
            for (int x = 0; x < width; x++) {
                Coordinate coordinate = new Coordinate(x, y);
                if (coordinateColorMap.containsKey(coordinate))
                    g.setColor(coordinateColorMap.get(coordinate));
                g.fillRect(x, y, 1, 1);
            }
        return image;
    }
}
