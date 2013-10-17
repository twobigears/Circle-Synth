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

import android.app.Dialog;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import com.twobigears.circlesynth.R;

public class TutorialDialog {
	public static class Counter {
		public int count = 0;
	}

	public static void showTutorialDialog(final SynthSettingsTwo context) {

		final Dialog alert = new Dialog(context);
		alert.requestWindowFeature(Window.FEATURE_NO_TITLE);
		final LayoutInflater inflater = context.getLayoutInflater();
		final LinearLayout tutorialFrame = (LinearLayout) inflater.inflate(
				R.layout.demoframe, new LinearLayout(context), false);
		final ViewGroup blanklayout = (ViewGroup) tutorialFrame
				.findViewById(R.id.dynocontent);
		final int[] slides = { R.layout.demoscreen0, R.layout.demoscreen1,
				R.layout.demoscreen2, R.layout.demoscreen3,
				R.layout.demoscreen4 };

		final Counter c = new Counter();
		c.count = 0;

		inflater.inflate(slides[c.count], blanklayout);

		alert.setContentView(tutorialFrame);

		final ImageButton next = (ImageButton) tutorialFrame
				.findViewById(R.id.next);
		final ImageButton back = (ImageButton) tutorialFrame
				.findViewById(R.id.back);
		back.setOnClickListener(new Button.OnClickListener() {
			public void onClick(View v) {
				c.count -= 1;
				blanklayout.removeAllViews();
				inflater.inflate(slides[c.count], blanklayout);
				if (c.count == 0) {
					back.setBackgroundResource(R.drawable.prevoff);
					back.setEnabled(false);
				} else {
					back.setBackgroundResource(R.drawable.prevon);
					back.setEnabled(true);
					next.setBackgroundResource(R.drawable.nexton);
				}

			}
		});
		back.setEnabled(false);

		next.setOnClickListener(new Button.OnClickListener() {
			public void onClick(View v) {
				c.count += 1;
				if (c.count == slides.length) {
					alert.dismiss();
				} else {
					WindowManager.LayoutParams lp = new WindowManager.LayoutParams();
					lp.copyFrom(alert.getWindow().getAttributes());
					lp.width = alert.getWindow().getDecorView().getWidth();
					lp.height = LayoutParams.WRAP_CONTENT;
					alert.getWindow().setAttributes(lp);

					blanklayout.setVisibility(View.INVISIBLE);
					blanklayout.removeAllViews();
					inflater.inflate(slides[c.count], blanklayout);
					blanklayout.setVisibility(View.VISIBLE);
					if (c.count == slides.length - 1) {
						next.setBackgroundResource(R.drawable.closeon);
						back.setBackgroundResource(R.drawable.prevon);
						back.setEnabled(true);
					} else if (c.count == 0) {
						back.setEnabled(false);
						back.setBackgroundResource(R.drawable.prevoff);
						next.setBackgroundResource(R.drawable.nexton);
					} else {
						next.setBackgroundResource(R.drawable.nexton);
						back.setBackgroundResource(R.drawable.prevon);
						back.setEnabled(true);
					}
				}
			}
		});

		alert.show();

	}

	public static void showTutorialDialog(final SynthCircle context) {

		final Dialog alert = new Dialog(context);
		alert.requestWindowFeature(Window.FEATURE_NO_TITLE);
		final LayoutInflater inflater = context.getLayoutInflater();
		final LinearLayout tutorialFrame = (LinearLayout) inflater.inflate(
				R.layout.demoframe, new LinearLayout(context), false);
		final ViewGroup blanklayout = (ViewGroup) tutorialFrame
				.findViewById(R.id.dynocontent);
		final int[] slides = { R.layout.demoscreen0, R.layout.demoscreen1,
				R.layout.demoscreen2, R.layout.demoscreen3,
				R.layout.demoscreen4 };

		final Counter c = new Counter();
		c.count = 0;

		inflater.inflate(slides[c.count], blanklayout);

		alert.setContentView(tutorialFrame);

		final ImageButton next = (ImageButton) tutorialFrame
				.findViewById(R.id.next);
		final ImageButton back = (ImageButton) tutorialFrame
				.findViewById(R.id.back);
		back.setOnClickListener(new Button.OnClickListener() {
			public void onClick(View v) {
				c.count -= 1;
				blanklayout.removeAllViews();
				inflater.inflate(slides[c.count], blanklayout);
				if (c.count == 0) {
					back.setBackgroundResource(R.drawable.prevoff);
					back.setEnabled(false);
				} else {
					back.setBackgroundResource(R.drawable.prevon);
					back.setEnabled(true);
					next.setBackgroundResource(R.drawable.nexton);
				}

			}
		});
		back.setEnabled(false);

		next.setOnClickListener(new Button.OnClickListener() {
			public void onClick(View v) {
				c.count += 1;
				if (c.count == slides.length) {
					alert.dismiss();
				} else {
					WindowManager.LayoutParams lp = new WindowManager.LayoutParams();
					lp.copyFrom(alert.getWindow().getAttributes());
					lp.width = alert.getWindow().getDecorView().getWidth();
					lp.height = LayoutParams.WRAP_CONTENT;
					alert.getWindow().setAttributes(lp);

					blanklayout.setVisibility(View.INVISIBLE);
					blanklayout.removeAllViews();
					inflater.inflate(slides[c.count], blanklayout);
					blanklayout.setVisibility(View.VISIBLE);
					if (c.count == slides.length - 1) {
						next.setBackgroundResource(R.drawable.closeon);
						back.setBackgroundResource(R.drawable.prevon);
						back.setEnabled(true);
					} else if (c.count == 0) {
						back.setEnabled(false);
						back.setBackgroundResource(R.drawable.prevoff);
						next.setBackgroundResource(R.drawable.nexton);
					} else {
						next.setBackgroundResource(R.drawable.nexton);
						back.setBackgroundResource(R.drawable.prevon);
						back.setEnabled(true);
					}
				}
			}
		});

		alert.show();

	}

}
