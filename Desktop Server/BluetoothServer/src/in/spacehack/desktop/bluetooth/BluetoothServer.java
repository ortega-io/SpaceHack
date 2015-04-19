package in.spacehack.desktop.bluetooth;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

import javax.bluetooth.*;
import javax.microedition.io.*;

import org.json.JSONObject;



/**
*
* BluetoothServer
*
* Implements a Bluetooth Server using the Serial Port Profile (SPP)
* and enables transmission and reception of data from multipĺe clients
*
*
* @package     Desktop
* @subpackage  Bluetooth
* @author      Otoniel Ortega <ortega_x2@hotmail.com>
* @copyright   2015 Otoniel Ortega (c)
* @version     1.0
* @license     CC BY-NC 4.0 (https://creativecommons.org/licenses/by-nc/4.0/)
*
*/


public class BluetoothServer
{

	/* Private Variables ==================================================== */
	

	/* Define service attributes */
    
    private static final String 	SERVICE_UUID 	= "1101";
    private static final String 	SERVICE_NAME 	= "Bluetooth Service";
    
    
    /* Declare service variables */
    
    private static String 						socketPath;
    private static UUID 						serviceUuid;
    private static StreamConnectionNotifier		connectionNotifier;
    protected List<InputObserver> 				observers = new ArrayList<InputObserver>();
    
	
    
	/* Public Variables ===================================================== */
    
    
    /**
     * ----------------------------------------------------
     * start()
     * ----------------------------------------------------
     *
     * Starts the Bluetooth service.
     *
     *
     * return void
     *
     */
    
    public void start()
    {
    	
    	log("--------------------------------------------");
    	log(">> Bluetooth Service Handler");
    	log("--------------------------------------------");
    	log("");
		
    	this.serviceUuid 			= new UUID(SERVICE_UUID, true);
        this.socketPath 			= "btspp://localhost:" + this.serviceUuid +";name="+SERVICE_NAME;
        
        
        // Open Bluetooth socket //
        
        try
        {
			this.connectionNotifier 	= (StreamConnectionNotifier)Connector.open( this.socketPath );
			
		}
        catch (IOException e1)
        {
        	log("Unable to start Bluetooth Service.");
        	 
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}

        
        
        log("Ready to accept connections.");
        log("");
        
        while(true)
        {
        	
        	try
        	{
				BluetoothClientHandler NewClient = new BluetoothClientHandler( this.connectionNotifier.acceptAndOpen() );
			}
        	catch (IOException e)
        	{
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
        	catch (Exception e)
        	{
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
        	
        	
        }
    	
    }
    
    
	/**
	* -------------------------------------------------------------
	* registerObserver(InputObserver newObserver)
	* -------------------------------------------------------------
	* 
	* Register a new observer
	* 
	*/
	
	public void registerObserver(InputObserver newObserver)
	{
	
		this.observers.add(newObserver);		
		  
	}
    
    
    
    /**
     * ----------------------------------------------------
     * log(String message)
     * ----------------------------------------------------
     *
     * Send log message to console.
     *
     *
     * @return void
     *
     * */

    public void log(String message)
    {
    	System.out.println(message);
    }
    
    
    
    // Private inner Classes [START] ======================================== //
    // ====================================================================== //

    

    // Bluetooth Client Handler [START] ===================================== //
    
    
    /**
    *
    * BluetoothClientHandler
    *
    * Handles remote clients in individual threads.
    *
    */
    
    
    public class BluetoothClientHandler extends Thread
    {

        
    	/* Public Variables ================================================== */
    	
    	
    	/* Client related */
    	
        private RemoteDevice 		client;
        private String 				clientAddress;
        private String 				clientName;
        
        
        /* Communication channel */
        
        private InputHandler        			inputHandler;
        private OutputHandler       			outputHandler;
        private ConcurrentLinkedQueue<String>   outboundMessages;
        private ConcurrentLinkedQueue<String>   inboundMessages;
        private boolean                         channelOpen;
        private StreamConnection 				connection;
        
        
        
        /* Private Variables ================================================== */
    	
        
        
        /**
    	* -------------------------------------------------------------
    	* BluetoothClientHandler(StreamConnection newConnection)
    	* -------------------------------------------------------------
    	* 
    	* Set new thread to handle client requests.
    	* 
    	*/
    	
        BluetoothClientHandler(StreamConnection newConnection) throws Exception
    	{


    		this.connection 	= newConnection;
    		this.client 		= RemoteDevice.getRemoteDevice(newConnection);
    		this.clientAddress 	= this.client.getBluetoothAddress();
    		this.clientName		= this.client.getFriendlyName(true);
    		this.channelOpen 	= true;
    		
    		log(">> New Client: [" + this.clientName + "] (" + this.clientAddress + ")");
    		
            this.start();
            
    	}
    	    


    	/**
    	* -------------------------------------------------------------
    	* run()
    	* -------------------------------------------------------------
    	* 
    	* Process requests from client.
    	* 
    	*/
    	
    	public void run()
    	{
    	
    		this.inboundMessages 		= new ConcurrentLinkedQueue<String>();
    		this.outboundMessages 		= new ConcurrentLinkedQueue<String>();
    	
            
    		InputStream inputStream		= null;
    		OutputStream outputStream 	= null;	
    		
			try
			{
				
				inputStream 		= this.connection.openInputStream();
				outputStream 		= this.connection.openOutputStream(); 	
				
	    		this.inputHandler 	= new InputHandler( inputStream );
	    		this.outputHandler 	= new OutputHandler( outputStream , this.outboundMessages);
	    		
			}
			catch (IOException e1)
			{
				log("Unable to open communcation streams.");
				
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
    		

			// Send connection established message //
			
    		String connectionEstablished = new String("{type:\"connection_established\", data:\"\"}\n");
    		this.outboundMessages.add(connectionEstablished);
    		
    		
    		while(this.channelOpen)
    		{
    			// LOL
    		}
    		
    		log("");
    		log(">> Client Disconnected: [" + this.clientName + "]");
    		log("");
    		
    		try
    		{
    			this.connection.close();
    		}
    		catch (IOException e)
    		{
    			// Nevermind... it's dead.
    		}
    		
    	}
    	
    	    	
    	
    	/**
    	* -------------------------------------------------------------
    	* Disconnect()
    	* -------------------------------------------------------------
    	* 
    	* Disconnect client from server.
    	* 
    	*/
    	
    	public void disconnect()
    	{
    	
    		this.channelOpen = false;
    		  
    	}
    	
    	
    	
		    // Begin child classes [START] -------------------------------------------------------------------------------------- //
		    // ------------------------------------------------------------------------------------------------------------------ //
    	
    	
    	
	        // Input Handler [START] ================================================ //
	
	        /**
	         * -------------------------------------------------------------
	         * 	InputHandler
	         * -------------------------------------------------------------
	         *
	         * Process incoming messages from client.
	         *
	         */
	
	        protected class InputHandler extends Thread
	        {
	
	            private InputStream                     inputStream;
	            private BufferedReader                  inputReader;
	            private ConcurrentLinkedQueue<String>   inboundMessages;
	            private boolean                         channelOpen;
	
	
	            /**
	             * ----------------------------------------------------
	             * InputHandler(InputStream inputStream, ConcurrentLinkedQueue<String> messagesQueue)
	             * ----------------------------------------------------
	             *
	             * Returns instance of the class and initialize methods.
	             *
	             * @return  InputHandler
	             *
	             */
	
	            InputHandler(InputStream stream)
	            {
	
	                this.inputStream        = stream;
	                this.inputReader        = new BufferedReader(new InputStreamReader(this.inputStream));
	                this.channelOpen        = true;
	                
	                this.start();
	
	            }
	
	
	            public void run()
	            {
	
	                while(this.channelOpen)
	                {
	                    try
	                    {
	                        
	                    	String inboundMessage;
	                    	
	                    	while((inboundMessage = this.inputReader.readLine()) != null)
	                    	{
	
	                            if(inboundMessage !="")
	                            {
	                            	
	                            	boolean delivered  = false;
	                    			JSONObject message = new JSONObject(inboundMessage);
	                    			String messageType = (String)message.get("type");
	                    				                    			
	                    			
	                        		for(InputObserver observer : observers)
	                        		{
	                        			
	                        			for(String targuet : observer.getTarguet())
	                        			{
	                        			
		                        			if(messageType.equals(targuet))
		                        			{
		                        			
		                        				JSONObject data = message.getJSONObject("data");
		                        				observer.processMessage(data);
		                        				delivered 		= true;
		                        				
		                        				break;
		                        					
		                        			}
	                        				
	                        			}
	                        			
	                        			// Asume single observer per package type
	                        			if(delivered)
	                        			{
	                        				break;
	                        			}

	                        		}	
	                        		
	                    			
	                            }
	                    	}
		
	                    }
	                    catch(Exception e)
	                    {
	                        disconnect();
	                        this.channelOpen = false;
	                    }
	
	                }
	
	            }
	
	        }
	
	        // Input Handler [END] ================================================== //
    		
    		
    		
	        // Output Handler [START] =============================================== //

	        /**
	         * -------------------------------------------------------------
	         * 	OutputHandler
	         * -------------------------------------------------------------
	         *
	         * Process outgoing messages to client.
	         *
	         */

	        protected class OutputHandler extends Thread
	        {

	            private OutputStream                    outputStream;
	            private BufferedWriter                  outputWriter;
	            private ConcurrentLinkedQueue<String>   outboundMessages;
	            private boolean                         channelOpen;


	            /**
	             * ----------------------------------------------------
	             * OutputHandler(OutputStream stream, ConcurrentLinkedQueue<String> messagesQueue)
	             * ----------------------------------------------------
	             *
	             * Returns instance of the class and initialize methods.
	             *
	             * @return  OutputHandler
	             *
	             */

	            OutputHandler(OutputStream stream, ConcurrentLinkedQueue<String> messagesQueue)
	            {

	                this.outputStream 		= stream;
	                this.outputWriter       = new BufferedWriter(new OutputStreamWriter(this.outputStream));
	                this.channelOpen        = true;
	                this.outboundMessages   = messagesQueue;

	                this.start();

	            }


	            public void run()
	            {

	                while(this.channelOpen)
	                {

	                    String outboundMessage = this.outboundMessages.poll();

	                    if(outboundMessage!=null)
	                    {
	                        try
	                        {
	                            log("*** SENDING OUTBOUND MESSAGE ***");


	                            this.outputWriter.write(outboundMessage);
	                            this.outputWriter.flush();

	                            outboundMessage = null;

	                        }
	                        catch (IOException e)
	                        {
	                            disconnect();
	                            this.channelOpen    = false;
	                        }
	                    }

	                }

	            }

	        }

	        // Output Handler [END] ================================================= //
	        
	        
		    // Begin child classes [END] ---------------------------------------------------------------------------------------- //
		    // ------------------------------------------------------------------------------------------------------------------ //
	        
    	
    }


    
    // Bluetooth Client Handler[END] ======================================== //
    
    
    
	
}
