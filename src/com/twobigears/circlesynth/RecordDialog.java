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
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

public class RecordDialog extends DialogFragment{




//	SeekBar timeBar;
//	int bpm;
//	TextView tv;
	
	Button playbutton;

	public interface OnRecordingListener {
	
		
		void onPlayClicked();
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
            // Instantiate the NoticeDialogListener so we can send events to the host
            tListener = (OnRecordingListener) activity;
        } catch (ClassCastException e) {
            // The activity doesn't implement the interface, throw exception
            throw new ClassCastException(activity.toString()
                    + " must implement OnRecordingListener");
        }
    }
    
    
	@Override
	public AlertDialog onCreateDialog(Bundle savedInstanceState) {

		setCancelable(true);
		// Use the Builder class for convenient dialog construction
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setMessage("Save File ?")
        .setPositiveButton("Set as Ringtone", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
               tListener.onPositiveAction();
            }
        })
        .setNegativeButton("Delete", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
         	   tListener.onNegativeAction();
            }
        })
        .setNeutralButton("Keep", new DialogInterface.OnClickListener() {
			
			public void onClick(DialogInterface dialog, int which) {
				tListener.onNeutralAction();
				
			}
			
		});
       
        // Get the layout inflater
        LayoutInflater inflater = getActivity().getLayoutInflater();

        // Inflate and set the layout for the dialog
        // Pass null as the parent view because its going in the dialog layout
        
        View view = inflater.inflate(R.layout.rec_dialog, null);
        //builder.setView(inflater.inflate(R.layout.rec_dialog, null));
        playbutton = (Button)view.findViewById(R.id.recordPlayToggle);
        
        playbutton.setOnClickListener(new OnClickListener(){
        	@Override
        	public void onClick(View v){
        		tListener.onPlayClicked();
        	}
        });
        
        public void onPlayClicked(View view) {
            // Is the toggle on?
            boolean on = ((ToggleButton) view).isChecked();
            
            if (on) {
                // Enable vibrate
            } else {
                // Disable vibrate
            }
        }
        
        builder.setView(view);
        // Create the AlertDialog object and return it
        return builder.create();

        
        
        
    }
	




}
