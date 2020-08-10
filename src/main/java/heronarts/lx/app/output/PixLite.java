package heronarts.lx.app.output;

import java.net.InetAddress;
import java.net.UnknownHostException;

import heronarts.lx.LX;
import heronarts.lx.app.model.TestGridModel;
import heronarts.lx.color.LXColor;
import heronarts.lx.model.LXPoint;
import heronarts.lx.output.LXBufferDatagram;
import heronarts.lx.output.LXOutputGroup;
import heronarts.lx.output.LXDatagramOutput;
import heronarts.lx.output.ArtNetDatagram;

public class PixLite extends LXOutputGroup {
    private LX lx;
    private String ipAddress;

    public PixLite(LX lx, String ipAddress) {
        super(lx, ipAddress);
        this.lx = lx;
        this.ipAddress = ipAddress;
    }

    public PixLite addPixliteChannel(int channel, PointsGrouping pointsGrouping) {
        try {
            addChild(new Channel(lx, channel, pointsGrouping));
        } catch (Exception e) {
            e.printStackTrace();
        }
        return this;
    }

    public class Channel extends LXDatagramOutput {
        private final int MAX_NUM_POINTS_PER_UNIVERSE = 170;
        private final int index;
        private final int firstUniverseOnChannel;

        public Channel(LX lx, int index, PointsGrouping pointsGrouping) throws Exception {
            super(lx);
            this.index = index;
            this.firstUniverseOnChannel = index * 2 - 1;
            setupDatagrams(pointsGrouping);
        }

        private void setupDatagrams(PointsGrouping pointsGrouping) {
            // the points for one pixlite output have to be spread across multiple universes
            int numPoints = pointsGrouping.size();
            int numUniverses = (numPoints / MAX_NUM_POINTS_PER_UNIVERSE) + 1;
            int counter = 0;

            for (int i = 0; i < numUniverses; i++) {
                int universe = firstUniverseOnChannel + i;
                int numIndices = ((i + 1) * MAX_NUM_POINTS_PER_UNIVERSE) > numPoints
                        ? (numPoints % MAX_NUM_POINTS_PER_UNIVERSE)
                        : MAX_NUM_POINTS_PER_UNIVERSE;
                int[] indices = new int[numIndices];
                for (int i1 = 0; i1 < numIndices; i1++) {
                    indices[i1] = pointsGrouping.getPoint(counter++).index;
                }
                try {
                    ArtNetDatagram datagram = new ArtNetDatagram(indices, universe - 1) {
                        // HACK to handle skipped pixels on the 6 x 6 test panel
                        @Override
                        protected LXBufferDatagram copyPoints(int[] colors, byte[] glut, int[] indexBuffer, int offset) {
                            for (LXPoint p : TestGridModel.skippedPoints) {
                                colors[p.index] = LXColor.BLACK;
                            }
                            return super.copyPoints(colors, glut, indexBuffer, offset);
                        }
                    };
                    datagram.setAddress(InetAddress.getByName(ipAddress));
                    addDatagram(datagram);
                } catch (UnknownHostException e) {
                    e.printStackTrace();
                }
            }
        }

        public int getIndex() {
            return index;
        }
    }
}

