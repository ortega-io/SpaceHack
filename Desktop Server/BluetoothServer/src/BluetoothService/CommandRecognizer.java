package BluetoothService;

import java.awt.AWTException;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.Toolkit;
import java.awt.event.InputEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.URI;
import java.net.URISyntaxException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;

public class CommandRecognizer
{
	public static String FILE_NAME 			= "binnacle.log";
	public static final int STEP 			= 50;
	public static final int STEP_SCROLL 	= 20;
	public static int xCenter;
	public static int yCenter;
	private static Robot robot 				= null;
	private static String q;
	
	public CommandRecognizer()
	{
		
		// center coordinate
		
		GraphicsDevice gd 	= GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();
		xCenter 			= gd.getDisplayMode().getWidth() / 2;
		yCenter  			= gd.getDisplayMode().getHeight() / 2;
		
		try
		{
			robot = new Robot();
		}
		catch (AWTException e)
		{
			e.printStackTrace();
		}
	}
	
	public void commandAction(String command)
	{
		
		String temporalComm = command.toLowerCase();
		String[] words 		= temporalComm.split(" ");
		
		if(words[0].equals("google"))
		{
			command = words[0];
			
			q = temporalComm.replace("google", "");
            q = q.replace(" ", "+");
		}
		else if(words[0].equals("record"))
		{
			command = words[0];
			
			q = temporalComm.replace("record", "");
			
		}
		
		
		switch (command) 
		{
			case "center":
				robot.mouseMove(xCenter, yCenter);
				
			case "right":
				robot.mouseMove(xCenter + STEP, yCenter);
			break;
			
			case "left":
				robot.mouseMove(xCenter - STEP, yCenter);
			break;
			
			case "up":
				robot.mouseMove(xCenter, yCenter - STEP);
			break;
			
			case "down":
				robot.mouseMove(xCenter, yCenter + STEP);
			break;
			
			case "right click":
				robot.mousePress(InputEvent.BUTTON3_MASK);
			break;
			
			case "click":
				robot.mousePress(InputEvent.BUTTON1_MASK);
			break;
			
			case "in": 
				robot.mouseWheel(-STEP_SCROLL);
			break;
			
			case "out": 
				robot.mouseWheel(STEP_SCROLL);
			break;
			
			case "go":
				robot.mousePress(InputEvent.BUTTON1_MASK);
				robot.mouseMove(xCenter - STEP, yCenter);
				robot.mouseRelease(InputEvent.BUTTON1_MASK);
			break;
			
			case "back":
				robot.mousePress(InputEvent.BUTTON1_MASK);
				robot.mouseMove(xCenter + STEP, yCenter);
				robot.mouseRelease(InputEvent.BUTTON1_MASK);
			break;
			
			case "google":
				findGoogle(q);
			break;
			
			case "record":
				logBookAction(q);
			break;
			
			case "facebook":
				findFacebook();
			break;
			
			case "screenshot":
				getScreenshot();
			break;
			
		};
		
		
		
	}
	
	
	
	public void logBookAction(String text)
	{
		

		// call the logBook method to save text
		writeLogBook(text + "\n", "record" +".txt");
		
	}
	
	private static void writeLogBook(String msg, String fileName)
	{
        
        File mFile	= new File(fileName);

        try 
        {
            if(!mFile.exists())
            {
            	mFile.createNewFile();
            }
            
            
            DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
            Date date = new Date();
            
            FileOutputStream fOut 			= new FileOutputStream(mFile, true);
            OutputStreamWriter myOutWriter 	= new OutputStreamWriter(fOut);

            myOutWriter.append(dateFormat.format(date)+": "+msg);
            myOutWriter.close();

            fOut.close();
        }
        catch
        (IOException e) 
        {
            e.printStackTrace();
        }
    }
	
	
	private void getScreenshot()
	{
		
		final JFrame showPictureFrame = new JFrame("================ Screenshot ==============");
	    // we will put the picture into this label
	    JLabel pictureLabel = new JLabel();
	    
	    Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
	    Rectangle screenRectangle = new Rectangle(screenSize);
	    Robot robot = null;
		try {
			robot = new Robot();
		} catch (AWTException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		   BufferedImage image = robot.createScreenCapture(screenRectangle);
		   try {
			ImageIO.write(image, "png", new File("Screenshot.png"));
			 BufferedImage imm = ImageIO.read(new File("Screenshot.png"));
			 pictureLabel.setIcon(new ImageIcon(imm));
			    // add the label to the frame
			    showPictureFrame.add(pictureLabel);
			    // pack everything (does many stuff. e.g. resizes the frame to fit the image)
			    showPictureFrame.pack();

			    //this is how you should open a new Frame or Dialog, but only using showPictureFrame.setVisible(true); would also work.
			    java.awt.EventQueue.invokeLater(new Runnable() {

			      public void run() {
			        showPictureFrame.setVisible(true);
			      }
			    });

		 } catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		 }
	}
	
	
	private void findGoogle(String word){
		try {
			Desktop.getDesktop().browse(new URI("https://www.google.com/search?q=google&ie=utf-8&oe=utf-8#q=" + word));
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (URISyntaxException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	private void findFacebook(){
		try {
			Desktop.getDesktop().browse(new URI("https://www.facebook.com"));
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (URISyntaxException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	
}
