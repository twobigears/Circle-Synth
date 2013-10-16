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

package com.twobigears.circlesynth;

import processing.core.*;

/**
 * Button with image. Extends ProcessingTouchEvents.java
 */
public abstract class UiImageButton extends ProcessingTouchEvents {

	final PApplet p;
	private float tX, tY, tWidth, tHeight;
	
	/** Image for false/off state of button */
	public PImage falseImage;
	/** Image for true/on state of button */
	public PImage trueImage;
	/** Toggle button state */
	public boolean state;
	/** Enable/disable button, also affects visiblity */
	public boolean isEnabled;
	
	/**
	 * Specify tint value to tint image
	 */
	public int tintValue;

	/**
	 * Constructor. Specify PApplet
	 * 
	 * @param p
	 *            PApplet
	 */
	UiImageButton(final PApplet p) {
		super(p);
		this.p = p;
		state = false;
		isEnabled = true;
	}

	/**
	 * Specify PApplet, default state and default mode
	 * 
	 * @param p
	 *            PApplet
	 * @param enabled
	 *            Set state to enabled (visible and active) or disabled
	 *            (invisible and inactive)
	 * @param alternateMode
	 *            Alternate touch mode, useful for click and drag
	 */
	UiImageButton(PApplet p, boolean enabled) {
		super(p);
		this.p = p;
		state = false;
		isEnabled = enabled;
		tintValue = 0;
	}

	/**
	 * Load toggle state images
	 * 
	 * @param falseImg
	 *            Image when toggle is false
	 * @param trueImg
	 *            Image when toggle is true
	 */
	void load(PImage falseImg, PImage trueImg) {
		falseImage = falseImg;
		trueImage = trueImg;
		tWidth = falseImage.width;
		tHeight = falseImage.height;

	}

	/**
	 * Draw the button
	 * 
	 * @param tempx
	 *            X position of button
	 * @param tempy
	 *            Y position of button
	 */
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

	/*
	 * Overriding method in ProcessingTouchEvents.java
	 */
	@Override
	public void touchDown(float x, float y) {
		if (isEnabled) {
			if ((x > tX) && (x < tX + tWidth) && (y > tY) && (y < tY + tHeight)) {
				state = true;
				isPressed();
			}
		}

	}

	/*
	 * Overriding method in ProcessingTouchEvents.java
	 */
	@Override
	public void touchUp(float x, float y) {
		if (isEnabled) {
			if ((x > tX) && (x < tX + tWidth) && (y > tY) && (y < tY + tHeight)
					&& state) {
				isReleased();
			}
			state = false;
		}
	}

	/**
	 * Override this method to trigger actions when button is pressed
	 */
	public void isPressed() {

	}

	/**
	 * Override this method to trigger actions when button is released
	 */
	public void isReleased() {

	}

	/**
	 * Returns the width of the button
	 * 
	 * @return Button Width
	 */
	public float getWidth() {
		return tWidth;
	}

	/**
	 * Returns the height of the button
	 * 
	 * @return Button Height
	 */
	public float getHeight() {
		return tHeight;
	}
}