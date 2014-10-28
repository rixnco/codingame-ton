package tron;

import java.util.Collections;

import tron.Grid.Direction;
import tron.Watchdog.Timeout;

public class SurvivalStrategy implements Strategy {
	private Watchdog watchdog;
	public int maxitr;

	public String toString() {
		return "Survival";
	}
	
	@Override
	public Move nextMove(final Move present, final Grid g, final int forPlayer, final int maxDepth, final Watchdog dog) {
		if(watchdog==null) this.watchdog= new Watchdog();

		Grid grid= g.copy(); // TODO Replace with static grid copy
		int itr;
		int xy= grid.head(forPlayer);
		int area= grid.fillableAreaAt(xy);
		
		if(Constants.DEBUG) {
			System.out.println("Survial search P"+forPlayer+" ("+Grid.getX(xy)+","+Grid.getY(xy)+")");
			System.out.println("fillable Area: "+area);
		}
		
		// int bestv = 0, bestm = 1;
		Move best= null;
		try {
		for (itr= 1; itr<=maxDepth; itr++) {
			if(Constants.DEBUG) {
				System.out.println("Depth "+itr);
			}
			maxitr= itr;
			Move m= spacefill(present, grid, forPlayer, itr, dog);
			if (best==null || m.value>best.value) {
				best= m;
			}
			if (dog.panic) break;
			if(m==null) continue;
			if (m.value<=itr) {
				if(Constants.DEBUG) {
					System.out.println("What the fuck does that mean !!!: "+m.value+"<="+itr);
				}
				break; // we can't possibly search any deeper TODO ???
			}
			if (m.value>=area) {
				if(Constants.DEBUG) {
					System.out.println("We found the perfect match !!"+m.value+"="+area);
				}
				break; // solved!
			}
		}
		} catch(Timeout t) {
			if(Constants.DEBUG) {
				System.out.println("Timeout !!");
			}
		}
		if(true || Constants.DEBUG) {
			System.out.println("Survival mode P"+forPlayer+" depth="+maxitr+" Move "+(best==null?"???":best.dir[forPlayer]));
		}
		return best;
	}

	@Override
	public Integer evaluate(final Grid g, final int player) {
		try { 
		return floodfill(g, g.head(player), Watchdog.createInfinite());
		} catch(Timeout t) {
		}
		return null;
	}
	
	
	
	
	// returns spaces unused (wasted); idea is to minimize waste
	// Evaluate present's heuristic and returns best future move or null is no future
	static public Move spacefill(final Move present, final Grid g, final int player, final int itr, final Watchdog dog) throws Timeout {
		
		dog.check();
		int xy= g.head(player);
		assert(xy>=Grid.FIRST && xy<=Grid.LAST);
		try {
		if (g.degree(xy)==0) {
			present.eval= 0;
			present.value= 0;
			if(Constants.DEBUG) {
				GridUtils.dump(g);
				System.out.println("P"+player+" ("+Grid.getX(xy)+","+Grid.getY(xy)+")");
				System.out.println("--> no more space");
			}
			return null;
		}
		} catch(ArrayIndexOutOfBoundsException e) {
			System.out.println("Player: "+player);
			GridUtils.dump(g);
			GridUtils.dumpComponents(g);
			GridUtils.dumpTerritory(g);
			throw e;
		}
		
		if (itr==0) {
			// We're evaluating present
			if(present.eval==null) {
				present.eval= floodfill(g, xy, dog);
				if(Constants.DEBUG) {
					GridUtils.dump(g);
					System.out.println("P"+player+" ("+Grid.getX(xy)+","+Grid.getY(xy)+")");
					System.out.println("-->floodfill="+present.eval);
				}
			}
			present.value=present.eval;
			return null;
		}

		
		
		if (present.future.size()==0) {
			// present has never been evaluated
			// populate future
			for (Direction d : Direction.ALL) {
				if(!g.empty(xy, d)) continue;
				Move m= new Move(player, d);
				present.addFuture(m);
			}
		} else {
			// present has already been evaluated
			// sort best evaluations first
			Collections.sort(present.future, Move.BEST_FIRST);
		}

		int spacesleft= g.getPlayerFillableArea(player);
		Move bestf= null;
		for (Move f : present.future) {
			try {
				f.move(g);
				spacefill(f, g, player, itr-1, dog);
			} finally {
				f.unmove(g);
			}
			if (bestf==null || f.value>bestf.value) {
				bestf= f;
			}
			if (f.value==spacesleft) break; // we solved it!
			dog.check();
		}
		present.value= (bestf==null?0:bestf.value);
		return bestf;
	}

	static public int floodfill(Grid g, int xy, Watchdog dog) throws Timeout {
		// flood fill heuristic: choose to remove as few edges from the graph as
		// possible (in other words, move onto the square with the lowest degree)
		dog.check();
		
		int bestv= 0;
		int b= xy;
		for (Direction d : Direction.ALL) {
			int xyd= d.move(xy);
			if (g.grid[xyd]!=0) continue;
			int v= g.connectedValueAt(xyd)+g.fillableAreaAt(xyd)-2*g.degree(xyd)-4*g.isArticulation(xyd);
			if (v>bestv) {
				bestv= v;
				b= xyd;
			}
		}
		if (bestv==0) return 0;
		int a;
		boolean recalc=false;
		try {
			g.grid[b]= 1;
			g.removeFromComponents(b, true);
			a= 1+floodfill(g, b, dog);
			recalc=true;
		} finally {
			g.grid[b]= 0;
			g.addToComponents(b, recalc);
		}
		return a;
	}

	
}
