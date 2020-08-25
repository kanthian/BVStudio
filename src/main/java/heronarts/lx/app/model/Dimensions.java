package heronarts.lx.app.model;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import heronarts.lx.LX;
import heronarts.lx.structure.JsonFixture;

import java.util.*;

/**
 * Various installation dimensions and dimension related utility functions.
 *
 * All units are in feet.
 */
public class Dimensions {
  public static final float SUBRECT_WIDTH = 600f/12f;
  public static final float SUBRECT_HEIGHT = 438f/12f - 83f/12f;
  public static final float FLAT_ROOF_MARGIN = 33f/12f;
  public static final float PEAK_OFFSET = 83f/12f;
  public static final float ROOF_SLOPE_RUN = 288.5f/12f;
  public static final float ROOF_SLOPE = 17f;  // degrees
  public static final float SIDE_MARGIN = 0f/12f;  // start with nothing.
  public static final float verticalSpacing = 6f/12f;
  public static final float horizontalSpacing = 6f/12f;
  public static final float verticalStagger = 3f/12f;


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

  static public JsonFixture createFixture(LX lx) {
    Map<Integer, Integer> stripLengths = new HashMap<Integer, Integer>();

    JsonFixture fixture = new JsonFixture(lx);

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

    float defaultYOffset = -9.0f;
    for (int stripNum = 0; stripNum < numStrips; stripNum++) {
      JsonObject oneStrip = new JsonObject();
      float yOffset;
      float yStaggerThisOne;
      if (stripNum % 2 == 0) {
        yOffset = defaultYOffset;
        yStaggerThisOne = 0f;
      } else {
        yOffset = defaultYOffset + verticalStagger;
        yStaggerThisOne = verticalStagger;
      }
      // Set the xPos to be centered around 0 in X in 3D worldspace coordinates.
      float xPos = -SUBRECT_WIDTH/2f + stripNum * horizontalSpacing;
      oneStrip.addProperty("x", -SUBRECT_WIDTH/2f + stripNum * horizontalSpacing);
      oneStrip.addProperty("y", yOffset);
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
      float height = getTotalVerticalHeight(roofRelativeXPos);

      int numPointsThisStrip = (int)((height - yStaggerThisOne) / verticalSpacing);

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

    System.out.println(jObj.toString());
    fixture.load(lx, jObj);
    fixture.save(lx, jObj);
    return fixture;
  }
}
