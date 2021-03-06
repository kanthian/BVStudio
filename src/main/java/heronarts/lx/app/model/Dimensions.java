package heronarts.lx.app.model;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import heronarts.lx.LX;
import heronarts.lx.color.LXColor;
import heronarts.lx.structure.JsonFixture;
import heronarts.p3lx.ui.UI;
import heronarts.p3lx.ui.UI3dComponent;
import processing.core.PConstants;
import processing.core.PGraphics;
import processing.core.PVector;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;

import static processing.core.PConstants.TRIANGLE_STRIP;

/**
 * Various installation dimensions and dimension related utility functions.
 * 600" across, 438" to the peak.  Peak offset is 7'9" or 93".
 *
 * All units are in feet.
 */
public class Dimensions {
  public static final float SUBRECT_WIDTH = 600f/12f;
  public static final float PEAK_HEIGHT = 438f/12f; // 36' 6" or 438"
  public static final float PEAK_OFFSET = 93f/12f; // 7'9" or 7.75 ft or 93" 83f/12f;
  public static final float SUBRECT_HEIGHT = 438f/12f - PEAK_OFFSET; // Also 28' 9" or 345" also 438 - 93
  public static final float FLAT_ROOF_MARGIN = 25f/12f;
  public static final float ROOF_SLOPE_RUN = 289f/12f;
  public static final float ROOF_SLOPE = 18f;  // degrees
  public static final float SIDE_MARGIN = 6f/12f;
  public static final float BIG_FIRST_COLUMN_EPSILON = 3f;
  public static final float TOP_MARGIN = (6f-BIG_FIRST_COLUMN_EPSILON)/12f;
  public static final float BOTTOM_MARGIN = 6f/12f;
  public static final float verticalSpacing = 6f/12f;
  public static final float horizontalSpacing = 6f/12f;
  public static final float verticalStagger = 3f/12f;
  public static final float DEFAULT_Y_OFFSET = -9.0f;



  /**
   * Given a point along the x-dimension, compute the height to the roof, accounting
   * for outside margins and roof slope.
   *
   *             peak
   *              / |
   *     height /   |
   *          / .   |
   * margin /   .   |
   * ----------------
   * corner^    ^ xPos
   *
   * @param xPos
   * @return
   */
  public static float getTotalVerticalHeight(float xPos) {
    float height = SUBRECT_HEIGHT;
    if (xPos < FLAT_ROOF_MARGIN)
      return SUBRECT_HEIGHT;
    if (xPos > SUBRECT_WIDTH - FLAT_ROOF_MARGIN)
      return SUBRECT_HEIGHT;
    // If we are in the sloped area, we need to compute how far away from
    // the peak of the roof we are.
    float peakXPos = SUBRECT_WIDTH / 2f;
    float distanceToPeak = Math.abs(xPos - peakXPos);
    float widthOfPeak = SUBRECT_WIDTH - 2f * FLAT_ROOF_MARGIN;
    float halfWidthOfPeak = widthOfPeak / 2f;  // gives us a right triangle.
    float distanceToCorner = halfWidthOfPeak - distanceToPeak;
    // Now sohcahtoa is your friend.  We have a right triangle represented by the
    // corner of the roof where it meets the flat spot and our xPos which is some distance
    // between the corner and the peak.  We want to find the height of the right triangle
    // formed between the corner of the roof, our x pos, and the height of the roof directly
    // above our x position.
    // tan(theta) = opposite / adjacent or adjacent * tan(theta) = opposite
    height = distanceToCorner * (float)Math.tan(Math.toRadians(ROOF_SLOPE));
    return SUBRECT_HEIGHT + height;
  }

  /**
   * Creates a JSONFixture based on various parameters.  This will overwrite the file Fixtures/bvgenerated.lxf each
   * time we start up.  Currently a restart is required for each parameter change because they require code changes.
   * TODO(tracy): Should this be migrated over to a custom Fixture?  I would like to re-use the existing JSONFixture
   * and outputs support if possible.
   * @param lx
   * @return
   */
  static public JsonFixture createFixture(LX lx) {
    Map<Integer, Integer> stripLengths = new HashMap<Integer, Integer>();

    JsonFixture fixture = new JsonFixture(lx, "bvgenerated");

    JsonObject jObj = new JsonObject();

    jObj.addProperty("label", "BVFixture");
    JsonArray modelKeys = new JsonArray();
    modelKeys.add("bvfixture");
    jObj.add("modelKeys", modelKeys);
    JsonObject parameters = new JsonObject();
    JsonObject hostName = new JsonObject();
    hostName.addProperty("type", "string");
    hostName.addProperty("default", "127.0.0.1");
    parameters.add("Host", hostName);
    jObj.add("parameters", parameters);

    JsonArray strips = new JsonArray();

    int numStrips = (int)((SUBRECT_WIDTH - 2f * SIDE_MARGIN) / horizontalSpacing) + 1;

    for (int stripNum = 0; stripNum < numStrips; stripNum++) {
      JsonObject oneStrip = new JsonObject();
      float yOffset;
      float yStaggerThisOne;
      if (stripNum % 2 == 0) {
        yOffset = DEFAULT_Y_OFFSET;
        yStaggerThisOne = 0f;
      } else {
        yOffset = DEFAULT_Y_OFFSET + verticalStagger;
        yStaggerThisOne = verticalStagger;
      }
      // Set the xPos to be centered around 0 in X in 3D worldspace coordinates.
      float xPos = -SUBRECT_WIDTH/2f + stripNum * horizontalSpacing + SIDE_MARGIN;
      oneStrip.addProperty("x", xPos);
      oneStrip.addProperty("y", yOffset + BOTTOM_MARGIN);
      oneStrip.addProperty("spacing", verticalSpacing);
      JsonObject directionObj = new JsonObject();
      // For now, all point up.  Eventually, depending on wiring, some might point down so we can
      // have back to back wiring.  In that case, the initial X position of the strip needs to be
      // swapped with the last X position of the strip when it was pointed up (i.e. starts at the top).
      directionObj.addProperty("x", 0);
      directionObj.addProperty("y", 1);
      directionObj.addProperty("z", 0);

      oneStrip.add("direction", directionObj);
      // For computing the height from the ground to the roof, we assume that x=0 is the left side of the building.
      // So we can't directly use our centered-around-0 worldspace X coordinate.  Shift it back over.
      float roofRelativeXPos = xPos + SUBRECT_WIDTH/2f;
      float height = getTotalVerticalHeight(roofRelativeXPos) - (TOP_MARGIN + BOTTOM_MARGIN);

      int numPointsThisStrip = (int)((height - yStaggerThisOne) / verticalSpacing) + 1;

      Integer count = stripLengths.getOrDefault(numPointsThisStrip, 0);
      count = count + 1;
      stripLengths.put(numPointsThisStrip, count);

      oneStrip.addProperty("numPoints", numPointsThisStrip);

      strips.add(oneStrip);
    }

    List<Integer> sortedKeys = new ArrayList<Integer>(stripLengths.keySet());
    Collections.sort(sortedKeys);

    int totalLeds = 0;
    for (Integer key : sortedKeys) {
      Integer thisCount = stripLengths.get(key);
      System.out.println("" + key + " LEDS strip count " + thisCount);
      totalLeds += key * thisCount;
    }
    System.out.println("Total LEDs: " + totalLeds);

    jObj.add("strips", strips);

    // Can't seem to get it to save to bvgenerated.lxf via fixture.save() so we will directly write the json obj.
    File fixtureFile = lx.getMediaFile(LX.Media.FIXTURES, "bvgenerated" + ".lxf", false);
    try {
      PrintWriter out = new PrintWriter(fixtureFile.getAbsolutePath());
      out.println(jObj.toString());
      out.flush();
      out.close();
    } catch (IOException ioex) {
      System.err.println("Error writing generated fixture: " + ioex.getMessage());
    }
    return fixture;
  }

  static public class FrameDebug extends UI3dComponent {
    private UICylinder bottom;
    private UICylinder right;
    private UICylinder left;
    private UICylinder leftFlatRoof;
    private UICylinder rightFlatRoof;
    private UICylinder leftPeak;
    private UICylinder rightPeak;
    private static float FRAME_RADIUS = 0.05f;

    public FrameDebug() {
      bottom = new UICylinder(FRAME_RADIUS, SUBRECT_WIDTH, 4, LXColor.rgb(255,0,0));
      left = new UICylinder(FRAME_RADIUS, SUBRECT_HEIGHT, 4, LXColor.rgb(255, 0, 0));
      right = new UICylinder(FRAME_RADIUS, SUBRECT_HEIGHT, 4, LXColor.rgb(255, 0, 0));
      leftFlatRoof = new UICylinder(FRAME_RADIUS, FLAT_ROOF_MARGIN, 4, LXColor.rgb(255, 0, 0));
      rightFlatRoof = new UICylinder(FRAME_RADIUS, FLAT_ROOF_MARGIN, 4, LXColor.rgb(255, 0, 0));
      leftPeak = new UICylinder(FRAME_RADIUS, ROOF_SLOPE_RUN, 4, LXColor.rgb(255, 0, 0));
      rightPeak = new UICylinder(FRAME_RADIUS, ROOF_SLOPE_RUN, 4, LXColor.rgb(255, 0, 0));
    }

    public void onDraw(UI ui, PGraphics pg) {
      pg.pushMatrix();
      pg.translate(-SUBRECT_WIDTH/2f, DEFAULT_Y_OFFSET, 0f);
      left.onDraw(ui, pg);
      pg.popMatrix();
      pg.pushMatrix();
      pg.translate(+SUBRECT_WIDTH/2f, DEFAULT_Y_OFFSET, 0f);
      right.onDraw(ui,pg);
      pg.popMatrix();
      pg.pushMatrix();
      pg.translate(SUBRECT_WIDTH/2f, DEFAULT_Y_OFFSET, 0f);
      pg.rotateZ((float)Math.toRadians(90f));
      bottom.onDraw(ui, pg);
      pg.popMatrix();
      pg.pushMatrix();
      pg.translate(-SUBRECT_WIDTH/2f + FLAT_ROOF_MARGIN, DEFAULT_Y_OFFSET + SUBRECT_HEIGHT, 0f);
      pg.rotateZ((float)Math.toRadians(90f));
      leftFlatRoof.onDraw(ui, pg);
      pg.popMatrix();
      pg.pushMatrix();
      pg.translate(SUBRECT_WIDTH/2f, DEFAULT_Y_OFFSET + SUBRECT_HEIGHT, 0f);
      pg.rotateZ((float)Math.toRadians(90f));
      rightFlatRoof.onDraw(ui, pg);
      pg.popMatrix();
      pg.pushMatrix();
      pg.translate(-SUBRECT_WIDTH/2f + FLAT_ROOF_MARGIN, DEFAULT_Y_OFFSET + SUBRECT_HEIGHT, 0f);
      pg.rotateZ((float)Math.toRadians(-(90f - ROOF_SLOPE)));
      leftPeak.onDraw(ui, pg);
      pg.popMatrix();
      pg.pushMatrix();
      pg.translate(0f, DEFAULT_Y_OFFSET + SUBRECT_HEIGHT + PEAK_OFFSET, 0f);
      pg.rotateZ((float)Math.toRadians(-(90f + ROOF_SLOPE)));
      rightPeak.onDraw(ui, pg);
      pg.popMatrix();
    }

  }

  /**
   * Utility class for drawing cylinders. Assumes the cylinder is oriented with the
   * y-axis vertical. Use transforms to position accordingly.
   */
  public static class UICylinder extends UI3dComponent {

    private final PVector[] base;
    private final PVector[] top;
    private final int detail;
    public final float len;
    private int fill;

    public UICylinder(float radius, float len, int detail, int fill) {
      this(radius, radius, 0, len, detail, fill);
    }

    public UICylinder(float baseRadius, float topRadius, float len, int detail, int fill) {
      this(baseRadius, topRadius, 0, len, detail, fill);
    }

    public UICylinder(float baseRadius, float topRadius, float yMin, float yMax, int detail, int fill) {
      this.base = new PVector[detail];
      this.top = new PVector[detail];
      this.detail = detail;
      this.len = yMax - yMin;
      this.fill = fill;
      for (int i = 0; i < detail; ++i) {
        float angle = i * PConstants.TWO_PI / detail;
        this.base[i] = new PVector(baseRadius * (float)Math.cos(angle), yMin, baseRadius * (float)Math.sin(angle));
        this.top[i] = new PVector(topRadius * (float)Math.cos(angle), yMax, topRadius * (float)Math.sin(angle));
      }
    }

    public void onDraw(UI ui, PGraphics pg) {
      pg.fill(fill);
      pg.noStroke();
      pg.beginShape(TRIANGLE_STRIP);
      for (int i = 0; i <= this.detail; ++i) {
        int ii = i % this.detail;
        pg.vertex(this.base[ii].x, this.base[ii].y, this.base[ii].z);
        pg.vertex(this.top[ii].x, this.top[ii].y, this.top[ii].z);
      }
      pg.endShape(PConstants.CLOSE);
    }
  }

}
