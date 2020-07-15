package heronarts.lx.app;

import heronarts.p3lx.ui.UI;
import heronarts.p3lx.ui.UI3dComponent;
import processing.core.PApplet;
import processing.core.PGraphics;
import processing.core.PImage;

public class BG_IMG extends UI3dComponent {
    PImage img;

    public BG_IMG(PApplet pApplet) {
        img = pApplet.loadImage("plain.png");
        if (img!= null) {
            System.out.println("image loaded!");
        } else {
            System.out.println("image not loaded!");
        }
    }

    public void onDraw(UI ui, PGraphics pg) {
        pg.pushMatrix();
        pg.beginShape();
        pg.texture(img);
        pg.vertex(-100, -100, 2, 0, img.height);  // bottom left
        pg.vertex(100, -100, 2, img.width, img.height);  // bottom right
        pg.vertex(100, 100, 2, img.width, 0); // top right
        pg.vertex(-100, 100, 2, 0, 0); // top left
        pg.endShape();
        pg.popMatrix();
    }
}

