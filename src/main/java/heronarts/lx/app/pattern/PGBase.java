package heronarts.lx.app.pattern;

import heronarts.lx.LX;
import heronarts.lx.model.LXModel;
import heronarts.lx.model.LXPoint;
import heronarts.lx.parameter.CompoundParameter;
import heronarts.p3lx.pattern.P3LXPattern;
import processing.core.PGraphics;
import processing.core.PImage;

import java.util.Random;

abstract public class PGBase extends P3LXPattern {
  public final CompoundParameter fpsKnob =
      new CompoundParameter("Fps", 60, 0.0, 60)
          .setDescription("Controls the frames per second.");

  protected PGraphics pg;

  protected double currentFrame = 0.0;
  protected int previousFrame = -1;
  protected double deltaDrawMs = 0.0;

  /** Indicates whether {@link #setup()} has been called. */
  private boolean setupCalled;
  // TODO: Fix this whole pattern lifecycle thing

  /** For subclasses to use. It's better to have one source. */
  protected static final Random random = new Random();

  public PGBase(LX lx, int width, int height) {
    super(lx);
    pg = applet.createGraphics(width, height);
    addParameter(fpsKnob);
  }

  /**
   * Subclasses <em>must</em> call {@code super.onInactive()}.
   */
  @Override
  public final void onInactive() {
    setupCalled = false;
    tearDown();
  }

  @Override
  public void run(double deltaMs) {
    if (!setupCalled) {
      pg.beginDraw();
      setup();
      pg.endDraw();
      setupCalled = true;
    }

    double fps = fpsKnob.getValue();
    currentFrame += (deltaMs / 1000.0) * fps;
    // We don't call draw() every frame so track the accumulated deltaMs for them.
    deltaDrawMs += deltaMs;
    if ((int) currentFrame > previousFrame) {
      // Time for new frame.  Draw
      // if glThread == null this is the default Processing renderer so it is always
      // okay to draw.  If it is not-null, we need to make sure the pattern is
      // executing on the glThread or else Processing will crash.
      // UPDATE: Removed this code because Processing already makes a best effort,
      //         and, in addition, the program crashes anyway if multithreading is
      //         set to 'true'.
      pg.beginDraw();
      draw(deltaDrawMs);
      pg.endDraw();

      previousFrame = (int) currentFrame;
      deltaDrawMs = 0.0;
    }
    // Don't let current frame increment forever.  Otherwise float will
    // begin to lose precision and things get wonky.
    if (currentFrame > 10000.0) {
      currentFrame = 0.0;
      previousFrame = -1;
    }
    renderToPoints();
  }

  protected void renderToPoints() {
    for (LXPoint p : model.points) {
      colors[p.index] = pointToImageColor(lx, p, pg);
    }
  }

  /**
   * Naive implementation of an image sampler.  This simply rounds the LXPoint's location to the nearest
   * image pixel.
   * @param p
   * @param img
   * @return
   */
  static public int pointToImageColor(LX lx, LXPoint p, PImage img) {
    float tCoordinates[] = pointToNormalizedCoordinates(lx, p);
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
  static public float[] pointToNormalizedCoordinates(LX lx, LXPoint p) {
    float[] coordinates = {0f, 0f};
    LXModel model = lx.getModel();
    coordinates[0] = (p.x - model.xMin)/(model.xRange);
    coordinates[1] = (p.y - model.yMin)/(model.yRange);
    return coordinates;
  }

  /**
   * Called once before all the draw calls, similar to how a Processing sketch has a setup()
   * call. onActive()/onInactive() call timings appear not to be able to be treated the same
   * as conceptual setup() and tearDown() calls.
   * <p>
   * Calls to {@link PGraphics#beginDraw()} and {@link PGraphics#endDraw()} will surround a call
   * to this method.</p>
   */
  protected void setup() {
  }

  /**
   * Called when {@link #onInactive()} is called. That method has been made {@code final}
   * so that it can guarantee {@link #setup()} is called. This may change in the future.
   */
  protected void tearDown() {
  }

  // Implement PGGraphics drawing code here.  PGTexture handles beginDraw()/endDraw();
  protected abstract void draw(double deltaDrawMs);
}
