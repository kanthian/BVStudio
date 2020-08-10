package heronarts.lx.app;

import heronarts.lx.LX;
import heronarts.lx.model.LXPoint;
import heronarts.lx.output.ArtNetDatagram;
import heronarts.lx.output.ArtSyncDatagram;
import heronarts.lx.output.LXDatagramOutput;
import heronarts.lx.structure.GridFixture;
import heronarts.lx.structure.LXFixture;

import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Output {
  private static final Logger logger = Logger.getLogger(Output.class.getName());

  public static void configurePixliteOutput(LX lx) {
    List<ArtNetDatagram> datagrams = new ArrayList<ArtNetDatagram>();

    String artNetIpAddress = "192.168.2.100";
    int artNetIpPort = 6565;

    int[] thisUniverseIndices = new int[170];
    int pointsAdded = 0;
    int curIndex = 0;
    for (LXFixture fixture: lx.structure.fixtures) {
      if (fixture instanceof GridFixture) {
        // Left to right, bottom to top is the default.  Hopefully that is the point order?
        // TODO(tracy): Deal with missing LEDs.
        // For 9", we only want the first 3 LEDs per row.
        // For 6", we only want first 4 LEDs per row.
        // For 9", the first grid is 6 high
        // for 9", the second grid is 5 high
        // For 6", both grids are 8 leds high.
        // For 9", the stride is 35 for grid 1
        // For 9", the stride is 34 for grid 2
        // For 6", the stride is unknown for grid 1
        // For 6", the stride is unknown for grid 2
        int ledsPerRow = 3;  // 4 for 6"
        int rowStride = 35; // ?? for 6"

        if (fixture.getLabel().equals("Bot 1 Grid")) {
          curIndex = 0;
          int colIndex = 0;
          for (LXPoint pt : fixture.points) {
            // add the point value to the datagram.
            thisUniverseIndices[curIndex * 2 * 2] = pt.index;
            pointsAdded++;
            curIndex++;
            ++colIndex;
            // We only want the first few columns of LEDs
            if (colIndex >= ledsPerRow) {
              curIndex += rowStride - colIndex;  // jump to next row
              colIndex = 0;
            }
            if (curIndex > 80) continue;  // Limit to less than half of the 160
          }
        } else if (fixture.getLabel().equals("Bot 2 Grid")) {
          rowStride = 34;  // For 9", grid 2, we have 34 columns
          curIndex = 0;
          int colIndex = 0;
          for (LXPoint pt : fixture.points) {
            thisUniverseIndices[curIndex * 2 * 2 + 2] = pt.index;
            pointsAdded++;
            ++colIndex;
            // We only want the first few columns of LEDs
            if (colIndex >= ledsPerRow) {
              curIndex += rowStride - colIndex;  // jump to next row
              colIndex = 0;
            }
            if (curIndex > 80) continue;
          }
        }
      }
    }
    ArtNetDatagram onlyDatagram = new ArtNetDatagram(thisUniverseIndices, pointsAdded*3, 0);
    try {
      onlyDatagram.setAddress(InetAddress.getByName(artNetIpAddress)).setPort(artNetIpPort);
    } catch (UnknownHostException uhex) {
      logger.log(Level.SEVERE, "Configuring ArtNet: " + artNetIpAddress + ":" + artNetIpPort, uhex);
    }
    datagrams.add(onlyDatagram);

    LXDatagramOutput datagramOutput = null;
    try {
      datagramOutput = new LXDatagramOutput(lx);
      for (ArtNetDatagram datagram : datagrams) {
        datagramOutput.addDatagram(datagram);
      }
      try {
        datagramOutput.addDatagram(new ArtSyncDatagram().setAddress(InetAddress.getByName(artNetIpAddress)).setPort(artNetIpPort));
      } catch (UnknownHostException uhex) {
        logger.log(Level.SEVERE, "Unknown host for ArtNet sync.", uhex);
      }
    } catch (SocketException sex) {
      logger.log(Level.SEVERE, "Initializing LXDatagramOutput failed.", sex);
    }
    if (datagramOutput != null) {
      datagramOutput.enabled.setValue(true);
      lx.engine.output.addChild(datagramOutput);
    } else {
      logger.log(Level.SEVERE, "Did not configure output, error during LXDatagramOutput init");
    }
  }
}
