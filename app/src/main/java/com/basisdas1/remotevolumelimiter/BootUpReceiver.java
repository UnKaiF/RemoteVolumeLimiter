package com.basisdas1.remotevolumelimiter;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import static com.basisdas1.remotevolumelimiter.AppTag.TAG;

public class BootUpReceiver extends BroadcastReceiver
	{


	@Override
	public void onReceive(Context context, Intent intent)
		{
		Log.d(TAG, "from bootUp receiver");
		Intent myIntent = new Intent(context, SilenceAccessibilityService.class);
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
			{
			context.startForegroundService(myIntent);
			}
		else
			context.startService(myIntent);
		}

	}