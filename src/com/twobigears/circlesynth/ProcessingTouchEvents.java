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

import processing.core.PApplet;
import processing.event.MouseEvent;

/**
 * A wrapper class for Processing's mouse event listener which also works for
 * touch
 */
public abstract class ProcessingTouchEvents {

	final PApplet p;
	private int mX;
	private int mY;
	
	/**
	 * Constructor. Specify PApplet.
	 * @param p PApplet
	 */
	ProcessingTouchEvents(PApplet p) {
		this.p = p;
		p.registerMethod("mouseEvent", this);
	}
	
	/**
	 * Mouse event listener
	 * @param event MouseEvent
	 */
	public void mouseEvent(MouseEvent event) {

		switch (event.getAction()) {

		case MouseEvent.PRESS:
			mX = event.getX();
			mY = event.getY();
			touchDown(mX, mY);

			break;

		case MouseEvent.RELEASE:
			mX = event.getX();
			mY = event.getY();
			touchUp(mX, mY);
		}
	}
	
	/**
	 * Override this for touch down event
	 * @param x
	 * @param y
	 */
	public void touchDown(float x, float y) {

	}
	
	/**
	 * Override this for touch up event
	 * @param x
	 * @param y
	 */
	public void touchUp(float x, float y) {

	}

}
