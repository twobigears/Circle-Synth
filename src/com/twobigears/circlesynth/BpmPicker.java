package com.twobigears.circlesynth;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.widget.SeekBar;
import android.widget.TextView;
import com.twobigears.circlesynth.R;

public class BpmPicker extends Dialog implements
		SeekBar.OnSeekBarChangeListener {

	SeekBar timeBar;
	int bpm;
	TextView tv;

	public interface OnBpmChangedListener {
		void bpmChanged(int t);
	}

	private OnBpmChangedListener tListener;

	public BpmPicker(Context context, OnBpmChangedListener listener, int bpm) {
		super(context);
		tListener = listener;
		this.bpm = bpm;

	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.my_dialog);
		setTitle("BPM");
		timeBar = (SeekBar) findViewById(R.id.seekBar1);
		tv = (TextView) findViewById(R.id.textView1);

		tv.setText(String.valueOf(bpm));
		timeBar.setProgress((int) ((bpm / 240.0) * 100));
		timeBar.setOnSeekBarChangeListener(this);

		setCancelable(true);
	}

	@Override
	public void onProgressChanged(SeekBar seekBar, int progress,
			boolean fromUser) {
		if (fromUser) {
			switch (seekBar.getId()) {
			case R.id.seekBar1:

				bpm = (int) ((progress / 100.0) * 240.0);

				tv.setText(String.valueOf(bpm));
				tListener.bpmChanged(bpm);
				break;

			}
		}

	}

	@Override
	public void onStartTrackingTouch(SeekBar seekBar) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onStopTrackingTouch(SeekBar seekBar) {
		// TODO Auto-generated method stub

	}

}
