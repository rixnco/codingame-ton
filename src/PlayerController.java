import java.io.IOException;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.io.PrintStream;
import java.util.Scanner;


public class PlayerController implements Runnable {
	private Player player;
	private PrintStream toPlayer;
	private Scanner fromPlayer;
	private Scanner log;
	
	
	PlayerController() throws IOException {
		PipedInputStream fromController= new PipedInputStream(2048);
		PipedInputStream toController= new PipedInputStream(2048);
		PipedInputStream toLog= new PipedInputStream(2048);
		
		toPlayer= new PrintStream(new PipedOutputStream(fromController));
		fromPlayer= new Scanner(toController);
		log= new Scanner(toLog);
		
		player= new Player(
				fromController, 
				new PrintStream(new PipedOutputStream(toController)),
				new PrintStream(new PipedOutputStream(toLog)));
		
	}
	
	public void send(String line) {
		toPlayer.println(line);
		toPlayer.flush();
	}
	
	public String receive() {
		return fromPlayer.nextLine();
	}
	
	public String getLog() {
		StringBuffer bf= new StringBuffer();
		while(log.hasNextLine()) {
			String line= log.nextLine();
			bf.append(line).append("\n");
			if(line.equals("READY")) break;
		}
		return bf.toString();
	}
	
	public void close() {
		toPlayer.close();
		fromPlayer.close();
		log.close();
	}
	
	public Grid getGrid() {
		return player.grid;
	}
	
	public Move getPresent() {
		return player.present;
	}
	
	@Override
	public void run() {
		try {
			player.play();
		} catch(Throwable th) {
		}
	}
	
}