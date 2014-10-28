package tron;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import tron.Grid.Color;
import tron.Grid.Direction;
import tron.Watchdog.Timeout;

public class TronStrategy implements Strategy {

	static enum Mode {
		NONE, SURVIVAL, FIGHT;
	}
	
	static final int DEPTH_INITIAL= 1;
	static final int DEPTH_MAX= 100;

	static final long PANIC_TIMEOUT= 90;

	static final int DRAW_PENALTY= 0;


	int evaluations;
	int _ab_runs= 0;
	int maxitr= 0;
	Mode mode=Mode.NONE;
	
	
	SurvivalStrategy survivalStrategy= new SurvivalStrategy();
	TerritoryStrategy territoryStrategy= new TerritoryStrategy();
	
	public String toString() {
		return "Territory/Survival";
	}
	
	
	@Override
	public Move nextMove(final Move present, final Grid g, final int forPlayer, final int maxDepth, Watchdog dog) {
		if(dog==null) dog= new Watchdog();
		
		Grid grid= g.copy();
		grid.calculateComponents(true);
		int cp= grid.playerComponent(forPlayer);

		boolean survival=true;
		for (int p= grid.nextPlayer(forPlayer); p!=forPlayer && survival; p= grid.nextPlayer(p)) {
			int co= grid.playerComponent(p);
			survival= cp!=co;
		}
		Move m;
		if (survival) {
			m= survivalStrategy.nextMove(present, grid, forPlayer, maxDepth, dog);
		} else {
			m= territoryStrategy.nextMove(present, grid, forPlayer, maxDepth, dog);
		}

		return m;
	}

	@Override
	public Integer evaluate(final Grid g, final int player) {
		g.calculateComponents();

		int cp= g.playerComponent(player);

		boolean survival=true;
		for (int p= g.nextPlayer(player); p!=player&& survival; p= g.nextPlayer(p)) {
			int co= g.playerComponent(p);
			survival= cp!=co;
		}
		if (survival) {
			return survivalStrategy.evaluate(g, player);
		} else {
			return territoryStrategy.evaluate(g, player);
		}
			
	}
	
	
//	private Move nextMoveBattlefront(final Grid grid, final Components c, final int player, final List<Integer> opponents, Move present) {
//		int itr;
//		Move lastm= new Move(player, Direction.NONE, Integer.MIN_VALUE);
//		evaluations= 0;
//
//		for (itr= DEPTH_INITIAL; itr<DEPTH_MAX&&!panic; itr++) {
//			// _maxitr = itr*2;
//			Move m= _alphabeta(grid, player, opponents, Integer.MIN_VALUE, Integer.MAX_VALUE, itr*2, present);
//			if (m.value==Integer.MAX_VALUE) // our opponent cannot move, so we win
//				return m;
//			if (m.value==Integer.MIN_VALUE) {
//				// deeper searching is apparently impossible (either because there are
//				// no
//				// more moves for us or because we don't have any search time left)
//				break;
//			}
//			lastm= m;
//		}
//		return lastm;
//	}
//



//	// returns the maximum "weight" of connected reachable components: we find the
//	// "region" bounded by all articulation points, traverse each adjacent region
//	// recursively, and return the maximum traversable area
//	static void _explore_space(Space result, Grid g, Articulations articulations, List<Integer> exits, int v, int[] territory, int pID) {
//		if (articulations.num[v]==0) return; // redundant; already explored
//		if (Color.at(v)==Color.RED) ++result.red;
//		else ++result.black;
//		articulations.num[v]= 0;
//		if (articulations.articd[v]!=0) {
//			// we're an articulation vertex; nothing to do but populate the exits
//			for (Direction d : Direction.ALL) {
//				int w= d.move(v);
//				if (g.grid[w]!=0) continue;
//				result.edges++;
//				if (territory!=null&&territory[w]>>16!=pID) {
//					result.front= 1;
//					continue;
//				}
//				if (articulations.num[w]==0) continue; // use 'num' from articulation
//																								// vertex pass to mark nodes
//																								// used
//				exits.add(w);
//			}
//		} else {
//			// this is a non-articulation vertex
//			for (Direction d : Direction.ALL) {
//				int w= d.move(v);
//				if (g.grid[w]!=0) continue;
//				result.edges++;
//
//				// filter out nodes not in our voronoi region
//				if (territory!=null&&territory[w]>>16!=pID) {
//					result.front= 1;
//					continue;
//				}
//
//				if (articulations.num[w]==0) continue; // use 'num' from articulation
//																								// vertex pass to mark nodes
//																								// used
//				if (articulations.articd[w]!=0) { // is this vertex articulated? then
//																					// add it as an exit and don't
//																					// traverse it yet
//					exits.add(w);
//				} else {
//					_explore_space(result, g, articulations, exits, w, territory, pID);
//				}
//			}
//		}
//	}
//
//	// this assumes the space is separated into a DAG of chambers
//	// if cycles or bidirectional openings really do exist, then we just get a bad
//	// estimate :/
//	static Space max_articulated_space(Grid g, Articulations articulations, int v) {
//		return max_articulated_space(g, articulations, v, null, 0);
//	}
//
//	static Space max_articulated_space(Grid g, Articulations articulations, int v, int[] territory) {
//		return max_articulated_space(g, articulations, v, territory, territory==null?0:territory[v]>>16);
//	}
//
//	static Space max_articulated_space(Grid g, Articulations articulations, int v, int[] territory, int pID) {
//		List<Integer> exits= new ArrayList<Integer>(100);
//		Space space= new Space();
//		_explore_space(space, g, articulations, exits, v, territory, pID);
//
//		Space maxspace= new Space(space);
//		int maxsteps= 0;
//		Color entrancecolor= Color.at(v);
//		int[] localsteps= { new Space(space.red, space.black+1, 0, 0).fillable(entrancecolor), new Space(space.red+1, space.black, 0, 0).fillable(entrancecolor) };
//		for (int i= 0; i<exits.size(); i++) {
//			Color exitcolor= Color.at(exits.get(i));
//			// space includes our entrance but not our exit node
//			Space child= max_articulated_space(g, articulations, exits.get(i), territory, pID);
//			// child includes our exit node
//			int steps= child.fillable(exitcolor);
//			if (child.front==0) steps+= localsteps[exitcolor.id];
//			else steps+= territory[exits.get(i)]&0xFFFF-1;
//			// now we need to figure out how to connect spaces via colored
//			// articulation vertices
//			// exits[i] gets counted in the child space
//			// fprintf(stderr, "space@%d,%d exit #%d steps=%d %s\n", v.x, v.y, i,
//			// steps, steps > maxsteps ? "new max" : "");
//			if (steps>maxsteps) {
//				maxsteps= steps;
//				if (child.front==0) {
//					maxspace= child.add(space);
//				} else {
//					maxspace= child;
//				}
//			}
//		}
//		return maxspace;
//	}
//
//	static final int K1= 55;
//	static final int K2= 194;
//	static final int K3= 3;
//
//	private int evaluate_board(Grid g, int player) {
//
//		// remove players from the board when evaluating connected components,
//		// because if a player is separating components he still gets to choose
//		// which
//		// one to move into.
//		Components cp= new Components(g);
//
//		g.grid[g.head(0)]= 0;
//		g.grid[g.head(1)]= 0;
//		cp.calculate(); // compute present's components
//		g.grid[g.head(0)]= 1;
//		g.grid[g.head(1)]= 1;
//
//		++evaluations;
//
//		int comp;
//		// follow the maximum territory gain strategy until we partition
//		// space or crash
//		if ((comp= cp.componentAt(g.head(0)))==cp.componentAt(g.head(1))) {
//			int v= evaluate_territory(g);
//			return v;
//		}
//
//		Articulations ar= new Articulations();
//		g.grid[g.head(0)]= 0;
//		g.grid[g.head(1)]= 0;
//		ar.calculate(g, g.head(0));
//		ar.calculate(g, g.head(1));
//
//		// since each bot is in a separate component by definition here, it's OK to
//		// destructively update cp for floodfill()
//
//		// now ideally we would separate regions by articulation vertices and then
//		// find the maximum traversable area.
//		Space ccount0= max_articulated_space(g, ar, g.head(0));
//		Space ccount1= max_articulated_space(g, ar, g.head(1));
//
//		int ff0= ccount0.fillable(Color.at(g.head(0))), ff1= ccount1.fillable(Color.at(g.head(1)));
//
//		int v= 10000*(ff0-ff1);
//		// if our estimate is really close, try some searching
//		if (v!=0&&Math.abs(v)<=30000) {
//			ff0= spacefill(g, cp, 0, 3, null).value;
//			ff1= spacefill(g, cp, 1, 3, null).value;
//			v= 10000*(ff0-ff1);
//		}
//		if (player==1) v= -v;
//		g.grid[g.head(0)]= 1;
//		g.grid[g.head(1)]= 1;
//		return v;
//	}
//
//	private int evaluate_territory(final Grid g) {
//		int[] territory= new int[Grid.AREA];
//
//		Dijkstra.calculate(territory, g.grid, g.head(0), 0);
//		Dijkstra.calculate(territory, g.grid, g.head(1), 1);
//
//		Articulations ar= new Articulations();
//		g.grid[g.head(0)]= 0;
//		g.grid[g.head(1)]= 0;
//		ar.calculate(g, g.head(0), territory);
//		ar.calculate(g, g.head(1), territory);
//
//		Space ccount0= max_articulated_space(g, ar, g.head(0), territory);
//		Space ccount1= max_articulated_space(g, ar, g.head(1), territory);
//
//		int nc0_= K1*(ccount0.front+ccount0.fillable(Color.at(g.head(0))))+K2*ccount0.edges;
//		int nc1_= K1*(ccount1.front+ccount1.fillable(Color.at(g.head(1))))+K2*ccount1.edges;
//
//		g.grid[g.head(0)]= 1;
//		g.grid[g.head(1)]= 1;
//		int nodecount= nc0_-nc1_;
//		return nodecount;
//	}




}
