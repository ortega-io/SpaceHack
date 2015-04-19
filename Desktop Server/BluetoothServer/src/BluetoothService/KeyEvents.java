package BluetoothService;

/**
*
* ConsolePrinter
*
* Dummy class to show use of the InputObserver class.
*
*/


import java.awt.AWTException;
import java.awt.Robot;
import java.awt.event.InputEvent;

import org.json.JSONObject;

import in.spacehack.desktop.bluetooth.BluetoothServer;
import in.spacehack.desktop.bluetooth.InputObserver;



public class KeyEvents extends InputObserver
{

	private Robot 	clicksHandler;

	KeyEvents()
	{
		

		this.targuet.add("media_key");
		
		try
		{
			this.clicksHandler	= new Robot();
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
		
		System.out.println(">> Key : ["+message.getString("event_type")+"]");
		
		String keyEvent = message.getString("event_type");

		try
		{

            if(keyEvent.equals("key_down"))
            {
            	System.out.println("on key down");
            	this.clicksHandler.mousePress(InputEvent.BUTTON1_DOWN_MASK);

            }
            else if(keyEvent.equals("key_up"))
            {
            	
            	System.out.println("on key up");
            	this.clicksHandler.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);            	
            }
            else if(keyEvent.equals("right_key_down"))
            {
            	System.out.println("on key down");
            	this.clicksHandler.mousePress(InputEvent.BUTTON3_DOWN_MASK);

            }
            else if(keyEvent.equals("right_key_up"))
            {
            	
            	System.out.println("on key up");
            	this.clicksHandler.mouseRelease(InputEvent.BUTTON3_DOWN_MASK);            	
            }
            else if(keyEvent.equals("touchpad_click"))
            {
            	
            	System.out.println("on touchpad click");
            	this.clicksHandler.mousePress(InputEvent.BUTTON1_DOWN_MASK);
            	this.clicksHandler.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);            	
            }
            else
            {
            	System.out.println("on key nothing");
            }

        }
        catch(Exception e)
        {
            System.out.println(e.getMessage());
        }
		
	}
	
	
}

