package tron;
import static tron.Constants.*;

import java.io.File;
import java.io.IOException;
import java.util.Timer;


public class Tron {

	
	static public void main(String[] args) throws IOException {
		File gameFile=null;
		if(args.length==0) {
			gameFile= DEFAULT_GAME;
		} else {
			gameFile= new File(args[0]);
		}
		
		
		Grid g= Utils.loadGrid(gameFile);
		
		Strategy strategy= new TronStrategy();
		Timer t= new Timer();
		Watchdog dog;
		
		Move next= new Move();
		while(next!=null && g.remainingPlayers>1) {
			dog= Watchdog.createInfinite();
//			dog= Watchdog.createDefault();
			t.schedule(dog, 100);
			next= strategy.nextMove(next, g, g.nextPlayer(), 3, dog);
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
		System.out.println("Done: Winner P"+g.nextPlayer()+" "+(g.alive(g.nextPlayer())?"ALIVE":"DEAD"));
		Utils.dump(g);
		
	}
	
}