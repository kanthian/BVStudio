package heronarts.lx.app;

import heronarts.p3lx.ui.UI;
import heronarts.p3lx.ui.UI3dComponent;
import processing.core.PApplet;
import processing.core.PGraphics;
import processing.core.PImage;

public class BG_IMG extends UI3dComponent {
    PImage img;

    public BG_IMG(PApplet pApplet) {
        // 5000 x 2813
        img = pApplet.loadImage("bvstudiobg.jpg");
        if (img!= null) {
            System.out.println("image loaded!");
        } else {
            System.out.println("image not loaded!");
        }
    }

    public void onDraw(UI ui, PGraphics pg) {
        float width = 5000f;
        float height = 2813f;
        width = img.width;
        height = img.height;
        float aspectRatio = width/height;
        float size = 30.5f;
        float xOffset = -2.7f;
        float yOffset = -0.0f;
        float z = 0.2f;
        pg.noStroke();
        pg.pushMatrix();
        pg.beginShape();
        pg.texture(img);
        pg.vertex(-size * aspectRatio + xOffset, -size + yOffset, z, 0, img.height);  // bottom left
        pg.vertex(size * aspectRatio + xOffset, -size + yOffset, z, img.width, img.height);  // bottom right
        pg.vertex(size*aspectRatio + xOffset, size + yOffset, z, img.width, 0); // top right
        pg.vertex(-size*aspectRatio + xOffset, size + yOffset, z, 0, 0); // top left
        pg.endShape();
        pg.popMatrix();
    }
}

