package com.twobigears.circlesynth;

import processing.core.*;

public abstract class UiTextButton extends ProcessingTouchEvents {
	
	final PApplet p;

	public PFont font;
	public float pad;
	public int offColor, onColor;
	private float tX, tY, tWidth, tHeight;
	public boolean boundBox, state, isEnabled;

	UiTextButton(final PApplet p) {
		super(p);
		this.p = p;
		boundBox = false;
		state = false;
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
		
		if(isEnabled) {
			if (boundBox) {
				p.fill(90);
				p.rect(tempx, tempy, tWidth, tHeight);
			}
			
			p.pushMatrix();
			p.translate(tX, tY);
			p.textAlign(p.CENTER, p.CENTER);
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

	public void isPressed() {

	}

	public void isReleased() {

	}

	float getWidth() {
		return tWidth;
	}

	float getHeight() {
		return tHeight;
	}
}
