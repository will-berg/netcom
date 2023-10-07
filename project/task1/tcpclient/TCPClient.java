package tcpclient;
import java.net.*;
import java.io.*;

/*
1. Open a TCP connection to a server at a given host address and port number.
2. Send data to the server.
3. Take the data that the server sends back in response, and return that as the result.
*/

// TCPClient sends and receives bytes, outputstream.write() to send and inputstream.read() to receive
public class TCPClient {

	// Constructor
    public TCPClient(boolean shutdown, Integer timeout, Integer limit) {
    }

	// Method to send bytes 'bytesToServer' to a server process with given hostname and port, returns the response
    public byte[] askServer(String hostname, int port, byte[] bytesToServer) throws IOException {
		// Send bytes to server
		Socket clientSocket = new Socket(hostname, port); 			// Create socket to server with specified hostname and port
		OutputStream outStream = clientSocket.getOutputStream(); 	// Create an outputstream to send data using the socket
		outStream.write(bytesToServer); 							// Write bytes to outputstream

		// Receive bytes from server
		byte[] response = receiveFromServer(clientSocket);
		return response;
    }

	// If there are no bytes to send, we will only receive
	public byte[] askServer(String hostname, int port) throws IOException {
		Socket clientSocket = new Socket(hostname, port);
		byte[] response = receiveFromServer(clientSocket);
		return response;
	}

	// Receive bytes from a server specified by given socket, returns response as byte array
	private byte[] receiveFromServer(Socket s) throws IOException {
		InputStream inStream = s.getInputStream(); 							// Create an inputstream to receive data using the socket
		ByteArrayOutputStream receiveBuffer = new ByteArrayOutputStream(); 	// Create dynamic buffer that stores data received
		// Receive and store bytes from server
		/* We will stop receiving data from the server when
		1. the server closes the connection
		2. when we have not received data from the server during a period of time we close the connection
		3. when we have received a certain amount of bytes we close the connection */

		// 3. Data limit
		int bytesReceived = 0;
		while (true) {
			byte[] intermediateBuffer = new byte[1024];
			int bytesRead = inStream.read(intermediateBuffer);				// Store received bytes in intermediateBuffer, return number of bytes read
			if (bytesRead == -1) break;										// read returns -1 if there are no more bytes to read
			receiveBuffer.write(intermediateBuffer, 0, bytesRead); 			// Write bytes from intermediate to receive buffer
		}

		s.close();
        return receiveBuffer.toByteArray();
	}
}
