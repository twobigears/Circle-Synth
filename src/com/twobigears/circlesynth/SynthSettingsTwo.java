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

import java.io.File;
import java.io.IOException;

import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceActivity;
import android.view.WindowManager;
import android.widget.Toast;

public class SynthSettingsTwo extends PreferenceActivity{
	
	static final String TAG = "circle-synth";
	public static final String PREF_DELETE = "deletefiles";
	public static final String PREF_TUTORIAL = "tutorial";
	public static final String PREF_FEEDBACK = "feedback";
	public static final String PREF_ABOUT = "about";
	public static final String PREF_DONATE = "donate";
	public static final String PREF_DELREC = "deleterecordings";
	
	
	
	
	
	// ZubhiumSDK sdk;

	@SuppressWarnings("deprecation")
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		
		super.onCreate(savedInstanceState);
		this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
				WindowManager.LayoutParams.FLAG_FULLSCREEN);
		addPreferencesFromResource(com.twobigears.circlesynth.R.xml.preferences);

		Preference deleteprefs = (Preference) findPreference(PREF_DELETE);
		deleteprefs
				.setOnPreferenceClickListener(new OnPreferenceClickListener() {

					@Override
					public boolean onPreferenceClick(Preference preference) {
						String path = Environment.getExternalStorageDirectory()
								+ "/circlesynth/sketches";
						deleteFiles(path,getString(R.string.sketch_delete));
						return false;

					}
				});
		Preference tutorialpref = (Preference) findPreference(PREF_TUTORIAL);
		tutorialpref
				.setOnPreferenceClickListener(new OnPreferenceClickListener() {

					@Override
					public boolean onPreferenceClick(Preference preference) {
						showTutorial();

						return false;

					}
				});

		Preference feedbackpref = (Preference) findPreference(PREF_FEEDBACK);
		feedbackpref
				.setOnPreferenceClickListener(new OnPreferenceClickListener() {

					@Override
					public boolean onPreferenceClick(Preference preference) {
						setupFeedback();
						return false;
					}
				});

		Preference donatepref = (Preference) findPreference(PREF_DONATE);
		donatepref.setOnPreferenceClickListener(new OnPreferenceClickListener() {

			@Override
			public boolean onPreferenceClick(Preference preference) {
				setupDonate();
				return false;
			}
		});

		Preference aboutpref = (Preference) findPreference(PREF_ABOUT);
		aboutpref.setOnPreferenceClickListener(new OnPreferenceClickListener() {

			@Override
			public boolean onPreferenceClick(Preference preference) {
				setupAbout();
				return false;
			}
		});
		
		Preference deleterecs = (Preference) findPreference(PREF_DELREC);
		deleterecs.setOnPreferenceClickListener(new OnPreferenceClickListener() {

					@Override
					public boolean onPreferenceClick(Preference preference) {
						String path = Environment.getExternalStorageDirectory()
								+ "/circlesynth/recordings";
						deleteFiles(path,getString(R.string.rec_delete));
						return false;

					}
				});
		
	
	}
	

	    
    

	public void deleteFiles(String path,String msg) {

		File file = new File(path);

		if (file.exists()) {
			Toast.makeText(SynthSettingsTwo.this, msg,
					Toast.LENGTH_SHORT).show();
			String deleteCmd = "rm -r " + path;
			Runtime runtime = Runtime.getRuntime();
			try {
				runtime.exec(deleteCmd);
			} catch (IOException e) {
			}
		}
	}

	public void showTutorial() {
		TutorialDialog.showTutorialDialog(SynthSettingsTwo.this);

	}

	public void setupFeedback() {

		Intent i = new Intent(Intent.ACTION_SEND);
		i.setType("message/rfc822");
		i.putExtra(Intent.EXTRA_EMAIL, new String[] { "apps@twobigears.com" });
		i.putExtra(Intent.EXTRA_SUBJECT, "Circle Synth feedback");

		try {
			startActivity(Intent.createChooser(i, "Send mail"));
		} catch (android.content.ActivityNotFoundException ex) {
			Toast.makeText(SynthSettingsTwo.this,
					getString(R.string.email_fail), Toast.LENGTH_SHORT)
					.show();
		}

	}

	public void setupDonate() {
		
//		SynthSettingsTwo.this.runOnUiThread(new Runnable() {
//			public void run() {
//			DialogFragment dialog = new DonateDialog();
//			dialog.show(getFragmentManager(), "donationfragment");
//			}
//		});
		
		Intent intent = new Intent(SynthSettingsTwo.this, DonateActivity.class);
		startActivity(intent);

	}

	public void setupAbout() {

		AboutDialog about = new AboutDialog(this);
		about.setTitle("Circle Synth");

		about.show();
	}

	@Override
	protected void onUserLeaveHint() {
		
		super.onUserLeaveHint();
		finish();
	}


	

	
	
}
