import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


public class TronEngine {
	public static final File DEFAULT_GAME=new File("./grids/default-tron-2P.tron");

	
	static ExecutorService executor= Executors.newFixedThreadPool(4);
	
	
	static public void main(String[] args) throws IOException {
		
		Grid g= Utils.loadGrid(DEFAULT_GAME);
		int nbPlayers= g.nbPlayers;
		
		List<PlayerController> players= new ArrayList<>(g.nbPlayers);
		
		int[] xy0= new int[nbPlayers];
		
		for(int p= 0; p<nbPlayers; ++p) {
			xy0[p]= g.cycles.get(p).getFirst();
			PlayerController player= new PlayerController();
			players.add(player);
			executor.submit(player);
		}
		
		while(g.remainingPlayers>1) {
			int p= g.nextPlayer();
			PlayerController player= players.get(p);

			player.send(""+nbPlayers+" "+p); // send N P
			
			for(int t=0; t<nbPlayers; ++t) {
				int x0,y0,x1,y1;
				
				if(!g.alive[t]) {
					x0=y0=x1=y1=-1;
				} else {
					x0= Grid.getX(xy0[t])-1;
					y0= Grid.getY(xy0[t])-1;
					x1= Grid.getX(g.head[t])-1;
					y1= Grid.getY(g.head[t])-1;
				}
				player.send(x0+" "+y0+" "+x1+" "+y1); //
			}

			String response= player.receive();
			
			String log= player.getLog();
			System.out.print(log);
			
			Direction d= Direction.parse(response);
			System.out.println("===> P"+p+" "+d+" ("+player.getPresent().strategy+")");
			g.move(p, d);
		}
		
		System.out.println("DONE");
		
		executor.shutdownNow();
		
	}
	
	
}
