package heronarts.lx.app.model;

import heronarts.lx.model.LXPoint;
import heronarts.lx.transform.LXTransform;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class TestGridModelDatalineBuilder {
    public static List<Dataline> build(TestGridModelConfig config) {
        LXTransform t = new LXTransform();
        List<Strip> strips = new ArrayList<>();
        List<LXPoint> points;

        boolean stagger = true;
        boolean reverseRow = false;
        boolean staggerRow = false;
        for (int row = 0; row < config.rows; row++) {
            points = new ArrayList<>();

            if (row != 0) {
                t.translateY(config.getColPitch());
            }

            t.push();
            if (stagger && staggerRow) {
                t.translateX(config.getStagger());
            }
            for (int col = 0; col < config.cols; col++) {
                if (col != 0) {
                    t.translateX(config.getRowPitch());
                }

                points.add(new LXPoint(t));
            }
            t.pop();

            if (reverseRow) {
                Collections.reverse(points);
            }
            Strip strip = new Strip(points);
            strips.add(strip);

            reverseRow = !reverseRow;
            staggerRow = !staggerRow;
        }

        List<Dataline> datalines = new ArrayList<>();

        datalines.add(new Dataline("grid", config.ipAddress, config.channel, strips));

        return datalines;
    }
}
