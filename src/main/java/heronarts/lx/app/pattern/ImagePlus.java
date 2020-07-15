package heronarts.lx.app.pattern;

import heronarts.lx.LX;
import heronarts.lx.model.LXModel;
import heronarts.lx.model.LXPoint;
import heronarts.lx.parameter.*;
import heronarts.lx.studio.LXStudio;
import heronarts.lx.studio.ui.device.UIDevice;
import heronarts.lx.studio.ui.device.UIDeviceControls;
import heronarts.p3lx.pattern.P3LXPattern;
import heronarts.p3lx.ui.UI;
import heronarts.p3lx.ui.UI2dContainer;
import heronarts.p3lx.ui.component.UIButton;
import heronarts.p3lx.ui.component.UIItemList;
import heronarts.p3lx.ui.component.UIKnob;
import heronarts.p3lx.ui.component.UISwitch;
import heronarts.p3lx.ui.component.UITextBox;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import processing.core.PConstants;
import processing.core.PGraphics;
import processing.core.PImage;

public class ImagePlus extends P3LXPattern implements UIDeviceControls<ImagePlus> {
  private static final Logger logger = Logger.getLogger(ImagePlus.class.getName());

  public final CompoundParameter fpsKnob =
      new CompoundParameter("Fps", 1.0, 61.0)
          .setDescription("Controls the frames per second.");
  public final BooleanParameter antialiasKnob =
      new BooleanParameter("antialias", true);
  public final StringParameter imgKnob =
      new StringParameter("img", "")
          .setDescription("Texture image.");
  public final BooleanParameter tileKnob = new BooleanParameter("tile", false);
  public final BooleanParameter scanKnob = new BooleanParameter("scan", false);
  public final CompoundParameter viewportScale = new CompoundParameter("scale", 10f, 1f, 200f);
  public final CompoundParameter speed = new CompoundParameter("speed", 1f, 1f, 20f);

  protected List<FileItem> fileItems = new ArrayList<FileItem>();
  protected UIItemList.ScrollList fileItemList;
  protected List<String> imgFiles;
  private static final int CONTROLS_MIN_WIDTH = 260;

  private static final String[] IMG_EXTS = {".gif", ".png", ".jpg"};

  protected PImage image;
  protected PImage tileImage;
  protected int renderTargetWidth = 30;
  protected int renderTargetHeight = 30;
  protected String filesDir;  // Must end in a '/'
  protected boolean includeAntialias;
  protected int paddingX;
  protected int numTiles;
  protected PGraphics pg;
  protected double currentFrame = 0.0;
  protected double previousFrame = -1.0;
  protected double deltaDrawMs = 0.0;
  protected LXParameterListener knobListener;

  //
  // Scan related parameters
  //
  protected int xOffset = 0;
  protected int yOffset = 0;
  protected boolean movingVertically = false;
  protected boolean movingForwards = true;
  protected int verticalMovement = 0;
  protected int scanViewportWidth = 100;
  protected int scanViewportHeight = 100;

  public ImagePlus(LX lx) {
    super(lx);
    String filesDir = ".";
    String defaultFile = "background.jpg";
    boolean includeAntialias = false;
    boolean scan = true;

    if (!filesDir.endsWith("/")) {
      filesDir = filesDir + "/";
    }
    this.filesDir = filesDir;
    this.includeAntialias = includeAntialias;
    reloadFileList();
    pg = applet.createGraphics(renderTargetWidth, renderTargetHeight);

    addParameter("scan", scanKnob);
    scanKnob.setValue(scan);
    addParameter("fps", fpsKnob);
    if (includeAntialias) {
      addParameter("antialias", antialiasKnob);
    }
    addParameter("img", imgKnob);

    knobListener = new LXParameterListener() {
      @Override
      public void onParameterChanged(LXParameter parameter) {
        StringParameter iKnob = (StringParameter) parameter;
        loadImg(iKnob.getString());
      }
    };

    imgKnob.addListener(knobListener);

    imgKnob.setValue(defaultFile);
    addParameter("tile", tileKnob);
    addParameter("vportscale",viewportScale);
    addParameter("speed", speed);
  }

  @Override
  public void dispose() {
    imgKnob.removeListener(knobListener);
    super.dispose();
  }

  private void loadImg(String imgname) {
    logger.info("Loading image: " + imgname);
    tileImage = applet.loadImage(filesDir + imgname);
    if (!tileKnob.getValueb()) {
      if (!scanKnob.getValueb()) {
        tileImage.resize(renderTargetWidth, renderTargetHeight);
        image = tileImage;
      } else {
        // Don't resize when we are scanning, we will just move a pg.width,pg.height rectangle around
        image = tileImage;
      }
    } else {
      // Tile the image to fill the space horizontally.  Scale the image vertically
      // to fit.
      float yScale = renderTargetHeight / tileImage.height;
      tileImage.resize((int)(tileImage.width * yScale), renderTargetHeight);
      tileImage.loadPixels();
      logger.info("tileImage.width=" + tileImage.width + " tileImage.height=" + tileImage.height);
      numTiles = renderTargetWidth / tileImage.width;
      int remainderPixelsX = renderTargetWidth - (numTiles * tileImage.width);
      // No vertical padding right now int paddingY = imageHeight - image.height;
      paddingX = remainderPixelsX / (numTiles+1);
      logger.info("Tiling image: " + imgname + " numTiles=" + numTiles + " paddingX=" + paddingX);
      pg.beginDraw();
      pg.background(0);

      for (int i = 0; i < numTiles; i++) {
        pg.image(tileImage, i * tileImage.width + (i +1) * paddingX, 0);
      }

      pg.endDraw();
      pg.updatePixels();
      pg.loadPixels();
      image = pg;
    }
  }

  @Override
  public void run(double deltaMs) {
    double fps = fpsKnob.getValue();
    currentFrame += (deltaMs / 1000.0) * fps;
    deltaDrawMs += deltaMs;
    if ((int) currentFrame > previousFrame) {
      try {
        renderToPoints();
      } catch (ArrayIndexOutOfBoundsException ex) {
        // "handle" race condition while reloading.
      }
      previousFrame = (int) currentFrame;
      deltaDrawMs = 0.0;
    }
  }

  protected void renderToPoints() {
    if (scanKnob.getValueb()) {
      renderToPointsScan();
    } else {
      for (LXPoint p : model.points) {
        colors[p.index] = pointToImageColor(p, image);
      }
    }
  }

  protected void renderToPointsScan() {
    for (LXPoint p : model.points) {
      colors[p.index] = pointToImageColorViewport(p, image, xOffset, yOffset);
    }

    if (!movingVertically) {
      if (movingForwards) xOffset += speed.getValuef();
      else xOffset -= speed.getValuef();
      if (xOffset >= tileImage.width - scanViewportWidth * viewportScale.getValuef()) {
        movingForwards = false;
        movingVertically = true;
      } else if (xOffset < 0) {
        movingForwards = true;
        movingVertically = true;
      }
    } else {
      yOffset += speed.getValuef();
      verticalMovement += speed.getValuef();
      if (verticalMovement > scanViewportHeight * viewportScale.getValuef()) {
        verticalMovement = 0;
        movingVertically = false;
      }
      if (yOffset + scanViewportHeight * viewportScale.getValuef() >= tileImage.height) {
        yOffset = 0;
        xOffset = 0;
      }
    }
  }

  public int pointToImageColorViewport(LXPoint p, PImage img, int xOffset, int yOffset) {
    float tCoordinates[] = pointToNormalizedCoordinates(p);
    float imgXCoord = tCoordinates[0] * (scanViewportWidth * viewportScale.getValuef() - 1);
    // LXModel coordinates are world space where increasing Y is pointing up.  Image coordinates
    // have an increasing Y going down the screen so flip the orientation by subtracting the
    // parameterized (0-1) LXPoint y coordinate from 1 (i.e. 1 in world space becomes 0
    // in image space and 0 in world space becomes 1 in image space).
    float imgYCoord = (1f - tCoordinates[1]) * (scanViewportHeight * viewportScale.getValuef() - 1);

    return img.get((int)Math.ceil(imgXCoord) + xOffset, (int)Math.ceil(imgYCoord) + yOffset);
  }

  /**
   * Naive implementation of an image sampler.  This simply rounds the LXPoint's location to the nearest
   * image pixel.
   * @param p
   * @param img
   * @return
   */
  public int pointToImageColor(LXPoint p, PImage img) {
    float tCoordinates[] = pointToNormalizedCoordinates(p);
    float imgXCoord = tCoordinates[0] * (img.width - 1);
    // LXModel coordinates are world space where increasing Y is pointing up.  Image coordinates
    // have an increasing Y going down the screen so flip the orientation by subtracting the
    // parameterized (0-1) LXPoint y coordinate from 1 (i.e. 1 in world space becomes 0
    // in image space and 0 in world space becomes 1 in image space).
    float imgYCoord = (1f - tCoordinates[1]) * (img.height - 1);
    return img.get((int)Math.ceil(imgXCoord), (int)Math.ceil(imgYCoord));
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

  protected void reloadFileList() {
    imgFiles = findImageFiles(filesDir, IMG_EXTS);
    fileItems.clear();
    for (String filename : imgFiles) {
      // Use a name that's suitable for the knob
      int index = filename.lastIndexOf('/');
      if (index >= 0) {
        filename = filename.substring(index + 1);
      }
      fileItems.add(new FileItem(filename));
    }
    if (fileItemList != null) {
      fileItemList.setItems(fileItems);
    }
  }

  public void buildDeviceControls(LXStudio.UI ui, UIDevice device, ImagePlus imagePlus) {
    device.setContentWidth(CONTROLS_MIN_WIDTH);
    device.setLayout(UI2dContainer.Layout.VERTICAL);
    device.setPadding(3, 3, 3, 3);

    UI2dContainer knobsContainer = new UI2dContainer(0, 0, device.getWidth(), 45);
    knobsContainer.setLayout(UI2dContainer.Layout.HORIZONTAL);
    knobsContainer.setPadding(3, 3, 3, 3);
    new UIKnob(fpsKnob).addToContainer(knobsContainer);
    if (includeAntialias) {
      UISwitch antialiasButton = new UISwitch(0, 0);
      antialiasButton.setParameter(antialiasKnob);
      antialiasButton.setMomentary(false);
      antialiasButton.addToContainer(knobsContainer);
    }

    new UIKnob(viewportScale).addToContainer(knobsContainer);
    new UIKnob(speed).addToContainer(knobsContainer);

    // We need to reload the image if the tile button is selected.  For tiled images, we build
    // an intermediate PGraphics object and tile the selected image into that and then use it
    // as our base PImage 'image'.
    new UIButton() {
      @Override
      public void onToggle(boolean on) {
        // Need to reload the image
        loadImg(imgKnob.getString());
      }
    }.setParameter(tileKnob).setLabel("tile").setTextOffset(0, 20)
        .setWidth(28).setHeight(25).addToContainer(knobsContainer);

    new UIButton() {
      @Override
      public void onToggle(boolean on) {
        if (on) {
          reloadFileList();
        }
      }
    }.setLabel("rescan dir")
        .setMomentary(true)
        .setWidth(60)
        .setHeight(25)
        .addToContainer(knobsContainer);

    knobsContainer.addToContainer(device);

    UI2dContainer filenameEntry = new UI2dContainer(0, 0, device.getWidth(), 30);
    filenameEntry.setLayout(UI2dContainer.Layout.HORIZONTAL);

    fileItemList =  new UIItemList.ScrollList(ui, 0, 5, CONTROLS_MIN_WIDTH, 80);
    new UITextBox(0, 0, device.getContentWidth() - 22, 20)
        .setParameter(imgKnob)
        .setTextAlignment(PConstants.LEFT)
        .addToContainer(filenameEntry);


    // Button for reloading image file list.
    new UIButton(device.getContentWidth() - 20, 0, 20, 20) {
      @Override
      public void onToggle(boolean on) {
        if (on) {
          loadImg(imgKnob.getString());
        }
      }
    }.setLabel("\u21BA")
        .setMomentary(true)
        .addToContainer(filenameEntry);
    filenameEntry.addToContainer(device);

    fileItemList =  new UIItemList.ScrollList(ui, 0, 5, CONTROLS_MIN_WIDTH, 80);
    fileItemList.setShowCheckboxes(false);
    fileItemList.setItems(fileItems);
    fileItemList.addToContainer(device);
  }

  public class FileItem extends FileItemBase {
    FileItem(String filename) {
      super(filename);
    }
    public void onActivate() {
      imgKnob.setValue(filename);
      loadImg(filename);
    }
  }

  /**
   * Utility base class to clean up all the patterns that have file
   * inputs.
   */
  class FileItemBase extends UIItemList.Item {
    protected final String filename;

    public FileItemBase(String str) {
      this.filename = str;
    }
    public boolean isActive() {
      return false;
    }
    public int getActiveColor(UI ui) {
      return ui.theme.getAttentionColor();
    }
    public String getLabel() {
      return filename;
    }
  }


  public static List<String> findImageFiles(String path, final String[] exts) {
    List<String> filenames = new ArrayList<String>();
    File dir = new File(path);
    File[] files = dir.listFiles(
        (directory, name) -> {
        for (String ext : exts) {
          if (name.toLowerCase().endsWith(ext)) {
            return true;
          }
        }
        return false;
      }
    );
    for (File file : files) {
      filenames.add(file.getName());
    }
    return filenames;
  }
}