package com.twobigears.circlesynth;

import processing.core.*;

abstract class ImageToggle {
	
	final PApplet p;

	PImage falseImage, trueImage;
	private float tX, tY, tWidth, tHeight;
	public boolean state, isEnabled;
	private boolean isDown;
	public int tintValue;

	ImageToggle(PApplet p) {
		this.p = p;
		state = isDown = false;
		isEnabled = true;
	}
	
	ImageToggle(PApplet p, boolean enabled) {
		this.p = p;
		state = isDown = false;
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
		
		if(tintValue != 0) {
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
		if ((x > tX) && (x < tX + tWidth) && (y > tY) && (y < tY + tHeight)) {
			isDown = true;
		}
		else isDown = false;
	}

	public void touchUp(float x, float y) {
		if (isEnabled) {

			if ((x > tX) && (x < tX + tWidth) && (y > tY) && (y < tY + tHeight)
					&& isDown) {
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
	
	public void altTouchDown(float x, float y) {
		if (isEnabled) {
			if ((x > tX) && (x < tX + tWidth) && (y > tY) && (y < tY + tHeight)) {
				isDown = true;
				state = true;
				isTrue();
			}
		}	
	}

	public void altTouchUp(float x, float y) {
		if (isEnabled) {
			
			state = false;
			isDown = false;
			isFalse();

			if ((x > tX) && (x < tX + tWidth) && (y > tY) && (y < tY + tHeight)
					&& isDown) {
				
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