
import java.io.File;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.Timer;


public class Tron {
	public static final File DEFAULT_GAME=new File("./grids/default-tron-3P.tron");

	
	static public void main(String[] args) throws IOException {
		File gameFile=null;
		if(args.length==0) {
			gameFile= DEFAULT_GAME;
		} else {
			gameFile= new File(args[0]);
		}
		
		Grid g= Utils.loadGrid(gameFile);
		
		Timer t= new Timer();
		Watchdog dog;
		
		Move next= Move.get();
		while(next!=null && g.remainingPlayers>1) {
			dog= Watchdog.getInfinite();
//			dog= Watchdog.createDefault();
			t.schedule(dog, 100);
			next= IA.nextMove(next, g, g.nextPlayer(), 3, dog);
			dog.cancel();
			if(next!=null) {
				System.out.println("Moving "+next);
				next.move(g);
				Utils.dump(g);
			} else {
				System.out.println("No moves");
			}
		}
		t.cancel();
		System.out.println("Done: Winner P"+g.nextPlayer()+" "+(g.alive[g.nextPlayer()]?"ALIVE":"DEAD"));
		Utils.dump(g);
		
	}
	
}
