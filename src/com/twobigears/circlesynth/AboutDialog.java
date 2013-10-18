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

import android.app.Dialog;
import android.content.Context;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Bundle;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

/** This dialog activity brings up the About dialog. The textual content is directly referenced from 
 * String resources. Handy little thing.
 */

public class AboutDialog extends Dialog {

	private static Context mContext = null;
	String versionName;

	public AboutDialog(Context context) {
		super(context);
		mContext = context;
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		setContentView(R.layout.about);

		try {
			versionName = mContext.getPackageManager().getPackageInfo(
					mContext.getPackageName(), 0).versionName;
		} catch (NameNotFoundException e) {
			Log.v("Circle Synth", e.getMessage());
		}
		
		TextView verionNameText = (TextView) findViewById(R.id.version_text);
		verionNameText.setText(versionName);

		String website = "<a href='http://www.twobigears.com'> "
				+ mContext.getResources().getString(R.string.tbe_web) + "</a>";
		TextView webLink = (TextView) findViewById(R.id.info_web);
		webLink.setMovementMethod(LinkMovementMethod.getInstance());
		webLink.setText(Html.fromHtml(website));

		webLink.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				dismiss();
			}
		});
	}

}
