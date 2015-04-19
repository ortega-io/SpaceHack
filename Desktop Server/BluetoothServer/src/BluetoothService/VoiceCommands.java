package BluetoothService;


/**
*
* ConsolePrinter
*
* Dummy class to show use of the InputObserver class.
*
*/


import java.util.HashMap;
import java.util.Map;

import org.json.JSONObject;

import in.spacehack.desktop.bluetooth.BluetoothServer;
import in.spacehack.desktop.bluetooth.InputObserver;



public class VoiceCommands extends InputObserver
{
	
	// Define list of commands ===================================================  //
	
	CommandRecognizer commandRecognize;
	

	VoiceCommands()
	{
		this.targuet.add("voice");
		
		// Define list of commands ===============================================  //
		
		this.commandRecognize = new CommandRecognizer();
		
		
	}

	
	@Override
	public void processMessage(JSONObject message)
	{
                
		System.out.println(">> Voice: "+message.get("command"));
		String command 	= message.getString("command");
		
		this.commandRecognize.commandAction(command.toLowerCase());
		
	}
	
	
}
