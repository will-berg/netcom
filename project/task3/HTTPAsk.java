import tcpclient.TCPClient;
import java.net.*;
import java.io.*;
import java.util.HashMap;

/*
 * What you need to work on in this task is mainly to process the HTTP request. You should extract the hostname, port,
 * and string parameters from it, as well as the optional arguments for askServer (ie., shutdown flag, timeout, and limit).
 * Your HTTP server only needs to recognize GET requests. In a GET request, parameters are sent in the URI component.

 * Use the ServerSocket class to create the socket for the web server.
 * The server should read and analyze an HTTP GET request, and extract a query string from it.
 * Then it should compose and return a valid HTTP response.
 * When HTTPAsk receives the HTTP request, it will call the method TCPClient.askServer(), and return the output as an HTTP response.
 * Build a web server that runs TCPAsk for you, and presents the result as a web page (in an HTTP response).
 */

/* The main method implements the server. It takes one argument: the port number.
The server should run in an infinite loop, and when one client has been served,
the server should be prepared to take care of the next. */
public class HTTPAsk {
	// Used to determine correct HTTP return code
	private static boolean badRequest = false;
	private static boolean exists = true;
	// Optional parameters
	private static byte[] string = null;
	private static boolean shutdown = false;
	private static Integer limit = null;
	private static Integer timeout = null;
	// Mandatory parameters
	private static String hostname;
	private static int port;


	// Parse the cli arguments and return the port number that the server should run on
	private static int parseArgs(String[] args) {
		int port = 0;
		if (args.length != 1) {
			System.out.println("Exactly one input argument required");
		}
		try {
			port = Integer.parseInt(args[0]);
		} catch (Exception ex) {
			System.err.println(ex);
		}
		return port;
	}


	// Receive initial HTTP request that contains the askServer parameters from the client
	private static String receiveFromClient(Socket s) throws IOException {
		// s.setSoTimeout(1500);
		InputStream inStream = s.getInputStream();
		ByteArrayOutputStream receiveBuffer = new ByteArrayOutputStream();

		int bytesRead = 0;
		while (true) {
			byte[] intermediateBuffer = new byte[1024];
			bytesRead = inStream.read(intermediateBuffer);
			if (bytesRead == -1) break;
			receiveBuffer.write(intermediateBuffer, 0, bytesRead);
			// The two empty lines mark the end of the request
			if (new String(receiveBuffer.toByteArray()).endsWith("\r\n\r\n")) break;
		}
        return new String(receiveBuffer.toByteArray());
	}


	// Extracts the hostname, port, and string parameters, as well as the optional arguments shutdown flag, timeout, and limit.
	private static HashMap<String, String> parseRequest(String request) throws IOException {
		// Reset these values
		badRequest = false;
		exists = true;

		String[] lines = request.split("\n");
		String firstLine = lines[0];
		String[] httpReq = firstLine.split(" ");
		String method = httpReq[0];
		String uri = httpReq[1];
		String protocol = httpReq[2];
		// Should be a GET request using the HTTP/1.1 protocol
		if (!method.equals("GET") || !protocol.contains("HTTP/1.1")) badRequest = true;
		// The server only recognizes the /ask resource
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


	/* Send an HTTP response to the client containing the output of the request determined by the parameters.
	Also include an HTTP header depending on if the request is bad (400 range) or ok (200) */
	private static void respondToClient(Socket s) throws IOException {
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


    public static void main(String[] args) throws IOException {
		// Get the port supplied on the cli and open a serversocket listening on that port
		int serverPort = parseArgs(args);
		ServerSocket serverSocket = new ServerSocket(serverPort);

		while (true) {
			// Start listening for client requests, blocks until connection is made, returns connection socket
			Socket s = serverSocket.accept();
			// Read data from the client, will be a string representation of an HTTP request made to the server
			String request = receiveFromClient(s);
			// Parse the HTTP request and return the parameters for TCPClient.askServer() in a hash map
			HashMap<String, String> params = parseRequest(request);
			//System.out.println(params.toString());

			// Hostname and port are mandatory parameters so the request is bad if they are missing
			try {
				// Convert hostname to string to ensure exception is thrown if params.get returns null
				hostname = new String(params.get("hostname"));
				port = Integer.parseInt(params.get("port"));
			} catch (Exception ex)  {
				badRequest = true;
			}

			/* Attempt to extract parameters, will throw an error if they don't exist in params
			since none of the parse methods or getBytes can be applied on null values */
			try { string = params.get("string").getBytes(); }
			// If not specified, use class defaults
			catch (Exception ex) { string = null; }
			try { shutdown = Boolean.parseBoolean(params.get("shutdown")); }
			catch (Exception ex) { shutdown = false; }
			try { limit = Integer.parseInt(params.get("limit")); }
			catch (Exception ex) { limit = null; }
			try { timeout = Integer.parseInt(params.get("timeout")); }
			catch (Exception ex) { timeout = null; }

			/*
			System.out.println("hostname: " + hostname);
			System.out.println("port: " + port);
			System.out.println("string: " + string);
			System.out.println("shutdown: " + shutdown);
			System.out.println("limit: " + limit);
			System.out.println("timeout: " + timeout);
			System.out.println("badRequest: " + badRequest);
			System.out.println("exists: " + exists);
			*/
			respondToClient(s);

			s.close();
		}
    }
}

