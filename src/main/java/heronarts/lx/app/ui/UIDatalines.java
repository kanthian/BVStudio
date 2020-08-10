package heronarts.lx.app.ui;

import heronarts.lx.LX;
import heronarts.lx.app.LXStudioApp;
import heronarts.lx.app.output.PixLite;
import heronarts.lx.output.LXOutput;
import heronarts.lx.studio.LXStudio.UI;
import heronarts.p3lx.ui.UI2dContainer;
import heronarts.p3lx.ui.component.UICollapsibleSection;
import heronarts.p3lx.ui.component.UIItemList;

import java.util.ArrayList;

public class UIDatalines extends UICollapsibleSection {
    private final LX lx;

    private final UIItemList.BasicList list;

    public UIDatalines(LX lx, UI ui) {
        super(ui, 0, 0, ui.leftPane.global.getContentWidth(), 0);

        this.lx = lx;
        setLayout(UI2dContainer.Layout.VERTICAL);

        this.list = new UIItemList.BasicList(ui, 0, 24, getContentWidth(), getContentHeight());
        this.list.setShowCheckboxes(true);
        this.list.addToContainer(this);

        setTitle("Datalines");
        this.list.setDescription("Datalines");

        ArrayList<Item> items = new ArrayList<Item>();
        LXStudioApp.pixlites.forEach((ipAddress, pixlite) -> {
            for (LXOutput output : pixlite.children) {
                items.add(new Item((PixLite.Channel) output));
            }
        });
        list.setItems(items);
    }
}

class Item extends UIItemList.Item {

    private final PixLite.Channel channel;

    public Item(PixLite.Channel channel) {
        this.channel = channel;
    }

    public boolean isChecked() {
        return this.channel.enabled.isOn();
    }

    public void onCheck(boolean checked) {
        this.channel.enabled.setValue(checked);
    }

    public String getLabel() {
        return String.valueOf(this.channel.getIndex());
    }
}

