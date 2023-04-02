package com.basisdas1.remotevolumelimiter;

import android.content.Context;
import android.util.Log;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;

import static com.basisdas1.remotevolumelimiter.AppTag.TAG;

public class ServerSocketThread extends Thread
	{
	public static final int SERVER_SOCKET_TIMEOUT = 10000; //ms
	public static final int SERVER_STOP_TIMEOUT = SERVER_SOCKET_TIMEOUT * 2;

	private final InetAddress serverAddress;
	private final int server_port;

	private final Context context;

	private static boolean serverIsRunning = false; //только пишем из этого потока
	private static boolean stopServerCmd = false; //только читаем в этом потоке

	public ServerSocketThread(Context ctx,InetAddress addr, int port)
		{
		serverAddress = addr;
		server_port = port;
		this.context = ctx;
		serverIsRunning = false;
		stopServerCmd = false;
		}

	public static boolean isServerRunning()
		{
		return serverIsRunning;
		}

	//to be executed from other thread. Blocking!!!
	public static void stopServer()
		{
		ServerSocketThread.stopServerCmd = true;
		Log.d(TAG,"Stopping server ...");

		int i;
		for (i = 20; i>0 && ServerSocketThread.serverIsRunning; i--)
			{
			try
				{
				Log.d(TAG, "Waiting for server thread stop&exit N " + i);
				Thread.sleep(SERVER_STOP_TIMEOUT / 20);
				}
			catch (InterruptedException ignored)	{}
			}

		if (i == 0 && ServerSocketThread.serverIsRunning)
			{
			ServerSocketThread.serverIsRunning = false;
			throw new RuntimeException("Cant stop server thread !! (memory leak?)");
			}
		ServerSocketThread.stopServerCmd = false;
		}

	@Override
	public void run()
		{
		ServerSocket serverSocket = null;

		if (serverAddress == null)
			{
			serverIsRunning = false;
			return;
			}

		try
			{
			serverSocket = new ServerSocket();
			serverSocket.setReuseAddress(true);
			serverSocket.bind(new InetSocketAddress(serverAddress, server_port), 10);
			serverSocket.setSoTimeout(SERVER_SOCKET_TIMEOUT);
			serverIsRunning = true;
			while (!stopServerCmd)
				{
				try
					{
					Socket newSocket = serverSocket.accept();
					Log.d(TAG, "Client accepted. Starting client thread..");
					Thread newClient = new ClientSocketThread(context, newSocket);
					newClient.start();
					}
				catch (SocketTimeoutException e)
					{
					Log.d(TAG, "Server socket timeout reached !");
					}
				catch (IOException e)
					{
					Log.d(TAG, "Server IO exception occured ! (disconnected from network)");
					break;
					}
				}

			}
		catch (Exception e)
			{
			e.printStackTrace();
			}

		try
			{
			if (serverSocket != null)
				serverSocket.close();
			}
		catch (Exception er)
			{
			er.printStackTrace();
			}

		serverIsRunning = false;
		Log.d(TAG,"Server thread stopped");
		}
	}
