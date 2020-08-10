package heronarts.lx.app.model;

import heronarts.lx.model.LXModel;

public class TestGridModelConfig {
    int rows;
    int cols;

    float rowPitch = 1;
    float colPitch = 1;
    float stagger = 0;

    String ipAddress;
    int channel;

    public TestGridModelConfig(int rows, int cols) {
        this.rows = rows;
        this.cols = cols;
    }

    public float getRowPitch() {
        return rowPitch;
    }

    public TestGridModelConfig setRowPitch(float rowPitch) {
        this.rowPitch = rowPitch;
        return this;
    }

    public float getColPitch() {
        return colPitch;
    }

    public TestGridModelConfig setColPitch(float colPitch) {
        this.colPitch = colPitch;
        return this;
    }

    public float getStagger() {
        return stagger;
    }

    public TestGridModelConfig setStagger(float stagger) {
        this.stagger = stagger;
        return this;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public TestGridModelConfig setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
        return this;
    }

    public int getChannel() {
        return channel;
    }

    public TestGridModelConfig setChannel(int channel) {
        this.channel = channel;
        return this;
    }
}
