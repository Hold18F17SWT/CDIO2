package socket;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashSet;
import java.util.Set;

import com.sun.xml.internal.ws.encoding.soap.SOAP12Constants;
import socket.SocketInMessage.SocketMessageType;

public class SocketController implements ISocketController {
	Set<ISocketObserver> observers = new HashSet<ISocketObserver>();
	//TODO Maybe add some way to keep track of multiple connections?
	private BufferedReader inStream;
	private DataOutputStream outStream;


	@Override
	public void registerObserver(ISocketObserver observer) {
		observers.add(observer);
	}

	@Override
	public void unRegisterObserver(ISocketObserver observer) {
		observers.remove(observer);
	}

	@Override
	public void sendMessage(SocketOutMessage message) {
		if (outStream!=null){
			try {
				outStream.writeBytes(message.getMessage() + '\n');
			} catch (IOException e) {
				e.printStackTrace();
			}


		} else {
			//TODO maybe tell someone that connection is closed?
		}
	}

	@Override
	public void run() {
		//TODO some logic for listening to a socket //(Using try with resources for auto-close of socket)
		try (ServerSocket listeningSocket = new ServerSocket(Port)){ 
			while (true){
				waitForConnections(listeningSocket); 	
			}		
		} catch (IOException e1) {
			// TODO Maybe notify MainController?
			e1.printStackTrace();
		} 


	}

	private void waitForConnections(ServerSocket listeningSocket) {
		try {
			Socket activeSocket = listeningSocket.accept(); //Blocking call
			inStream = new BufferedReader(new InputStreamReader(activeSocket.getInputStream()));
			outStream = new DataOutputStream(activeSocket.getOutputStream());
			String inLine;
			//.readLine is a blocking call 
			//TODO How do you handle simultaneous input and output on socket?
			//TODO this only allows for one open connection - how would you handle multiple connections?
			while (true){
				inLine = inStream.readLine();
				System.out.println(inLine);
				if (inLine==null) break;
				switch (inLine.split(" ")[0]) {
				case "RM20": // Display a message in the secondary display and wait for response
					notifyObservers(new SocketInMessage(SocketMessageType.RM208, inLine.split(" ")[1]));
					sendMessage(new SocketOutMessage("Wait for responce\n"));
					break;
				case "D":// Display a message in the primary display
					//TODO Refactor to make sure that faulty messages doesn't break the system
					notifyObservers(new SocketInMessage(SocketMessageType.D, inLine.split(" ")[1]));
					sendMessage(new SocketOutMessage("Display Updated\n"));
					break;
				case "DW": //Clear primary display
					notifyObservers(new SocketInMessage(SocketMessageType.D, ""));
					sendMessage(new SocketOutMessage("Cleared primary display\n"));
					break;
				case "P111": //Show something in secondary display
					notifyObservers(new SocketInMessage(SocketMessageType.P111, inLine.split(" ")[1]));
					sendMessage(new SocketOutMessage("Message Displayed\n"));
					break;
				case "T": // Tare the weight
					notifyObservers(new SocketInMessage(SocketMessageType.T, ""));
					sendMessage(new SocketOutMessage("Weight tared"));
					break;
				case "S": // Request the current load
					notifyObservers(new SocketInMessage(SocketMessageType.S,""));
					break;
				case "K":
					if (inLine.split(" ").length>1){
						notifyObservers(new SocketInMessage(SocketMessageType.K, inLine.split(" ")[1]));
						sendMessage(new SocketOutMessage("OK"));
					}
					break;
				case "B": // Set the load
					notifyObservers(new SocketInMessage(SocketMessageType.B, inLine.split(" ")[1]));
					sendMessage(new SocketOutMessage("Load set"));
					break;
				case "Q": // Quit
					notifyObservers(new SocketInMessage(SocketMessageType.Q,""));
					sendMessage(new SocketOutMessage("Goodbye"));
					break;
				default: //Something went wrong?
					sendMessage(new SocketOutMessage("ES"));
					break;
				}
			}
		} catch (IOException e) {
			//TODO maybe notify mainController?
			e.printStackTrace();
		}
	}

	private void notifyObservers(SocketInMessage message) {
		for (ISocketObserver socketObserver : observers) {
			socketObserver.notify(message);
		}
	}

}

