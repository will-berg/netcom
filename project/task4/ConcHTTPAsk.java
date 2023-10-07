import java.net.*;
import java.io.*;
import myrunnable.MyRunnable;
import java.lang.Thread;

/* The server should Use Java threading to handle many clients at the same time.
let the ConcHTTPAsk server create one thread for each client. In other words,
when a new client contacts the server (the server returns from calling the accept method on its welcoming socket),
the server creates a new thread, and this thread will serve the client. */
public class ConcHTTPAsk {
	private static int parseArgs(String[] args) {
		int port = 0;
		if (args.length != 1) {
			System.out.println("Exactly one input argument required");
			System.exit(1);
		}
		try {
			port = Integer.parseInt(args[0]);
		} catch (Exception ex) {
			System.err.println(ex);
			System.exit(1);
		}
		return port;
	}


	// The main method implements the server. It takes one argument: the port number.
    public static void main(String[] args) throws IOException {
		int serverPort = parseArgs(args);
		ServerSocket serverSocket = new ServerSocket(serverPort);

		while (true) {
			// Start listening for client requests, blocks until connection is made, returns connection socket
			Socket s = serverSocket.accept();
			Runnable r = new MyRunnable(s);
			/* Create a new thread that will serve the client connected on socket s Calling the thread's
			start method will in turn call the run method where the thread will do its work */
			new Thread(r).start();
		}
    }
}
