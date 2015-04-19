package BluetoothService;

/**
*
* ConsolePrinter
*
* Dummy class to show use of the InputObserver class.
*
*/


import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.MouseInfo;
import java.awt.Robot;

import org.json.JSONObject;

import in.spacehack.desktop.bluetooth.BluetoothServer;
import in.spacehack.desktop.bluetooth.InputObserver;



public class MouseMove extends InputObserver
{

	float 	lastTethaX 		= 0;
	float 	lastTethaY 		= 0;
	float 	lastTethaZ 		= 0;
	int     step 			= 1;
	
	
	boolean isReady			= false;

	
	MouseMove()
	{

		this.targuet.add("coordinates");
		
	}

	
	@Override
	public void processMessage(JSONObject message)
	{
                
		//System.out.println(message.toString());

		JSONObject orientation = new JSONObject(message.get("orientation").toString());

		float tethaX = Float.parseFloat(orientation.get("x").toString());
		float tethaY = Float.parseFloat(orientation.get("y").toString());
		float tethaZ = Float.parseFloat(orientation.get("z").toString());
		
		float tethaXr = Float.parseFloat(orientation.get("x").toString());
		float tethaYr = Float.parseFloat(orientation.get("y").toString());					
		float tethaZr = Float.parseFloat(orientation.get("z").toString());
		
		tethaX  = (tethaX<0) ? (360- Math.abs(tethaX)) : tethaX;
		tethaY  = (tethaY<0) ? (360- Math.abs(tethaY)) : tethaY;
		tethaZ  = (tethaZ<0) ? (360- Math.abs(tethaZ)) : tethaZ;
		
		
		System.out.println("I:["+tethaX+","+tethaY+","+tethaZ+"]"+" IR:["+tethaXr+","+tethaYr+","+tethaZr+"]");
		
		if(this.isReady)
		{
					
			float deltaX 	= (this.lastTethaX > tethaX) ? (this.lastTethaX - tethaX) : (tethaX - this.lastTethaX) ;
			float deltaY 	= (this.lastTethaY > tethaY) ? (this.lastTethaY - tethaY) : (tethaY - this.lastTethaY) ;
			float deltaZ 	= (this.lastTethaZ > tethaZ) ? (this.lastTethaZ - tethaZ) : (tethaZ - this.lastTethaZ) ;
			
			System.out.println("L:["+this.lastTethaX+","+this.lastTethaY+","+this.lastTethaZ+"] "+"C:["+tethaX+","+tethaY+","+tethaZ+"] D:["+deltaX+","+deltaY+","+deltaZ+"]");
			
			this.lastTethaX = tethaX;
			this.lastTethaY = tethaY;
			this.lastTethaZ = tethaZ;
			
		}
		else
		{
			
			this.lastTethaX = tethaX;
			this.lastTethaY = tethaY;
			this.lastTethaZ = tethaZ;
			
			this.isReady 	= true;
			
		}
		
	}

	
	
	public void move(float deltaX, float deltaY, float deltaZ)
	{
		try
		{
			
			int poxX = MouseInfo.getPointerInfo().getLocation().x + Math.round(deltaZ);
			
			
            Robot robot = new Robot();
            //robot.mouseMove(posX, posY);
        }
        catch(Exception e)
        {
            System.out.println(e.getMessage());
        }
	}
	
	
	
}
