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
