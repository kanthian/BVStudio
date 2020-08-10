package heronarts.lx.app.pattern;

import heronarts.lx.LX;
import heronarts.lx.LXCategory;
import heronarts.lx.app.model.Dataline;
import heronarts.lx.app.model.Strip;
import heronarts.lx.app.model.TestGridModel;
import heronarts.lx.color.LXColor;
import heronarts.lx.model.LXPoint;
import heronarts.lx.parameter.DiscreteParameter;
import heronarts.lx.parameter.LXParameter;
import heronarts.lx.parameter.LXParameterListener;
import heronarts.lx.pattern.LXModelPattern;

import java.util.List;

@LXCategory(LXCategory.TEST)
public class DatalineSelector extends LXModelPattern<TestGridModel> {

    private final DiscreteParameter selectedDataline;
    private final DiscreteParameter selectedStrip;

    public DatalineSelector(LX lx) {
        super(lx);
        this.selectedDataline = new DiscreteParameter("dataline", getDatalineIds());
        this.selectedStrip = new DiscreteParameter("strip", getSelectedStrips().size());

        selectedDataline.addListener(new LXParameterListener() {
            public void onParameterChanged(LXParameter parameter) {
                selectedStrip.setRange(getSelectedStrips().size());
            }
        });

        addParameter(selectedDataline);
        addParameter(selectedStrip);
    }

    public void run(double deltaMs) {
        setColors(0);
        for (Strip strip : getSelectedStrips()) {
            for (LXPoint p : strip.points) {
                colors[p.index] = lx.hsb(0, 100, 40);
            }
        }
        for (LXPoint p : getSelectedStrip().points) {
            colors[p.index] = LXColor.GREEN;
        }
    }

    private String[] getDatalineIds() {
        String[] ids = new String[TestGridModel.getDatalinesArray().length];
        for (int i = 0; i < ids.length; i++) {
            ids[i] = TestGridModel.getDatalinesArray()[i].getId();
        }
        return ids;
    }

    private Dataline getSelectedDataline() {
        return TestGridModel.getDatalinesArray()[selectedDataline.getValuei()];
    }

    private List<Strip> getSelectedStrips() {
        return getSelectedDataline().getStrips();
    }

    private Strip getSelectedStrip() {
        return getSelectedStrips().get(selectedStrip.getValuei());
    }
}

