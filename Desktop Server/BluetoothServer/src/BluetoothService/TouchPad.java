package BluetoothService;

import java.awt.AWTException;
import java.awt.MouseInfo;
import java.awt.Robot;

import org.json.JSONObject;

import in.spacehack.desktop.bluetooth.InputObserver;

public class TouchPad extends InputObserver
{

	Robot movementHandler;
	
	TouchPad()
	{
		this.targuet.add("touchpad");
		
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
		
		float rawMovementX 	= (Float.parseFloat(movement.get("x").toString())/100);
		float rawMovementY 	= (Float.parseFloat(movement.get("y").toString())/100);
		
		int movementX 		= Math.round(rawMovementX);
		int movementY 		= Math.round(rawMovementY);				
		
		int posX = MouseInfo.getPointerInfo().getLocation().x + movementX;
		int posY = MouseInfo.getPointerInfo().getLocation().y + movementY;
		
		movementHandler.mouseMove(posX, posY);
		
	}
	
}
