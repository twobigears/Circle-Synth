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
 * Draws the FX circles on drag/drop
 */
public class FxCircleDrag {

	private final PApplet p;
	private float posX, posY;
	
	/** Circle color, assigned to tint value */ 
	public int color;
	/** Image for filled circle */
	public PImage filledCircle;
	/** Image for "empty" uncoloured circle */
	public PImage emptyCircle;
	/** Is enabled or disabled. Also affects visibility */
	public boolean isEnabled;

	/**
	 * Constructor. Specify PApplet
	 * 
	 * @param p
	 *            PApplet
	 */
	FxCircleDrag(final PApplet p) {
		this.p = p;
		isEnabled = false;
		posX = posY = 0;
	}

	/**
	 * Set X and Y position of circles
	 * 
	 * @param x
	 *            X Position
	 * @param y
	 *            Y Position
	 */
	public void setXY(float x, float y) {
		posX = x;
		posY = y;
	}

	/**
	 * Draw circles based on x/y positions and color values
	 */
	public void drawIt() {
		if (isEnabled) {
			// For "no fx" circle (empty circle with no colour)
			if (color == -1) {
				p.pushMatrix();
				p.pushStyle();
				p.translate(posX, posY);
				p.scale(1.25f);
				p.imageMode(PConstants.CENTER);
				p.image(emptyCircle, 0, 0);
				p.popStyle();
				p.popMatrix();

			}
			// Coloured circles
			else {
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
