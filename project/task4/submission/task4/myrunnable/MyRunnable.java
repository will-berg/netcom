package myrunnable;

import java.net.*;
import java.io.*;
import tcpclient.TCPClient;
import java.util.HashMap;

public class MyRunnable implements Runnable {
	// The connection socket of the client being served
	private Socket s;

	private boolean badRequest = false;
	private boolean exists = true;

	private byte[] string = null;
	private boolean shutdown = false;
	private Integer limit = null;
	private Integer timeout = null;
	private String hostname;
	private int port;

	// Constructor to create MyRunnable objects
	public MyRunnable(Socket s) {
		this.s = s;
	}


	private String receiveFromClient(Socket s) throws IOException {
		InputStream inStream = s.getInputStream();
		ByteArrayOutputStream receiveBuffer = new ByteArrayOutputStream();

		int bytesRead = 0;
		while (true) {
			byte[] intermediateBuffer = new byte[1024];
			bytesRead = inStream.read(intermediateBuffer);
			if (bytesRead == -1) break;
			receiveBuffer.write(intermediateBuffer, 0, bytesRead);
			if (new String(receiveBuffer.toByteArray()).endsWith("\r\n\r\n")) break;
		}
        return new String(receiveBuffer.toByteArray());
	}


	private HashMap<String, String> parseRequest(String request) throws IOException {
		String[] lines = request.split("\n");
		String firstLine = lines[0];
		String[] httpReq = firstLine.split(" ");
		String method = httpReq[0];
		String uri = httpReq[1];
		String protocol = httpReq[2];
		if (!method.equals("GET") || !protocol.contains("HTTP/1.1")) badRequest = true;
		if (!uri.substring(0, 4).equals("/ask")) exists = false;
		String[] values = uri.split("&");

		HashMap<String, String> params = new HashMap<>();
		for (String v : values) {
			if (v.contains("hostname=")) params.put("hostname", v.substring(v.indexOf("=") + 1));
			if (v.contains("port=")) params.put("port", v.substring(v.indexOf("=") + 1));
			if (v.contains("string=")) params.put("string", v.substring(v.indexOf("=") + 1) + "\n");
			if (v.contains("shutdown=")) params.put("shutdown", v.substring(v.indexOf("=") + 1));
			if (v.contains("timeout=")) params.put("timeout", v.substring(v.indexOf("=") + 1));
			if (v.contains("limit=")) params.put("limit", v.substring(v.indexOf("=") + 1));
		}
		return params;
	}


	private void respondToClient(Socket s) throws IOException {
		String response = "HTTP/1.1 200 OK\r\n\r\n";
		String serverResponse = "";

		if (badRequest) {
			response = "HTTP/1.1 400 Bad Request\r\n\r\n";
		} else if (!exists) {
			response = "HTTP/1.1 404 Not Found\r\n\r\n";
		} else {
			TCPClient tcpclient = new TCPClient(shutdown, timeout, limit);
			if (string != null) {
				try {
					serverResponse = new String(tcpclient.askServer(hostname, port, string));
				} catch (java.net.UnknownHostException ex) {
					serverResponse = "HTTP/1.1 404 Not Found\r\n\r\nThe specified hostname " + hostname + " does not exist";
				}
			} else {
				try {
					serverResponse = new String(tcpclient.askServer(hostname, port));
				} catch (java.net.UnknownHostException ex) {
					serverResponse = "HTTP/1.1 404 Not Found\r\n\r\nThe specified hostname " + hostname + " does not exist";
				}
			}
			response += serverResponse;
		}

		OutputStream outStream = s.getOutputStream();
		outStream.write(response.getBytes());
	}


	/* The run method is where there thread will do its work. This is where you should
	implement the code that corresponds to the HTTPAsk server from Task 3 */
	public void run() {
		HashMap<String, String> params = new HashMap<>();
		try {
			String request = receiveFromClient(s);
			params = parseRequest(request);
		} catch (IOException ex) {
			System.err.println(ex);
		}

		try {
			hostname = new String(params.get("hostname"));
			port = Integer.parseInt(params.get("port"));
		} catch (Exception ex)  {
			badRequest = true;
		}

		try { string = params.get("string").getBytes(); }
		catch (Exception ex) { string = null; }
		try { shutdown = Boolean.parseBoolean(params.get("shutdown")); }
		catch (Exception ex) { shutdown = false; }
		try { limit = Integer.parseInt(params.get("limit")); }
		catch (Exception ex) { limit = null; }
		try { timeout = Integer.parseInt(params.get("timeout")); }
		catch (Exception ex) { timeout = null; }

		try {
			respondToClient(s);
			s.close();
		} catch (IOException ex) {
			System.err.println(ex);
		}
	}
}
