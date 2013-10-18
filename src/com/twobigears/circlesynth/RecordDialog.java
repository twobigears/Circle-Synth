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
import android.app.AlertDialog;
import android.app.DialogFragment;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageButton;

/*
 * Dialog interface that pops up when the REC button is selected. Has an ImageButton inside the 
 * content area which triggers the playback of the recorded sound fragment. The interface methods
 * are referenced in the host activity
 * 
 *
 */
public class RecordDialog extends DialogFragment {

	ImageButton playbutton;
	SharedPreferences prefs;
	Context context;
	
	long start;
	boolean state=false;
	
	//interface methods
	public interface OnRecordingListener {

		void onPlayTrue();

		void onPlayFalse();

		void onPositiveAction();

		void onNegativeAction();

		void onNeutralAction();
	}

	private OnRecordingListener tListener;

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		// Verify that the host activity implements the callback interface
		try {
			// Instantiate the NoticeDialogListener so we can send events to the
			// host
			tListener = (OnRecordingListener) activity;
		} catch (ClassCastException e) {
			// The activity doesn't implement the interface, throw exception
			throw new ClassCastException(activity.toString()
					+ " must implement OnRecordingListener");
		}
		
		
	}
	//define the dialog attributes here.
	@Override
	public AlertDialog onCreateDialog(Bundle savedInstanceState) {
		
		setCancelable(false);
		
		
		
		prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
		
		//Fetch the duration of recording from the host activity by accessing Shared Preferences.
		start = ((long) prefs.getFloat("timer", 0))*1000;
		//System.out.println("timer from preference "+String.valueOf(start));
		// Use the Builder class for convenient dialog construction
		AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
		builder.setMessage("Save File ?")
				.setPositiveButton("Set as Ringtone",
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int id) {
								tListener.onPositiveAction();
							}
						})
				.setNegativeButton("Delete",
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int id) {
								tListener.onNegativeAction();
							}
						})
				.setNeutralButton("Keep",
						new DialogInterface.OnClickListener() {

							public void onClick(DialogInterface dialog,
									int which) {
								tListener.onNeutralAction();

							}

						});

		// Get the layout inflater
		LayoutInflater inflater = getActivity().getLayoutInflater();

		// Inflate and set the layout for the dialog
		// Pass null as the parent view because its going in the dialog layout

		View view = inflater.inflate(R.layout.rec_dialog, null);
		playbutton = (ImageButton) view.findViewById(R.id.recordPlayToggle);
		
		//The status of the play button changes here when it is clicked, and also when the CountDownTimer
		//runs out
		playbutton.setOnClickListener(new OnClickListener() {
			//boolean state = false;
			
			@Override
			public void onClick(View v) {
				if(!state) {
					startTimer();
					playbutton.setImageResource(R.drawable.stop);
					tListener.onPlayTrue();	
					state  = true;
				
					
				}
				else {
					stopTimer();
					playbutton.setImageResource(R.drawable.play);
					tListener.onPlayFalse();
					state  = false;
				}
			}
		});
		
		builder.setView(view);
		// Create the AlertDialog object and return it
		return builder.create();

	}
	
	
	//Implementing a CountDownTimer to automatically change the state of the play button when 
	//file play back is finished
	CountDownTimer timer;
	float test;
	
	public void startTimer(){
		
		
		timer = new CountDownTimer(start,1000){
					
					@Override
					public void onFinish() {
					playbutton.setImageResource(R.drawable.play);  
					System.out.println("countdownfinished");
					
					state=false;
					tListener.onPlayFalse();
						
					}

					@Override
					public void onTick(long millisUntilFinished) {
					System.out.println(String.valueOf(millisUntilFinished))	;					
					}
					
				}.start();

		
		
	}
	
	public void stopTimer(){
		timer.cancel();
	}

}
