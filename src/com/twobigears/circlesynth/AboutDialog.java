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
import android.os.Bundle;

public class AboutDialog extends Dialog {
	private static Context mContext = null;

	public AboutDialog(Context context) {
		super(context);
		mContext = context;

	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		setContentView(R.layout.about);

		
	}

}
