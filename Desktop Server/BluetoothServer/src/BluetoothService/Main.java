package BluetoothService;

import in.spacehack.desktop.bluetooth.BluetoothServer;

/**
*
* BluetoothServer Demo
*
* Instanciates a Bluetooth Server.
*
*/

public class Main
{

	public static void main(String[] args) throws Exception
	{
		
	
		BluetoothServer newServer 		= new BluetoothServer();
		
		// Spaw Observers //
		
		MouseMove mouseMovements 		= new MouseMove();
		//mouseMovements.attach(newServer);
		
		Gestures  gesturesRecognizer 	= new Gestures();
		gesturesRecognizer.attach(newServer);
		
		TouchPad  touchPad 				= new TouchPad();
		touchPad.attach(newServer);
		
		ScrollBar  scrollBar 			= new ScrollBar();
		scrollBar.attach(newServer);
		
		
		KeyEvents 	keyEvents 			= new KeyEvents();		
		keyEvents.attach(newServer);
		
		
		VoiceCommands 	voiceCommands	= new VoiceCommands();		
		voiceCommands.attach(newServer);
		
		
		newServer.start();
		
	
	}
	
}
