package heronarts.lx.app.pattern;

import heronarts.lx.LX;
import heronarts.lx.model.LXPoint;
import heronarts.lx.parameter.*;
import heronarts.lx.studio.LXStudio;
import heronarts.lx.studio.ui.device.UIDevice;
import heronarts.lx.studio.ui.device.UIDeviceControls;
import heronarts.p3lx.pattern.P3LXPattern;
import heronarts.p3lx.ui.UI2dContainer;
import heronarts.p3lx.ui.component.*;
import processing.core.PApplet;
import processing.core.PConstants;
import processing.core.PImage;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import static processing.core.PConstants.ARGB;

public class AnimatedGIF extends P3LXPattern implements UIDeviceControls<AnimatedGIF> {
  private static final Logger logger = Logger.getLogger(AnimatedGIF.class.getName());

  private final CompoundParameter fpsKnob =
      new CompoundParameter("Fps", 1.0, 60.0).setDescription("Controls the frames per second.");
  private final CompoundParameter scaleAmt =
      new CompoundParameter("sAmt", 1.0, 0.01, 2.0).setDescription("Scale src image amount");
  private final BooleanParameter scaleSrc =
      new BooleanParameter("scale", true).setDescription("Whether to scale source image");
  private final BooleanParameter fitSrc =
      new BooleanParameter("fitSrc", true).setDescription("Fit src image to output");
  private final StringParameter gifKnob =
      new StringParameter("gif", "").setDescription("Animated gif");

  private List<FileItem> fileItems = new ArrayList<>();
  private UIItemList.ScrollList fileItemList;

  private static final int CONTROLS_MIN_WIDTH = 200;
  private LXParameterListener filenameListener;

  private PImage[] images;
  private double currentFrame = 0.0;
  // Our render target should be twice the pixel dimensions of the bounding box since each
  // successive vertical strip of LEDs is vertically offset by half the pitch.  By doubling the
  // rendering target resolution, we should be able to have the offset led pixel land on an original
  // pixel in our image.
  private int renderTargetWidth = 200;  // This should be 2x the vertical lines
  private int renderTargetHeight = 200;        // This should be 2x the horizontal lines.
  private String filesDir;  // Must end in a '/'

  public AnimatedGIF(LX lx) {
    super(lx);

    filesDir = "./";
    reloadFileList();

    addParameter("fps", fpsKnob);
    addParameter("samt", scaleAmt);
    addParameter("scale", scaleSrc);
    addParameter("fit", fitSrc);

    addParameter("file", gifKnob);
    filenameListener = new LXParameterListener() {
      @Override
      public void onParameterChanged(LXParameter parameter) {
        loadGif(((StringParameter)parameter).getString());
      }
    };
    gifKnob.addListener(filenameListener);
    fpsKnob.setValue(10);
  }

  @Override
  public void dispose() {
    gifKnob.removeListener(filenameListener);
    super.dispose();
  }

  /**
   * @param gifname the sprite's name, not including parent paths or the ".gif" suffix
   */
  private void loadGif(String gifname) {
    logger.info("Loading gif: " + gifname);
    PImage[] newImages = loadSprite(applet, filesDir + gifname + ".gif");
    logger.info("frames: " + newImages.length);
    if (scaleSrc.getValueb()) {
      for (PImage image : newImages) {
        if (fitSrc.getValueb())
          image.resize(renderTargetWidth, renderTargetHeight);
        else {
          image.resize((int) ((float) image.width * scaleAmt.getValue()),
              (int) ((float) image.height * scaleAmt.getValue()));
        }
      }
    }
    // minimize race condition when reloading.
    images = newImages;
  }

  public void run(double deltaMs) {
    double fps = fpsKnob.getValue();
    currentFrame += (deltaMs / 1000.0) * fps;
    if (images == null) return;
    if (currentFrame >= images.length) {
      currentFrame -= images.length;
    }
    try {
      renderToPoints();
    } catch (ArrayIndexOutOfBoundsException ex) {
      // Sometimes caused by race condition when reloading, just skip a frame.
    }
  }

  // TODO(tracy): There should be an ImageUtils class for the sampling stuff.
  private void renderToPoints() {
    for (LXPoint p : model.points) {
      colors[p.index] = ImagePlus.pointToImageColor(lx, p, images[(int) currentFrame]);
    }
  }

  /**
   * Loads a sprite from the given path. This does not append ".gif" to the path.
   * If there was any kind of loading error, then this returns only the images
   * that were successfully loaded.
   *
   * @return the sprite's sequence of images.
   */
  private static PImage[] loadSprite(PApplet applet, String path) {
    ArrayList<PImage> frames = new ArrayList<>();

    // gifAnimator isn't written well to handle exceptions properly :(
    try (InputStream in = applet.createInput(path)) {
      if (in == null) {
        return new PImage[0];
      }
      Iterator<ImageReader> iter = ImageIO.getImageReadersByFormatName("gif");
      if (!iter.hasNext()) {
        return new PImage[0];
      }
      ImageReader r = iter.next();
      try (ImageInputStream iis = ImageIO.createImageInputStream(in)) {
        r.setInput(iis);
        int count = r.getNumImages(true);
        for (int i = 0; i < count; i++) {
          BufferedImage img = r.read(i);
          // NOTE: The PImage(java.awt.Image) constructor may not respect alpha
          PImage pImg = applet.createImage(img.getWidth(), img.getHeight(), ARGB);
          frames.add(pImg);
          pImg.loadPixels();
          img.getRGB(0, 0, img.getWidth(), img.getHeight(), pImg.pixels, 0, img.getWidth());
          pImg.updatePixels();
        }
      }
    } catch (IOException | RuntimeException ex) {
      // There's a potential "ArrayIndexOutOfBoundsException: 4096" from ImageIO loading GIFs
      logger.log(Level.SEVERE, "Error loading sprite: " + path, ex);
    }

    return frames.toArray(new PImage[0]);
  }

  public void buildDeviceControls(LXStudio.UI ui, UIDevice device, AnimatedGIF animatedGIF) {
    device.setContentWidth(CONTROLS_MIN_WIDTH);
    device.setLayout(UI2dContainer.Layout.VERTICAL);
    device.setPadding(3, 3, 3, 3);

    UI2dContainer knobsContainer = new UI2dContainer(0, 30, device.getWidth(), 45);
    knobsContainer.setLayout(UI2dContainer.Layout.HORIZONTAL);
    knobsContainer.setPadding(1, 1, 1, 1);
    new UIKnob(fpsKnob).addToContainer(knobsContainer);
    new UIButton(CONTROLS_MIN_WIDTH, 10, 30, 20) {
      @Override
      public void onToggle(boolean on) {
        if (on) {
          reloadFileList();
        }
      }
    }.setLabel("rescn").setMomentary(true).addToContainer(knobsContainer);

    new UIKnob(scaleAmt).addToContainer(knobsContainer);

    UISwitch scaleButton = new UISwitch(0, 0);
    scaleButton.setParameter(scaleSrc);
    scaleButton.setMomentary(false);
    scaleButton.addToContainer(knobsContainer);
    UISwitch fitButton = new UISwitch(0, 0);
    fitButton.setParameter(fitSrc);
    fitButton.setMomentary(false);
    fitButton.addToContainer(knobsContainer);
    knobsContainer.addToContainer(device);

    UI2dContainer filenameEntry = new UI2dContainer(0, 0, device.getWidth(), 30);
    filenameEntry.setLayout(UI2dContainer.Layout.HORIZONTAL);

    fileItemList = new UIItemList.ScrollList(ui, 0, 5, CONTROLS_MIN_WIDTH, 50);
    new UITextBox(0, 0, device.getContentWidth() - 22, 20)
        .setParameter(gifKnob)
        .setTextAlignment(PConstants.LEFT)
        .addToContainer(filenameEntry);

    new UIButton(device.getContentWidth() - 20, 0, 20, 20) {
      @Override
      public void onToggle(boolean on) {
        if (on) {
          loadGif(gifKnob.getString());
        }
      }
    }.setLabel("\u21BA").setMomentary(true).addToContainer(filenameEntry);
    filenameEntry.addToContainer(device);

    fileItemList = new UIItemList.ScrollList(ui, 0, 5, CONTROLS_MIN_WIDTH, 80);
    fileItemList.setShowCheckboxes(false);
    fileItemList.setItems(fileItems);
    fileItemList.addToContainer(device);
  }

  public class FileItem extends ImagePlus.FileItemBase {
    FileItem(String filename) {
      super(filename);
    }

    public void onActivate() {
      gifKnob.setValue(filename);
      loadGif(filename);
    }
  }

  private void reloadFileList() {
    String[] exts = {".gif"};
    List<String> gifFiles = ImagePlus.findImageFiles(filesDir, exts);
    fileItems.clear();
    for (String filename : gifFiles) {
      // Use a name that's suitable for the knob
      int index = filename.lastIndexOf('/');
      if (index >= 0) {
        filename = filename.substring(index + 1);
      }
      index = filename.lastIndexOf('.');
      if (index >= 0) {
        filename = filename.substring(0, index);
      }
      fileItems.add(new FileItem(filename));
    }
    if (fileItemList != null) fileItemList.setItems(fileItems);
  }
}