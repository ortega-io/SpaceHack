package in.spacehack.android.bluetooth;

import org.json.JSONException;
import org.json.JSONObject;

/**
 *
 * BluetoothMessage
 *
 * Simple wrapper to define a Bluetooth message.
 *
 *
 * @package     Android
 * @subpackage  Bluetooth
 * @author      Otoniel Ortega <ortega_x2@hotmail.com>
 * @copyright   2015 Otoniel Ortega (c)
 * @version     1.0
 * @license     CC BY-NC 4.0 (https://creativecommons.org/licenses/by-nc/4.0/)
 *
 */

public class BluetoothMessage extends JSONObject
{


    public void setType(String type)
    {
        try
        {
            this.put("type", type);
        }
        catch (JSONException e)
        {
            // Error?
        }
    }


    public void setData(JSONObject data)
    {
        try
        {
            this.put("data", data);
        }
        catch (JSONException e)
        {
            // Error?
        }
    }

    public void setEmptyData()
    {
        try
        {
            JSONObject data = new JSONObject();
            this.put("data", data);
        }
        catch (JSONException e)
        {
            // Error?
        }
    }


}
