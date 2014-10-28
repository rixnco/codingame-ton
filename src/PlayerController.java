import java.io.IOException;
import java.io.InputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.io.PrintStream;
import java.util.NoSuchElementException;
import java.util.Scanner;

import tron.Grid;


public class PlayerController implements Runnable {
	Player player;
	
	PrintStream toPlayer;
	Scanner fromPlayer;
	Scanner debugPlayer;

	PrintStream playerOut;
	PrintStream playerDebug; 

	
	String 		 name;
	Thread       thread;
	Thread       logsGobbler;
	StringBuffer logs= new StringBuffer();
	
	public PlayerController(String name) throws IOException {
		this.name= name;
	}

	public void run() {
		try {
			player.play();
		} catch(NoSuchElementException nse) {
		} catch(Throwable th) {
			playerOut.close();
			playerDebug.close();
			th.printStackTrace(System.out);
		}
		player=null;
		System.out.println(name+" stopped");
	}

	public void send(String string) {
		toPlayer.println(string);
		toPlayer.flush();
	}

	public String response() {
		return fromPlayer.nextLine();
	}

	public synchronized void start() throws IOException {
		if(thread!=null) return;
		
		PipedInputStream fromController= new PipedInputStream(2048);
		PipedInputStream toController= new PipedInputStream(2048);
		PipedInputStream toDebug= new PipedInputStream(2048);
		
		
		toPlayer= new PrintStream(new PipedOutputStream(fromController));
		fromPlayer= new Scanner(toController);
		debugPlayer= new Scanner(toDebug);
		
		playerOut= new PrintStream(new PipedOutputStream(toController));
		playerDebug= new PrintStream(new PipedOutputStream(toDebug)); 
		
		player= new Player(
				fromController, 
				playerOut, 
				playerDebug);
		
		logs.setLength(0);
		logsGobbler= new Thread(name+"-logs") {
			public void run() {
				try {
					while(!interrupted()) {
						String l= debugPlayer.nextLine();
						synchronized(logs) {
							logs.append(l);
						}
					}
				} catch(Throwable th) {
				}
			}
		};
		
		logsGobbler.start();
		
		
		thread= new Thread(this, name);
		thread.start();
	}
	
	public synchronized void stop() {
		
		if(thread!=null) thread.interrupt();
		if(logsGobbler!=null) logsGobbler.interrupt();

		toPlayer.close();
		fromPlayer.close();
		debugPlayer.close();
		
		thread=null;
		logsGobbler= null;
		synchronized(logs) {
			logs.setLength(0);
		}
		
	}
	
	public String getLogs() {
		synchronized(logs) {
			String res= logs.toString();
			logs.setLength(0);
			return res;
		}
	}
	
	public Grid getState() {
		return (player==null?null:player.getState());
	}
	
}
