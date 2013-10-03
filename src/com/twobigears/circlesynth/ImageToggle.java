package com.twobigears.circlesynth;

import processing.core.*;

abstract class ImageToggle {
	
	final PApplet p;

	PImage falseImage, trueImage;
	private float tX, tY, tWidth, tHeight;
	boolean state, isEnabled;

	ImageToggle(PApplet p) {
		this.p = p;
		state = false;
		isEnabled = true;
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

		p.imageMode(PConstants.CORNER);

		if (isEnabled) {
			if (!state) {
				p.image(falseImage, tempx, tempy);
			} else
				p.image(trueImage, tempx, tempy);

		}

	}

	public void touchDown(float x, float y) {

	}

	public void touchUp(float x, float y) {
		if (isEnabled) {

			if ((x > tX) && (x < tX + tWidth) && (y > tY) && (y < tY + tHeight)) {
				if (!state) {
					state = true;
					isTrue();
				} else {
					state = false;
					isFalse();
				}
			}
		}
	}

	public void isTrue() {

	}

	public void isFalse() {

	}

	public float getWidth() {
		return tWidth;
	}

	public float getHeight() {
		return tHeight;
	}
}