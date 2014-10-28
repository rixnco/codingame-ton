import static tron.Constants.*;

import java.io.File;
import java.io.IOException;
import java.util.Timer;

import tron.Grid;
import tron.GridUtils;
import tron.Move;
import tron.Strategy;
import tron.TerritoryStrategy;
import tron.TronStrategy;
import tron.Watchdog;


public class Tron {

	
	static public void main(String[] args) throws IOException {
		File gameFile=null;
		if(args.length==0) {
			gameFile= DEFAULT_GAME;
		} else {
			gameFile= new File(args[0]);
		}
		
		
		Grid g= GridUtils.load(gameFile);
		
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
				GridUtils.dump(g);
			} else {
				System.out.println("No moves");
			}
		}
		t.cancel();
		System.out.println("Done: Winner P"+g.nextPlayer()+" "+(g.alive(g.nextPlayer())?"ALIVE":"DEAD"));
		GridUtils.dump(g);
		
	}
	
}
