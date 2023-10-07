package tcpclient;
import java.net.*;
import java.io.*;

/*
 * 1.If shutdown is true, TCPClient will shut down the connection in the outgoing direction (but only in that direction)
 * after having sent the (optional) data to the server. Otherwise, TCPClient will not shut down the connection.
 * 2. The limit parameter is the maximum amount of data (in bytes) that the client should receive before returning.
 * If there is no upper limit for how much data the client should receive, limit is null.
 * 3. When askServer has not received any data from the server during a period of time, it closes the connection and returns.
 */

// TCPClient sends and receives bytes, outputstream.write() to send and inputstream.read() to receive
public class TCPClient {
	private boolean shutdown;
	private Integer timeout;
	private Integer limit;

	// Constructor
    public TCPClient(boolean shutdown, Integer timeout, Integer limit) {
		this.shutdown = shutdown;
		this.timeout = timeout;
		this.limit = limit;
    }

	// Method to send bytes 'bytesToServer' to a server process with given hostname and port, returns the response
    public byte[] askServer(String hostname, int port, byte[] bytesToServer) throws IOException {
		Socket clientSocket = new Socket(hostname, port);
		OutputStream outStream = clientSocket.getOutputStream();
		outStream.write(bytesToServer);
		byte[] response = receiveFromServer(clientSocket);
		return response;
    }

	// If there are no bytes to send, we will only receive
	public byte[] askServer(String hostname, int port) throws IOException {
		Socket clientSocket = new Socket(hostname, port);
		byte[] response = receiveFromServer(clientSocket);
		return response;
	}

	// Receive data from a server specified by given socket, returns response as byte array
	private byte[] receiveFromServer(Socket s) throws IOException {
		// Shutdown
		if (shutdown) s.shutdownOutput();
		if (timeout != null) s.setSoTimeout(timeout);

		InputStream inStream = s.getInputStream();
		ByteArrayOutputStream receiveBuffer = new ByteArrayOutputStream();

		int bytesRead;
		int totalBytesRead = 0;
		while (true) {
			byte[] intermediateBuffer = new byte[1024];

			// Timeout
			try {
				bytesRead = inStream.read(intermediateBuffer);	// Blocking operation, reads stream and stores in buffer
			} catch (SocketTimeoutException ex) {
				break;
			}

			// Nothing more to read
			if (bytesRead == -1) break;

			// Data limit
			totalBytesRead += bytesRead;
			if (limit != null && totalBytesRead >= limit) {
				int bytesToWrite = limit - (totalBytesRead - bytesRead);
				receiveBuffer.write(intermediateBuffer, 0, bytesToWrite);
				break;
			}

			receiveBuffer.write(intermediateBuffer, 0, bytesRead);
		}

		// On breaks, close the connection and return
		s.close();
        return receiveBuffer.toByteArray();
	}
}
