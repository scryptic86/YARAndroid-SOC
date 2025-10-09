package com.netsec.clamav;

import java.io.DataOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import android.content.Context;
import android.net.Uri;
import android.util.Log;

public class UploadFileToServer {

	/* The address to server where the PHP file is stored */
	static String upLoadServerUri = "http://192.168.1.115/test/upload_file.php";

	/* Function which is used to create connection and file upload using a SAF Uri */
	public static int uploadFile(Context ctx, Uri fileUri) {
		HttpURLConnection conn = null;
		DataOutputStream dos = null;
		String lineEnd = "\r\n";
		String twoHyphens = "--";
		String boundary = "*****";
		int maxBufferSize = 1 * 1024 * 1024;

		try (InputStream fileInputStream = ctx.getContentResolver().openInputStream(fileUri)) {
			URL url = new URL(upLoadServerUri);
			conn = (HttpURLConnection) url.openConnection();
			conn.setDoInput(true);
			conn.setDoOutput(true);
			conn.setUseCaches(false);
			conn.setRequestMethod("POST");
			conn.setRequestProperty("Connection", "Keep-Alive");
			conn.setRequestProperty("ENCTYPE", "multipart/form-data");
			conn.setRequestProperty("Content-Type", "multipart/form-data;boundary=" + boundary);
			conn.setRequestProperty("uploaded_file", fileUri.toString());

			dos = new DataOutputStream(conn.getOutputStream());
			dos.writeBytes(twoHyphens + boundary + lineEnd);
			dos.writeBytes("Content-Disposition: form-data; name=\"uploaded_file\";filename=\""
					+ fileUri.getLastPathSegment() + "\"" + lineEnd);
			dos.writeBytes(lineEnd);

			int bytesAvailable = fileInputStream.available();
			int bufferSize = Math.min(bytesAvailable, maxBufferSize);
			byte[] buffer = new byte[bufferSize];

			int bytesRead = fileInputStream.read(buffer, 0, bufferSize);

			while (bytesRead > 0) {
				dos.write(buffer, 0, bufferSize);
				bytesAvailable = fileInputStream.available();
				bufferSize = Math.min(bytesAvailable, maxBufferSize);
				bytesRead = fileInputStream.read(buffer, 0, bufferSize);
			}

			dos.writeBytes(lineEnd);
			dos.writeBytes(twoHyphens + boundary + twoHyphens + lineEnd);

			MainActivity.uploadResponseCode = conn.getResponseCode();
			String serverResponseMessage = conn.getResponseMessage();
			Log.i("uploadFile", "HTTP Response is : " + serverResponseMessage
					+ ": " + MainActivity.uploadResponseCode);

			dos.flush();
			dos.close();

		} catch (Exception e) {
			e.printStackTrace();
			Log.e("Upload file to server Exception", "Exception : " + e.getMessage(), e);
		}

		return MainActivity.uploadResponseCode;
	}

	// Legacy helper kept for compatibility (keeps original signature)
	public static int uploadFile(String sourceFileUri) {
		// Not implemented for file path in this modernized flow.
		// Could be implemented to open a FileInputStream if needed.
		return -1;
	}

}
