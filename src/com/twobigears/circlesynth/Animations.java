package com.twobigears.circlesynth;

public class Animations {
	
	private float value;
	public float increment;
	public float totalFrames;
	public float animateValue;
	
	Animations(float lengthInFrames) {
		totalFrames = lengthInFrames;
		reset();
	}
	
	public void reset() {
		value = 0;
		increment = 1;
	}
	
	public void animate() {
		value += increment;
		animateValue = Math.min(value/totalFrames, 1f);
	}	
}
