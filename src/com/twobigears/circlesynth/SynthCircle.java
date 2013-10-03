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
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.IBinder;
import android.preference.PreferenceManager;
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

//import controlP5.Button;
//import controlP5.ControlP5;
//import controlP5.ControllerView;
//import controlP5.Toggle;

public class SynthCircle extends PApplet implements OnBpmChangedListener,
		OnSharedPreferenceChangeListener, SensorEventListener {

	public static final String TAG = "CircleSynth";

	Context context;
	SharedPreferences prefs;
	int fRate;

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
	int storeSize = 0;

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

	boolean playValue = false; // global boolean of the play button state
	boolean revValue = false; // global reverse value for play
	boolean bpmPopup = false; // bpm popup button value
	boolean clearButton = false;
	boolean fxToggle = false;
	boolean fxCirc1Toggle = false;
	boolean fxCirc2Toggle = false;
	boolean fxCirc3Toggle = false;
	boolean fxCirc4Toggle = false;
	boolean fxCirc0Toggle = false;
	boolean fxCheck = false;
	boolean settingsButton = false;
	boolean fxClearButton = false;
	boolean saveButton = false;
	boolean loadButton = false;
	boolean shareButton = false;

	private SensorManager mSensorManager;
	private Sensor mAccelerometer;
	boolean accel;
	float currentAccel = 0;
	static final float ALPHA = 0.15f;
	float oldAccel = 0;
	
	float pX, pY;
	public float density;
	DecimalFormat df, df1;

	float a;
	int effect;
	int col;

	boolean moveflag = false;
	boolean headerflag = false;
	public float scanline;

	

//	public ControlP5 cp5;

	// Processing vars and obj

	PFont robotoFont;
	PGraphics header, sketchBG, scanSquare;
	
	/*
	PGraphics bpmButtonOff;
	PGraphics bpmButtonOn;
	PGraphics fxToggleOff;
	PGraphics fxToggleOn;
	PGraphics fxCirc0;
	PGraphics fxCirc1;
	PGraphics fxCirc2;
	PGraphics fxCirc3;
	PGraphics fxCirc4;
	PGraphics fxClearButtonOff;
	PGraphics fxClearButtonOn;
	*/
	
	// button images
	PImage shareImg, playImg, stopImg, revImg, forImg, clearOnImg, clearOffImg,
			loadOffImg, loadOnImg, saveOffImg, saveOnImg, shareOffImg,
			shareOnImg, settingsOnImg, settingsOffImg, innerCircleImg,
			outerCircleImg, lineCircleImg;
	
	PlayToggle playToggleB;
	ReverseToggle reverseToggleB;
	FxToggle fxToggleB; 
	BpmButton bpmButtonB;
	ClearButton clearButtonB;
	
	float outerCircSize, innerCircSize, animCircSize;
	
	int mainHeadHeight, shadowHeight, scanSquareY, headerHeight, buttonPad;
	
	float textAscent;

	int bpm = 120;
	float bpmScale = constrain(bpm / 24, 4, 20);

	final int bgCol = color(28, 28, 28);
	final int headCol = color(41, 41, 41);
	final int buttonActCol = color(255);
	final int buttonInActCol = color(175);
	final int opaInActCol = color(255, 100);
	final int scanHeadCol = color(255, 40);
	final int circ2Col = color(0, 153, 204);
	final int col1 = color(255, 68, 68);
	final int col2 = color(255, 187, 51);
	final int col3 = color(153, 204, 0);
	final int col4 = color(170, 102, 204);
	final int col5 = color(51, 181, 229);

	// setup libpd stuff//

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

	protected void initPd() throws IOException {

		// Configure the audio glue
		int sampleRate = AudioParameters.suggestSampleRate();

		// reducing sample rate based on no. of cores
		if (getNumCores() == 1)
			sampleRate = sampleRate / 4;
		else
			sampleRate = sampleRate / 2;

		pdService.initAudio(sampleRate, 0, 2, 10.0f);
		// pdService.startAudio();
		pdService.startAudio(new Intent(this, SynthCircle.class),
				R.drawable.notif_icon, "CircleSynth", "Return to Circle Synth");

		dispatcher = new PdUiDispatcher();
		PdBase.setReceiver(dispatcher);

		dispatcher.addListener("scan", new PdListener.Adapter() {
			@Override
			public void receiveFloat(String source, final float x) {
				scanline = x;
				detect();
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

		// Set context
		EasyTracker.getInstance().setContext(getApplicationContext());
		// Instantiate the Tracker
		tracker = EasyTracker.getTracker();
		symbols.setDecimalSeparator('.');
		df = new DecimalFormat("#.##", symbols);
		df1 = new DecimalFormat("#.#", symbols);
		prefs = PreferenceManager.getDefaultSharedPreferences(this);
		mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
		mAccelerometer = mSensorManager
				.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);

		mSensorManager.registerListener(this, mAccelerometer, 100000);
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

		initPdService();
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
		// dispatcher.release();
		unbindService(pdConnection);
		// EasyTracker.getInstance().activityStop(this); // Add this method.

		prefs.unregisterOnSharedPreferenceChangeListener(this);
		mSensorManager.unregisterListener(this);

	}

	@Override
	protected void onStart() {
		super.onStart();
		EasyTracker.getInstance().activityStart(this); // Add this method
	}

	@Override
	protected void onStop() {
		super.onStop();
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

//		if (getNumCores() <= 1)
//			fRate = 20;
//		else
//			fRate = 30;
//		frameRate(fRate);
//		orientation(LANDSCAPE);
		
		dots = new ArrayList<Dot>();
		stored = new ArrayList<String>();

		background(bgCol);
		smooth(8);
		CHECK = new String[10];
		SAVE = new String[10];
		for (int i = 0; i < maxCircle; i++) {
			CHECK[i] = "0 5 5 5 5 0";
		}
		
		String resSuffix = "30";

		DisplayMetrics dm = new DisplayMetrics();
		getWindowManager().getDefaultDisplay().getMetrics(dm);
		float densityR = dm.density; // get android screen density

		if (densityR < 0.9) {
			density = (float) 0.75;
			textAscent = 0.62f;
			resSuffix = "_x75";
		} else if (densityR > 0.9 && densityR <= 1.2) {
			density = (float) 1;
			textAscent = 0.52f;
			resSuffix = "_x100";
		} else if (densityR > 1.2 && densityR <= 1.6) {
			density = (float) 1.5;
			textAscent = 0.32f;
			resSuffix = "_x150";
		} else if (densityR > 1.6) {
			density = (float) 2;
			textAscent = 0.22f;
			resSuffix = "_x200";
		}
		
		playImg = loadImage("play"+resSuffix+".png");
		stopImg = loadImage("stop"+resSuffix+".png");
		revImg = loadImage("reverse"+resSuffix+".png");
		forImg = loadImage("forward"+resSuffix+".png");
		clearOnImg = loadImage("clearOn"+resSuffix+".png");
		clearOffImg = loadImage("clearOff"+resSuffix+".png");
		loadOffImg = loadImage("loadOff"+resSuffix+".png");
		loadOnImg = loadImage("loadOn"+resSuffix+".png");
		saveOffImg = loadImage("saveOff"+resSuffix+".png");
		saveOnImg = loadImage("saveOn"+resSuffix+".png");
		shareOffImg = loadImage("shareOff"+resSuffix+".png");
		shareOnImg = loadImage("shareOn"+resSuffix+".png");
		settingsOffImg = loadImage("settingsOff"+resSuffix+".png");
		settingsOnImg = loadImage("settingsOn"+resSuffix+".png");
		innerCircleImg = loadImage("inner_circle"+resSuffix+".png");
		outerCircleImg = loadImage("outer_circle"+resSuffix+".png");
		lineCircleImg = loadImage("line_circle"+resSuffix+".png");
		
		robotoFont = createFont("Roboto-Thin-12", 22 * density, true);
		
		// buttons
		playToggleB = new PlayToggle(this);
		playToggleB.load(playImg, stopImg);
		reverseToggleB = new ReverseToggle(this);
		reverseToggleB.load(revImg, forImg);
		fxToggleB = new FxToggle(this);
		fxToggleB.load(robotoFont);
		fxToggleB.setSize(revImg.width, revImg.height);
		bpmButtonB = new BpmButton(this);
		bpmButtonB.load(robotoFont);
		bpmButtonB.setSize(revImg.width, revImg.height);
		clearButtonB = new ClearButton(this);
		clearButtonB.load(clearOffImg, clearOnImg);
		

		mainHeadHeight = (int) (40 * density); 
		shadowHeight = (int) (density);
		scanSquareY = (int) (3 * density);
		headerHeight = mainHeadHeight + scanSquareY + shadowHeight;	 
		buttonPad = (int) (10 * density);

		outerCircSize = 40 * density; // outer circle size configured to dpi
		innerCircSize = 15 * density; // inner circle size configured to dpi
		animCircSize = 18 * density;
		
		header = createGraphics(width, headerHeight);
		header.beginDraw();
		header.noStroke();
		header.background(headCol);
		header.fill(buttonInActCol, 40);
		header.rect(0, mainHeadHeight, width, scanSquareY);
		header.fill(10);
		header.rect(0, mainHeadHeight+scanSquareY, width, shadowHeight);
		header.endDraw();
		
		sketchBG = createGraphics(width, height - headerHeight);
		sketchBG.beginDraw();
		sketchBG.background(bgCol);
		sketchBG.endDraw();
		
		scanSquare = createGraphics((int) (10 * density), scanSquareY);
		scanSquare.beginDraw();
		scanSquare.noStroke();
		scanSquare.fill(120);
		scanSquare.rect(0, 0, 7 * density, 3 * density, 7 * density);
		scanSquare.endDraw();

	}

	private void initialisepatch() {

		String value = prefs.getString("preset", null);
		SharedPreferences prefs1 = getPreferences(SynthCircle.MODE_PRIVATE);
		if (value == null) {

			Editor editor = prefs1.edit();
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

		String val = prefs.getString("scale", "1");
		int valf = Integer.valueOf(val);
		PdBase.sendFloat("pd_scales", valf);

		String tran = prefs.getString("transposeOct", "3");
		int tranf = Integer.valueOf(tran);
		tranf = tranf - 3;
		PdBase.sendFloat("pd_octTrans", tranf);

		String tran1 = prefs.getString("transposeNote", "0");
		int tranf1 = Integer.valueOf(tran1);
		PdBase.sendFloat("pd_noteTrans", tranf1);

		boolean accely = prefs1.getBoolean("accel", false);
		if (accely)
			PdBase.sendFloat("pd_accelToggle", 1);
		else
			PdBase.sendFloat("pd_accelToggle", 0);

		boolean delayy = prefs1.getBoolean("delay", false);
		if (delayy)
			PdBase.sendFloat("pd_delayToggle", 1);
		else
			PdBase.sendFloat("pd_delayToggle", 0);

		boolean reverby = prefs1.getBoolean("reverb", false);
		if (reverby)
			PdBase.sendFloat("pd_reverbToggle", 1);
		else
			PdBase.sendFloat("pd_reverbToggle", 0);

		boolean tuts = prefs.getBoolean("first_tutorial", false);
		if (!tuts) {

			MiscDialogs.showTutorialDialog(SynthCircle.this);
			tuts = true;
			Editor editor = prefs.edit();
			editor.putBoolean("first_tutorial", true);
			editor.commit();
		}

	}
	
	/*
	private void settingup() {
		
		TODO Use image res >
		  
		bpmButtonOn = createGraphics(headerButtonSize * 2, headerButtonSize);
		bpmButtonOn.beginDraw();
		bpmButtonOn.stroke(buttonActCol);
		bpmButtonOn.strokeWeight(buttonWeight);
		bpmButtonOn.noFill();
		bpmButtonOn.rect(0, 0, headerButtonSize * 2, headerButtonSize);
		bpmButtonOn.endDraw();

		bpmButtonOff = createGraphics(headerButtonSize * 2, headerButtonSize);
		bpmButtonOff.beginDraw();
		bpmButtonOff.stroke(buttonInActCol);
		bpmButtonOff.strokeWeight(buttonWeight);
		bpmButtonOff.noFill();
		bpmButtonOff.rect(0, 0, headerButtonSize * 2, headerButtonSize);
		bpmButtonOff.endDraw();

		float pad = (float) (buttonSize2 * 0.25);

		fxToggleOff = createGraphics(headerButtonSize, headerButtonSize);
		fxToggleOff.beginDraw();
		fxToggleOff.stroke(buttonInActCol);
		fxToggleOff.strokeWeight(buttonWeight);
		fxToggleOff.noFill();
		fxToggleOff.rect(0, 0, headerButtonSize, headerButtonSize);
		fxToggleOff.textFont(f);
		fxToggleOff.textAlign(CENTER);
		fxToggleOff.fill(buttonInActCol);
		float asc = (float) (textAscent() * 0.3 * density);
		fxToggleOff.text("FX", (float) (headerButtonSize * 0.5),
				(float) ((headerButtonSize * 0.5) + asc));
		fxToggleOff.endDraw();

		fxToggleOn = createGraphics(headerButtonSize, headerButtonSize);
		fxToggleOn.beginDraw();
		fxToggleOn.stroke(buttonActCol);
		fxToggleOn.strokeWeight(buttonWeight);
		fxToggleOn.noFill();
		fxToggleOn.rect(0, 0, headerButtonSize, headerButtonSize);
		fxToggleOn.textFont(f);
		fxToggleOn.textAlign(CENTER);
		fxToggleOn.fill(buttonActCol);
		fxToggleOn.text("FX", (float) (headerButtonSize * 0.5),
				(float) ((headerButtonSize * 0.5) + asc));
		fxToggleOn.endDraw();

		fxCirc1 = createGraphics(buttonSize2, buttonSize2);
		fxCirc1.beginDraw();
		fxCirc1.noStroke();
		fxCirc1.fill(col2);
		fxCirc1.ellipseMode(CORNER);
		fxCirc1.ellipse(0, 0, buttonSize2, buttonSize2);
		fxCirc1.endDraw();

		fxCirc2 = createGraphics(buttonSize2, buttonSize2);
		fxCirc2.beginDraw();
		fxCirc2.noStroke();
		fxCirc2.fill(col3);
		fxCirc2.ellipseMode(CORNER);
		fxCirc2.ellipse(0, 0, buttonSize2, buttonSize2);
		fxCirc2.endDraw();

		fxCirc3 = createGraphics(buttonSize2, buttonSize2);
		fxCirc3.beginDraw();
		fxCirc3.noStroke();
		fxCirc3.fill(col4);
		fxCirc3.ellipseMode(CORNER);
		fxCirc3.ellipse(0, 0, buttonSize2, buttonSize2);
		fxCirc3.endDraw();

		fxCirc4 = createGraphics(buttonSize2, buttonSize2);
		fxCirc4.beginDraw();
		fxCirc4.noStroke();
		fxCirc4.fill(col5);
		fxCirc4.ellipseMode(CORNER);
		fxCirc4.ellipse(0, 0, buttonSize2, buttonSize2);
		fxCirc4.endDraw();

		float strokeSize = 2 * density;
		fxCirc0 = createGraphics((int) (buttonSize2 + strokeSize),
				(int) (buttonSize2 + strokeSize));
		fxCirc0.beginDraw();
		fxCirc0.stroke(buttonInActCol);
		fxCirc0.strokeWeight(strokeSize);
		fxCirc0.noFill();
		fxCirc0.ellipseMode(CENTER);
		fxCirc0.ellipse(buttonSize2 * 0.5f, buttonSize2 * 0.5f, buttonSize2
				- strokeSize, buttonSize2 - strokeSize);
		fxCirc0.endDraw();

		fxClearButtonOff = createGraphics(buttonSize2, buttonSize2);
		fxClearButtonOff.beginDraw();
		fxClearButtonOff.stroke(buttonInActCol);
		fxClearButtonOff.noFill();
		fxClearButtonOff.strokeWeight(strokeSize);
		fxClearButtonOff.line(pad, pad, buttonSize2 - pad, buttonSize2 - pad);
		fxClearButtonOff.line(pad, buttonSize2 - pad, buttonSize2 - pad, pad);
		fxClearButtonOff.ellipse((float) (buttonSize2 * 0.5f),
				(float) (buttonSize2 * 0.5f),
				(float) (buttonSize2 - strokeSize),
				(float) (buttonSize2 - strokeSize));
		fxClearButtonOff.endDraw();

		fxClearButtonOn = createGraphics(buttonSize2, buttonSize2);
		fxClearButtonOn.beginDraw();
		fxClearButtonOn.stroke(buttonActCol);
		fxClearButtonOn.noFill();
		fxClearButtonOn.strokeWeight(strokeSize);
		fxClearButtonOn.line(pad, pad, buttonSize2 - pad, buttonSize2 - pad);
		fxClearButtonOn.line(pad, buttonSize2 - pad, buttonSize2 - pad, pad);
		fxClearButtonOn.ellipse((float) (buttonSize2 * 0.5f),
				(float) (buttonSize2 * 0.5f),
				(float) (buttonSize2 - strokeSize),
				(float) (buttonSize2 - strokeSize));
		fxClearButtonOn.endDraw();
		
		TODO Controlp5 stuff. Replace! >
		
		cp5 = new ControlP5(this);
		cp5.addToggle("playValue")
				// create play/stop toggle
				.setPosition(buttonSpace, 0).setSize(headerButtonSize, headerButtonSize)
				.setView(new PlayToggle());

		cp5.addToggle("revValue")
				// create reverse toggle
				.setPosition(headerButtonSize + (buttonSpace * 2), 0)
				.setSize(headerButtonSize, headerButtonSize).setView(new RevToggle());

		cp5.addButton("bpmPopup")
				// create bpm popup button
				.setPosition((headerButtonSize * 2) + (buttonSpace * 3), 0)
				.setSize(headerButtonSize, headerButtonSize).setView(new BpmButton());

		cp5.addButton("clearButton")
				// create clear button
				.setPosition((headerButtonSize * 3) + (buttonSpace * 4), 0)
				.setSize(headerButtonSize, headerButtonSize).setView(new ClearButton());
		cp5.addToggle("fxToggle")
				// create fx toggle
				.setPosition((headerButtonSize * 4) + (buttonSpace * 5), 0)
				.setSize(headerButtonSize, headerButtonSize).setView(new FxToggle());

		cp5.addToggle("fxCirc1Toggle").hide()
				// create fx1 toggle
				.setPosition(buttonPad + button2Space, buttonPad)
				.setSize(buttonSize2, buttonSize2).setView(new FxCircle1());

		cp5.addToggle("fxCirc2Toggle")
				.hide()
				// create fx2 toggle
				.setPosition(
						buttonSize2 + (button2Space * 2) + (buttonPad * 2),
						buttonPad).setSize(buttonSize2, buttonSize2)
				.setView(new FxCircle2());

		cp5.addToggle("fxCirc3Toggle")
				.hide()
				// create fx3 toggle
				.setPosition(
						(buttonSize2 * 2) + (button2Space * 3)
								+ (buttonPad * 3), buttonPad)
				.setSize(buttonSize2, buttonSize2).setView(new FxCircle3());

		cp5.addToggle("fxCirc4Toggle")
				.hide()
				// create fx4 toggle
				.setPosition(
						(buttonSize2 * 3) + (button2Space * 4)
								+ (buttonPad * 4), buttonPad)
				.setSize(buttonSize2, buttonSize2).setView(new FxCircle4());

		cp5.addToggle("fxCirc0Toggle")
				.hide()
				// create fx clear toggle
				.setPosition(
						(buttonSize2 * 4) + (button2Space * 5)
								+ (buttonPad * 5), buttonPad)
				.setSize(buttonSize2, buttonSize2).setView(new FxCircle0());

		cp5.addButton("fxClearButton")
				// create clear button
				.setPosition(
						(buttonSize2 * 5) + (button2Space * 6)
								+ (buttonPad * 6), buttonPad)
				.setSize(buttonSize2, buttonSize2).setView(new FxClearButton());

		cp5.addButton("settingsButton")
				.setPosition(width - headerButtonSize - buttonSpace, 0)
				.setSize(headerButtonSize, headerButtonSize).setView(new SettingsButton());

		cp5.addButton("shareButton")
				.setPosition(width - (headerButtonSize * 2) - (buttonSpace * 2), 0)
				.setSize(headerButtonSize, headerButtonSize).setView(new ShareButton());

		cp5.addButton("saveButton")
				.setPosition(width - (headerButtonSize * 3) - (buttonSpace * 3), 0)
				.setSize(headerButtonSize, headerButtonSize).setView(new SaveButton());

		cp5.addButton("loadButton")
				.setPosition(width - (headerButtonSize * 4) - (buttonSpace * 4), 0)
				.setSize(headerButtonSize, headerButtonSize).setView(new LoadButton());	
	}
	*/

	public void draw() {

		image(sketchBG, 0, headerHeight);

		// draw dots on the screen based on touch data stored in Dot class
		drawThis();
		
		image(header, 0, 0);
		image(scanSquare, scanline * width, mainHeadHeight);
		
		//buttons
		playToggleB.drawIt(buttonPad, 0);
		reverseToggleB.drawIt(playToggleB.getWidth()+buttonPad, 0);
		bpmButtonB.drawIt(String.valueOf(bpm), (playToggleB.getWidth()+buttonPad)*2, 0);
		clearButtonB.drawIt((playToggleB.getWidth()+buttonPad)*3, 0);
		fxToggleB.drawIt("FX", (playToggleB.getWidth()+buttonPad)*4, 0);

	}

	/* 
	 * TODO Move below to button listeners
	 * 
	if (clearButton) {

		dots.clear();
		stored.clear();
		PdBase.sendFloat("pd_playToggle", 0);
		toast("Sketch Cleared");

	}
	if (loadButton) {

		File mPath = new File(Environment.getExternalStorageDirectory()
				+ "/circlesynth");
		fileDialog = new FileDialog(this, mPath);
		fileDialog.setFileEndsWith(".txt");
		fileDialog.addFileListener(new FileDialog.FileSelectedListener() {
			public void fileSelected(File file) {
				fName = file.getName();
				try {

					dots.clear();
					stored.clear();

					FileInputStream input = new FileInputStream(file);
					DataInputStream din = new DataInputStream(input);

					for (int i = 0; i < maxCircle; i++) { // Read lines
						String line = din.readUTF();
						stored.add(line);
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

		SynthCircle.this.runOnUiThread(new Runnable() {
			public void run() {

				fileDialog.showDialog();
			}
		});
		loadButton = false;
	}

	if (fxClearButton) {

		for (int i = 0; i < dots.size(); i++) {
			Dot d = (Dot) dots.get(i);
			d.fxClear();
		}
		toast("All FX cleared");
		fxClearButton = false;
	}

	else {
		if (playValue) {

			PdBase.sendFloat("pd_playToggle", 1);
			sendPdValue();

		}

		else {
			PdBase.sendFloat("pd_playToggle", 0);
			scanline = 0;
		}
	}

	clearButton = false;

	if (bpmPopup) {
		toast("Set BPM/Speed");
		SynthCircle.this.runOnUiThread(new Runnable() {
			public void run() {
				new BpmPicker(SynthCircle.this, SynthCircle.this, bpm)
						.show();
			}
		});

		bpmPopup = false;
	}

	if (settingsButton) {
		tracker.trackEvent("Buttons Category", "Settings", "", 0L);
		Intent intent = new Intent(this, SynthSettingsTwo.class);
		startActivity(intent);
		prefs.registerOnSharedPreferenceChangeListener(this);
		settingsButton = false;

	}
	if (saveButton) {
		saveButton = false;
		stored.clear();
		int t1 = 0;
		int t2 = 0;
		int t3 = 0;
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

		}

		String root = Environment.getExternalStorageDirectory().toString();
		File myDir = new File(root + "/circlesynth");
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
	if (revValue)
		PdBase.sendFloat("pd_revToggle", 1);
	else
		PdBase.sendFloat("pd_revToggle", 0);

	if (fxCirc1Toggle == true) {
		fxCirc(col2);
	} else if (fxCirc2Toggle == true) {
		fxCirc(col3);
	} else if (fxCirc3Toggle == true) {
		fxCirc(col4);
	} else if (fxCirc4Toggle == true) {
		fxCirc(col5);
	} else if (fxCirc0Toggle == true) {
		fxCirc(buttonInActCol);
	}
	
	*/
	
	public void detect() {

		if (dots.size() > 0) {
			for (int i = 0; i < dots.size(); i++) {
				Dot d = dots.get(i);
				if (!d.touched2) {
					if (!revValue) {
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
					if (!revValue) {
						if (Math.abs(scanline - dxd) <= .01) {
							d.selected1 = d.selected2 = true;
						}

					}
					if (revValue) {
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
			if (!d.selected1)
				d.size5 = 0;
			if (!playValue)
				d.selected1 = d.selected2 = false;

			if (d.touched3)
				band(d.xDown, d.yDown, d.xLine, d.yLine);

			if (d.touched1)
				d.create1(d.xDown, d.yDown);

			if (d.touched2)
				d.create2(d.xUp, d.yUp);
			if (d.selected1 && d.selected2)
				d.create4(d.xDown, d.yDown, d.xUp, d.yUp);
			if (d.selected1 && !d.selected2)
				d.create3(d.xDown, d.yDown);

		}
	}

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

	public void splitString(String string) {

		String[] pieces = string.split(" ");

		int index = Integer.parseInt(pieces[0]);
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
			if (t2 == 1)
				d.touched2 = true;
			else
				d.touched2 = false;

			if (t3 == 1)
				d.touched3 = true;
			else
				d.touched3 = false;

		} else {
			d.xDown = (Float.parseFloat(pieces[1]));
			d.yDown = (Float.parseFloat(pieces[2]));
			d.xLine = d.xUp = (Float.parseFloat(pieces[3]));
			d.yLine = d.yUp = (Float.parseFloat(pieces[4]));
		}

	}
/*
	class PlayToggle implements ControllerView<Toggle> {
		public void display(PApplet theApplet, Toggle theButton) {
			theApplet.pushMatrix();
			if (playValue == true) {
				theApplet.image(stopImg, 0, 0);

			} else {
				theApplet.image(playImg, 0, 0);
			}
			theApplet.popMatrix();
		}
	}

	class RevToggle implements ControllerView<Toggle> {
		public void display(PApplet theApplet, Toggle theButton) {
			theApplet.pushMatrix();
			if (revValue == true) {
				theApplet.image(forImg, 0, 0);
			} else {
				theApplet.image(revImg, 0, 0);
			}
			theApplet.popMatrix();
		}
	}

	class BpmButton implements ControllerView<Button> {
		public void display(PApplet theApplet, Button theButton) {
			float a;
			theApplet.pushMatrix();
			if (theButton.isPressed()) {
				theApplet.textFont(f);
				theApplet.textAlign(CENTER);
				theApplet.fill(buttonActCol);
				a = textAscent() * textAscent * density;
				theApplet.text((int) (bpm), (headerButtonSize) * 0.5f,
						(headerButtonSize * 0.5f) + a);
			} else {
				theApplet.textFont(f);
				theApplet.textAlign(CENTER);
				theApplet.fill(buttonInActCol);
				a = textAscent() * textAscent * density;
				theApplet.text((int) (bpm), (headerButtonSize) * 0.5f,
						(headerButtonSize * 0.5f) + a);
			}
			theApplet.popMatrix();
		}
	}

	class ClearButton implements ControllerView<Button> {
		public void display(PApplet theApplet, Button theButton) {
			theApplet.pushMatrix();
			if (theButton.isPressed()) {
				theApplet.image(clearOnImg, 0, 0);
			} else {
				theApplet.image(clearOffImg, 0, 0);
			}
			theApplet.popMatrix();
		}
	}

	class FxCircle1 implements ControllerView<Toggle> {
		public void display(PApplet theApplet, Toggle theButton) {
			theApplet.pushMatrix();
			if (fxCirc1Toggle == true) {
				theApplet.image(fxCirc1, 0, 0);
			} else {
				theApplet.image(fxCirc1, 0, 0);
			}
			theApplet.popMatrix();
		}
	}

	class FxCircle2 implements ControllerView<Toggle> {
		public void display(PApplet theApplet, Toggle theButton) {
			theApplet.pushMatrix();
			if (fxCirc2Toggle == true) {
				theApplet.image(fxCirc2, 0, 0);
			} else {
				theApplet.image(fxCirc2, 0, 0);
			}
			theApplet.popMatrix();
		}
	}

	class FxCircle3 implements ControllerView<Toggle> {
		public void display(PApplet theApplet, Toggle theButton) {
			theApplet.pushMatrix();
			if (fxCirc3Toggle == true) {
				theApplet.image(fxCirc3, 0, 0);
			} else {
				theApplet.image(fxCirc3, 0, 0);
			}
			theApplet.popMatrix();
		}
	}

	class FxCircle4 implements ControllerView<Toggle> {
		public void display(PApplet theApplet, Toggle theButton) {
			theApplet.pushMatrix();
			if (fxCirc4Toggle == true) {
				theApplet.image(fxCirc4, 0, 0);
			} else {
				theApplet.image(fxCirc4, 0, 0);
			}
			theApplet.popMatrix();
		}
	}

	class FxCircle0 implements ControllerView<Toggle> {
		public void display(PApplet theApplet, Toggle theButton) {
			theApplet.pushMatrix();
			if (fxCirc0Toggle == true) {
				theApplet.image(fxCirc0, 0, 0);
			} else {
				theApplet.image(fxCirc0, 0, 0);
			}
			theApplet.popMatrix();
		}
	}

	class FxToggle implements ControllerView<Toggle> {
		public void display(PApplet theApplet, Toggle theButton) {
			float a;
			theApplet.pushMatrix();
			if (fxToggle == true) {
				theApplet.textFont(f);
				theApplet.textAlign(CENTER);
				theApplet.fill(buttonActCol);
				a = textAscent() * textAscent * density;
				theApplet.text("FX", (headerButtonSize) * 0.5f, (headerButtonSize * 0.5f)
						+ a);
				cp5.getController("playValue").setVisible(false);
				cp5.getController("revValue").setVisible(false);
				cp5.getController("bpmPopup").setVisible(false);
				cp5.getController("clearButton").setVisible(false);
				cp5.getController("fxCirc1Toggle").setVisible(true);
				cp5.getController("fxCirc2Toggle").setVisible(true);
				cp5.getController("fxCirc3Toggle").setVisible(true);
				cp5.getController("fxCirc4Toggle").setVisible(true);
				cp5.getController("fxCirc0Toggle").setVisible(true);
				cp5.getController("fxClearButton").setVisible(true);
			} else {
				theApplet.textFont(f);
				theApplet.textAlign(CENTER);
				theApplet.fill(buttonInActCol);
				a = textAscent() * textAscent * density;
				theApplet.text("FX", (headerButtonSize) * 0.5f, (headerButtonSize * 0.5f)
						+ a);
				cp5.getController("playValue").setVisible(true);
				cp5.getController("revValue").setVisible(true);
				cp5.getController("bpmPopup").setVisible(true);
				cp5.getController("clearButton").setVisible(true);
				cp5.getController("fxCirc1Toggle").setVisible(false);
				cp5.getController("fxCirc2Toggle").setVisible(false);
				cp5.getController("fxCirc3Toggle").setVisible(false);
				cp5.getController("fxCirc4Toggle").setVisible(false);
				cp5.getController("fxCirc0Toggle").setVisible(false);
				cp5.getController("fxClearButton").setVisible(false);
			}
			theApplet.popMatrix();
		}
	}

	class SettingsButton implements ControllerView<Button> {
		public void display(PApplet theApplet, Button theButton) {
			theApplet.pushMatrix();
			if (theButton.isPressed()) {
				theApplet.image(settingsOnImg, 0, 0);
			} else {
				theApplet.image(settingsOffImg, 0, 0);
			}
			theApplet.popMatrix();
		}
	}

	class FxClearButton implements ControllerView<Button> {
		public void display(PApplet theApplet, Button theButton) {
			theApplet.pushMatrix();
			if (theButton.isPressed()) {
				theApplet.image(fxClearButtonOn, 0, 0);
			} else {
				theApplet.image(fxClearButtonOff, 0, 0);
			}
			theApplet.popMatrix();
		}
	}

	class SaveButton implements ControllerView<Button> {
		public void display(PApplet theApplet, Button theButton) {
			theApplet.pushMatrix();
			if (theButton.isPressed()) {
				theApplet.image(saveOnImg, 0, 0);
				toast("Sketch Saved in /circlesynth");

			} else {
				theApplet.image(saveOffImg, 0, 0);
			}
			theApplet.popMatrix();
		}
	}

	class LoadButton implements ControllerView<Button> {
		public void display(PApplet theApplet, Button theButton) {
			theApplet.pushMatrix();
			if (theButton.isPressed()) {
				theApplet.image(loadOnImg, 0, 0);
				toast("Load Sketch");

			} else {
				theApplet.image(loadOffImg, 0, 0);
			}
			theApplet.popMatrix();
		}

	}

	class ShareButton implements ControllerView<Button> {
		public void display(PApplet theApplet, Button theButton) {
			theApplet.pushMatrix();
			if (theButton.isPressed()) {
				theApplet.image(shareOnImg, 0, 0);
				shareIt();
				shareButton = false;
			} else {
				theApplet.image(shareOffImg, 0, 0);
			}
			theApplet.popMatrix();
		}
	}
*/
	void fxCirc(int col) {
		if (col == buttonInActCol) {
			stroke(buttonInActCol);
			strokeWeight(2 * density);
			noFill();
			ellipse((float) (mouseX), (float) (mouseY),
					(float) (outerCircSize * 1.2), (float) (outerCircSize * 1.2));
		} else {
			noStroke();
			fill(col, 200);
			ellipse((float) (mouseX), (float) (mouseY),
					(float) (outerCircSize * 1.2), (float) (outerCircSize * 1.2));
		}
	}

	void band(float bX, float bY, float eX, float eY) {

		stroke(opaInActCol);
		strokeWeight(1.5f * density);
		line(bX, bY, eX, eY);

	}

	public int delCheck(float mX, float mY) {
		int checkdelete = -1;
		float disp1, disp2;
		disp1 = disp2 = 0;
		if (dots.size() > 0) {
			for (int i = 0; i < dots.size(); i++) {
				Dot d = (Dot) dots.get(i);
				disp1 = distancecalc(d.xDown, d.yDown, mX, mY);
				disp2 = distancecalc(d.xUp, d.yUp, mX, mY);
				if (disp1 < 40 || disp2 < 40) {
					checkdelete = i;
					i = dots.size();
				}
			}
		}
		return checkdelete;

	}

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

	// The Dot class

	public class Dot {

		float xDown, xUp, yDown, yUp, xLine, yLine, posX, posY;
		boolean touched1, touched2, touched3, selected1, selected2,isMoving,isDeleted;
		float size1 = 0;
		float size2 = 0;
		float size3 = 0;
		float size4 = 0;
		float size5 = 0;
		float opa = 0;
		int doteffect;
		int dotcol = color(255, 68, 68);

		Dot() {
			touched1 = touched2 = touched3 = selected1 = selected2 = isMoving = isDeleted = false;
			size1 = 0;
			size2 = 0;
			size3 = 0;
			size4 = 0;
			size5 = 0;
			opa = 100;
			effect = 0;

		}

		public void fxClear() {
			//effects : 0 - none, 1 - 4 are corresponding fx
			this.doteffect = 0;
			this.dotcol = color(255, 68, 68);
		}

		public void createCircle1(float mX1, float mY1) {
			xDown = mX1;
			yDown = mY1;
			xUp = xDown;
			yUp = yDown;
			touched1 = true;
			touched2 = false;
			touched3 = false;
			

		}

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

		}

		public void createLine(float mX3, float mY3) {
			xLine = mX3;
			yLine = mY3;

			touched3 = true;

		}

		public void create1(float tempX, float tempY) {

			posX = tempX;
			posY = tempY;

			size1 = constrain(size1 += (6 * density), 0, outerCircSize);
			stroke(opaInActCol);
			strokeWeight((float) (1.5 * density));
			fill(bgCol);
			ellipse(posX, posY, size1, size1);

			if (size1 >= 10 * density) {
				size2 = constrain(size2 += (2.5 * density), 0, innerCircSize);
				noStroke();
				fill(dotcol);
				ellipse(posX, posY, size2, size2);

			}

		}

		public void create2(float tempX, float tempY) {

			posX = tempX;
			posY = tempY;

			size3 = constrain(size3 += (6 * density), 0, outerCircSize);
			stroke(opaInActCol);
			strokeWeight((float) (1.5 * density));
			fill(bgCol);
			ellipse(posX, posY, size3, size3);

			if (size3 >= 10 * density) {
				size4 = constrain(size4 += (2.5 * density), 0, innerCircSize);
				noStroke();
				fill(dotcol);
				ellipse(posX, posY, size4, size4);

			}

		}

		public void create3(float mX5, float mY5) {
			opa = 180;
			size5 = constrain(size5 += (2 * bpmScale * density), 0, animCircSize);

			noFill();
			stroke(this.dotcol, opa);
			strokeWeight(2.3f * density);
			ellipse(mX5, mY5, outerCircSize, outerCircSize);

			noStroke();
			fill(this.dotcol);
			ellipse(posX, posY, size5, size5);
		}

		public void create4(float xDown2, float yDown2, float xUp2, float yUp2) {
			opa = 180;
			size5 = constrain(size5 += (2 * bpmScale * density), 0, animCircSize);

			stroke(this.dotcol, opa);
			strokeWeight(2.3f * density);
			line(xDown2, yDown2, xUp2, yUp2);

			fill(bgCol);
			stroke(this.dotcol, opa);
			strokeWeight(2 * density);
			ellipse(xDown2, yDown2, outerCircSize, outerCircSize);

			fill(this.dotcol);
			noStroke();
			ellipse(xDown2, yDown2, size5, size5);

			fill(bgCol);
			stroke(this.dotcol, opa);
			strokeWeight(2.3f * density);
			ellipse(xUp2, yUp2, outerCircSize, outerCircSize);

			fill(this.dotcol);
			noStroke();
			ellipse(xUp2, yUp2, size5, size5);

		}

		public void fx(int f, int col) {
			this.doteffect = f;
			this.dotcol = col;
		}
		
		public int getNodes(){
			int x = 0;
			if(touched1 && touched2)
				x=2;
			else if(touched1 && !touched2)
				x=1;
			else
				x=0;
			return x;
		}
		
		public void updateCircles(float mX, float mY){
			if(Math.abs(mX-xDown)<outerCircSize){
				xDown=mX;
				yDown=mY;
				xLine=xUp;
				yLine=yUp;
				
			}
			else{
				xLine=xUp=mX;
				yLine=yUp=mY;
				
			}
			
				
		}

	}

	@Override
	public boolean dispatchTouchEvent(MotionEvent event) {

		width = displayWidth;
		height = displayHeight;
		float x = (event.getX());
		float y = (event.getY());
		int action = event.getActionMasked();
		int checkdelete = -1;
		if (dots != null) {
			checkdelete = delCheck(x, y);
			Log.d("checkdelete", String.valueOf(checkdelete));
			
				switch (action) {
				case MotionEvent.ACTION_DOWN:

					// button interfaces here
					bpmButtonB.touchDown(x, y);
					clearButtonB.touchDown(x, y);

					if (y > mainHeadHeight) {
						dots.add(new Dot());
						if (checkdelete < 0) {
							Dot d = (Dot) dots.get(dots.size() - 1);
							if (y > mainHeadHeight && dots.size() <= maxCircle) {
								d.createCircle1(x, y);
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
							dots.remove(dots.size() - 1);

							// Log.d("dotsMove",String.valueOf(checkdelete)+" "+String.valueOf(d.isMoving));
						}
						headerflag = false;
					} else
						headerflag = true;

					break;
				case MotionEvent.ACTION_UP:

					// button interfaces here
					playToggleB.touchUp(x, y);
					reverseToggleB.touchUp(x, y);
					fxToggleB.touchUp(x, y);
					bpmButtonB.touchUp(x, y);
					clearButtonB.touchUp(x, y);

					if (!headerflag) {
						if (dots.size() > 0) {
							Dot d1 = (Dot) dots.get(dots.size() - 1);
							if (checkdelete < 0) {
								// if (moveflag)
								// dots.remove(dots.size() - 1);
								if (y < mainHeadHeight) {
									dots.remove(dots.size() - 1);
									toast("You can't draw there");
								}

								if (dots.size() > maxCircle) {
									toast("Circle Limit Reached");
									dots.remove(dots.size() - 1);
								}
								if (d1.touched1 == true) {
									d1.createCircle2(x, y);

								}

							}
							if (checkdelete >= 0) {
								if (x < 10 || x > width - 10 || y > height - 10
										|| y < mainHeadHeight) {
									dots.remove(checkdelete);
									toast("Circle gone!");
								}

							}
							// if (checkdelete >= 0) {
							// if (Math.abs(pX - x) <= 15
							// && Math.abs(pY - y) <= 15 && !moveflag) {
							//
							// checkdelete = -1;
							// } else {
							// Dot d2 = (Dot) dots.get(dots.size() - 1);
							// if (d2.touched3 == true
							// && d2.touched2 == false) {
							// dots.remove(dots.size() - 1);
							// } else if (!d2.touched1) {
							// dots.remove(dots.size() - 1);
							// dots.remove(checkdelete);
							// }
							// }
							//
							// }

						}

					}
					break;
				case MotionEvent.ACTION_POINTER_DOWN:

					break;
				case MotionEvent.ACTION_POINTER_UP:

					break;
				case MotionEvent.ACTION_MOVE:

					if (!headerflag) {
						if (checkdelete >= 0) {
							Dot dnew = (Dot) dots.get(checkdelete);
							if (dnew.isMoving) {
								// dnew.createCircle1(x, y);
								dnew.updateCircles(x, y);

							}
						}

						else if (dots.size() > 0) {
							Dot d11 = (Dot) dots.get(dots.size() - 1);
							if (d11.touched1)
								d11.createLine(x, y);
						}
					}
					break;
				}

				/*
				switch (action) {
				case MotionEvent.ACTION_DOWN:
					if (!fxCirc1Toggle && !fxCirc2Toggle && !fxCirc3Toggle
							&& !fxCirc4Toggle && !fxCirc0Toggle)
						fxCheck = false;

					break;
				case MotionEvent.ACTION_UP:
					if (checkdelete >= 0 && fxCheck) {
						Dot d = dots.get(checkdelete);
						d.fx(effect, col);
					}

					fxCirc1Toggle = fxCirc2Toggle = fxCirc3Toggle = fxCirc4Toggle = false;

					break;
				case MotionEvent.ACTION_MOVE:
					if (fxCirc1Toggle) {
						effect = 1;
						col = col2;
						fxCheck = true;
					} else if (fxCirc2Toggle) {
						effect = 2;
						col = col3;
						fxCheck = true;
					} else if (fxCirc3Toggle) {
						effect = 3;
						col = col4;
						fxCheck = true;
					} else if (fxCirc4Toggle) {
						effect = 4;
						col = col5;
						fxCheck = true;
					} else if (fxCirc0Toggle) {
						effect = 0;
						col = col1;
						fxCheck = true;
					}

					break;
				}
			}
			*/
		}
		
		return super.surfaceTouchEvent(event);
	}
		

	@Override
	public void bpmChanged(int t) {
		bpm = t;
		PdBase.sendFloat("pd_bpm", bpm);
	}

	@Override
	protected void onResume() {

		super.onResume();

	}

	@Override
	public void onSharedPreferenceChanged(SharedPreferences pref, String key) {

		SharedPreferences activityPreferences = getPreferences(SynthCircle.MODE_PRIVATE);
		SharedPreferences.Editor editor = activityPreferences.edit();

		if (key.equals("preset")) {
			String pres = pref.getString("preset", "1");
			int presf = Integer.valueOf(pres);
			PdBase.sendFloat("pd_presets", presf);

			editor.putFloat("preset", presf);
			editor.commit();
		}

		if (key.equals("scale")) {
			String val = pref.getString("scale", "1");
			int valf = Integer.valueOf(val);
			PdBase.sendFloat("pd_scales", valf);
			editor.putFloat("scale", valf);
			editor.commit();

		}
		// Octave transpose preference

		if (key.equals("transposeOct")) {
			String tran = pref.getString("transposeOct", "3");
			int tranf = Integer.valueOf(tran);
			tranf = tranf - 3;
			PdBase.sendFloat("pd_octTrans", tranf);
			editor.putFloat("transposeOct", tranf);
			editor.commit();
		}

		// Note transpose preference

		if (key.equals("transposeNote")) {
			String tran1 = pref.getString("transposeNote", "0");
			int tranf1 = Integer.valueOf(tran1);
			PdBase.sendFloat("pd_noteTrans", tranf1);
			editor.putFloat("transposeNote", tranf1);
			editor.commit();
		}

		if (key.equals("accel")) {
			boolean accely = pref.getBoolean("accel", false);
			if (accely)
				PdBase.sendFloat("pd_accelToggle", 1);

			else
				PdBase.sendFloat("pd_accelToggle", 0);
			editor.putBoolean("accel", accely);
			editor.commit();

		}
		if (key.equals("delay")) {
			boolean delayy = pref.getBoolean("delay", true);
			if (delayy)
				PdBase.sendFloat("pd_delayToggle", 1);

			else
				PdBase.sendFloat("pd_delayToggle", 0);
			editor.putBoolean("delay", delayy);
			editor.commit();

		}
		if (key.equals("reverb")) {
			boolean reverby = pref.getBoolean("reverb", true);
			if (reverby)
				PdBase.sendFloat("pd_reverbToggle", 1);

			else
				PdBase.sendFloat("pd_reverbToggle", 0);
			editor.putBoolean("reverb", reverby);
			editor.commit();

		}

	}

	// lores~ through accelerometer

	@Override
	public void onSensorChanged(SensorEvent event) {

		if (event.sensor.getType() != Sensor.TYPE_ACCELEROMETER)
			return;
		SharedPreferences getPrefs1 = PreferenceManager
				.getDefaultSharedPreferences(getBaseContext());
		accel = getPrefs1.getBoolean("accel", true);
		currentAccel = lowPass(event.values[0]);
		float y = Math.abs(currentAccel / 10);

		if (accel == true)
			PdBase.sendFloat("pd_accely", (1 - y));

	}

	@Override
	public void onAccuracyChanged(Sensor sensor, int accuracy) {

	}

	// smoothening accelerometer data
	DecimalFormat dfnew = new DecimalFormat("#");

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
		String content = "Hey!Check out my awesome sketch!! Click on Load and navigate to the folder where you downloaded the sketch to open it";
		Intent sharingIntent = new Intent(android.content.Intent.ACTION_SEND);
		sharingIntent.setType("audio/mpeg3");

		String shareBody = content;
		sharingIntent.putExtra(android.content.Intent.EXTRA_TEXT, shareBody);
		sharingIntent.putExtra(Intent.EXTRA_SUBJECT, "Circle Synth sketch");
		String root = Environment.getExternalStorageDirectory().toString();
		File myDir = new File(root + "/circlesynth");
		myDir.mkdirs();
		int count = new File(root + "/circlesynth").listFiles().length;
		if (count == 0)
			fName = null;

		if (fName != null) {
			File file = new File(myDir, fName);
			Uri uri = Uri.fromFile(file);

			sharingIntent.putExtra(Intent.EXTRA_STREAM, uri);

			startActivity(Intent.createChooser(sharingIntent, "Share via"));
		} else
			toast("Please save the sketch before sharing");
	}
	
	// Button interfaces here
	class PlayToggle extends ImageToggle {

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
	
	class ReverseToggle extends ImageToggle {
		
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
	
	class FxToggle extends TextToggle {
		
		public FxToggle(PApplet p) {
			super(p);
		}

		@Override
		public void isFalse() {
		}
		
	}
	
	class BpmButton extends TextButton {
		
		public BpmButton(PApplet p) {
			super(p);
		}

		@Override
		public void isReleased() {
			toast("Set BPM/Speed");
			SynthCircle.this.runOnUiThread(new Runnable() {
				public void run() {
					new BpmPicker(SynthCircle.this, SynthCircle.this, bpm)
							.show();
				}
			});
			
		}
	}
	
	class ClearButton extends ImageButton {

		ClearButton(PApplet p) {
			super(p);
		}

		@Override
		public void isReleased() {
			dots.clear();
			stored.clear();
			PdBase.sendFloat("pd_playToggle", 0);
			toast("Sketch Cleared");
		}
	}

}
