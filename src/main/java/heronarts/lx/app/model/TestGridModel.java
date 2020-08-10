package heronarts.lx.app.model;

import heronarts.lx.model.LXModel;
import heronarts.lx.model.LXPoint;

import java.util.ArrayList;
import java.util.List;

public class TestGridModel extends LXModel {
    private static List<Dataline> datalines = new ArrayList<>();
    private static List<Strip> strips = new ArrayList<>();

    // HACK to handle skipped pixels on 6 x 6 test grid
    public static List<LXPoint> skippedPoints;

    public TestGridModel(TestGridModelConfig config) {
        super(setup(config));
    }

    private static Strip[] setup(TestGridModelConfig config) {
        List<Dataline> datalines = TestGridModelDatalineBuilder.build(config);
        List<Strip> strips = new ArrayList<>();

        for (Dataline dataline : datalines) {
            strips.addAll(dataline.getStrips());
        }

        TestGridModel.datalines.addAll(datalines);
        TestGridModel.strips.addAll(strips);
        return stripsListToArray(strips);
    }

    public static List<Dataline> getDatalines() {
        return datalines;
    }

    // quick hack
    public static Strip[] stripsListToArray(List<Strip> strips) {
        return strips.stream().toArray(Strip[]::new);
    }

    public static Dataline[] getDatalinesArray() {
        return datalines.stream().toArray(Dataline[]::new);
    }
}
