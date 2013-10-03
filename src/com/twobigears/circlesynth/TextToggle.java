package com.twobigears.circlesynth;

import processing.core.*;

public abstract class TextToggle {

	final PApplet p;

	public PFont font;
	public float pad;
	public int offColor, onColor;
	private float tX, tY, tWidth, tHeight;
	public boolean boundBox, state, isEnabled;
	private boolean isDown;

	TextToggle(final PApplet p) {
		this.p = p;
		boundBox = state = isDown = false;
		isEnabled = true;
		onColor = p.color(255, 255, 255);
		offColor = p.color(175, 175, 175);
	}

	void load(PFont pfont) {

		font = pfont;
		p.textFont(font);
	}

	void setSize(float buttonWidth, float buttonHeight) {
		tWidth = buttonWidth;
		tHeight = buttonHeight;
	}

	void drawIt(String displayText, float tempx, float tempy) {
		tX = tempx;
		tY = tempy;

		if (isEnabled) {
			if (boundBox) {
				p.fill(90);
				p.rect(tempx, tempy, tWidth, tHeight);
			}

			p.pushMatrix();
			p.translate(tX, tY);
			p.textAlign(PConstants.CENTER, PConstants.CENTER);
			if (!state) {
				p.fill(offColor);
				p.text(displayText, tWidth * 0.5f, tHeight * 0.5f);
			} else {
				p.fill(onColor);
				p.text(displayText, tWidth * 0.5f, tHeight * 0.5f);
			}
			p.popMatrix();

		}

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

	public void isTrue() {

	}

	public void isFalse() {

	}

	float getWidth() {
		return tWidth;
	}

	float getHeight() {
		return tHeight;
	}
}