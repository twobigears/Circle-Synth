/*  Copyright (C) 2013  Two Big Ears Ltd.
 
	This program is free software; you can redistribute it and/or modify
	it under the terms of the GNU General Public License as published by
	the Free Software Foundation; either version 2 of the License, or
	(at your option) any later version.
	
	This program is distributed in the hope that it will be useful,
	but WITHOUT ANY WARRANTY; without even the implied warranty of
	MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
	GNU General Public License for more details.
	
	You should have received a copy of the GNU General Public License along
	with this program; if not, write to the Free Software Foundation, Inc.,
	51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */

/**
 * Class documentation goes here
 */

package com.twobigears.circlesynth;

import processing.core.*;

public abstract class UiTextToggle extends ProcessingTouchEvents {

	final PApplet p;

	private PFont font;
	public float pad;
	public int offColor, onColor;
	private float tX, tY, tWidth, tHeight;
	public boolean boundBox, state, isEnabled;
	private boolean isDown;

	UiTextToggle(final PApplet p) {
		super(p);
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
			p.textFont(font);
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
	
	@Override
	public void touchDown(float x, float y) {
		if (isEnabled) {
			if ((x > tX) && (x < tX + tWidth) && (y > tY) && (y < tY + tHeight)) {
				isDown = true;
			}
			else isDown = false;
		}
	}
	
	@Override
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