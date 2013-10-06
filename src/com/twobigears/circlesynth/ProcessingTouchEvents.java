package com.twobigears.circlesynth;

import android.util.Log;
import processing.core.PApplet;
import processing.event.MouseEvent;

public abstract class ProcessingTouchEvents {

	PApplet p;
	public int releaseX;
	public int releaseY;
	public int pressX;
	public int pressY;
	public int mX;
	public int mY;

	ProcessingTouchEvents(PApplet p) {
		this.p = p;
		p.registerMethod("mouseEvent", this);
	}

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

	public void touchDown(float x, float y) {

	}

	public void touchUp(float x, float y) {

	}


}
