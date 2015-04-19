package in.spacehack.desktop.bluetooth;

import java.util.ArrayList;

import org.json.JSONObject;


/**
*
* InputObserver
*
* Defines the base for observers watching the Bluetooth input.  
*
* @package     Desktop
* @subpackage  Bluetooth
* @author      Otoniel Ortega <ortega_x2@hotmail.com>
* @copyright   2015 Otoniel Ortega (c)
* @version     1.0
* @license     CC BY-NC 4.0 (https://creativecommons.org/licenses/by-nc/4.0/)
*
*/

public abstract class InputObserver
{

	/* Defines types of messages to watch */
	
	protected ArrayList<String>     targuet		= new ArrayList<String>();
	
	
	/* Defines the Bluetooth channel to observe */
	
	protected BluetoothServer 	channel;
	
	
	/* Function to handle messages to be implemented by child classes */
	
	public abstract void 		processMessage(JSONObject message);
	
	
	
    /**
     * ----------------------------------------------------
     * attach(BluetoothServer channel)
     * ----------------------------------------------------
     *
     * Links the current observer to the Bluetooth channel
     * and registers it on its observers list.
     *
     * @param	BluetoothServer channel
     *
     * @return  void
     *
     */
	
	
	public void attach(BluetoothServer channel)
	{
		
		this.channel = channel;
		this.channel.registerObserver((InputObserver)this);
		
	}
	
	
    /**
     * ----------------------------------------------------
     * attach(BluetoothServer channel)
     * ----------------------------------------------------
     *
     * Links the current observer to the Bluetooth channel
     * and registers it on its observers list.
     *
     * @param	BluetoothServer channel
     *
     * @return  void
     *
     */
	
	public ArrayList<String> getTarguet()
	{
		return this.targuet;
	}
	
	
	
}
