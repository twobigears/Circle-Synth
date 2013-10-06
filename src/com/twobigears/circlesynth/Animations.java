package com.twobigears.circlesynth;

import android.view.animation.Interpolator;
import android.view.animation.OvershootInterpolator;

public class Animations {
	
	private float value;
	public float totalFrames, animateValue, increment;
	
	Interpolator animationInterpolator;
	
	Animations(float lengthInFrames) {
		totalFrames = lengthInFrames;
		animationInterpolator = new OvershootInterpolator(1.7f);
		reset();
	}
	
	public void reset() {
		value = 0;
		increment = 1;
	}
	
	public void animate() {
		value += increment;
		animateValue = animationInterpolator.getInterpolation(Math.min(value/totalFrames, 1f));
	}	
}
