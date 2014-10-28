import java.io.FileInputStream;
import java.io.PrintStream;
import java.util.NoSuchElementException;
import java.util.Random;

import tron.Constants;
import tron.Grid;
import tron.GridFactory;
import tron.Grid.Direction;
import tron.GridFactory.TemplateFactory;


public class GameEngine implements Runnable {

	static final PrintStream out= System.out;
	
	static final Random random= new Random();

	
	PlayerController[] players;  
	
	Thread executor;
	private int nbPlayers;
	Grid state;
	private GameListener controller;
	
	public GameEngine(int nbPlayers) {
		this.nbPlayers= nbPlayers;
	}
	
	public void start() {
		if(executor!=null) throw new IllegalStateException("Already playing...");
		
		executor= new Thread(this, "BoardController");
		executor.start();
	}
	
	public void run() {	
		try {
			GridFactory.setDefault(new TemplateFactory(new FileInputStream("grids/test1.txt")));
			
			state= GridFactory.getInstance().create(nbPlayers);
	
			int[] X0= new int[nbPlayers];
			int[] Y0= new int[nbPlayers];
			
			players= new PlayerController[nbPlayers];
			for(int p=0; p<nbPlayers; ++p) {
				players[p]= new PlayerController("Player-"+p);
				int xy;
				do {
					X0[p]= random.nextInt(30);
					Y0[p]= random.nextInt(20);
					
					xy= Grid.getXY(X0[p]+1,Y0[p]+1);
				} while(!state.empty(xy));
				
				System.out.println("Player-"+p+" X0="+X0[p]+" Y0="+Y0[p]);
				state.move(p, xy);
			}
			
			if(controller!=null) controller.gameInitialized(state);
			
			int remaining= nbPlayers;
			for(PlayerController p:players) p.start();
			
			long start;
			while(!Thread.currentThread().isInterrupted() && remaining>1) {
				System.out.println("next round ...");
				for(int p= 0; p<nbPlayers && remaining>1; ++p) {
					if(!state.alive(p)) continue;
					start= System.currentTimeMillis();
					out.println("player's "+p+" turn");
					players[p].send(nbPlayers+" "+p);
					out.println(nbPlayers+" "+p);
					for(int t=0; t<nbPlayers; ++t) {
						int x0,y0;
						int x1,y1;
						if(state.alive(t)) {
							x0= X0[t];
							y0= Y0[t];
							int xy= state.head(t);
							x1= Grid.getX(xy) -1;
							y1= Grid.getY(xy) -1;
						} else {
							x0=x1=y0=y1=-1;
						}
							players[p].send(x0+" "+y0+" "+x1+" "+y1);
							out.println(x0+" "+y0+" "+x1+" "+y1);
					}
					
					String line= players[p].response();
					out.println("move: "+line);
					Direction d= Direction.parse(line);
					state.move(p, d);
				
					if(!state.alive(p)) {
						--remaining;
					}
					out.println("player's "+p+" turn DONE - "+(System.currentTimeMillis()-start));
					
					if(controller!=null) controller.playerMoved(state, p);
				}
			}
			
			int winner=-1;
			for(int p=0; p<nbPlayers; ++p) {
				players[p].stop();
				if(state.alive(p)) {
					winner=p;
				}
			}
			out.println("Winner: Player"+winner);

			
			
		} catch(NoSuchElementException nse) {
		} catch(Throwable th) {
			th.printStackTrace();
		}
		out.println("Game Over");
		if(controller!=null) controller.gameTerminated(state);
		synchronized(executor) {
			executor.notifyAll();
		}
		executor=null;
	}

	public void setListener(GameListener listener) {
		controller= listener;
	}

	public void stop() {
		if(executor!=null) {
			synchronized(executor) {
				executor.interrupt();
				try {
					executor.wait();
				} catch (InterruptedException e) {
				}
			}
			
		}
	}
	
	
}

