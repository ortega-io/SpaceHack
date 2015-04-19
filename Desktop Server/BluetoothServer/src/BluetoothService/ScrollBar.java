package BluetoothService;

import java.awt.AWTException;
import java.awt.MouseInfo;
import java.awt.Robot;

import org.json.JSONObject;

import in.spacehack.desktop.bluetooth.InputObserver;

public class ScrollBar extends InputObserver
{

	Robot movementHandler;
	
	ScrollBar()
	{
		this.targuet.add("scrollbar");
		
		try
		{
			movementHandler = new Robot();
		}
		catch (AWTException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	
	@Override
	public void processMessage(JSONObject message)
	{	

		JSONObject movement = new JSONObject(message.get("movement").toString());
		float rawMovementY 	= (Float.parseFloat(movement.get("y").toString())/400);
		int movementY 		= Math.round(rawMovementY);				
		int notches 		= 2*movementY;
		
		movementHandler.mouseWheel(notches);
		
	}
	
}
