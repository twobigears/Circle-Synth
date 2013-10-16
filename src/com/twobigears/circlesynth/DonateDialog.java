package com.twobigears.circlesynth;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.RadioGroup;

public class DonateDialog extends DialogFragment {
	
	RadioGroup rgroup;
	float donate_amount;
	
	public interface OnDonateListener{
		
		void onPositiveAction();
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
						dListener.onPositiveAction();
					}
				});
		// Get the layout inflater
				LayoutInflater inflater = getActivity().getLayoutInflater();

				// Inflate and set the layout for the dialog
				// Pass null as the parent view because its going in the dialog layout

				View view = inflater.inflate(R.layout.donate, null);
				
				//query which radio buton is pressed
				rgroup = (RadioGroup) view.findViewById(R.id.radioGroup1);
				int selected = rgroup.getCheckedRadioButtonId();
				
				System.out.println("button selection "+String.valueOf(selected));
				
				builder.setView(view);
				// Create the AlertDialog object and return it
				return builder.create();
				
	}
}
