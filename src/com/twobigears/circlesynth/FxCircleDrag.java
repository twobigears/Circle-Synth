package com.twobigears.circlesynth;

import processing.core.*;

public class FxCircleDrag {

	private final PApplet p;
	public int color;
	public PImage filledCircle, emptyCircle;
	public boolean isEnabled;
	private float posX, posY;

	FxCircleDrag(final PApplet p) {
		this.p = p;
		isEnabled = false;
		posX = posY = 0;
	}
	
	public void setXY (float x, float y) {
		posX = x;
		posY = y;
	}

	public void drawIt() {
		if (isEnabled) {
			if (color == -1) {
				p.pushMatrix();
				p.pushStyle();
				p.translate(posX, posY);
				p.scale(1.25f);
				p.imageMode(PConstants.CENTER);
				p.image(emptyCircle, 0, 0);
				p.popStyle();
				p.popMatrix();

			} else {
				p.pushMatrix();
				p.pushStyle();
				p.translate(posX, posY);
				p.tint(color);
				p.imageMode(PConstants.CENTER);
				p.image(filledCircle, 0, 0);
				p.popStyle();
				p.popMatrix();
			}
		}
	}

}
