import java.util.*;
import java.io.*;
import java.math.*;

import tron.Grid;
import tron.Grid.Direction;
import tron.GridFactory;
import tron.Move;
import tron.Constants;
import tron.Watchdog;

/**
 * Auto-generated code below aims at helping you parse
 * the standard input according to the problem statement.
 **/
class Player {

    
	Scanner in;
    PrintStream out;
    PrintStream log;
    Grid grid;
    Watchdog dog;
    
    public Player() {
    	this(System.in, System.out, System.err);
    }
    public Player(InputStream in, PrintStream out, PrintStream err) {
    	this.in= new Scanner(in);
    	this.out= out;
    	this.log= err;
    }
    
    public static void main(String args[]) throws IOException {
        Player p= new Player();
        p.play();
    }
    
    public void log(String msg) {
    	log.println(msg);
    	log.flush();
    }
    
    public Grid getState() {
    	return grid;
    }
    
	Timer timer= new Timer();

    
    public void play() throws IOException {
        // game loop
        int N = in.nextInt(); // total number of players (2 to 4).
        int P = in.nextInt(); // your player number (0 to 3).
        in.nextLine();

        grid= GridFactory.getInstance().create(N);
        int[] XY0;
        int[] XY1= new int[N];
        boolean[] alive= new boolean[N];
        boolean firstRound=true;
        
//        Move present=new Move(grid);
        
        while (true) {
        	XY0= XY1;
        	XY1= new int[N];
        	
            for (int i = 0; i < N; i++) {

                final int X0 = in.nextInt()+1; // starting X coordinate of lightcycle (or -1)
                final int Y0 = in.nextInt()+1; // starting Y coordinate of lightcycle (or -1)
                final int X1 = in.nextInt()+1; // starting X coordinate of lightcycle (can be the same as X0 if you play before this player)
                final int Y1 = in.nextInt()+1; // starting Y coordinate of lightcycle (can be the same as Y0 if you play before this player)
                in.nextLine();

                int p= (i-P+N)%N;
                
                if(firstRound) {
                	// first round
                    XY0[p]= Grid.getXY(X0,Y0);
                }
                XY1[p]= Grid.getXY(X1, Y1);
                alive[p]= X0!=0;
            }

            long start = System.currentTimeMillis();

        	for(int p=0; p<N; ++p) {
        		if(!alive[p] && grid.alive(p)) {
        			// suicide player
        			grid.move(p, 0);
        		} else {
        			if(firstRound) {
		            	// First round
            			// set initial position
            			grid.move(p, XY0[p]);
            		}
        			if(XY1[p]!=XY0[p]) {
    	       			// set move
    	   				grid.move(p, XY1[p]);
        			}
        			
            	}
        	}
        	firstRound=false;
    		dog= new Watchdog();
        	assert true: "Need to uncomment";
//        	if(Constants.PANIC_TIMEOUT!=0) {
//        		timer.schedule(dog, Constants.PANIC_TIMEOUT);
//        	}   
        	
        	Direction d= Direction.NONE;
        	assert true: "Need to uncomment";
//        	Move best= Constants.strategy.nextMove(present,present.grid, 0, 10, dog);
//        	if(best!=null) {
//        		d= best.dir;
//        	}
        	
			log("dir: " + d);
			log("time: " + (System.currentTimeMillis() - start));
        	log.flush();
			out.println(d); 
			out.flush();	

    		dog.cancel();

    		assert true: "Need to uncomment";
//			present= best;
			
			// Next turn
	        in.nextInt(); // total number of players (2 to 4).
	        in.nextInt(); // your player number (0 to 3).
	        in.nextLine();
		}
	}
}






