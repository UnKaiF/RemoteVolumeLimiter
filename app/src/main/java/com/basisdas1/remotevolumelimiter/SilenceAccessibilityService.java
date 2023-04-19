package com.basisdas1.remotevolumelimiter;

import android.accessibilityservice.AccessibilityService;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.util.Log;
import android.view.KeyEvent;
import android.view.accessibility.AccessibilityEvent;
import androidx.core.app.NotificationCompat;

import java.util.Calendar;
import java.util.Date;

import static com.basisdas1.remotevolumelimiter.AppTag.TAG;


public class SilenceAccessibilityService extends AccessibilityService
	{

	public static final int SERVICE_NOTIFICATION_ID = 42;
	private LocalHttpServer httpServer = null;
	private VolumeController volumeController = null;
	private NotificationCompat.Builder notificationBuilder;

	@Override
	public void onAccessibilityEvent(AccessibilityEvent accessibilityEvent)
		{
		Log.d(TAG, "Accessibility event is: " + accessibilityEvent.toString());
		}

	@Override
	public void onInterrupt()
		{
		Log.e(TAG, "Service interrupted " + java.text.DateFormat.getDateTimeInstance().format(new Date()));
		}

	@Override
	public void onServiceConnected()
		{
		Log.d(TAG, "onServiceConnected SilenceAccessibilityService...");
		volumeController = VolumeController.getInstance(this.getApplicationContext());

		if (httpServer != null)
			httpServer.stopLocalHttpServer();

		httpServer = new LocalHttpServer(getApplicationContext(), notificationBuilder);
		httpServer.start();
		volumeController.setVolumeKeyInterceptor();
		super.onServiceConnected();
		}


	@Override
	public void onCreate()
		{
        Log.d(TAG, "onCreate SilenceAccessibilityService...");
		String channelId = getString(R.string.app_name);

		createNotificationChannel(channelId);

		PendingIntent pendingIntent = PendingIntent.getService(getApplicationContext(), 1, new Intent(), PendingIntent.FLAG_UPDATE_CURRENT );

		notificationBuilder = new NotificationCompat.Builder(this, channelId);
		Notification notification = notificationBuilder
				.setOngoing(true)
				.setContentTitle(getString(R.string.ongoing_notification_title))
				.setContentText(getString(R.string.ongoing_notification_text))
				.setSmallIcon(R.mipmap.ic_launcher)
				.setLargeIcon(BitmapFactory.decodeResource(getResources(), R.mipmap.ic_launcher))
				.setPriority(NotificationCompat.PRIORITY_HIGH)
				.setContentIntent(pendingIntent)
				.build();

		startForeground(SERVICE_NOTIFICATION_ID, notification);
		}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId)
		{
		super.onStartCommand(intent, 0, startId);
		return START_STICKY;
		}

	@Override
	public boolean onKeyEvent(KeyEvent event)
		{
		Log.d(TAG, "Key pressed via AccessibilitySevrice is: " + event.getKeyCode());
		if (event != null)
			{
			if (event.getAction() == KeyEvent.ACTION_DOWN )
				{
				if (event.getKeyCode() == KeyEvent.KEYCODE_VOLUME_UP)
					{
					volumeController.adjustUserVolume();
					return true;
					}
				}
			}
		return super.onKeyEvent(event);
		}

	private void createNotificationChannel(String channelId)
		{
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
			{
			NotificationChannel channel = new NotificationChannel(channelId, getString(R.string.running_indicator), NotificationManager.IMPORTANCE_MIN);
			NotificationManager notificationManager = getSystemService(NotificationManager.class);
			notificationManager.createNotificationChannel(channel);
			}
		}

	private class ServerStopper extends Thread
		{
		private final LocalHttpServer server;
		public ServerStopper(LocalHttpServer serv)
			{
			this.server = serv;
			}

			@Override
			public void run()
				{
				if (server != null)
					{
					Log.d(TAG,"stopper got VALID reference !");
					server.stopLocalHttpServer();
					}
				else
					{
					Log.d(TAG,"stopper got NULL reference !");
					}
				}
		}

	@Override
	public boolean onUnbind(Intent intent)
		{
        Log.d(TAG,"onUnbind Stopping Service...");

        ServerStopper serverStopper = new ServerStopper(httpServer);
        serverStopper.start();

        httpServer = null;
        Log.d(TAG,"Service stopped");
        volumeController.saveMaxVolume(getApplicationContext());
        volumeController.removeVolumeKeyInterceptor();
        stopForeground(STOP_FOREGROUND_REMOVE);
        return false;
		}
	}
