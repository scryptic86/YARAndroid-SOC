package com.netsec.clamav;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

import android.content.Context;
import android.util.Log;

public class DownLoadFileFromServer {
	/*The address to server from where the scan result is downloaded */
	static String downLoadFilePath = "http://192.168.1.115/test/scan_result";
	
	/*Function which is used to intitiate connection and complete file download*/
	public static File downloadFile(Context context) throws Exception {

		/*url object which contains the file path for download of the scan result*/
		URL url = new URL(downLoadFilePath);
		HttpURLConnection conn = (HttpURLConnection) url.openConnection();
		conn.setRequestMethod("GET");//GET Method used for file download
		conn.setDoOutput(true);
		conn.connect();

		File dir = context.getExternalFilesDir(null);
		if (dir == null) {
			dir = context.getCacheDir();
		}
		File file = new File(dir, "scan_result.txt");
		FileOutputStream fileOutput = new FileOutputStream(file);
		/*Uses the connection to receive data using inputStream object*/
		InputStream inputStream = conn.getInputStream();

		byte[] buffer = new byte[1024];
		int bufferLength = 0;

		while ((bufferLength = inputStream.read(buffer)) > 0) {
			fileOutput.write(buffer, 0, bufferLength);
		}
		fileOutput.close();
		return file;
		/*Catch blocks to handle exceptions if there is error in file download*/
	}
}
