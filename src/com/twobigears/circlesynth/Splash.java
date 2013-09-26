package com.twobigears.circlesynth;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Window;
import android.view.WindowManager;
import com.twobigears.circlesynth.R;

public class Splash extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		this.requestWindowFeature(Window.FEATURE_NO_TITLE);
		this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
				WindowManager.LayoutParams.FLAG_FULLSCREEN);
		super.onCreate(savedInstanceState);

		setContentView(R.layout.splash);

		Thread pause = new Thread() {
			public void run() {
				try {
					sleep(2000);
				} catch (InterruptedException e) {
					e.printStackTrace();
				} finally {
					Intent stuff = new Intent(
							"com.twobigears.circlesynth.SynthCircle");
					startActivity(stuff);
				}
			}
		};
		pause.start();
	}

	@Override
	protected void onPause() {

		super.onPause();
		finish();
	}

}
