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

//	boolean playValue = false; // global boolean of the play button state
//	boolean revValue = false; // global reverse value for play
//	boolean bpmPopup = false; // bpm popup button value
//	boolean clearButton = false;
//	boolean fxToggle = false;
//	boolean fxCirc1Toggle = false;
//	boolean fxCirc2Toggle = false;
//	boolean fxCirc3Toggle = false;
//	boolean fxCirc4Toggle = false;
//	boolean fxCirc0Toggle = false;
	boolean fxCheck = false;
//	boolean settingsButton = false;
//	boolean fxClearButton = false;
//	boolean saveButton = false;
//	boolean loadButton = false;
//	boolean shareButton = false;

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

	PFont robotoFont;
	PGraphics header, sketchBG, scanSquare;

	PImage shareImg, playImg, stopImg, revImg, forImg, clearOnImg, clearOffImg,
			loadOffImg, loadOnImg, saveOffImg, saveOnImg, shareOffImg,
			shareOnImg, settingsOnImg, settingsOffImg, innerCircleImg,
			outerCircleImg, lineCircleImg, fxCircleToggleImg, fxEmptyToggleImg,
			fxClearOffImg, fxClearOnImg, fxDragFilledImg, fxDragEmptyImg, fxFilledImg;

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
	
	FxCircleDrag fxCircleDrag;
	
	float outerCircSize, innerCircSize;
	
	int mainHeadHeight, shadowHeight, scanSquareY, headerHeight, buttonPad1, buttonPad2, buttonFxPad;
	
	float textAscent;

	int bpm = 120;
	float bpmScale = constrain(bpm / 24, 4, 20);

	final int bgCol = color(28, 28, 28);
	final int headCol = color(41, 41, 41);
	final int buttonInActCol = color(175);
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
		fxCircleToggleImg = loadImage("fxCircleToggle"+resSuffix+".png");
		fxEmptyToggleImg = loadImage("fxCircleEmpty"+resSuffix+".png");
		fxClearOffImg = loadImage("fxClearOff"+resSuffix+".png");
		fxClearOnImg = loadImage("fxClearOn"+resSuffix+".png");
		fxFilledImg = loadImage("fxFilled"+resSuffix+".png");
		
		robotoFont = createFont("Roboto-Thin-12", 24 * density, true);
		
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
		loadButtonB = new LoadButton(this);
		loadButtonB.load(loadOffImg, loadOnImg);
		saveButtonB = new SaveButton(this);
		saveButtonB.load(saveOffImg, saveOnImg);
		shareButtonB = new ShareButton(this);
		shareButtonB.load(shareOffImg, shareOnImg);
		settingsButtonB = new SettingsButton(this);
		settingsButtonB.load(settingsOffImg, settingsOnImg);
		fx1ToggleB = new Fx1Toggle(this, false);
		fx1ToggleB.load(fxCircleToggleImg, fxCircleToggleImg);
		fx2ToggleB = new Fx2Toggle(this, false);
		fx2ToggleB.load(fxCircleToggleImg, fxCircleToggleImg);
		fx3ToggleB = new Fx3Toggle(this, false);
		fx3ToggleB.load(fxCircleToggleImg, fxCircleToggleImg);
		fx4ToggleB = new Fx4Toggle(this, false);
		fx4ToggleB.load(fxCircleToggleImg, fxCircleToggleImg);
		fxEmptyToggleB = new FxEmptyToggle(this, false);
		fxEmptyToggleB.load(fxEmptyToggleImg, fxEmptyToggleImg);
		fxClearButtonB = new FxClearButton(this, false);
		fxClearButtonB.load(fxClearOffImg, fxClearOnImg);
		
		fxCircleDrag = new FxCircleDrag(this);
		fxCircleDrag.emptyCircle = outerCircleImg;
		fxCircleDrag.filledCircle = fxFilledImg;
		
		// header
		mainHeadHeight = (int) (40 * density); 
		shadowHeight = (int) (density);
		scanSquareY = (int) (3 * density);
		headerHeight = mainHeadHeight + scanSquareY + shadowHeight;	 
		buttonPad1 = (int) (10 * density);
		buttonPad2 = (int) (20 * density);
		buttonFxPad = (int) (1 * density);

		outerCircSize = outerCircleImg.width;
		innerCircSize = innerCircleImg.width;
		
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

	public void draw() {

		image(sketchBG, 0, headerHeight);

		// draw dots on the screen based on touch data stored in Dot class
		drawThis();
		
		image(header, 0, 0);
		image(scanSquare, scanline * width, mainHeadHeight);
		
		//buttons
		playToggleB.drawIt(buttonPad1, 0);
		reverseToggleB.drawIt(playToggleB.getWidth()+buttonPad2, 0);
		bpmButtonB.drawIt(String.valueOf(bpm), (playToggleB.getWidth()+buttonPad2)*2, 0);
		clearButtonB.drawIt((playToggleB.getWidth()+buttonPad2)*3, 0);
		fxToggleB.drawIt("FX", (playToggleB.getWidth()+buttonPad2)*4, 0);
		settingsButtonB.drawIt(width-(playToggleB.getWidth()+buttonPad1), 0);
		shareButtonB.drawIt(width-((playToggleB.getWidth()+buttonPad1)*2), 0);
		saveButtonB.drawIt(width-((playToggleB.getWidth()+buttonPad1)*3), 0);
		loadButtonB.drawIt(width-((playToggleB.getWidth()+buttonPad1)*4), 0);
		fx1ToggleB.drawIt(buttonFxPad, 0);
		fx2ToggleB.drawIt(fx1ToggleB.getWidth()+buttonFxPad, 0);
		fx3ToggleB.drawIt((fx1ToggleB.getWidth())*2+buttonFxPad, 0);
		fx4ToggleB.drawIt((fx1ToggleB.getWidth())*3+buttonFxPad, 0);
		fxEmptyToggleB.drawIt((fx1ToggleB.getWidth())*4+buttonFxPad, 0);
		fxClearButtonB.drawIt((fx1ToggleB.getWidth())*5+buttonFxPad, 0);

		fxCircleDrag.drawIt();
	}
	
	public void detect() {

		if (dots.size() > 0) {
			for (int i = 0; i < dots.size(); i++) {
				Dot d = dots.get(i);
				if (!d.touched2) {
					if (!reverseToggleB.state) {
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
					if (!reverseToggleB.state) {
						if (Math.abs(scanline - dxd) <= .01) {
							d.selected1 = d.selected2 = true;
						}

					}
					if (reverseToggleB.state) {
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
			if (!playToggleB.state)
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
		boolean touched1, touched2, touched3, selected1, selected2,isMoving,isDeleted,hasLine;
		int doteffect;
		int dotcol = color(255, 68, 68);
		private Animations circle1InnerAnim, circle1OuterAnim,
				circle2InnerAnim, circle2OuterAnim;
		private PGraphics lineBuffer;
		private float angle, dist;
		private int lineImgWidth, outerCircleWidth;
		boolean node1,node2;
		boolean isLocked;

		Dot() {
			touched1 = touched2 = touched3 = selected1 = selected2 = isMoving = isDeleted = hasLine = false;
			effect = 0;
			circle1InnerAnim = new Animations(10);
			circle1OuterAnim = new Animations(10);
			circle2InnerAnim = new Animations(10);
			circle2OuterAnim = new Animations(10);
			lineBuffer = createGraphics(20, lineCircleImg.height);
			lineImgWidth = (int) (lineCircleImg.width - density);
			outerCircleWidth = (int) (outerCircleImg.width - density);
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
			node1=true;
			

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
			node2=true;
			

		}

		public void createLine(float mX3, float mY3) {
			xLine = mX3;
			yLine = mY3;
			hasLine=true;
			touched3 = true;

		}
		
		public void drawCircleOne() {
			pushMatrix();
			pushStyle();
			translate(xDown, yDown);
			imageMode(CENTER);
			
			pushMatrix();
			if(selected1) tint(dotcol);
			else noTint();
			circle1OuterAnim.animate();
			scale(circle1OuterAnim.animateValue);
			image(outerCircleImg, 0, 0);
			popMatrix();
		    
			pushMatrix();
			if(selected1) scale(1.5f);
			else scale(1);
			tint(dotcol);
			if (circle1OuterAnim.animateValue > 0.5)
				circle1InnerAnim.animate();
			scale(circle1InnerAnim.animateValue);
			image(innerCircleImg, 0, 0);
			popMatrix();

			popStyle();
			popMatrix();
		}
		
		public void drawCircleTwo() {
			pushMatrix();
			pushStyle();
			translate(xUp, yUp);
			imageMode(CENTER);
			
			pushMatrix();
			if(selected2) tint(dotcol);
			else noTint();
			circle2OuterAnim.animate();
			scale(circle2OuterAnim.animateValue);
			image(outerCircleImg, 0, 0);
			popMatrix();
		    
			pushMatrix();
			if(selected2) scale(1.5f);
			else scale(1);
			tint(dotcol);
			if (circle2OuterAnim.animateValue > 0.5)
				circle2InnerAnim.animate();
			scale(circle2InnerAnim.animateValue);
			image(innerCircleImg, 0, 0);
			popMatrix();

			popStyle();
			popMatrix();
		}
		
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
		
		public void drawLine() {
			
			float deltaX = xLine - xDown;
			float deltaY = yLine - yDown;
			
			angle = atan(deltaY/deltaX);
			if (deltaX < 0) angle += PI;
			
			float tempDist = (float) (sqrt((deltaX * deltaX)
					+ (deltaY * deltaY)) - outerCircleWidth);
			
			if(tempDist != dist) computeLine(tempDist);

			pushMatrix();
			pushStyle();
			translate(xDown, yDown);
			rotate(angle);
			if(selected1 && selected2) tint(dotcol);
			else noTint();
			imageMode(CORNER);
			image(lineBuffer, (float) (outerCircleWidth*0.5), (float) (-lineCircleImg.height*0.5));
			popStyle();
			popMatrix();

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
			float deltaX = Math.abs(mX-xDown);
			float deltaY = Math.abs(mY-yDown);
			
			double dist = Math.sqrt(deltaX*deltaX + deltaY*deltaY);
			
			if(dist < outerCircSize/2){
				xDown=mX;
				yDown=mY;
				if(hasLine){
					xLine=xUp;
					yLine=yUp;
				}
				else{
					xUp=xDown;
					yUp=yDown;
				}
				
			}
			else{
				xLine=xUp=mX;
				yLine=yUp=mY;
				
			}
			
			if(xDown>xUp){
				float tempX=xDown;
				float tempY=yDown;
				xDown=xUp;
				yDown=yUp;
				xUp=tempX;
				yUp=tempY;
			}
			
				
		}

	}

	int checkdelete;
	int fxcheckdelete;
	@Override
	public boolean dispatchTouchEvent(MotionEvent event) {
		
		
		//System.out.println(String.valueOf(moveflag));
		
		
		width = displayWidth;
		height = displayHeight;
		float x = (event.getX());
		float y = (event.getY());
		int action = event.getActionMasked();
		//int checkdelete = -1;
		if (dots != null) {
			fxcheckdelete = delCheck(x, y);
			Log.d("checkdelete", String.valueOf(checkdelete));
			
			fxCircleDrag.setXY(x, y);
			
				switch (action) {
				case MotionEvent.ACTION_DOWN:

					// button interfaces here
					playToggleB.touchDown(x, y);
					reverseToggleB.touchDown(x, y);
					fxToggleB.touchDown(x, y);
					bpmButtonB.touchDown(x, y);
					clearButtonB.touchDown(x, y);
					shareButtonB.touchDown(x, y);
					settingsButtonB.touchDown(x, y);
					loadButtonB.touchDown(x, y);
					saveButtonB.touchDown(x, y);
					fx1ToggleB.altTouchDown(x, y);
					fx2ToggleB.altTouchDown(x, y);
					fx3ToggleB.altTouchDown(x, y);
					fx4ToggleB.altTouchDown(x, y);
					fxEmptyToggleB.altTouchDown(x, y);
					fxClearButtonB.touchDown(x, y);
					
					checkdelete = delCheck(x, y);
					
					
					
					if (y > mainHeadHeight) {
						dots.add(new Dot());
						if (checkdelete < 0) {
							Dot d = (Dot) dots.get(dots.size() - 1);
							if (dots.size() <= maxCircle) {
								d.createCircle1(x, y);
								d.isLocked=false;
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
							d.isLocked=true;
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
					shareButtonB.touchUp(x, y);
					settingsButtonB.touchUp(x, y);
					loadButtonB.touchUp(x, y);
					saveButtonB.touchUp(x, y);
					fx1ToggleB.altTouchUp(x, y);
					fx2ToggleB.altTouchUp(x, y);
					fx3ToggleB.altTouchUp(x, y);
					fx4ToggleB.altTouchUp(x, y);
					fxEmptyToggleB.altTouchUp(x, y);
					fxClearButtonB.touchUp(x, y);
					
					//reset checkdelete
					//checkdelete = -1;
					
					
					if (!headerflag) {
						if (dots.size() > 0) {
							Dot d1 = (Dot) dots.get(dots.size() - 1);
							if (checkdelete < 0) {
								// if (moveflag)
								// dots.remove(dots.size() - 1);
								if (y < mainHeadHeight && !moveflag) {
									dots.remove(dots.size() - 1);
									toast("You can't draw there");
								}

								if (dots.size() > maxCircle) {
									toast("Circle Limit Reached");
									dots.remove(dots.size() - 1);
								}
								if (d1.hasLine == true && !d1.isLocked) {
									d1.createCircle2(x, y);
									d1.isMoving = false;

								}

							}
							if (checkdelete >= 0 && dots.size()>0) {
								Dot d = (Dot) dots.get(dots.size()-1);
								if (x < 20 || x > width - 20 || y > height - 20
										|| y < mainHeadHeight) {
									dots.remove(checkdelete);
									toast("Circle gone!");
								}
								
								if (d.hasLine && !d.touched2)
									dots.remove(dots.size()-1);
								
							
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
					
					//assign fx dragged in and dropped
					if (fxcheckdelete >= 0 && fxCheck) {
						Dot d = dots.get(fxcheckdelete);
						d.fx(effect, col);
					}
					
					
					//reset moveflag to false
					moveflag = false;
					
					break;
				case MotionEvent.ACTION_POINTER_DOWN:

					break;
				case MotionEvent.ACTION_POINTER_UP:

					break;
				case MotionEvent.ACTION_MOVE:
					
					
					
					if (!headerflag && dots.size()>0) {
						if (checkdelete >= 0) {
							Dot dnew = (Dot) dots.get(checkdelete);
							if (dnew.isMoving) {
								int historySize = event.getHistorySize();
							    for (int i = 0; i < historySize; i++) {
							          float historicalX = event.getHistoricalX(i);
							          float historicalY = event.getHistoricalY(i);
							          dnew.updateCircles(historicalX,historicalY);
							    }
								

							}
						}

						else if (!moveflag){
							Dot d11 = (Dot) dots.get(dots.size() - 1);
							if (d11.node1)
								d11.createLine(x, y);
								
							
						}
					}
					
					// set fx assign color/value based on move
					if (fx1ToggleB.state) {
						effect = 1;
						col = col2;
						fxCheck = true;
					} else if (fx2ToggleB.state) {
						effect = 2;
						col = col3;
						fxCheck = true;
					} else if (fx3ToggleB.state) {
						effect = 3;
						col = col4;
						fxCheck = true;
					} else if (fx4ToggleB.state) {
						effect = 4;
						col = col5;
						fxCheck = true;
					} else if (fxEmptyToggleB.state) {
						effect = 0;
						col = col1;
						fxCheck = true;
					}
					else fxCheck = false;
					
					break;
				}

		}
		
		return super.dispatchTouchEvent(event);
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
			toast("Sketch Cleared");
		}
	}
	
	class LoadButton extends ImageButton {
		
		LoadButton(PApplet p) {
			super(p);
		}
		
		@Override
		public void isReleased() {
			File mPath = new File(Environment.getExternalStorageDirectory()
					+ "/circlesynth");
			fileDialog = new FileDialog(SynthCircle.this, mPath);
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
		}
	}
	
	class SaveButton extends ImageButton {
		
		SaveButton(PApplet p) {
			super(p);
		}
		
		@Override
		public void isReleased() {
			toast("Sketch Saved in /circlesynth");
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
	}

	class ShareButton extends ImageButton {

		ShareButton(PApplet p) {
			super(p);
		}

		@Override
		public void isReleased() {
			shareIt();
		}
	}
	
	class SettingsButton extends ImageButton {

		SettingsButton(PApplet p) {
			super(p);
		}

		@Override
		public void isReleased() {
			tracker.trackEvent("Buttons Category", "Settings", "", 0L);
			Intent intent = new Intent(SynthCircle.this, SynthSettingsTwo.class);
			startActivity(intent);
			prefs.registerOnSharedPreferenceChangeListener(SynthCircle.this);
		}
	}
	
	class Fx1Toggle extends ImageToggle {

		Fx1Toggle(PApplet p, boolean enabled) {
			super(p, enabled);
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
	
	class Fx2Toggle extends ImageToggle {

		Fx2Toggle(PApplet p, boolean enabled) {
			super(p, enabled);
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
	
	class Fx3Toggle extends ImageToggle {

		Fx3Toggle(PApplet p, boolean enabled) {
			super(p, enabled);
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
	
	class Fx4Toggle extends ImageToggle {

		Fx4Toggle(PApplet p, boolean enabled) {
			super(p, enabled);
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
	
	class FxEmptyToggle extends ImageToggle {

		FxEmptyToggle(PApplet p, boolean enabled) {
			super(p, enabled);
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
	
	class FxClearButton extends ImageButton {

		FxClearButton(PApplet p, boolean enabled) {
			super(p, enabled);
		}
		
		@Override
		public void isReleased() {
			for (int i = 0; i < dots.size(); i++) {
				Dot d = (Dot) dots.get(i);
				d.fxClear();
			}
			toast("All FX cleared");
		}
	}


}
