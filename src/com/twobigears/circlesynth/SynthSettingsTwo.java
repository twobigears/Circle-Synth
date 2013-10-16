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

import android.app.AlertDialog;
import android.app.DialogFragment;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceActivity;
import android.util.Log;
import android.view.WindowManager;
import android.widget.Toast;

import com.twobigears.circlesynth.DonateDialog.OnDonateListener;
import com.twobigears.circlesynth.util.IabHelper;
import com.twobigears.circlesynth.util.IabResult;
import com.twobigears.circlesynth.util.Purchase;

public class SynthSettingsTwo extends PreferenceActivity implements OnDonateListener{
	
	static final String TAG = "circle-synth";
	public static final String PREF_DELETE = "deletefiles";
	public static final String PREF_TUTORIAL = "tutorial";
	public static final String PREF_FEEDBACK = "feedback";
	public static final String PREF_ABOUT = "about";
	public static final String PREF_DONATE = "donate";
	public static final String PREF_DELREC = "deleterecordings";
	
	//SKUs for the donation values(products);small=0.69,medium=0.99,large=1.39 and xxl=4.99
	
	static final String SKU_SMALL = "donate_small";
	static final String SKU_MEDIUM = "donate_medium";
	static final String SKU_LARGE="donate_large";
	static final String SKU_XXL="donate_xxl";
	static String SKU_CHECK;
	
	// (arbitrary) request code for the purchase flow
	static final int RC_REQUEST = 10001;
	
	//the helper object
	IabHelper mHelper;
	
	
	
	
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
						deleteFiles(path,"All saved sketches deleted");
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
						deleteFiles(path,"All saved recordings deleted");
						return false;

					}
				});
		//In-app purchase stuff
		String base64EncodedPublicKey = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAiJi4eRylaAhP+siaKWRAkk4LLIcUJ8aPvsVmoTlCllbCfMQ9Jq6cxzCa+PiQqA9WoEAiJ3/D5k6i9quInKlZJg2OxkC6b3NU9oVaQvIJD9UyxNTagoxIiipPbn6EoQLaQKcbnE4O9Bo6vQ/+6UXEewGbKhAKvVWvI7x5CjmjoQjlLRtl4tHh4waGI3t2ENAB4QhNVgUkKj1OT38HONfAVVIX8h97SaorPSmwIY+PXDCFXjAVZI/g0mM1weqve5NJt5b5j01hQpC6PFRwCt81X0KILy+6+tBGz78SRreNMG+pRchbIwMv3gQeqEZNPXuprpnZStPdqnq82SnYiPNL3wIDAQAB";
		Log.d(TAG, "Creating IAB helper.");
        mHelper = new IabHelper(this, base64EncodedPublicKey);

        // enable debug logging (for a production application, you should set this to false).
        mHelper.enableDebugLogging(true);

        // Start setup. This is asynchronous and the specified listener
        // will be called once setup completes.
        Log.d(TAG, "Starting setup.");
        mHelper.startSetup(new IabHelper.OnIabSetupFinishedListener() {
            public void onIabSetupFinished(IabResult result) {
                Log.d(TAG, "Setup finished.");

                if (!result.isSuccess()) {
                    // Oh noes, there was a problem.
                    complain("Problem setting up in-app billing: " + result);
                    return;
                }

                // Have we been disposed of in the meantime? If so, quit.
                if (mHelper == null) return;

//                // IAB is fully set up. Now, let's get an inventory of stuff we own.
//                Log.d(TAG, "Setup successful. Querying inventory.");
//                mHelper.queryInventoryAsync(mGotInventoryListener);
            }
        });
	
	}
	
    @Override
    public void onDestroy() {
        super.onDestroy();

        // very important:
        Log.d(TAG, "Destroying helper.");
        if (mHelper != null) {
            mHelper.dispose();
            mHelper = null;
        }
    }
	    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.d(TAG, "onActivityResult(" + requestCode + "," + resultCode + "," + data);
        if (mHelper == null) return;

        // Pass on the activity result to the helper for handling
        if (!mHelper.handleActivityResult(requestCode, resultCode, data)) {
            // not handled, so handle it ourselves (here's where you'd
            // perform any handling of activity results not related to in-app
            // billing...
            super.onActivityResult(requestCode, resultCode, data);
        }
        else {
            Log.d(TAG, "onActivityResult handled by IABUtil.");
        }
    }
    
    /** Verifies the developer payload of a purchase. */
    boolean verifyDeveloperPayload(Purchase p) {
        String payload = p.getDeveloperPayload();

        /*
         * TODO: verify that the developer payload of the purchase is correct. It will be
         * the same one that you sent when initiating the purchase.
         *
         * WARNING: Locally generating a random string when starting a purchase and
         * verifying it here might seem like a good approach, but this will fail in the
         * case where the user purchases an item on one device and then uses your app on
         * a different device, because on the other device you will not have access to the
         * random string you originally generated.
         *
         * So a good developer payload has these characteristics:
         *
         * 1. If two different users purchase an item, the payload is different between them,
         *    so that one user's purchase can't be replayed to another user.
         *
         * 2. The payload must be such that you can verify it even when the app wasn't the
         *    one who initiated the purchase flow (so that items purchased by the user on
         *    one device work on other devices owned by the user).
         *
         * Using your own server to store and verify developer payloads across app
         * installations is recommended.
         */

        return true;
    }
    
 // Callback for when a purchase is finished
    IabHelper.OnIabPurchaseFinishedListener mPurchaseFinishedListener = new IabHelper.OnIabPurchaseFinishedListener() {
        public void onIabPurchaseFinished(IabResult result, Purchase purchase) {
            Log.d(TAG, "Purchase finished: " + result + ", purchase: " + purchase);

            // if we were disposed of in the meantime, quit.
            if (mHelper == null) return;

            if (result.isFailure()) {
                complain("Error purchasing: " + result);
                //setWaitScreen(false);
                return;
            }
            if (!verifyDeveloperPayload(purchase)) {
                complain("Error purchasing. Authenticity verification failed.");
                //setWaitScreen(false);
                return;
            }

            Log.d(TAG, "Purchase successful.");

            if (purchase.getSku().equals(SKU_SMALL)) {
                
                Log.d(TAG, "small donation");
                mHelper.consumeAsync(purchase, mConsumeFinishedListener);
            }
            
        }
    };
    
 // Called when consumption is complete
    IabHelper.OnConsumeFinishedListener mConsumeFinishedListener = new IabHelper.OnConsumeFinishedListener() {
        public void onConsumeFinished(Purchase purchase, IabResult result) {
            Log.d(TAG, "Consumption finished. Purchase: " + purchase + ", result: " + result);

            // if we were disposed of in the meantime, quit.
            if (mHelper == null) return;

            // We know this is the "gas" sku because it's the only one we consume,
            // so we don't check which sku was consumed. If you have more than one
            // sku, you probably should check...
            if (result.isSuccess()) {
                // successfully consumed, so we apply the effects of the item in our
                // game world's logic, which in our case means filling the gas tank a bit
                Log.d(TAG, "Consumption successful. Provisioning.");
               
                alert("Thank you for helping the developers!");
            }
            else {
                complain("Error while consuming: " + result);
            }
            //updateUi();
            //setWaitScreen(false);
            Log.d(TAG, "End consumption flow.");
        }
    };

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
		MiscDialogs.showTutorialDialog(SynthSettingsTwo.this);

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
					"There are no email clients installed.", Toast.LENGTH_SHORT)
					.show();
		}

	}

	public void setupDonate() {
		
		SynthSettingsTwo.this.runOnUiThread(new Runnable() {
			public void run() {
			DialogFragment dialog = new DonateDialog();
			dialog.show(getFragmentManager(), "donationfragment");
			}
		});

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

	@Override
	public void onPositiveAction(float donation) {
		
		Log.d(TAG, "Launching donation purchase flow.");

        /* TODO: for security, generate your payload here for verification. See the comments on
         *        verifyDeveloperPayload() for more info. Since this is a SAMPLE, we just use
         *        an empty string, but on a production app you should carefully generate this. */
        String payload = "";
		
		if(donation == 0.69)
			mHelper.launchPurchaseFlow(this, SKU_SMALL, RC_REQUEST,
	                mPurchaseFinishedListener, payload);
		else if(donation == 0.99)
			mHelper.launchPurchaseFlow(this, SKU_MEDIUM, RC_REQUEST,
	                mPurchaseFinishedListener, payload);
		else if(donation == 1.39)
			mHelper.launchPurchaseFlow(this, SKU_LARGE, RC_REQUEST,
	                mPurchaseFinishedListener, payload);
		else if(donation == 4.99)
			mHelper.launchPurchaseFlow(this, SKU_XXL, RC_REQUEST,
	                mPurchaseFinishedListener, payload);

		System.out.println("donation amount = "+String.valueOf(donation));
		
	}
	

	
	void complain(String message) {
        Log.e(TAG, "**** CircleSynth Error: " + message);
        alert("Error: " + message);
    }
	
	void alert(String message) {
        AlertDialog.Builder bld = new AlertDialog.Builder(this);
        bld.setMessage(message);
        bld.setNeutralButton("OK", null);
        Log.d(TAG, "Showing alert dialog: " + message);
        bld.create().show();
    }
}
