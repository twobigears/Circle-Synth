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

import android.view.animation.AccelerateInterpolator;
import android.view.animation.Interpolator;
import android.view.animation.OvershootInterpolator;

/**
 * Create animations: a basic ramp up/down between 0f and 1f over a length
 * specified in frames
 */
public class Animations {

	private float value;

	/** Length of animation in frames */
	public float totalFrames;
	/** Animation output in float (between 0f and 1f) */
	public float animateValue;
	/** Animation incremenet value with every frame. Default is 1 */
	public float increment;

	// Using Android interpolators
	Interpolator overshootInterpolator;
	Interpolator accelerateInterpolator;

	/**
	 * Object constructor. Specify length of animation in frames
	 * 
	 * @param lengthInFrames Length of animation in frames
	 */
	Animations(float lengthInFrames) {
		totalFrames = lengthInFrames;
		overshootInterpolator = new OvershootInterpolator(1.7f);
		accelerateInterpolator = new AccelerateInterpolator();
		reset();
	}

	/**
	 * Resets animation object: initial value to 0f, incremenet value to 1
	 */
	public void reset() {
		value = 0;
		increment = 1;
		animateValue = 0;
	}

	/**
	 * Trigger default ramp up (0f to 1f) animation with overshoot interpolator
	 */
	public void animate() {
		value += increment;
		animateValue = overshootInterpolator.getInterpolation(Math.min(value
				/ totalFrames, 1f));
	}

	/**
	 * Trigger ramp up (0f to 1f) animation with overshoot interpolator
	 */
	public void accelerateUp() {
		value = Math.min(value, totalFrames);
		value += increment;
		animateValue = overshootInterpolator.getInterpolation(value
				/ totalFrames);
	}

	/**
	 * Trigger ramp down (1f to 0f) animation with overshoot interpolator
	 */
	public void accelerateDown() {
		value = Math.max(value, 0);
		value -= increment;
		animateValue = overshootInterpolator.getInterpolation(Math.max(value
				/ totalFrames, 0f));
	}
}
