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

import android.app.Activity;
import android.content.Context;
import android.preference.Preference;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import com.google.ads.AdRequest;
import com.google.ads.AdSize;
import com.google.ads.AdView;

public class AdPreference extends Preference {
	public AdPreference(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
	}

	public AdPreference(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public AdPreference(Context context) {
		super(context);
	}

	@SuppressWarnings("deprecation")
	@Override
	protected View onCreateView(ViewGroup parent) {
		// this will create the linear layout defined in ads_layout.xml
		View view = super.onCreateView(parent);

		// the context is a PreferenceActivity
		Activity activity = (Activity) getContext();

		// Create the adView
		// AdView adView = new AdView(activity, AdSize.BANNER,
		// "a151293482b37f2");

		AdView adView = new AdView(activity, AdSize.BANNER, "82c43bc4379944d0");
		((LinearLayout) view).addView(adView);

		// Initiate a generic request to load it with an ad
		AdRequest request = new AdRequest();
		adView.loadAd(request);
		request.setTesting(true);
		
		request.addTestDevice("B4F5B9DA474E70A297C4302325DE1D6E");
		request.addTestDevice("133BA1E36B62B14E019C4965E8E5F83B");

		return view;
	}

}
