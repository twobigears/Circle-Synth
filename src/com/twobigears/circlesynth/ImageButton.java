package com.twobigears.circlesynth;

import processing.core.*;

public abstract class ImageButton {

	final PApplet p;

	PImage falseImage, trueImage;
	private float tX, tY, tWidth, tHeight;
	boolean state, isEnabled;
	public int tintValue;

	ImageButton(final PApplet p) {
		this.p = p;
		state = false;
		isEnabled = true;
	}

	ImageButton(PApplet p, boolean enabled) {
		this.p = p;
		state = false;
		isEnabled = enabled;
		tintValue = 0;
	}

	void load(PImage falseImg, PImage trueImg) {
		falseImage = falseImg;
		trueImage = trueImg;
		tWidth = falseImage.width;
		tHeight = falseImage.height;

	}

	void drawIt(float tempx, float tempy) {
		tX = tempx;
		tY = tempy;

		p.pushStyle();

		if (tintValue != 0) {
			p.tint(tintValue);
		}

		p.imageMode(PConstants.CORNER);

		if (isEnabled) {
			if (!state) {
				p.image(falseImage, tempx, tempy);
			} else
				p.image(trueImage, tempx, tempy);

		}
		
		p.popStyle();

	}

	public void touchDown(float x, float y) {
		if (isEnabled) {
			if ((x > tX) && (x < tX + tWidth) && (y > tY) && (y < tY + tHeight)) {
				state = true;
				isPressed();
			}
		}

	}

	public void touchUp(float x, float y) {
		if (isEnabled) {
			if ((x > tX) && (x < tX + tWidth) && (y > tY) && (y < tY + tHeight)
					&& state) {
				isReleased();
			}
			state = false;
		}
	}

	public void isPressed() {

	}

	public void isReleased() {

	}

	public float getWidth() {
		return tWidth;
	}

	public float getHeight() {
		return tHeight;
	}
}