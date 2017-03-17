package controller;

import socket.ISocketController;
import socket.ISocketObserver;
import socket.SocketInMessage;
import socket.SocketOutMessage;
import weight.IWeightInterfaceController;
import weight.IWeightInterfaceObserver;
import weight.KeyPress;
/**
 * MainController - integrating input from socket and ui. Implements ISocketObserver and IUIObserver to handle this.
 * @author Christian Budtz
 * @version 0.1 2017-01-24
 *
 */
public class MainController implements IMainController, ISocketObserver, IWeightInterfaceObserver {

	private ISocketController socketHandler;
	private IWeightInterfaceController weightController;
	private KeyState keyState = KeyState.K1;
	//Things for UI controller
	private int i=0;
	private String text ="";
	private char[] a = new char[30];
	private double tara = 0.0;
	private double weightCurrent = 0.0;

	public MainController(ISocketController socketHandler, IWeightInterfaceController weightInterfaceController) {
		this.init(socketHandler, weightInterfaceController);
	}

	@Override
	public void init(ISocketController socketHandler, IWeightInterfaceController weightInterfaceController) {
		this.socketHandler = socketHandler;
		this.weightController = weightInterfaceController;
	}

	@Override
	public void start() {
		if (socketHandler!=null && weightController!=null) {
			//Makes this controller interested in messages from the socket
			socketHandler.registerObserver(this);
			//Starts socketHandler in own thread
			new Thread(socketHandler).start();
			//FIXME (Måske jeg har lavet det) set up weightController - Look above for inspiration (Keep it simple ;))
            weightController.registerObserver(this);
			new Thread(weightController).start();
		} else {
			System.err.println("No controllers injected!");
		}
	}

	//Listening for socket input
	@Override
	public void notify(SocketInMessage message) {
		switch (message.getType()) {
		case B:
			notifyWeightChange(Double.parseDouble(message.getMessage()));
			break;
		case D:
			weightController.showMessagePrimaryDisplay(message.getMessage()); 
			break;
		case Q:
			System.exit(1);
			break;
		case RM204:
			break;
		case RM208:
			weightController.showMessageSecondaryDisplay(message.getMessage());
			break;
		case S:
			socketHandler.sendMessage(new SocketOutMessage(""+(weightCurrent-tara)));
			break;
		case T:
			tara = weightCurrent;
			notifyWeightChange(tara);
			break;
		case DW:
			break;
		case K:
			handleKMessage(message);
			break;
		case P111:
			weightController.showMessageSecondaryDisplay(message.getMessage());
			break;
		}
	}

	private void handleKMessage(SocketInMessage message) {
		switch (message.getMessage()) {
		case "1" :
			this.keyState = KeyState.K1;
			break;
		case "2" :
			this.keyState = KeyState.K2;
			break;
		case "3" :
			this.keyState = KeyState.K3;
			break;
		case "4" :
			this.keyState = KeyState.K4;
			break;
		default:
			socketHandler.sendMessage(new SocketOutMessage("ES"));
			break;
		}
	}
	//Listening for UI input
	@Override
	public void notifyKeyPress(KeyPress keyPress) {
		//FIXME Ting vi har lavet er her
		switch (keyPress.getType()) {
		case SOFTBUTTON:
			//We dont know what to implement here
			break;
		case TARA:
            tara = weightCurrent;
            notifyWeightChange(tara);
			break;
		case TEXT:
			a[i]=keyPress.getCharacter();
			text += a[i];
			i++;
			weightController.showMessageSecondaryDisplay(text);
			break;
		case ZERO:
		    tara = 0.0;
			notifyWeightChange(0.0);
			break;
		case C:
			i=0;
			text="";
			weightController.showMessageSecondaryDisplay(text);
			break;
		case EXIT:
			System.exit(1);
			break;
		case SEND:
			if (keyState.equals(KeyState.K4) || keyState.equals(KeyState.K3) ) {
				socketHandler.sendMessage(new SocketOutMessage("K A 3"));
			}
			break;
		}
	}

	@Override
    //FIXME også noget vi har lavet her
	public void notifyWeightChange(double newWeight) {
	    weightCurrent = newWeight;
		weightController.showMessagePrimaryDisplay(""+(weightCurrent-tara));
	}
}
