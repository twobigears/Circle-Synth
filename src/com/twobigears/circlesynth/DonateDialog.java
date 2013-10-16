package com.twobigears.circlesynth;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.graphics.Color;
import android.graphics.PorterDuff.Mode;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

public class DonateDialog extends DialogFragment {
	
	Button small,medium,large,xl,xxl;
	
	float donate_amount;
	
	public interface OnDonateListener{
		
		void onPositiveAction(float donation);
	}
	
	
	private OnDonateListener dListener ;
	
	
	
	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		// Verify that the host activity implements the callback interface
		try {
			// Instantiate the NoticeDialogListener so we can send events to the
			// host
			dListener = (OnDonateListener) activity;
		} catch (ClassCastException e) {
			// The activity doesn't implement the interface, throw exception
			throw new ClassCastException(activity.toString()
					+ " must implement OnDonateListener");
		}
		
		
	}
	
	
	@Override
	public AlertDialog onCreateDialog(Bundle savedInstanceState) {
		
		AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
		builder.setMessage("Not really begging").
		setPositiveButton("Donate",
				new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int id) {
						dListener.onPositiveAction(donate_amount);
					}
				});
		// Get the layout inflater
				LayoutInflater inflater = getActivity().getLayoutInflater();

				// Inflate and set the layout for the dialog
				// Pass null as the parent view because its going in the dialog layout

				View view = inflater.inflate(R.layout.donate, null);
				
				//which donation amount is selected?
				small = (Button)view.findViewById(R.id.donate_button_small);
				medium=(Button)view.findViewById(R.id.donate_button_medium);
				large=(Button)view.findViewById(R.id.donate_button_large);
				xl=(Button)view.findViewById(R.id.donate_button_xl);
				xxl=(Button)view.findViewById(R.id.donate_button_xxl);
				
				small.setOnClickListener(new OnClickListener() {
					//boolean state = false;
					
					@Override
					public void onClick(View v) {
						donate_amount=0.69f;
					}
				});
				
				medium.setOnClickListener(new OnClickListener() {
					//boolean state = false;
					
					@Override
					public void onClick(View v) {
						donate_amount=0.99f;
					}
				});
				large.setOnClickListener(new OnClickListener() {
					//boolean state = false;
					
					@Override
					public void onClick(View v) {
						donate_amount=1.39f;
					}
				});
				xl.setOnClickListener(new OnClickListener() {
					//boolean state = false;
					
					@Override
					public void onClick(View v) {
						donate_amount=4.99f;
					}
				});
				xxl.setOnClickListener(new OnClickListener() {
					//boolean state = false;
					
					@Override
					public void onClick(View v) {
						donate_amount=20f;
					}
				});
				

				
				builder.setView(view);
				// Create the AlertDialog object and return it
				return builder.create();
				
	}
}
