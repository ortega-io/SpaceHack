package BluetoothService;

import java.awt.AWTException;
import java.awt.List;
import java.awt.Robot;
import java.awt.event.KeyEvent;

import org.json.JSONObject;

import in.spacehack.desktop.bluetooth.InputObserver;

/**
 * This class handles the moves of the accelerometer and searches patterns of moves.
 * The supported moves are the basics: LEFT, RIGHT, UP, DOWN, FRONT and REAR.
 * You can also add your personalized patterns to search for.
 * @author Mario Guillén
 *
 */
public class Gestures extends InputObserver {
	
	private List moves;
	private long lasttime;
	private int sumpattern;
	private Robot executer;
	
	private static final float XMIN		= 7.0f; //filtro para eliminar ruido en X
	private static final float YMIN		= 7.0f; //filtro para eliminar ruido en Y
	public static final float ZMIN		= 7.0f; //filtro para eliminar ruido en Z
	public static final long MINTIME	= 500; //filtro para eliminar rebote
	public static final long MAXTIME	= 2000; //máximo tiempo de espera para asociar movimientos
	
	public static final int PRIMELEFT	= 2; //Número primo asignado a left
	public static final int PRIMERIGHT	= 3; //Número primo asignado a right
	public static final int PRIMEUP		= 5; //Número primo asignado a up
	public static final int PRIMEDOWN	= 7; //Número primo asignado a down
	public static final int PRIMEFRONT	= 11; //Número primo asignado a front
	public static final int PRIMEREAR	= 13; //Número primo asignado a rear
	
	/**
	 * Constructor
	 */
	public Gestures()
	{
		this.targuet.add("coordinates");
		
		this.moves		= new List();
		this.lasttime	= 0;
		this.sumpattern	= 0;
		
		try {
			this.executer   = new Robot();
		} catch (AWTException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	
	/**
	 * Override function that handles with the message received and also gets the
	 * magnitude of acceleration for X, Y an Z axis.
	 */
	@Override
	public void processMessage(JSONObject message)
	{

		//System.out.println(message.get("accelerometer").toString());
		
		
		JSONObject accelerometer = new JSONObject(message.get("accelerometer").toString());
		
		float x 	= Float.parseFloat(accelerometer.get("x").toString());
		float y 	= Float.parseFloat(accelerometer.get("y").toString());
		float z 	= Float.parseFloat(accelerometer.get("z").toString());
		long time 	= System.currentTimeMillis();
		 
		String currentmove	= "";
		int currentprime	= 0;
		
		//borrando ruido
		if( Math.abs(x) >= XMIN)
		{
			//borrando rebote
			if(time - this.lasttime >= MINTIME)
			{
				//right
				if(x > 0)
				{
					currentmove  = "RIGHT";
					currentprime = PRIMERIGHT;
				}
				//left
				else if(x < 0)
				{
					currentmove  = "LEFT";
					currentprime = PRIMELEFT;
				}
			}
		}
		//borrando ruido
		else if( Math.abs(y) >= YMIN)
		{
			//borrando rebote
			if(time - this.lasttime >= MINTIME)
			{
				//front
				if(y > 0)
				{
					currentmove = "FRONT";
					currentprime = PRIMEFRONT;
				}
				//rear
				else if(y < 0)
				{
					currentmove = "REAR";
					currentprime = PRIMEREAR;
				}
			}
		}
		
		//borrando ruido
		else if( Math.abs(z) >= ZMIN)
		{
			//borrando rebote
			if(time - this.lasttime >= MINTIME)
			{
				//up
				if(z > 0)
				{
					currentmove = "UP";
					currentprime = PRIMEUP;
				}
				//down
				else if(z < 0){
					currentmove = "DOWN";
					currentprime = PRIMEDOWN;
				}
			}
		}
		
		//agregando movimiento
		if(currentprime > 0)
		{
			//si se detecta inactividad, se desasocian los movimientos previos
			if(time - this.lasttime >= MAXTIME)
			{
				this.moves.removeAll();
				this.sumpattern = 0;
			}
			
			//asociando movimiento
			this.moves.add(currentmove);
			
			//suma al patrón el orden del movimiento * el primo del movimiento
			this.sumpattern += this.moves.getItemCount() * currentprime;
			this.lasttime = time;
			
			//buscar patrón
			//this.searchPattern();
			
			System.out.println(currentmove);
			this.tempMove(currentmove);
		}
		
		
	}
	
	
	private void tempMove(String move)
	{
		switch(move)
		{
		
			case "LEFT":
				
				this.executer.keyPress(KeyEvent.VK_LEFT);
				this.executer.delay(100);
				this.executer.keyRelease(KeyEvent.VK_LEFT);
				
			break;
				
		
			case "RIGHT":
				
				this.executer.keyPress(KeyEvent.VK_RIGHT);
				this.executer.delay(100);
				this.executer.keyRelease(KeyEvent.VK_RIGHT);
				
			break;
			
		}
	}
	
	/**
	 * Search a pattern and sets the patterns to search for.
	 * @author Mario Guillén
	 */
	private void searchPattern(){
		/*
		 * Para agregar patrones, solo basta que el case lleve la suma de los primos
		 * multiplicados por el orden de aparición del movimiento 
		 */
		
		switch(this.sumpattern){
			//RIGHT -> LEFT
			case 1*PRIMERIGHT + 2*PRIMELEFT:
				//hacer algo
				System.out.println("Pattern: " + this.getMovesList());
			break;
			
			//UP -> DOWN
			case 1*PRIMEUP + 2*PRIMEDOWN:
				//hacer algo
				System.out.println("Pattern: " + this.getMovesList());
			break;
			
			//FRONT -> REAR
			case 1*PRIMEFRONT + 2*PRIMEREAR:
				//hacer algo
				System.out.println("Pattern: " + this.getMovesList());
			break;
			
			//LEFT -> RIGHT -> UP -> DOWN
			case 1*PRIMELEFT + 2*PRIMERIGHT + 3*PRIMEUP + 4*PRIMEDOWN:
				//hacer algo
				System.out.println("Pattern: " + this.getMovesList());
			break;
			
			//RIGHT -> RIGHT -> UP -> UP
			case 1*PRIMERIGHT + 2*PRIMERIGHT + 3*PRIMEUP + 4*PRIMEUP:
				//hacer algo
				System.out.println("Pattern: " + this.getMovesList());
			break;
			
			//FRONT -> REAR -> UP -> LEFT
			case 1*PRIMEFRONT + 2*PRIMEREAR + 3*PRIMEUP + 4*PRIMELEFT:
				//hacer algo
				System.out.println("Pattern: " + this.getMovesList());
			break;
		}
	}
	
	/**
	 * Gets a formatted patter of the moves.
	 * @return String
	 */
	private String getMovesList(){
		String movesList = "";
		for(int i = 0; i < this.moves.getItemCount() ; i++){
			if(i > 0){
				movesList += " -> " + this.moves.getItem(i);
			}
			else{
				movesList = this.moves.getItem(i);
			}
		}
		return movesList;
	}
	
}
