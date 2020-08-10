package heronarts.lx.app.model;

import heronarts.lx.model.LXModel;

import java.util.ArrayList;
import java.util.List;

public class Dataline extends LXModel {
    private String id;
    private String ipAddress;
    private int channel;
    private List<Strip> strips = new ArrayList<Strip>();

    public Dataline(String id, String ipAddress, int channel, List<Strip> strips) {
        super(TestGridModel.stripsListToArray(strips));
        this.id = id;
        this.ipAddress = ipAddress;
        this.channel = channel;
        this.strips.addAll(strips);
    }

    public String getId() {
        return id;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public int getChannel() {
        return channel;
    }

    public List<Strip> getStrips() {
        return strips;
    }

    public Strip[] getStripsArray() {
        return strips.stream().toArray(Strip[]::new);
    }


}