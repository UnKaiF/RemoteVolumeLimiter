package com.basisdas1.remotevolumelimiter;



import android.content.Context;
import android.net.ConnectivityManager;
import android.net.LinkAddress;
import android.net.LinkProperties;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.util.Log;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import java.net.InetAddress;
import java.util.List;

import static com.basisdas1.remotevolumelimiter.AppTag.TAG;
import static java.lang.Integer.parseInt;


public class LocalHttpServer extends Thread
	{
	private final Context context;
	private final ConnectivityManager connectivityManager;
	private ConnectivityManager.NetworkCallback networkCallback = null;
	private Network mNetwork = null;
	private static final int server_port = 9000;
	private static boolean isLocalServerReady = false;
	private final NotificationCompat.Builder notificationBuilder;

	public LocalHttpServer(final Context ctx, NotificationCompat.Builder notificationBuilder)
		{
		this.context = ctx;
		connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
		this.notificationBuilder = notificationBuilder;
		}


	private void updateURLNotification(String url)
		{
		notificationBuilder.setContentText(url);
		NotificationManagerCompat notificationManagerCompat = NotificationManagerCompat.from(context);
		notificationManagerCompat.notify(SilenceAccessibilityService.SERVICE_NOTIFICATION_ID, notificationBuilder.build());
		}

	private InetAddress getLocalAddress(Network network)
		{
		InetAddress address = null;
		List<LinkAddress> linkAddresses = connectivityManager.getLinkProperties(network).getLinkAddresses();
		for (LinkAddress laddress : linkAddresses)
			{
			if (laddress.getAddress().isSiteLocalAddress())
				{
				address = laddress.getAddress();
				break;
				}
			}
		return address;
		}

	@Override
	public void run()
		{
		Log.d(TAG, "LocalHttpServer starting...");

		NetworkRequest networkRequest = new NetworkRequest.Builder()
				.addCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED)
				.addCapability(NetworkCapabilities.NET_CAPABILITY_NOT_VPN)
				.addTransportType(NetworkCapabilities.TRANSPORT_ETHERNET)
				.addTransportType(NetworkCapabilities.TRANSPORT_BLUETOOTH)
				.addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
				.build();

		networkCallback =  new MyNetworkCallback();
		connectivityManager.registerNetworkCallback(networkRequest, networkCallback);
		isLocalServerReady = true;
		}

	private class MyNetworkCallback extends ConnectivityManager.NetworkCallback
		{

		@Override
		public void onAvailable(Network network) {	}

		@Override
		public void onLost(Network network)
			{
			mNetwork = null;
			ServerSocketThread.stopServer();
			updateURLNotification(context.getString(R.string.ongoing_notification_text));
			Log.d(TAG, "Network lost !" );
			}

		@Override
		public void onCapabilitiesChanged(Network network, NetworkCapabilities networkCapabilities) { }

		@Override
		public void onLinkPropertiesChanged(Network network, LinkProperties linkProperties)
			{
			Log.d(TAG, "onLinkPropertiesChanged. " + linkProperties + " network:" + network);
			if ((mNetwork != null) && (network.equals(mNetwork) && ServerSocketThread.isServerRunning()))
				{
				return;
				}

			InetAddress address = getLocalAddress(network);
			mNetwork = network;
			if (address != null)
				{
				updateURLNotification("http://" + address.getHostAddress() + ":" + server_port);
				Thread newServer = new ServerSocketThread(context, address, server_port);
				newServer.start();
				}
			}
		};


	public void stopLocalHttpServer()
		{
		ServerSocketThread.stopServer();
		if (networkCallback != null)
			{
			connectivityManager.unregisterNetworkCallback(networkCallback);
			networkCallback = null;
			}
		isLocalServerReady = false;
		}

	public static boolean localHttpServerIsStarted()
		{
		return isLocalServerReady;
		}


	}