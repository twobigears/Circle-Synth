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

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.regex.Pattern;

import org.puredata.android.io.AudioParameters;
import org.puredata.android.service.PdService;
import org.puredata.android.utils.PdUiDispatcher;
import org.puredata.core.PdBase;
import org.puredata.core.PdListener;
import org.puredata.core.utils.IoUtils;

import processing.core.PApplet;
import processing.core.PFont;
import processing.core.PGraphics;
import processing.core.PImage;
import android.app.DialogFragment;
import android.content.ComponentName;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.res.AssetManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Environment;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.MotionEvent;
import android.view.WindowManager;
import android.widget.Toast;

import com.google.analytics.tracking.android.EasyTracker;
import com.google.analytics.tracking.android.Tracker;
import com.twobigears.circlesynth.BpmPicker.OnBpmChangedListener;
import com.twobigears.circlesynth.RecordDialog.OnRecordingListener;

public class SynthCircle extends PApplet implements OnBpmChangedListener,
		OnSharedPreferenceChangeListener, SensorEventListener,
		OnRecordingListener {

	public static final String TAG = "CircleSynth";

	Context context;
	SharedPreferences prefs;

	protected PdUiDispatcher dispatcher;
	private Tracker tracker;

	public int sketchWidth() {
		return displayWidth;
	}

	public int sketchHeight() {
		return displayHeight;
	}

	public String sketchRenderer() {
		return OPENGL;
	}

	private PdService pdService = null;
	float pd = 0;

	ArrayList<Dot> dots;
	ArrayList<String> stored;
	FileDialog fileDialog;
	String fName;

	DecimalFormatSymbols symbols = DecimalFormatSymbols.getInstance();

	public int maxCircle = 10;
	String CHECK[] = null;
	String SAVE[] = null;
	String baseDir = Environment.getExternalStorageDirectory()
			.getAbsolutePath();

	boolean fxCheck = false;

	private SensorManager mSensorManager;
	private Sensor mAccelerometer;
	boolean accel;
	float currentAccelx = 0;
	float currentAccely = 0;
	static final float ALPHA = 0.15f;
	float oldAccel = 0;

	float pX, pY;
	public float density;
	DecimalFormat df, df1;

	int effect;
	int col;

	boolean moveflag = false;
	boolean headerflag = false;
	float scanline;

	CountDownTimer timer;

	PGraphics header, sketchBG, scanSquare, dragFocus;

	PImage fxFilledImg, innerCircleImg, outerCircleImg, lineCircleImg;

	Toolbar toolbar;

	FxCircleDrag fxCircleDrag;

	int mainHeadHeight, headerHeight, buttonPad1, buttonPad2, buttonFxPad;

	float outerCircSize, dragDeleteBoundary;

	String resSuffix = "_x100";

	int bpm = 120;

	final int bgCol = color(28, 28, 28);
	final int headCol = color(41, 41, 41);
	final int buttonInActCol = color(175);
	final int col1 = color(255, 68, 68);
	final int col2 = color(255, 187, 51);
	final int col3 = color(153, 204, 0);
	final int col4 = color(170, 102, 204);
	final int col5 = color(51, 181, 229);

	/**
	 * setting up libPd as a background service the initPdService() method binds
	 * the service to the background thread. call initPdService in onCreate() to
	 * start the service.
	 */

	protected final ServiceConnection pdConnection = new ServiceConnection() {
		@Override
		public void onServiceConnected(ComponentName name, IBinder service) {
			pdService = ((PdService.PdBinder) service).getService();

			try {
				initPd();
				loadPatch();
			} catch (IOException e) {
				Log.e(TAG, e.toString());
				finish();
			}

		}

		@Override
		public void onServiceDisconnected(ComponentName name) {
			// Never called

		}
	};

	/* Bind pd service */

	private void initPdService() {

		new Thread() {
			@Override
			public void run() {
				bindService(new Intent(SynthCircle.this, PdService.class),
						pdConnection, BIND_AUTO_CREATE);
			}
		}.start();
	}

	/* initialise pd, also setup listeners here */
	protected void initPd() throws IOException {

		// Configure the audio glue
		int sampleRate = AudioParameters.suggestSampleRate();

		// reducing sample rate based on no. of cores
		if (getNumCores() == 1)
			sampleRate = sampleRate / 4;
		else
			sampleRate = sampleRate / 2;

		pdService.initAudio(sampleRate, 0, 2, 10.0f);
		
		pdService.startAudio(new Intent(this, SynthCircle.class),
				R.drawable.notif_icon, "CircleSynth", "Return to Circle Synth");

		dispatcher = new PdUiDispatcher();
		PdBase.setReceiver(dispatcher);

		dispatcher.addListener("scan", new PdListener.Adapter() {
			@Override
			public void receiveFloat(String source, final float x) {
				scanline = x;
				detect();
				sendPdValue();
			}
		});

	}

	protected void loadPatch() throws IOException {

		if (pd == 0) {
			File dir = getFilesDir();
			IoUtils.extractZipResource(
					getResources().openRawResource(
							com.twobigears.circlesynth.R.raw.vnsequencer), dir,
					true);
			File patchFile = new File(dir, "vnsequencer.pd");
			pd = PdBase.openPatch(patchFile.getAbsolutePath());

			// send initial data to the patch from preferences
			initialisepatch();

		}
	}

	private Toast toast = null;

	/**
	 * creating a handy toast notification message method here
	 * 
	 * @param msg
	 *            String MESSAGE_TO_BE_DISPLAYED_AS_TOAST
	 */
	private void toast(final String msg) {
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				if (toast == null) {
					toast = Toast.makeText(getApplicationContext(), "",
							Toast.LENGTH_SHORT);
				}
				toast.setText(msg);
				toast.show();
			}
		});
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// Setting up Google analytics. GA parameters in the analytics.xml file
		EasyTracker.getInstance().setContext(getApplicationContext());
		// Instantiate the Tracker
		tracker = EasyTracker.getTracker();

		/**
		 * IMPORTANT : if using decimal format, remember to set the decimal
		 * separator explicitly, else your app will crash when used in locales
		 * which use ',' as the decimal separator.
		 */
		symbols.setDecimalSeparator('.');
		df = new DecimalFormat("#.##", symbols);
		df1 = new DecimalFormat("#.#", symbols);

		// Accessing default Shared Preferences
		prefs = PreferenceManager.getDefaultSharedPreferences(this);

		// Call and setup the accelerometer which we will be using later
		mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
		mAccelerometer = mSensorManager
				.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
		mSensorManager.registerListener(this, mAccelerometer, 100000);

		// make sure the screen doesn't turn off
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

		initPdService();

		/*
		 * IMPORTANT:make sure you have the READ_PHONE_STATE permission
		 * specified to use the method below. This method will ensure that
		 * pdservice is paused during a phone call.
		 */
		initSystemServices();

	}

	private void initSystemServices() {
		TelephonyManager telephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
		telephonyManager.listen(new PhoneStateListener() {
			@Override
			public void onCallStateChanged(int state, String incomingNumber) {
				if (pdService == null)
					return;
				if (state == TelephonyManager.CALL_STATE_IDLE) {
					pdService.startAudio();
				} else {
					pdService.stopAudio();
				}
			}
		}, PhoneStateListener.LISTEN_CALL_STATE);
	}

	@Override
	public void onDestroy() {
		super.onDestroy();

		// release all resources called by pdservice
		dispatcher.release();
		unbindService(pdConnection);

		// unregister listener for shared preference clicks and accelerometer
		prefs.unregisterOnSharedPreferenceChangeListener(this);
		mSensorManager.unregisterListener(this);

	}

	@Override
	protected void onStart() {
		super.onStart();
		// this gives analytics the cue to start tracking
		EasyTracker.getInstance().activityStart(this); // Add this method
	}

	@Override
	protected void onStop() {
		super.onStop();
		// stop GA tracking
		EasyTracker.getInstance().activityStop(this); // Add this method
	}

	/**
	 * Gets the number of cores available in this device, across all processors.
	 **/

	private int getNumCores() {
		// Private Class to display only CPU devices in the directory listing
		class CpuFilter implements FileFilter {
			@Override
			public boolean accept(File pathname) {
				// Check if filename is "cpu", followed by a single digit number
				if (Pattern.matches("cpu[0-9]", pathname.getName())) {
					return true;
				}
				return false;
			}
		}

		try {
			// Get directory containing CPU info
			File dir = new File("/sys/devices/system/cpu/");
			// Filter to only list the devices we care about
			File[] files = dir.listFiles(new CpuFilter());
			// Return the number of cores (virtual CPU devices)
			return files.length;
		} catch (Exception e) {
			// Default to return 1 core
			return 1;
		}
	}

	// processing code begins//

	public void setup() {

		frameRate(60);

		dots = new ArrayList<Dot>();
		stored = new ArrayList<String>();

		background(bgCol);
		smooth(8);
		CHECK = new String[10];
		SAVE = new String[10];
		for (int i = 0; i < maxCircle; i++) {
			CHECK[i] = "0 5 5 5 5 0";
		}

		// get android screen density
		DisplayMetrics dm = new DisplayMetrics();
		getWindowManager().getDefaultDisplay().getMetrics(dm);
		float densityR = dm.density;

		// set denity scale value and suffix for image resources
		if (densityR <= 1.2) {
			density = 1f;
			resSuffix = "_x100";
		} else if (densityR > 1.2 && densityR <= 1.6) {
			density = 1.5f;
			resSuffix = "_x150";
		} else if (densityR > 1.6 && densityR <= 2.5) {
			density = 2f;
			resSuffix = "_x200";
		} else if (densityR > 2.5 && densityR < 3.5) {
			density = 3f;
			resSuffix = "_x300";
		} else if (densityR >= 3.5) {
			density = 4f;
			resSuffix = "_x400";
		}

		// load images
		innerCircleImg = loadImage("inner_circle" + resSuffix + ".png");
		outerCircleImg = loadImage("outer_circle" + resSuffix + ".png");
		lineCircleImg = loadImage("line_circle" + resSuffix + ".png");
		fxFilledImg = loadImage("fxFilled" + resSuffix + ".png");

		toolbar = new Toolbar();

		fxCircleDrag = new FxCircleDrag(this);
		fxCircleDrag.emptyCircle = outerCircleImg;
		fxCircleDrag.filledCircle = fxFilledImg;

		// header values
		mainHeadHeight = (int) (40 * density);
		final int shadowHeight = (int) (density);
		final int scanSquareY = (int) (3 * density);
		headerHeight = mainHeadHeight + scanSquareY + shadowHeight;
		buttonPad1 = (int) (10 * density);
		buttonPad2 = (int) (20 * density);
		buttonFxPad = (int) (1 * density);

		// drag to delete boundary thickness
		dragDeleteBoundary = 10 * density;

		// use later in the code for hit detection
		outerCircSize = outerCircleImg.width;

		// create header as PGraphic
		header = createGraphics(width, headerHeight);
		header.beginDraw();
		header.noStroke();
		header.background(headCol);
		header.fill(buttonInActCol, 40);
		header.rect(0, mainHeadHeight, width, scanSquareY);
		header.fill(10);
		header.rect(0, mainHeadHeight + scanSquareY, width, shadowHeight);
		header.endDraw();

		// sketch background as PGraphic, instead of using background()
		sketchBG = createGraphics(width, height - headerHeight);
		sketchBG.beginDraw();
		sketchBG.background(bgCol);
		sketchBG.endDraw();

		// scan square that moves across the timeline
		scanSquare = createGraphics((int) (10 * density), scanSquareY);
		scanSquare.beginDraw();
		scanSquare.noStroke();
		scanSquare.fill(120);
		scanSquare.rect(0, 0, 7 * density, 3 * density, 7 * density);
		scanSquare.endDraw();

		// the sketch border when a circle/node is dragged
		dragFocus = createGraphics(width, height - headerHeight);
		dragFocus.beginDraw();
		dragFocus.noFill();
		dragFocus.stroke(255, 30);
		dragFocus.strokeWeight(dragDeleteBoundary);
		dragFocus.rect(0, 0, dragFocus.width, dragFocus.height);
		dragFocus.endDraw();

	}

	int save_preset, save_bpm, save_scale, save_octTrans, save_noteTrans;

	// We need to have pd ready with presets when the app is first loaded.
	private void initialisepatch() {

		/*
		 * Since the presets sent to pd are gathered from persistent storage,
		 * check if there are any pre-saved settings. if there aren't, allocate
		 * default settings.
		 */
		String value = prefs.getString("preset", null);

		if (value == null) {

			Editor editor = prefs.edit();
			editor.putString("preset", "1");
			editor.putString("scale", "1");
			editor.putString("transposeOct", "3");
			editor.putString("transposeNote", "0");
			editor.putBoolean("accel", true);
			editor.putBoolean("delay", true);
			editor.putBoolean("reverb", true);
			editor.putBoolean("first_tutorial", false);
			editor.commit();
		}

		String pres = prefs.getString("preset", "1");
		int presf = Integer.valueOf(pres);
		PdBase.sendFloat("pd_presets", presf);
		save_preset = presf;

		String val = prefs.getString("scale", "1");
		int valf = Integer.valueOf(val);
		PdBase.sendFloat("pd_scales", valf);
		save_scale = valf;

		String tran = prefs.getString("transposeOct", "3");
		int tranf = Integer.valueOf(tran);
		tranf = tranf - 3;
		PdBase.sendFloat("pd_octTrans", tranf);
		save_octTrans = tranf;

		String tran1 = prefs.getString("transposeNote", "0");
		int tranf1 = Integer.valueOf(tran1);
		PdBase.sendFloat("pd_noteTrans", tranf1);
		save_noteTrans = tranf1;

		boolean accely = prefs.getBoolean("accel", false);
		if (accely)
			PdBase.sendFloat("pd_accelToggle", 1);
		else
			PdBase.sendFloat("pd_accelToggle", 0);

		boolean delayy = prefs.getBoolean("delay", false);
		if (delayy)
			PdBase.sendFloat("pd_delayToggle", 1);
		else
			PdBase.sendFloat("pd_delayToggle", 0);

		boolean reverby = prefs.getBoolean("reverb", false);
		if (reverby)
			PdBase.sendFloat("pd_reverbToggle", 1);
		else
			PdBase.sendFloat("pd_reverbToggle", 0);

		boolean tuts = prefs.getBoolean("first_tutorial", false);

		// If this is the first time the app is laoded, then..
		if (!tuts) {
			// display tutorial dialog
			TutorialDialog.showTutorialDialog(SynthCircle.this);
			tuts = true;
			Editor editor = prefs.edit();
			editor.putBoolean("first_tutorial", true);
			editor.commit();

			// copy asset files and paste them to the demos folder
			String basepath = Environment.getExternalStorageDirectory()
					.toString() + "/circlesynth";
			File demodir = new File(basepath + "/demos/");
			if (!demodir.exists()) {
				demodir.mkdirs();
				copyDemos();
			}

			File circledir = new File(basepath + "/sketches");
			if (!circledir.exists())
				circledir.mkdirs();
		}

	}

	// call this method to apply settings along with a saved sketch, like
	// presets, scales etc.
	public void loadSettings() {
		PdBase.sendFloat("pd_presets", save_preset);
		PdBase.sendFloat("pd_scales", save_scale);
		PdBase.sendFloat("pd_octTrans", save_octTrans);
		PdBase.sendFloat("pd_noteTrans", save_noteTrans);
		PdBase.sendFloat("pd_bpm", save_bpm);
		bpm = save_bpm;

		Editor editor = prefs.edit();

		editor.putString("preset", String.valueOf(save_preset));
		editor.putString("scale", String.valueOf(save_scale));
		editor.putString("transposeOct", String.valueOf(save_octTrans + 3));
		editor.putString("transposeNote", String.valueOf(save_noteTrans));
		editor.apply();

	}

	public void draw() {

		image(sketchBG, 0, headerHeight);

		// draw dots on the screen based on touch data stored in Dot class
		drawThis();

		image(header, 0, 0);
		image(scanSquare, scanline * width, mainHeadHeight);

		if (moveflag)
			image(dragFocus, 0, headerHeight);

		toolbar.drawIt();

		fxCircleDrag.drawIt();
	}

	/*
	 * This method detects where the scanline is, sees if there are any dots
	 * which need to be animated and sets the corresponding selected_ flag for
	 * that dot object. When processing is drawing each frame, depending on the
	 * selected_ flag, it animated the dots accordingly. Gives the illusion of
	 * the dots animating as the scanline moves along.
	 */
	public void detect() {

		if (dots.size() > 0) {
			for (int i = 0; i < dots.size(); i++) {
				Dot d = dots.get(i);
				if (!d.touched2) {
					if (!toolbar.reverseToggleB.state) {
						if (Math.abs(scanline
								- Float.parseFloat(df.format(d.xDown / width))) <= .01)
							d.selected1 = true;

						if (Math.abs(scanline
								- Float.parseFloat(df.format(d.xDown / width))) >= .03)
							d.selected1 = false;
					} else {
						if (Math.abs(scanline
								- Float.parseFloat(df.format(d.xUp / width))) <= .03)
							d.selected1 = true;

						if (Math.abs(scanline
								- Float.parseFloat(df.format(d.xDown / width))) >= .03)
							d.selected1 = false;
					}

				} else {
					float dxd = Float.parseFloat(df.format(d.xDown / width));
					float dxu = Float.parseFloat(df.format(d.xUp / width));
					if (!toolbar.reverseToggleB.state) {
						if (Math.abs(scanline - dxd) <= .01) {
							d.selected1 = d.selected2 = true;
						}

					}
					if (toolbar.reverseToggleB.state) {
						if (Math.abs(scanline - dxu) <= .01) {
							d.selected1 = d.selected2 = true;
						}

						if (Math.abs(scanline - dxu) >= (dxu - dxd + .03)) {
							d.selected1 = d.selected2 = false;
						}

					}
					if (Math.abs(scanline
							- Float.parseFloat(df.format(d.xDown / width))) >= (dxu
							- dxd + .03)) {
						d.selected1 = d.selected2 = false;
					}
				}

			}

		}
	}

	// Associate touch drawing data with the dot ArrayList
	private void drawThis() {

		for (int i = 0; i < dots.size(); i++) {

			Dot d = (Dot) dots.get(i);

			// turning lights off
			if (!toolbar.playToggleB.state)
				d.selected1 = d.selected2 = false;

			// for line
			if (d.touched3)
				d.drawLine();

			// single dot
			if (d.touched1)
				d.drawCircleOne();

			// second circle
			if (d.touched2)
				d.drawCircleTwo();

		}
	}

	/*
	 * This is the main method where all the dot object information is sent to
	 * pd. Each dot represents a pd dot module which makes the corresponding
	 * sound. Here the screen coordinates are normalised, and saved as a string.
	 * Decimal format is used here since the pd modules respond to a maximum of
	 * 2 decimal places. For non-existing dots, a value of 5 is sent to the
	 * module, which in turn switches it off. For example, if there are 3 active
	 * dots on the screen, 3 modules will be on and the remaining 7(since the
	 * max number of circles is 10 in the current version) will be sent a value
	 * of 5 for the screen coordinates, which automatically shuts them off.
	 * 
	 * The string format is as follows :
	 * index_xvalue1_yvalue1_xvalue2_yvalue2_fxvalue(int)
	 * 
	 * Run as a separate thread to increase efficiency and responsiveness of the
	 * UI.
	 */
	public void sendPdValue() {
		new Thread() {
			@Override
			public void run() {
				String TAG1;
				String TAG2;
				String FINAL = null;

				for (int i = 0; i < maxCircle; i++) {
					if (i < dots.size()) {
						Dot d = (Dot) dots.get(i);
						double dxd = d.xDown / width;
						double dyd = 1.0 - ((d.yDown - mainHeadHeight) / (height - mainHeadHeight));
						double dxu = d.xUp / width;
						double dyu = 1.0 - ((d.yUp - mainHeadHeight) / (height - mainHeadHeight));
						if (dxu == dxd)
							dxu = dxd + .03;
						TAG1 = df.format(dxd) + " " + df.format(dyd);
						TAG2 = df.format(dxu) + " " + df.format(dyu);
						FINAL = String.valueOf(i) + " " + TAG1 + " " + TAG2
								+ " " + String.valueOf(d.doteffect);

					} else {
						FINAL = String.valueOf(i) + " " + "5 5 5 5 0";

					}

					// make sure a module doesn't keep receiving the same
					// values.
					if (CHECK[i] != FINAL) {
						// Log.d("pdsend",FINAL);
						// send list of dot values to pd
						String[] pieces = FINAL.split(" ");
						Object[] list = new Object[pieces.length];
						for (int j = 0; j < pieces.length; j++) {
							try {
								list[j] = Float.parseFloat(pieces[j]);
							} catch (NumberFormatException e) {

							}
						}

						PdBase.sendList("pd_dots", list);

					}
					CHECK[i] = FINAL;
				}
			}
		}.start();

	}

	// parse the string contents to populate the dot arraylist. Also extract the
	// sketch params here.
	public void splitString(String string) {

		String[] pieces = string.split(" ");

		int index = Integer.parseInt(pieces[0]);
		if (index < 10) {
			Dot d = dots.get(index);

			if (Float.parseFloat(pieces[1]) <= 1) {
				d.xDown = (Float.parseFloat(pieces[1]) * width);
				d.yDown = (Float.parseFloat(pieces[2]) * height);
				d.xLine = d.xUp = (Float.parseFloat(pieces[3]) * width);
				d.yLine = d.yUp = (Float.parseFloat(pieces[4]) * height);
				d.doteffect = Integer.parseInt(pieces[5]);
				d.dotcol = Integer.parseInt(pieces[6]);
				int t1 = Integer.parseInt(pieces[7]);
				int t2 = Integer.parseInt(pieces[8]);
				int t3 = Integer.parseInt(pieces[9]);
				if (t1 == 1)
					d.touched1 = true;
				else
					d.touched1 = false;
				if (t2 == 1) {
					d.touched2 = true;
					// d.hasLine=true;
				} else {
					d.touched2 = false;
					// d.hasLine=false;
				}

				if (t3 == 1) {
					d.touched3 = true;
					if (d.xDown != d.xUp)
						d.hasLine = true;
					else
						d.hasLine = false;
				} else {
					d.touched3 = false;
					d.hasLine = false;
				}

			} else {
				d.xDown = (Float.parseFloat(pieces[1]));
				d.yDown = (Float.parseFloat(pieces[2]));
				d.xLine = d.xUp = (Float.parseFloat(pieces[3]));
				d.yLine = d.yUp = (Float.parseFloat(pieces[4]));
			}
		} else {
			save_preset = (int) Float.parseFloat(pieces[1]);
			save_scale = (int) Float.parseFloat(pieces[2]);
			save_octTrans = (int) Float.parseFloat(pieces[3]);
			save_noteTrans = (int) Float.parseFloat(pieces[4]);
			save_bpm = (int) Float.parseFloat(pieces[5]);
			loadSettings();

		}

	}

	/*
	 * if the screen is touched where a dot already exists, it return the index
	 * of that dot object, else returns -1.
	 */
	public int delCheck(float mX, float mY) {
		int checkdelete = -1;
		float disp1, disp2;
		disp1 = disp2 = 0;
		if (dots.size() > 0) {
			for (int i = 0; i < dots.size(); i++) {
				Dot d = (Dot) dots.get(i);
				disp1 = distancecalc(d.xDown, d.yDown, mX, mY);
				disp2 = distancecalc(d.xUp, d.yUp, mX, mY);
				if (disp1 < outerCircSize || disp2 < outerCircSize) {
					checkdelete = i;
					i = dots.size();
				}
			}
		}
		return checkdelete;

	}

	// calculates the displacement between two coordinates
	public float distancecalc(float x1, float y1, float x2, float y2) {

		// calculating the px distance between two coordinates (x1,y1) and
		// (x2,y2)
		float xdist = x2 - x1;
		float ydist = y2 - y1;
		double squareX1 = Math.pow(xdist, 2);
		double squareY1 = Math.pow(ydist, 2);
		float distance = (float) Math.sqrt(squareX1 + squareY1);
		return distance;
	}

	/*
	 * The main Dot class Contains parameters to define where the dots or nodes
	 * will be drawn. The class has two main sections, the create(Circle/line)_
	 * methods and the drawCircle_ methods, the former assigning screen
	 * coordinates to the circles and the other responsible for actual drawing
	 * by processing. Also contains the FX parameters to be passed on to libPd.
	 */

	public class Dot {

		float xDown, xUp, yDown, yUp, xLine, yLine, posX, posY;
		boolean touched1, touched2, touched3, selected1, selected2, isMoving,
				isDeleted, hasLine;
		int doteffect;
		int dotcol = color(255, 68, 68);
		private Animations circle1InnerAnim, circle1OuterAnim,
				circle2InnerAnim, circle2OuterAnim;
		private PGraphics lineBuffer;
		private float angle, dist;
		private int lineImgWidth, outerCircleWidth;
		boolean node1, node2;
		boolean isLocked;

		Dot() {
			touched1 = touched2 = touched3 = selected1 = selected2 = isMoving = isDeleted = hasLine = false;
			effect = 0;
			col = col1;

			// initialise animations, each with a duration of 20 frames
			circle1InnerAnim = new Animations(20);
			circle1OuterAnim = new Animations(20);
			circle2InnerAnim = new Animations(20);
			circle2OuterAnim = new Animations(20);

			// PGraphic for line connecting two dots
			lineBuffer = createGraphics(20, lineCircleImg.height);
			lineImgWidth = (int) (lineCircleImg.width - density);

			outerCircleWidth = (int) (outerCircleImg.width - density);
		}

		public void fxClear() {
			// effects : 0 - none, 1 - 4 are corresponding fx
			this.doteffect = 0;
			this.dotcol = color(255, 68, 68);
		}

		// assign coordinates for first circle
		public void createCircle1(float mX1, float mY1) {
			xDown = mX1;
			yDown = mY1;
			xUp = xDown;
			yUp = yDown;
			touched1 = true;
			touched2 = false;
			touched3 = false;
			node1 = true;

		}

		// assign coordinates for second circle
		public void createCircle2(float mX2, float mY2) {
			if (Float.parseFloat(df.format(mX2 / width)) < Float.parseFloat(df
					.format(this.xDown / width))) {
				xUp = this.xDown;
				yUp = this.yDown;
				this.xDown = mX2;
				this.yDown = mY2;
			} else {
				xUp = mX2;
				yUp = mY2;
			}

			xLine = xUp;
			yLine = yUp;
			touched2 = true;
			node2 = true;
		}

		// assign coordinates for line
		public void createLine(float mX3, float mY3) {
			xLine = mX3;
			yLine = mY3;
			hasLine = true;
			touched3 = true;
		}

		// draw the first circle using image resources
		public void drawCircleOne() {
			pushMatrix();
			pushStyle();
			translate(xDown, yDown);
			imageMode(CENTER);

			// the inner circle image is tinted based on assigned fx value
			pushMatrix();
			if (selected1)
				tint(dotcol);
			else
				noTint();
			circle1OuterAnim.animate();
			scale(circle1OuterAnim.animateValue);
			image(outerCircleImg, 0, 0);
			popMatrix();

			pushMatrix();
			if (selected1)
				scale(1.5f);
			else
				scale(1);
			tint(dotcol);
			if (circle1OuterAnim.animateValue > 0.5)
				circle1InnerAnim.animate();
			scale(circle1InnerAnim.animateValue);
			image(innerCircleImg, 0, 0);
			popMatrix();

			popStyle();
			popMatrix();
		}

		// draw the second circle using image resources
		public void drawCircleTwo() {
			pushMatrix();
			pushStyle();
			translate(xUp, yUp);
			imageMode(CENTER);

			pushMatrix();
			if (selected2)
				tint(dotcol);
			else
				noTint();
			circle2OuterAnim.animate();
			scale(circle2OuterAnim.animateValue);
			image(outerCircleImg, 0, 0);
			popMatrix();

			pushMatrix();
			if (selected2)
				scale(1.5f);
			else
				scale(1);
			tint(dotcol);
			if (circle2OuterAnim.animateValue > 0.5)
				circle2InnerAnim.animate();
			scale(circle2InnerAnim.animateValue);
			image(innerCircleImg, 0, 0);
			popMatrix();

			popStyle();
			popMatrix();
		}

		// computer and draw line into PGraphic only when dot coordinates are
		// changed
		private void computeLine(float distanceVal) {
			int countMax = (int) (distanceVal / lineImgWidth);
			if (countMax > 0)
				lineBuffer = createGraphics((int) distanceVal,
						lineCircleImg.height);
			pushStyle();
			lineBuffer.beginDraw();
			lineBuffer.noTint();
			lineBuffer.imageMode(CORNER);
			for (int i = 0; i < countMax; i++) {
				lineBuffer.image(lineCircleImg, lineImgWidth * i, 0);
				if (i == countMax - 1)
					dist = distanceVal;
			}
			lineBuffer.endDraw();
			popStyle();
		}

		// draw line from PGraphic
		public void drawLine() {

			float deltaX = xLine - xDown;
			float deltaY = yLine - yDown;

			// some trigonometry
			angle = atan(deltaY / deltaX);
			if (deltaX < 0)
				angle += PI;

			float tempDist = (float) (sqrt((deltaX * deltaX)
					+ (deltaY * deltaY)) - (outerCircleWidth - density * 3));

			if (tempDist != dist)
				computeLine(tempDist);

			pushMatrix();
			pushStyle();
			translate(xDown, yDown);
			rotate(angle);
			if (selected1 && selected2)
				tint(dotcol);
			else
				noTint();
			imageMode(CORNER);
			image(lineBuffer, (float) ((outerCircleWidth * 0.5) - density),
					(float) (-lineCircleImg.height * 0.5));
			popStyle();
			popMatrix();

		}

		// assign fx value
		public void fx(int f, int col) {
			this.doteffect = f;
			this.dotcol = col;
		}

		public int getNodes() {
			int x = 0;
			if (touched1 && touched2)
				x = 2;
			else if (touched1 && !touched2)
				x = 1;
			else
				x = 0;
			return x;
		}

		public void updateCircles(float mX, float mY) {
			float deltaX = Math.abs(mX - xDown);
			float deltaY = Math.abs(mY - yDown);

			double dist = Math.sqrt(deltaX * deltaX + deltaY * deltaY);

			if (dist < outerCircSize) {
				xDown = mX;
				yDown = mY;
				if (hasLine) {
					xLine = xUp;
					yLine = yUp;
				} else {
					xUp = xDown;
					yUp = yDown;
				}

			} else {
				xLine = xUp = mX;
				yLine = yUp = mY;

			}

			if (xDown > xUp) {
				float tempX = xDown;
				float tempY = yDown;
				xDown = xUp;
				yDown = yUp;
				xUp = tempX;
				yUp = tempY;
			}

		}

	}

	int checkdelete;
	int fxcheckdelete;

	private static float mSavedPlayState = 0;

	/**
	 * This is where the magic happens. All touch events are sent here, and then
	 * worked on accordingly.
	 */
	@Override
	public boolean dispatchTouchEvent(MotionEvent event) {

		// System.out.println(String.valueOf(moveflag));

		width = displayWidth;
		height = displayHeight;
		float x = (event.getX());
		float y = (event.getY());
		int action = event.getActionMasked();
		// int checkdelete = -1;
		if (dots != null) {
			fxcheckdelete = delCheck(x, y);
			// Log.d("checkdelete", String.valueOf(checkdelete));

			fxCircleDrag.setXY(x, y);

			switch (action) {
			case MotionEvent.ACTION_DOWN:

				checkdelete = delCheck(x, y);

				if (y > mainHeadHeight) {
					dots.add(new Dot());
					if (checkdelete < 0) {
						Dot d = (Dot) dots.get(dots.size() - 1);
						if (dots.size() <= maxCircle) {
							d.createCircle1(x, y);
							d.isLocked = false;
							pX = x;
							pY = y;
							d.isMoving = false;
							moveflag = false;
						}

					}
					if (checkdelete >= 0) {
						moveflag = true;

						Dot d = (Dot) dots.get(checkdelete);
						d.isMoving = true;
						d.isLocked = true;
						dots.remove(dots.size() - 1);
					}
					headerflag = false;
				} else
					headerflag = true;

				break;
			case MotionEvent.ACTION_UP:

				if (!headerflag) {
					if (dots.size() > 0) {
						Dot d1 = (Dot) dots.get(dots.size() - 1);
						if (checkdelete < 0) {
							// if (moveflag)
							// dots.remove(dots.size() - 1);
							if (y < mainHeadHeight && !moveflag) {
								dots.remove(dots.size() - 1);
								toast(getString(R.string.cant_draw));
							}

							if (dots.size() > maxCircle) {
								toast(getString(R.string.limit_Reached));
								dots.remove(dots.size() - 1);
							}
							if (d1.hasLine == true && !d1.isLocked) {

								d1.createCircle2(x, y);
								d1.isMoving = false;

							}

						}
						if (checkdelete >= 0 && dots.size() > 0) {
							Dot d = (Dot) dots.get(dots.size() - 1);
							if (x < dragDeleteBoundary
									|| x > width - dragDeleteBoundary
									|| y > height - dragDeleteBoundary
									|| y < mainHeadHeight + dragDeleteBoundary) {
								dots.remove(checkdelete);
								toast(getString(R.string.gone));
							}

							if (d.hasLine && !d.touched2)
								dots.remove(dots.size() - 1);

						}

					}

				}

				// assign fx dragged in and dropped
				if (fxcheckdelete >= 0 && fxCheck) {
					Dot d = dots.get(fxcheckdelete);
					d.fx(effect, col);
				}

				// reset moveflag to false
				moveflag = false;

				break;
			case MotionEvent.ACTION_POINTER_DOWN:

				break;
			case MotionEvent.ACTION_POINTER_UP:

				break;
			case MotionEvent.ACTION_MOVE:

				if (!headerflag && dots.size() > 0) {
					if (checkdelete >= 0) {
						Dot dnew = (Dot) dots.get(checkdelete);
						if (dnew.isMoving) {
							int historySize = event.getHistorySize();
							if (historySize > 0) {
								for (int i = 0; i < historySize; i++) {
									float historicalX = event.getHistoricalX(i);
									float historicalY = event.getHistoricalY(i);
									dnew.updateCircles(historicalX, historicalY);
								}
							} else
								dnew.updateCircles(x, y);
						}
					}

					else if (!moveflag) {
						Dot d11 = (Dot) dots.get(dots.size() - 1);
						if (d11.node1
								&& distanceChecker(d11.xDown, d11.yDown, x, y))
							d11.createLine(x, y);

					}
				}

				// set fx assign color/value based on move
				if (toolbar.fx1ToggleB.state) {
					effect = 1;
					col = col2;
					fxCheck = true;
				} else if (toolbar.fx2ToggleB.state) {
					effect = 2;
					col = col3;
					fxCheck = true;
				} else if (toolbar.fx3ToggleB.state) {
					effect = 3;
					col = col4;
					fxCheck = true;
				} else if (toolbar.fx4ToggleB.state) {
					effect = 4;
					col = col5;
					fxCheck = true;
				} else if (toolbar.fxEmptyToggleB.state) {
					effect = 0;
					col = col1;
					fxCheck = true;
				} else
					fxCheck = false;

				break;
			}

		}

		return super.dispatchTouchEvent(event);
	}

	// return true if the distance between two dots meets a certain limit
	public boolean distanceChecker(float x1, float y1, float x2, float y2) {
		boolean check = false;

		double dist = Math.sqrt((x1 - x2) * (x1 - x2) + (y1 - y2) * (y1 - y2));
		if (dist < outerCircSize)
			check = false;
		else
			check = true;

		return check;
	}

	// interface method from the bpm dialog.
	@Override
	public void bpmChanged(int t) {
		bpm = t;
		PdBase.sendFloat("pd_bpm", bpm);
	}

	@Override
	protected void onPause() {
		PdBase.sendFloat("pd_playToggle", 0);
		if (toolbar.playToggleB.isEnabled) {
			mSavedPlayState = 1;
		}
		else {
			mSavedPlayState = 0;
		}
		super.onPause();
	}

	@Override
	protected void onResume() {
		PdBase.sendFloat("pd_playToggle", mSavedPlayState);
		super.onResume();
	}

	// Changes in the settings screen are sent to pd here
	@Override
	public void onSharedPreferenceChanged(SharedPreferences pref, String key) {

		Editor editor = prefs.edit();

		if (key.equals("preset")) {
			String pres = prefs.getString("preset", "1");
			int presf = Integer.valueOf(pres);
			PdBase.sendFloat("pd_presets", presf);
			save_preset = presf;
			editor.putString("preset", String.valueOf(presf));
			editor.commit();
		}

		if (key.equals("scale")) {
			String val = prefs.getString("scale", "1");
			int valf = Integer.valueOf(val);
			PdBase.sendFloat("pd_scales", valf);
			editor.putString("scale", String.valueOf(valf));
			editor.commit();
			save_scale = valf;
		}
		// Octave transpose preference

		if (key.equals("transposeOct")) {
			String tran = prefs.getString("transposeOct", "3");
			int tranf = Integer.valueOf(tran);
			tranf = tranf - 3;
			PdBase.sendFloat("pd_octTrans", tranf);
			editor.putString("transposeOct", String.valueOf(tranf + 3));
			editor.commit();
			save_octTrans = tranf;
		}

		// Note transpose preference

		if (key.equals("transposeNote")) {
			String tran1 = prefs.getString("transposeNote", "0");
			int tranf1 = Integer.valueOf(tran1);
			PdBase.sendFloat("pd_noteTrans", tranf1);
			editor.putString("transposeNote", String.valueOf(tranf1));
			editor.commit();
			save_noteTrans = tranf1;
		}

		if (key.equals("accel")) {
			boolean accely = prefs.getBoolean("accel", false);
			if (accely)
				PdBase.sendFloat("pd_accelToggle", 1);

			else
				PdBase.sendFloat("pd_accelToggle", 0);
			editor.putBoolean("accel", accely);
			editor.commit();

		}
		if (key.equals("delay")) {
			boolean delayy = prefs.getBoolean("delay", true);
			if (delayy)
				PdBase.sendFloat("pd_delayToggle", 1);

			else
				PdBase.sendFloat("pd_delayToggle", 0);
			editor.putBoolean("delay", delayy);
			editor.commit();

		}
		if (key.equals("reverb")) {
			boolean reverby = prefs.getBoolean("reverb", true);
			if (reverby)
				PdBase.sendFloat("pd_reverbToggle", 1);

			else
				PdBase.sendFloat("pd_reverbToggle", 0);
			editor.putBoolean("reverb", reverby);
			editor.commit();

		}

	}

	/*
	 * low pass filtering based on the accelerometer tilting the device in
	 * either x or y axes generates the filter params used in pd in the lores~
	 * object
	 */

	@Override
	public void onSensorChanged(SensorEvent event) {

		if (event.sensor.getType() != Sensor.TYPE_ACCELEROMETER)
			return;
		SharedPreferences getPrefs1 = PreferenceManager
				.getDefaultSharedPreferences(getBaseContext());
		accel = getPrefs1.getBoolean("accel", true);
		currentAccelx = lowPass(event.values[0]);
		currentAccely = lowPass(event.values[1]);
		float y = Math.abs(currentAccelx / 10);
		float z = Math.abs(currentAccely / 10);

		if (y > z)
			z = y;
		else
			y = z;

		if (accel == true)
			PdBase.sendFloat("pd_accely", (1 - y));

	}

	@Override
	public void onAccuracyChanged(Sensor sensor, int accuracy) {

	}

	DecimalFormat dfnew = new DecimalFormat("#");

	// smoothening accelerometer data

	// lowpass filter function
	protected float lowPass(float input) {

		double accel = 0;
		accel = (float) (ALPHA * accel + (1.0 - ALPHA) * oldAccel);
		oldAccel = input;
		// accel = Float.parseFloat(dfnew.format(accel));
		accel = Math.round(accel);
		return (float) accel;
	}

	// clearing blank dots after load
	private void dotcleanup() {

		for (int i = dots.size() - 1; i >= 0; i--) {
			Dot d = (Dot) dots.get(i);
			if (d.xDown == 5 && d.yDown == 5)
				dots.remove(i);
		}

	}

	// sharing intent
	private void shareIt() {
		String content = "Hey! Check out my awesome sketch!! In Circle Synth, click on Load and navigate to the folder where you downloaded the sketch to open it.";
		Intent sharingIntent = new Intent(android.content.Intent.ACTION_SEND);
		sharingIntent.setType("audio/mpeg3");

		String shareBody = content;
		sharingIntent.putExtra(android.content.Intent.EXTRA_TEXT, shareBody);
		sharingIntent.putExtra(Intent.EXTRA_SUBJECT, "Circle Synth sketch");
		String root = Environment.getExternalStorageDirectory().toString();
		File myDir = new File(root + "/circlesynth/sketches");
		myDir.mkdirs();
		int count = new File(root + "/circlesynth/sketches").listFiles().length;
		if (count == 0)
			fName = null;

		if (fName != null) {
			File file = new File(myDir, fName);
			Uri uri = Uri.fromFile(file);

			sharingIntent.putExtra(Intent.EXTRA_STREAM, uri);

			startActivity(Intent.createChooser(sharingIntent, "Share via"));
		} else
			toast(getString(R.string.save_sketch));
	}

	// The toolbar including all buttons and animations
	public class Toolbar {

		// Images for buttons
		PImage shareImg, playImg, stopImg, revImg, forImg, clearOnImg,
				clearOffImg, loadOffImg, loadOnImg, saveOffImg, saveOnImg,
				shareOffImg, shareOnImg, settingsOnImg, settingsOffImg,
				fxCircleToggleImg, fxEmptyToggleImg, fxClearOffImg,
				fxClearOnImg, moreToggleImg, lessToggleImg;

		PFont robotoFont, robotoSmallFont;

		// All button classes
		PlayToggle playToggleB;
		ReverseToggle reverseToggleB;
		FxToggle fxToggleB;
		BpmButton bpmButtonB;
		ClearButton clearButtonB;
		LoadButton loadButtonB;
		SaveButton saveButtonB;
		ShareButton shareButtonB;
		SettingsButton settingsButtonB;
		Fx1Toggle fx1ToggleB;
		Fx2Toggle fx2ToggleB;
		Fx3Toggle fx3ToggleB;
		Fx4Toggle fx4ToggleB;
		FxEmptyToggle fxEmptyToggleB;
		FxClearButton fxClearButtonB;
		MoreToggle moreToggleB;
		RecordToggle recordToggleB;

		// For toolbar slide animation
		Animations toolbarAnimate;

		// Animation slide value
		private float slideX;

		Toolbar() {

			// Doing the needful. Initialisation time.
			robotoFont = createFont("Roboto-Thin-12", 24 * density, true);
			robotoSmallFont = createFont("Roboto-Thin-12", 20 * density, true);

			playImg = loadImage("play" + resSuffix + ".png");
			stopImg = loadImage("stop" + resSuffix + ".png");
			revImg = loadImage("reverse" + resSuffix + ".png");
			forImg = loadImage("forward" + resSuffix + ".png");
			clearOnImg = loadImage("clearOn" + resSuffix + ".png");
			clearOffImg = loadImage("clearOff" + resSuffix + ".png");
			loadOffImg = loadImage("loadOff" + resSuffix + ".png");
			loadOnImg = loadImage("loadOn" + resSuffix + ".png");
			saveOffImg = loadImage("saveOff" + resSuffix + ".png");
			saveOnImg = loadImage("saveOn" + resSuffix + ".png");
			shareOffImg = loadImage("shareOff" + resSuffix + ".png");
			shareOnImg = loadImage("shareOn" + resSuffix + ".png");
			settingsOffImg = loadImage("settingsOff" + resSuffix + ".png");
			settingsOnImg = loadImage("settingsOn" + resSuffix + ".png");
			fxCircleToggleImg = loadImage("fxCircleToggle" + resSuffix + ".png");
			fxEmptyToggleImg = loadImage("fxCircleEmpty" + resSuffix + ".png");
			fxClearOffImg = loadImage("fxClearOff" + resSuffix + ".png");
			fxClearOnImg = loadImage("fxClearOn" + resSuffix + ".png");
			moreToggleImg = loadImage("more" + resSuffix + ".png");
			lessToggleImg = loadImage("less" + resSuffix + ".png");

			playToggleB = new PlayToggle(SynthCircle.this);
			playToggleB.load(playImg, stopImg);
			reverseToggleB = new ReverseToggle(SynthCircle.this);
			reverseToggleB.load(revImg, forImg);
			fxToggleB = new FxToggle(SynthCircle.this);
			fxToggleB.load(robotoFont);
			fxToggleB.setSize(revImg.width, revImg.height);
			bpmButtonB = new BpmButton(SynthCircle.this);
			bpmButtonB.load(robotoFont);
			bpmButtonB.setSize(revImg.width, revImg.height);
			clearButtonB = new ClearButton(SynthCircle.this);
			clearButtonB.load(clearOffImg, clearOnImg);
			loadButtonB = new LoadButton(SynthCircle.this);
			loadButtonB.load(loadOffImg, loadOnImg);
			saveButtonB = new SaveButton(SynthCircle.this);
			saveButtonB.load(saveOffImg, saveOnImg);
			shareButtonB = new ShareButton(SynthCircle.this);
			shareButtonB.load(shareOffImg, shareOnImg);
			settingsButtonB = new SettingsButton(SynthCircle.this);
			settingsButtonB.load(settingsOffImg, settingsOnImg);
			fx1ToggleB = new Fx1Toggle(SynthCircle.this, false, true);
			fx1ToggleB.load(fxCircleToggleImg, fxCircleToggleImg);
			fx2ToggleB = new Fx2Toggle(SynthCircle.this, false, true);
			fx2ToggleB.load(fxCircleToggleImg, fxCircleToggleImg);
			fx3ToggleB = new Fx3Toggle(SynthCircle.this, false, true);
			fx3ToggleB.load(fxCircleToggleImg, fxCircleToggleImg);
			fx4ToggleB = new Fx4Toggle(SynthCircle.this, false, true);
			fx4ToggleB.load(fxCircleToggleImg, fxCircleToggleImg);
			fxEmptyToggleB = new FxEmptyToggle(SynthCircle.this, false, true);
			fxEmptyToggleB.load(fxEmptyToggleImg, fxEmptyToggleImg);
			fxClearButtonB = new FxClearButton(SynthCircle.this, false);
			fxClearButtonB.load(fxClearOffImg, fxClearOnImg);
			moreToggleB = new MoreToggle(SynthCircle.this);
			moreToggleB.load(moreToggleImg, lessToggleImg);
			recordToggleB = new RecordToggle(SynthCircle.this);
			recordToggleB.load(robotoSmallFont);
			recordToggleB.setSize(revImg.width, revImg.height);
			recordToggleB.offColor = color(120, 120, 120);
			recordToggleB.onColor = color(255, 0, 0);

			// Init animation with animation time in frames
			toolbarAnimate = new Animations(30);

		}

		// Draw it all
		public void drawIt() {

			// Toggle slide animation
			if (moreToggleB.state)
				toolbarAnimate.accelerateUp();
			else
				toolbarAnimate.accelerateDown();

			pushMatrix();

			// Animation value * distance needed to animate in px
			slideX = (playToggleB.getWidth() * 3) + (buttonPad1 * 3);
			float xAnimate = toolbarAnimate.animateValue * -slideX;

			// All buttons
			playToggleB.drawIt(buttonPad1 + xAnimate, 0);
			reverseToggleB.drawIt((playToggleB.getWidth() + buttonPad2)
					+ xAnimate, 0);
			bpmButtonB.drawIt(String.valueOf(bpm),
					((playToggleB.getWidth() + buttonPad2) * 2) + xAnimate, 0);
			clearButtonB.drawIt(((playToggleB.getWidth() + buttonPad2) * 3)
					+ xAnimate, 0);
			fxToggleB.drawIt("FX", ((playToggleB.getWidth() + buttonPad2) * 4)
					+ xAnimate, 0);

			moreToggleB.drawIt((width - (playToggleB.getWidth() + buttonPad1))
					+ xAnimate, 0);
			settingsButtonB.drawIt(
					(width - ((playToggleB.getWidth() + buttonPad1) * 2))
							+ xAnimate, 0);
			recordToggleB.drawIt(REC_TEXT,
					(width - ((playToggleB.getWidth() + buttonPad1) * 3))
							+ xAnimate, 0);

			shareButtonB.drawIt((width) + xAnimate, 0);
			saveButtonB.drawIt(
					(width + ((playToggleB.getWidth()) + (buttonPad1)))
							+ xAnimate, 0);
			loadButtonB.drawIt(
					(width + ((playToggleB.getWidth() * 2) + (buttonPad1 * 2)))
							+ xAnimate, 0);

			fx1ToggleB.drawIt(buttonFxPad + xAnimate, 0);
			fx2ToggleB.drawIt((fx1ToggleB.getWidth() + buttonFxPad) + xAnimate,
					0);
			fx3ToggleB.drawIt(((fx1ToggleB.getWidth()) * 2 + buttonFxPad)
					+ xAnimate, 0);
			fx4ToggleB.drawIt(((fx1ToggleB.getWidth()) * 3 + buttonFxPad)
					+ xAnimate, 0);
			fxEmptyToggleB.drawIt(((fx1ToggleB.getWidth()) * 4 + buttonFxPad)
					+ xAnimate, 0);
			fxClearButtonB.drawIt(((fx1ToggleB.getWidth()) * 5 + buttonFxPad)
					+ xAnimate, 0);

			popMatrix();
		}

		// String for record button
		String REC_TEXT = "REC";

		// Button specs below, extending/overriding UI classes
		class PlayToggle extends UiImageToggle {

			PlayToggle(PApplet p) {
				super(p);
			}

			@Override
			public void isTrue() {
				PdBase.sendFloat("pd_playToggle", 1);
				sendPdValue();
			}

			@Override
			public void isFalse() {
				PdBase.sendFloat("pd_playToggle", 0);
				scanline = 0;
			}
		}

		class ReverseToggle extends UiImageToggle {

			ReverseToggle(PApplet p) {
				super(p);
			}

			@Override
			public void isTrue() {
				PdBase.sendFloat("pd_revToggle", 1);
			}

			@Override
			public void isFalse() {
				PdBase.sendFloat("pd_revToggle", 0);
			}

		}

		class FxToggle extends UiTextToggle {

			public FxToggle(PApplet p) {
				super(p);
			}

			// Enable/disable buttons
			@Override
			public void isTrue() {
				playToggleB.isEnabled = reverseToggleB.isEnabled = bpmButtonB.isEnabled = clearButtonB.isEnabled = false;
				fx1ToggleB.isEnabled = fx2ToggleB.isEnabled = fx3ToggleB.isEnabled = fx4ToggleB.isEnabled = fxEmptyToggleB.isEnabled = fxClearButtonB.isEnabled = true;
			}

			@Override
			public void isFalse() {
				playToggleB.isEnabled = reverseToggleB.isEnabled = bpmButtonB.isEnabled = clearButtonB.isEnabled = true;
				fx1ToggleB.isEnabled = fx2ToggleB.isEnabled = fx3ToggleB.isEnabled = fx4ToggleB.isEnabled = fxEmptyToggleB.isEnabled = fxClearButtonB.isEnabled = false;
			}

		}

		class BpmButton extends UiTextButton {

			public BpmButton(PApplet p) {
				super(p);
			}

			@Override
			public void isReleased() {

				// Open BPM popup dialog
				toast(getString(R.string.set_bpm));
				SynthCircle.this.runOnUiThread(new Runnable() {
					public void run() {
						new BpmPicker(SynthCircle.this, SynthCircle.this, bpm)
								.show();
					}
				});

			}
		}

		class ClearButton extends UiImageButton {

			ClearButton(PApplet p) {
				super(p);
			}

			@Override
			public void isReleased() {
				dots.clear();
				stored.clear();
				toast(getString(R.string.clear));
			}
		}

		class LoadButton extends UiImageButton {

			LoadButton(PApplet p) {
				super(p);
			}

			@Override
			public void isReleased() {

				// Load saved text file

				File mPath = new File(Environment.getExternalStorageDirectory()
						+ "/circlesynth/sketches");
				fileDialog = new FileDialog(SynthCircle.this, mPath);
				fileDialog.setFileEndsWith(".txt");
				fileDialog
						.addFileListener(new FileDialog.FileSelectedListener() {
							public void fileSelected(File file) {
								fName = file.getName();
								try {

									dots.clear();
									stored.clear();

									FileInputStream input = new FileInputStream(
											file);
									DataInputStream din = new DataInputStream(
											input);

									for (int i = 0; i <= maxCircle; i++) { // Read
																			// lines
										String line = din.readUTF();
										stored.add(line);
										if (i < maxCircle)
											dots.add(new Dot());
										splitString(stored.get(i));

									}
									din.close();
								} catch (IOException exc) {
									exc.printStackTrace();
								}
								dotcleanup();

							}

						});

				// show the in-app file manager
				SynthCircle.this.runOnUiThread(new Runnable() {
					public void run() {

						fileDialog.showDialog();
					}
				});
			}
		}

		class SaveButton extends UiImageButton {

			SaveButton(PApplet p) {
				super(p);
			}

			@Override
			public void isReleased() {
				// Analytics Tracker

				tracker.sendEvent("ui_Action", "button_press", "save_button",
						0L);

				// Save sketch and settings as text file

				toast(getString(R.string.saved));
				stored.clear();
				int t1 = 0;
				int t2 = 0;
				int t3 = 0;
				int count = 0;
				String SAVE = null;
				for (int i = 0; i < maxCircle; i++) {
					if (i < dots.size()) {
						Dot d = (Dot) dots.get(i);
						if (d.touched1)
							t1 = 1;
						if (d.touched2)
							t2 = 1;
						if (d.touched3)
							t3 = 1;
						SAVE = String.valueOf(i) + " "
								+ String.valueOf(d.xDown / width) + " "
								+ String.valueOf(d.yDown / height) + " "
								+ String.valueOf(d.xUp / width) + " "
								+ String.valueOf(d.yUp / height) + " "
								+ String.valueOf(d.doteffect) + " "
								+ String.valueOf(d.dotcol) + " "
								+ String.valueOf(t1) + " " + String.valueOf(t2)
								+ " " + String.valueOf(t3);
					} else {
						SAVE = String.valueOf(i) + " 5 5 5 5 0 0 0 0 0";
					}
					stored.add(i, SAVE);
					t1 = 0;
					t2 = 0;
					t3 = 0;
					count = i;
				}
				save_bpm = bpm;

				String SAVE_EXTRA = String.valueOf(++count) + " "
						+ String.valueOf(save_preset) + " "
						+ String.valueOf(save_scale) + " "
						+ String.valueOf(save_octTrans) + " "
						+ String.valueOf(save_noteTrans) + " "
						+ String.valueOf(save_bpm);
				stored.add(count, SAVE_EXTRA);

				String root = Environment.getExternalStorageDirectory()
						.toString();
				File myDir = new File(root + "/circlesynth/sketches");
				myDir.mkdirs();
				SimpleDateFormat formatter = new SimpleDateFormat("MMddHHmm");
				Date now = new Date();
				String fileName = formatter.format(now);
				String fname = "sketch_" + fileName + ".txt";
				fName = fname;
				File file = new File(myDir, fname);
				try {
					FileOutputStream output = new FileOutputStream(file);
					DataOutputStream dout = new DataOutputStream(output);

					// Save line count
					for (String line : stored)
						// Save lines
						dout.writeUTF(line);
					dout.flush(); // Flush stream ...
					dout.close(); // ... and close.

				} catch (IOException exc) {
					exc.printStackTrace();
				}

			}
		}

		class ShareButton extends UiImageButton {

			ShareButton(PApplet p) {
				super(p);
			}

			@Override
			public void isReleased() {
				// Analytics Tracker

				tracker.sendEvent("ui_Action", "button_press", "share_button",
						0L);

				shareIt();
			}
		}

		class SettingsButton extends UiImageButton {

			SettingsButton(PApplet p) {
				super(p);
			}

			@SuppressWarnings("deprecation")
			@Override
			public void isReleased() {

				// Open share preferences

				tracker.sendEvent("ui_Action", "button_press",
						"settings_button", 0L);
				Intent intent = new Intent(SynthCircle.this,
						SynthSettingsTwo.class);
				startActivity(intent);
				prefs.registerOnSharedPreferenceChangeListener(SynthCircle.this);
			}
		}

		// FX circle toggles are in "alternate mode" for drag n' drop
		class Fx1Toggle extends UiImageToggle {

			Fx1Toggle(PApplet p, boolean enabled, boolean alternateMode) {
				super(p, enabled, alternateMode);
				tintValue = col2;
			}

			@Override
			public void isTrue() {
				fxCircleDrag.color = col2;
				fxCircleDrag.isEnabled = true;
			}

			@Override
			public void isFalse() {
				fxCircleDrag.isEnabled = false;
			}

		}

		class Fx2Toggle extends UiImageToggle {

			Fx2Toggle(PApplet p, boolean enabled, boolean alternateMode) {
				super(p, enabled, alternateMode);
				tintValue = col3;
			}

			@Override
			public void isTrue() {
				fxCircleDrag.color = col3;
				fxCircleDrag.isEnabled = true;
			}

			@Override
			public void isFalse() {
				fxCircleDrag.isEnabled = false;
			}
		}

		class Fx3Toggle extends UiImageToggle {

			Fx3Toggle(PApplet p, boolean enabled, boolean alternateMode) {
				super(p, enabled, alternateMode);
				tintValue = col4;
			}

			@Override
			public void isTrue() {
				fxCircleDrag.color = col4;
				fxCircleDrag.isEnabled = true;
			}

			@Override
			public void isFalse() {
				fxCircleDrag.isEnabled = false;
			}
		}

		class Fx4Toggle extends UiImageToggle {

			Fx4Toggle(PApplet p, boolean enabled, boolean alternateMode) {
				super(p, enabled, alternateMode);
				tintValue = col5;
			}

			@Override
			public void isTrue() {
				fxCircleDrag.color = col5;
				fxCircleDrag.isEnabled = true;
			}

			@Override
			public void isFalse() {
				fxCircleDrag.isEnabled = false;
			}
		}

		class FxEmptyToggle extends UiImageToggle {

			FxEmptyToggle(PApplet p, boolean enabled, boolean alternateMode) {
				super(p, enabled, alternateMode);
			}

			@Override
			public void isTrue() {
				fxCircleDrag.color = -1;
				fxCircleDrag.isEnabled = true;
			}

			@Override
			public void isFalse() {
				fxCircleDrag.isEnabled = false;
			}
		}

		class FxClearButton extends UiImageButton {

			FxClearButton(PApplet p, boolean enabled) {
				super(p, enabled);
			}

			@Override
			public void isReleased() {
				for (int i = 0; i < dots.size(); i++) {
					Dot d = (Dot) dots.get(i);
					d.fxClear();
				}
				toast(getString(R.string.fx_clear));
			}
		}

		class MoreToggle extends UiImageToggle {

			MoreToggle(PApplet p) {
				super(p);
			}

			@Override
			public void isTrue() {
			}

			@Override
			public void isFalse() {

			}
		}

		long start;
		int countertest;
		Editor edit1;

		/**
		 * 
		 * Pd starts recording the audio into a buffer. It is then either saved,
		 * set as a ringtone or deleted, depending on the user choice. These
		 * actions are triggered based on the state of the REC button.
		 */
		class RecordToggle extends UiTextToggle {

			public RecordToggle(PApplet p) {
				super(p);
			}

			@Override
			public void isTrue() {

				// Note: maximum record time is 30s, this is set independently
				// in Pd
				start = 30000;
				// tell pd to start recording into a temp buffer
				PdBase.sendFloat("pd_record", 1);
				isRecording = true;
				// integer counter to count the rec time. used later to set the
				// rec play button state
				countertest = 0;
				edit1 = prefs.edit();

				// in case the recording button is pressed when the play button
				// is off, start playing
				if (!toolbar.playToggleB.state) {
					toolbar.playToggleB.isTrue();
					toolbar.playToggleB.state = true;
				}

				// run countdowntimer thread
				runOnUiThread(new Runnable() {
					@Override
					public void run() {
						timer = new CountDownTimer(start, 1000) {

							@Override
							public void onFinish() {
								PdBase.sendFloat("pd_record", 0);
								REC_TEXT = "REC";
								toolbar.recordToggleB.state = false;
								System.out.println("Recording length = "
										+ String.valueOf(countertest));
								// save length to prefs
								edit1.putFloat("timer", 30);
								edit1.commit();
								countertest = 0;

								record();
								isRecording = false;

							}

							@Override
							public void onTick(long millisUntilFinished) {
								REC_TEXT = String
										.valueOf(millisUntilFinished / 1000);
								countertest++;
							}

						}.start();
					}
				});

			}

			@Override
			public void isFalse() {

				PdBase.sendFloat("pd_record", 0);
				REC_TEXT = "REC";
				timer.cancel();
				if (isRecording) {
					edit1.putFloat("timer", countertest);
					edit1.commit();

					record();
					System.out.println("Recording length = "
							+ String.valueOf(countertest));

					countertest = 0;
					tracker.sendEvent("ui_Action", "button_press",
							"record_button", 0L);
				}
			}

		}

	}

	boolean isRecording = false;

	public void record() {
		toolbar.playToggleB.isFalse();
		toolbar.playToggleB.state = false;

		PdBase.sendFloat("pd_playToggle", 0);

		SynthCircle.this.runOnUiThread(new Runnable() {
			public void run() {
				DialogFragment dialog = new RecordDialog();
				dialog.show(getFragmentManager(), "recordingfragment");
			}
		});
		isRecording = false;
	}

	// interface methods from the recording dialog
	@Override
	public void onPlayTrue() {
		System.out.println("play called from dialog");
		PdBase.sendFloat("pd_recordPlay", 1);

	}

	@Override
	public void onPlayFalse() {
		System.out.println("stop called from dialog");
		PdBase.sendFloat("pd_recordPlay", 0);

	}

	@Override
	public void onPositiveAction() {
		String root = Environment.getExternalStorageDirectory().toString();
		prepareRecord();
		copyFile(saveFilePath + "/" + saveFileName, root
				+ "/Ringtones/CircleSynthRing.wav");
		setRingtone();
		// new LoadViewTask().execute();
		toast(getString(R.string.ringtone));

	}

	@Override
	public void onNegativeAction() {
		// Log.d("listener","cancel");
		PdBase.sendFloat("pd_recordPlay", 0);
		PdBase.sendBang("pd_recordCancel");
	}

	@Override
	public void onNeutralAction() {
		// Log.d("listener","Save");
		prepareRecord();
		toast(getString(R.string.rec_saved));
	}

	String saveFilePath;
	String saveFileName;

	// prepare the file system to store the recordings
	public void prepareRecord() {

		String root = Environment.getExternalStorageDirectory().toString();
		File myDir = new File(root + "/circlesynth/recordings");
		myDir.mkdirs();
		SimpleDateFormat formatter = new SimpleDateFormat("MMddHHmm");
		Date now = new Date();
		String fileName = formatter.format(now);
		String fname = "recording_" + fileName;
		saveFilePath = myDir.getAbsolutePath();
		saveFileName = fname + ".wav";
		PdBase.sendSymbol("pd_path", myDir + "/" + fname);

	}

	/**
	 * Here we must copy the recording to be set as a ringtone into the user's
	 * /Ringtones folder, and then create the MediaStore entry so that we can
	 * proceed to set it as the user's default ringtone.
	 */
	public void setRingtone() {

		String path = Environment.getExternalStorageDirectory().toString()
				+ "/Ringtones";
		File k = new File(path, "CircleSynthRing.wav");
		ContentValues values = new ContentValues();
		values.put(MediaStore.MediaColumns.DATA, k.getAbsolutePath());
		values.put(MediaStore.MediaColumns.TITLE, "CircleSynthRingtone");
		values.put(MediaStore.MediaColumns.SIZE, k.length());
		values.put(MediaStore.MediaColumns.MIME_TYPE, "audio/vnd.wave");
		values.put(MediaStore.Audio.Media.ARTIST, "CircleSynth");
		values.put(MediaStore.Audio.Media.IS_RINGTONE, true);
		values.put(MediaStore.Audio.Media.IS_NOTIFICATION, false);
		values.put(MediaStore.Audio.Media.IS_ALARM, false);
		values.put(MediaStore.Audio.Media.IS_MUSIC, false);

		System.out.println("getting the right file "
				+ String.valueOf(k.length()));

		// Insert it into the database
		Uri uri = MediaStore.Audio.Media.getContentUriForPath(k.toString());
		// delete previous entries - do this to avoid duplicate entries of the
		// same name
		getContentResolver().delete(
				uri,
				MediaStore.MediaColumns.DATA + "=\"" + k.getAbsolutePath()
						+ "\"", null);
		System.out.println("file in question " + k.getAbsolutePath());

		// insert new values into the DB
		Uri newUri = this.getContentResolver().insert(uri, values);

		// set as default ringtone
		try {
			RingtoneManager.setActualDefaultRingtoneUri(getBaseContext(),
					RingtoneManager.TYPE_RINGTONE, newUri);
		} catch (Throwable t) {
			Log.d(TAG, "catch exception");
			System.out.println("ringtone set exception " + t.getMessage());
		}

	}

	// copying the file from the recordings folder to the Ringtones folder on
	// the sdcard
	public static boolean copyFile(String from, String to) {
		try {
			int bytesum = 0;
			int byteread = 0;
			File oldfile = new File(from);
			File newfile = new File(Environment.getExternalStorageDirectory()
					.toString() + "/Ringtones");
			if (!newfile.exists())
				newfile.mkdir();

			if (oldfile.exists()) {
				InputStream inStream = new FileInputStream(from);
				FileOutputStream fs = new FileOutputStream(to);
				byte[] buffer = new byte[1024];
				while ((byteread = inStream.read(buffer)) != -1) {
					bytesum += byteread;
					fs.write(buffer, 0, byteread);
				}
				inStream.close();
				fs.close();
				// System.out.println("success copy");
			}

			return true;
		} catch (Exception e) {
			System.out.println(e.getMessage());
			return false;
		}
	}

	// unpack the demo sketches from the assets resource folder to the sdcard
	private void copyDemos() {
		String basepath = Environment.getExternalStorageDirectory().toString()
				+ "/circlesynth";
		AssetManager assetManager = getResources().getAssets();
		String[] files = null;
		try {
			files = assetManager.list("demos");
		} catch (Exception e) {
			Log.e("read demo ERROR", e.toString());
			e.printStackTrace();
		}
		for (int i = 0; i < files.length; i++) {
			InputStream in = null;
			OutputStream out = null;
			try {
				in = assetManager.open("demos/" + files[i]);
				out = new FileOutputStream(basepath + "/demos/" + files[i]);
				copyFile(in, out);
				in.close();
				in = null;
				out.flush();
				out.close();
				out = null;
			} catch (Exception e) {
				Log.e("copy demos ERROR", e.toString());
				e.printStackTrace();
			}
		}
	}

	// do the actual copying. HORRIBLE naming. beat yourself up.
	private void copyFile(InputStream in, OutputStream out) throws IOException {
		byte[] buffer = new byte[1024];
		int read;
		while ((read = in.read(buffer)) != -1) {
			out.write(buffer, 0, read);
		}
	}

}
