package com.twobigears.circlesynth;

import android.util.Log;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.Interpolator;
import android.view.animation.OvershootInterpolator;

public class Animations {

	private float value;
	public float totalFrames, animateValue, increment;

	Interpolator overshootInterpolator;
	Interpolator accelerateInterpolator;

	Animations(float lengthInFrames) {
		totalFrames = lengthInFrames;
		overshootInterpolator = new OvershootInterpolator(1.7f);
		accelerateInterpolator = new AccelerateInterpolator();
		reset();
	}

	public void reset() {
		value = 0;
		increment = 1;
		animateValue = 0;
	}

	public void animate() {
		value += increment;
		animateValue = overshootInterpolator.getInterpolation(Math.min(value
				/ totalFrames, 1f));
	}

	public void accelerateUp() {
		value = Math.min(value, totalFrames);
		value += increment;
		animateValue = overshootInterpolator.getInterpolation(value
				/ totalFrames);
	}

	public void accelerateDown() {
		value = Math.max(value, 0);
		value -= increment;
		animateValue = overshootInterpolator.getInterpolation(Math.max(value
				/ totalFrames, 0f));
	}
}
