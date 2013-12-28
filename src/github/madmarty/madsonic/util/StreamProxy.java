package github.madmarty.madsonic.util;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.URLDecoder;
import java.net.UnknownHostException;
import java.util.StringTokenizer;

import org.apache.http.HttpRequest;
import org.apache.http.message.BasicHttpRequest;

import github.madmarty.madsonic.service.DownloadFile;
import github.madmarty.madsonic.service.DownloadService;
import github.madmarty.madsonic.domain.MusicDirectory;

import android.util.Log;

public class StreamProxy implements Runnable {
	private static final String TAG = StreamProxy.class.getSimpleName();

	private Thread thread;
	private boolean isRunning;
	private ServerSocket socket;
	private int port;
	private DownloadService downloadService;

	public StreamProxy(DownloadService downloadService) {

		// Create listening socket
		try {
			socket = new ServerSocket(0, 0, InetAddress.getByAddress(new byte[] { 127, 0, 0, 1 }));
			socket.setSoTimeout(5000);
			port = socket.getLocalPort();
			this.downloadService = downloadService;
		} catch (UnknownHostException e) { // impossible
		} catch (IOException e) {
			Log.e(TAG, "IOException initializing server", e);
		}
	}
	
	public int getPort() {
		return port;
	}

	public void start() {
		thread = new Thread(this);
		thread.start();
	}

	public void stop() {
		isRunning = false;
		thread.interrupt();
	}

	@Override
	public void run() {
		isRunning = true;
		while (isRunning) {
			try {
				Socket client = socket.accept();
				if (client == null) {
					continue;
				}
				Log.i(TAG, "client connected");

				StreamToMediaPlayerTask task = new StreamToMediaPlayerTask(client);
				if (task.processRequest()) {
					new Thread(task).start();
				}

			} catch (SocketTimeoutException e) {
				// Do nothing
			} catch (IOException e) {
				Log.e(TAG, "Error connecting to client", e);
			}
		}
		Log.i(TAG, "Proxy interrupted. Shutting down.");
	}

	private class StreamToMediaPlayerTask implements Runnable {

		String localPath;
		Socket client;
		int cbSkip;

		public StreamToMediaPlayerTask(Socket client) {
			this.client = client;
		}
		
		private HttpRequest readRequest() {
			HttpRequest request = null;
			InputStream is;
			String firstLine;
			try {
				is = client.getInputStream();
				BufferedReader reader = new BufferedReader(new InputStreamReader(is), 8192);
				firstLine = reader.readLine();
			} catch (IOException e) {
				Log.e(TAG, "Error parsing request", e);
				return request;
			}

			if (firstLine == null) {
				Log.i(TAG, "Proxy client closed connection without a request.");
				return request;
			}

			StringTokenizer st = new StringTokenizer(firstLine);
			String method = st.nextToken();
			String uri = st.nextToken();
			String realUri = uri.substring(1);
			Log.i(TAG, realUri);
			request = new BasicHttpRequest(method, realUri);
			return request;
		}

		public boolean processRequest() {
			HttpRequest request = readRequest();
			if (request == null) {
				return false;
			}
			
			// Read HTTP headers
			Log.i(TAG, "Processing request");

			try {
				localPath = URLDecoder.decode(request.getRequestLine().getUri(), Constants.UTF_8);
			} catch (UnsupportedEncodingException e) {
				Log.e(TAG, "Unsupported encoding", e);
				return false;
			}
			
			Log.i(TAG, String.format("Processing request for file %s", localPath));
			File file = new File(localPath);
			if (!file.exists()) {
				Log.e(TAG, String.format("File %s does not exist", localPath));
				return false;
			}
			
			return true;
		}

		@Override
		public void run() {
			Log.i(TAG, "Streaming song in background");
			DownloadFile downloadFile = downloadService.getCurrentPlaying();
			MusicDirectory.Entry song = downloadFile.getSong();
			long fileSize = downloadFile.getBitRate() * ((song.getDuration() != null) ? song.getDuration() : 0) * 1000 / 8;
			Log.i(TAG, String.format("Streaming fileSize: %d", fileSize));

            // Create HTTP header
            String headers = "HTTP/1.0 200 OK\r\n";
            headers += "Content-Type: " + "application/octet-stream" + "\r\n";
            
            headers += "Connection: close\r\n";
            headers += "\r\n";

            long cbToSend = fileSize - cbSkip;
            OutputStream output = null;
            byte[] buff = new byte[64 * 1024];
            try {
                output = new BufferedOutputStream(client.getOutputStream(), 32*1024);                           
                output.write(headers.getBytes());

				if(!downloadFile.isWorkDone()) {
					// Loop as long as there's stuff to send
					while (isRunning && !client.isClosed()) {

						// See if there's more to send
						File file = new File(localPath);
						int cbSentThisBatch = 0;
						if (file.exists()) {
							FileInputStream input = new FileInputStream(file);
							input.skip(cbSkip);
							int cbToSendThisBatch = input.available();
							while (cbToSendThisBatch > 0) {
								int cbToRead = Math.min(cbToSendThisBatch, buff.length);
								int cbRead = input.read(buff, 0, cbToRead);
								if (cbRead == -1) {
									break;
								}
								cbToSendThisBatch -= cbRead;
								cbToSend -= cbRead;
								output.write(buff, 0, cbRead);
								output.flush();
								cbSkip += cbRead;
								cbSentThisBatch += cbRead;
							}
							input.close();
						}

						// Done regardless of whether or not it thinks it is
						if(downloadFile.isWorkDone() && cbSkip >= file.length()) {
							break;
						}

						// If we did nothing this batch, block for a second
						if (cbSentThisBatch == 0) {
							Log.d(TAG, String.format("Blocking until more data appears (%d)", cbToSend));
							Thread.sleep(1000);
						}
					}
				} else {
					Log.w(TAG, "Requesting data for completely downloaded file");
				}
            }
            catch (SocketException socketException) {
                Log.e(TAG, "SocketException() thrown, proxy client has probably closed. This can exit harmlessly");
            }
            catch (Exception e) {
                Log.e(TAG, "Exception thrown from streaming task:");
				Log.e(TAG, String.format("%s : %s", e.getClass().getName(), e.getLocalizedMessage()));
            }

            // Cleanup
            try {
                if (output != null) {
                    output.close();
                }
                client.close();
            }
            catch (IOException e) {
                Log.e(TAG, "IOException while cleaning up streaming task:");                
				Log.e(TAG, String.format("%s : %s", e.getClass().getName(), e.getLocalizedMessage()));
            }
        }
	}
}