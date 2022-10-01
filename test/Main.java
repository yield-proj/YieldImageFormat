import com.xebisco.yif.YIF;

import javax.imageio.ImageIO;
import java.io.File;
import java.io.IOException;
import java.util.Objects;

public class Main {
    public static void main(String[] args) throws IOException {
        File file = new File("test/test.yif");
        YIF.toYifFile(ImageIO.read(Objects.requireNonNull(Main.class.getResourceAsStream("yieldLogo.png"))), file);
        ImageIO.write(YIF.fromYifFile(file), "PNG", new File("test/test.png"));
    }
}
