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
 * Button with text label. Extends ProcessingTouchEvents.java
 */
public abstract class UiTextButton extends ProcessingTouchEvents {
	
	final PApplet p;
	private PFont font;
	private float tX, tY, tWidth, tHeight;
	
	/** Text colour on toggle off/false */
	public int offColor;
	/** Text colour on toggle on/true */
	public int onColor;
	/** Toggle visibility of bounding box */
	public boolean boundBox;
	/** Toggle button state */
	public boolean state;
	/** Enable/disable button, also affects visiblity */
	public boolean isEnabled;
	
	/**
	 * Constructor. Specify PApplet
	 * @param p PApplet
	 */
	UiTextButton(final PApplet p) {
		super(p);
		this.p = p;
		boundBox = false;
		state = false;
		isEnabled = true;
		onColor = p.color(255, 255, 255);
		offColor = p.color(175, 175, 175);
	}
	
	/**
	 * Load PFont to be used for the label
	 * @param pfont
	 */
	void load(PFont pfont) {

		font = pfont;
		p.textFont(font);
	}
	
	/**
	 * Set size of button, necessary for centering text within an invisible box.
	 * The box can be made visible by setting boundBox to true
	 * @param buttonWidth Width of button
	 * @param buttonHeight Height of button
	 */
	void setSize(float buttonWidth, float buttonHeight) {
		tWidth = buttonWidth;
		tHeight = buttonHeight;
	}
	
	/**
	 * Draw the button
	 * @param displayText Label to be displayed
	 * @param tempx X position of button
	 * @param tempy Y position of button
	 */
	void drawIt(String displayText, float tempx, float tempy) {
		tX = tempx;
		tY = tempy;
		
		if(isEnabled) {
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
				p.text(displayText, tWidth*0.5f, tHeight*0.5f);
			} else {
				p.fill(onColor);	
				p.text(displayText, tWidth*0.5f, tHeight*0.5f);
			}
			p.popMatrix();
			
		}

		
	}
	
	/*
	 * Overriding method in ProcessingTouchEvents.java
	 */
	@Override
	public void touchDown(float x, float y) {
		if (isEnabled) {		
			if ((x > tX) && (x < tX + tWidth) && (y > tY)
					&& (y < tY + tHeight)) {
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
			if ((x > tX) && (x < tX + tWidth) && (y > tY)
					&& (y < tY + tHeight) && state) {
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
	 * @return Button Width
	 */
	float getWidth() {
		return tWidth;
	}
	
	/**
	 * Returns the height of the button
	 * @return Button height
	 */
	float getHeight() {
		return tHeight;
	}
}
