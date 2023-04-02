package com.basisdas1.remotevolumelimiter;

import android.content.Context;
import android.content.res.AssetManager;
import android.util.Log;

import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import static com.basisdas1.remotevolumelimiter.AppTag.TAG;

public class ClientSocketThread extends Thread
	{
	private final Context context;
	protected Socket socket;
	private String content_type = "";
	private final VolumeController maxVolumeSaver;

	public ClientSocketThread(Context ctx, Socket clientSocket)
		{
		this.socket = clientSocket;
		this.context = ctx;
		maxVolumeSaver = VolumeController.getInstance(context);
		}

	@Override
	public void run()
		{
		try
			{
			DataInputStream in = null;
			DataOutputStream out = null;

			if (this.socket.isConnected())
				{
				try
					{
					in = new DataInputStream(this.socket.getInputStream());
					}
				catch (IOException e)
					{
					e.printStackTrace();
					}
				try
					{
					out = new DataOutputStream(this.socket.getOutputStream());
					}
				catch (IOException e)
					{
					e.printStackTrace();
					}
				}

			byte[] data = new byte[1500];

			if (in != null)
				{
				while (in.read(data) != -1)
					{
					String recData = new String(data).trim();
					String[] header = recData.split("[\\r?\\n]+");
					String[] h1 = header[0].split(" ");

					String requestedFile = "";
					boolean isFile = true;

					String status_code;
					if (h1.length > 1)
						{
						final String requestLocation = h1[1];

						status_code = "200";

						switch (requestLocation)
							{
							//case "/get-media":
							//currentMedia =
							case "/get-volume":
								isFile = false;
								content_type = "text/plain";
								break;
							case "/set-volume":
								isFile = false;
								int newVolume = Integer.parseInt(header[header.length - 1].substring(7));
								maxVolumeSaver.setMaxVolume(newVolume);
								content_type = "text/plain";
								break;
							case "/volume-up":
								maxVolumeSaver.increaseMaxVolume();
								break;
							case "/volume-down":
								maxVolumeSaver.decreaseMaxVolume();
								break;
							case "/volume-up.png":
							case "/volume-down.png":
								requestedFile = requestLocation.substring(1);
								content_type = "image/png";
								break;
							case "/":
								requestedFile = "remote-volume-limiter.html";
								content_type = "text/html";
								break;
							default:
								status_code = "404";
								break;
							}
						}
					else
						{
						status_code = "404";
						}

					byte[] buffer = new byte[0];
					if (isFile)
						{
						if (!requestedFile.isEmpty())
							{
							InputStream fileStream = context.getAssets().open(requestedFile, AssetManager.ACCESS_BUFFER);
							int size = fileStream.available();
							buffer = new byte[size];
							int readResult = fileStream.read(buffer);
							}
						}
					else
						{
						String responce = maxVolumeSaver.getSystemMaxVolume() + "-" + maxVolumeSaver.getMaxVolume() + "";
						buffer = responce.getBytes();
						}
					writeResponse(out, buffer.length + "", buffer, status_code, content_type);
					}
				socket.close();
				}
			}
		catch (IOException e)
			{
			e.printStackTrace();
			}
		Log.d(TAG, "Exiting client thread");
		}


	protected void printHeader(PrintWriter pw, String key, String value)
		{
		pw.append(key).append(": ").append(value).append("\r\n");
		}

	private void writeResponse(DataOutputStream output, String size, byte[] data, String status_code, String content_type)
		{
		try
			{
			SimpleDateFormat gmtFrmt = new SimpleDateFormat("E, d MMM yyyy HH:mm:ss 'GMT'", Locale.US);
			gmtFrmt.setTimeZone(TimeZone.getTimeZone("GMT"));
			PrintWriter pw = new PrintWriter(new BufferedWriter(new OutputStreamWriter(output)), false);
			pw.append("HTTP/1.1 ").append(status_code).append(" \r\n");
			if (!content_type.isEmpty())
				{
				printHeader(pw, "Content-Type", content_type);
				}
			printHeader(pw, "Date", gmtFrmt.format(new Date()));
			printHeader(pw, "Connection", "close");
			printHeader(pw, "Content-Length", size);
			printHeader(pw, "Server", socket.getLocalAddress().getHostAddress());
			pw.append("\r\n");
			pw.flush();
			switch (content_type)
				{
				case "text/plain":
					pw.append(new String(data));
					break;
				case "text/html":
					pw.append(new String(data));
					break;
				case "image/png":
					output.write(data);
					output.flush();
					break;
				}
			pw.flush();
			//pw.close();
			}
		catch (Exception er)
			{
			er.printStackTrace();
			}
		}
	}
