package com.basisdas1.remotevolumelimiter;



import android.content.Context;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.media.MediaRouter;
import android.os.Build;
import android.util.Log;

import static android.content.Context.MEDIA_ROUTER_SERVICE;
import static android.media.MediaRouter.CALLBACK_FLAG_UNFILTERED_EVENTS;
import static android.media.MediaRouter.ROUTE_TYPE_USER;
import static com.basisdas1.remotevolumelimiter.AppTag.TAG;

public class VolumeController
	{
	private static VolumeController mInstance = null;
	private static Integer maxVolume = -1;
	private static Integer systemMaxVolume = -1;
	private static Integer systemMinVolume = -1;
	private MediaRouter mediaRouter;
	private static MediaRouter.Callback mediaRouterCallback = null;
	private AudioManager audioManager;


	public static synchronized VolumeController getInstance(Context context)
		{
		if (mInstance == null)
			{
			mInstance = new VolumeController(context.getApplicationContext());
			}
		return mInstance;
		}

	public void setVolumeKeyInterceptor()
		{
		if (mediaRouterCallback == null)
			{
			mediaRouterCallback = new OnVolumeChangedByUser();
			mediaRouter.addCallback(ROUTE_TYPE_USER, mediaRouterCallback, CALLBACK_FLAG_UNFILTERED_EVENTS);
			}
		}

	public void removeVolumeKeyInterceptor()
		{
		if (mediaRouterCallback != null)
			{
			mediaRouter.removeCallback(mediaRouterCallback);
			mediaRouterCallback = null;
			}
		}

	private VolumeController(Context context)
		{
		mediaRouter = (MediaRouter) context.getSystemService(MEDIA_ROUTER_SERVICE);
		SharedPreferences mySharedPreferences = context.getSharedPreferences("preferences", Context.MODE_PRIVATE);
		audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
		if (maxVolume < 0)
			{
			maxVolume = mySharedPreferences.getInt("maxVolume", audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC));
			systemMaxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P)
				systemMinVolume = audioManager.getStreamMinVolume(AudioManager.STREAM_MUSIC);
			else
				systemMinVolume = 0;
			}
		adjustUserVolume();
		}

	public Integer getMaxVolume()
		{
		return maxVolume;
		}

	public synchronized void adjustUserVolume()
		{
		Log.d(TAG, "IN adjustUserVolume()");
		if (maxVolume < audioManager.getStreamVolume(AudioManager.STREAM_MUSIC))
			{
			audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, maxVolume , AudioManager.FLAG_REMOVE_SOUND_AND_VIBRATE);
			}
		}

	public synchronized void  setMaxVolume(Integer maxVolume)
		{
		if (maxVolume > systemMaxVolume)
			VolumeController.maxVolume = systemMaxVolume;
		else if (maxVolume < systemMinVolume)
			VolumeController.maxVolume = systemMinVolume;
		else
			VolumeController.maxVolume = maxVolume;
		adjustUserVolume();
		}

	public void increaseMaxVolume()
		{
		setMaxVolume(maxVolume + 1);
		}

	public void decreaseMaxVolume()
		{
		setMaxVolume(maxVolume - 1);
		}

	public Integer getSystemMaxVolume()
		{
		return systemMaxVolume;
		}
/*
	public static Integer getSystemMinVolume()
		{
		return systemMinVolume;
		}
*/
	private class OnVolumeChangedByUser extends MediaRouter.Callback
		{
			@Override
			public void onRouteSelected(MediaRouter mediaRouter, int i, MediaRouter.RouteInfo routeInfo) {}
			@Override
			public void onRouteUnselected(MediaRouter mediaRouter, int i, MediaRouter.RouteInfo routeInfo)	{}
			@Override
			public void onRouteAdded(MediaRouter mediaRouter, MediaRouter.RouteInfo routeInfo)	{}
			@Override
			public void onRouteRemoved(MediaRouter mediaRouter, MediaRouter.RouteInfo routeInfo) {}
			@Override
			public void onRouteChanged(MediaRouter mediaRouter, MediaRouter.RouteInfo routeInfo) {}
			@Override
			public void onRouteGrouped(MediaRouter mediaRouter, MediaRouter.RouteInfo routeInfo, MediaRouter.RouteGroup routeGroup, int i)	{}
			@Override
			public void onRouteUngrouped(MediaRouter mediaRouter, MediaRouter.RouteInfo routeInfo, MediaRouter.RouteGroup routeGroup) {}
			@Override
			public void onRouteVolumeChanged(MediaRouter mediaRouter, MediaRouter.RouteInfo routeInfo)
				{
				int volume = routeInfo.getVolume();
				Log.d(TAG, "MediaRouter VOLUME Key interceptor: " + volume);
				if (volume > maxVolume)
					{
					audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, maxVolume, AudioManager.FLAG_REMOVE_SOUND_AND_VIBRATE);
					Log.d(TAG, "VOLUME adjusted automatically to " + maxVolume);
					}
				}
		}


	public synchronized void saveMaxVolume(Context context)
		{
		SharedPreferences mySharedPreferences = context.getSharedPreferences("preferences", Context.MODE_PRIVATE);
		SharedPreferences.Editor e = mySharedPreferences.edit();
		e.putInt("maxVolume", maxVolume);
		e.apply();
		}

	}
