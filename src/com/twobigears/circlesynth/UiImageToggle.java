package com.twobigears.circlesynth;

import processing.core.*;

abstract class UiImageToggle extends ProcessingTouchEvents {

	final PApplet p;

	PImage falseImage, trueImage;
	private float tX, tY, tWidth, tHeight;
	public boolean state, isEnabled, isAlternateMode;
	private boolean isDown;
	public int tintValue;

	UiImageToggle(PApplet p) {
		super(p);
		this.p = p;
		state = isDown = false;
		isEnabled = true;
	}

	UiImageToggle(PApplet p, boolean enabled, boolean alternateMode) {
		super(p);
		this.p = p;
		state = isDown = false;
		isEnabled = enabled;
		tintValue = 0;
		isAlternateMode = alternateMode;
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

	@Override
	public void touchDown(float x, float y) {
		if (isEnabled) {
			if (isAlternateMode) {
				if ((x > tX) && (x < tX + tWidth) && (y > tY)
						&& (y < tY + tHeight)) {
					isDown = true;
					state = true;
					isTrue();
				}

			} else {
				if ((x > tX) && (x < tX + tWidth) && (y > tY)
						&& (y < tY + tHeight)) {
					isDown = true;
				} else
					isDown = false;
			}
		}

	}

	@Override
	public void touchUp(float x, float y) {
		if (isEnabled) {
			
			if (isAlternateMode) {
				state = false;
				isDown = false;
				isFalse();

				if ((x > tX) && (x < tX + tWidth) && (y > tY)
						&& (y < tY + tHeight) && isDown) {

				}

			} else {
				if ((x > tX) && (x < tX + tWidth) && (y > tY)
						&& (y < tY + tHeight) && isDown) {
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