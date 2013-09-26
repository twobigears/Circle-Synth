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
		// request.setTesting(true);

		// request.addTestDevice("B6DE158C61D1A52D63734AE5A4DC3D22");
		// request.addTestDevice("56A90CA18D3C91AA8F87A566BE1507FB");

		return view;
	}

}
