package heronarts.lx.app.pattern;

import heronarts.lx.LX;
import heronarts.lx.model.LXModel;
import heronarts.lx.model.LXPoint;
import heronarts.lx.pattern.LXPattern;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.FileInputStream;
import java.io.InputStream;

public class ImgPattern extends LXPattern {
  String filename = "background.jpg";
  BufferedImage img;

  public ImgPattern(LX lx) {
    super(lx);
    img = loadImageIO(filename);
  }

  @Override
  public void run(double deltaMs) {
    for (LXPoint p : model.points) {
      colors[p.index] = pointToImageColor(p, img);
    }
  }

  /**
   * Naive implementation of an image sampler.  This simply rounds the LXPoint's location to the nearest
   * image pixel.
   * @param p
   * @param img
   * @return
   */
  public int pointToImageColor(LXPoint p, BufferedImage img) {
    float tCoordinates[] = pointToNormalizedCoordinates(p);
    float imgXCoord = tCoordinates[0] * (img.getWidth() - 1);
    // LXModel coordinates are world space where increasing Y is pointing up.  Image coordinates
    // have an increasing Y going down the screen so flip the orientation by subtracting the
    // parameterized (0-1) LXPoint y coordinate from 1 (i.e. 1 in world space becomes 0
    // in image space and 0 in world space becomes 1 in image space).
    float imgYCoord = (1f - tCoordinates[1]) * (img.getHeight() - 1);
    return img.getRGB((int)Math.ceil(imgXCoord), (int)Math.ceil(imgYCoord));
  }

  /**
   * Given an LXPoint, compute it's normalized coordinates with respect to the model bounds.
   * These will be between 0 and 1.
   * @param p The
   * @return A float array containing the tx,ty normalized coordinates (from 0 to 1).
   */
  public float[] pointToNormalizedCoordinates(LXPoint p) {
    float[] coordinates = {0f, 0f};
    LXModel model = lx.getModel();
    coordinates[0] = (p.x - model.xMin)/(model.xRange);
    coordinates[1] = (p.y - model.yMin)/(model.yRange);
    return coordinates;
  }

  /**
   * Use Java ImageIO directly instead of PApplet.loadImage().  Allows for headless use.
   * 
   * @param filename
   * @return
   */
  public BufferedImage loadImageIO(String filename) {
    try {
      InputStream stream = new FileInputStream(filename);
      if (stream == null) {
        System.err.println("The image " + filename + " could not be found.");
        return null;
      }
      BufferedImage bi = ImageIO.read(stream);
      stream.close();
      return bi;
    } catch (Exception e) {
      System.err.println("Exception loading image: " + e.getMessage());
      return null;
    }
  }
}
