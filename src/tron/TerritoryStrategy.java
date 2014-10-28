package tron;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import tron.Grid.Color;
import tron.Grid.Direction;
import tron.Watchdog.Timeout;
import static tron.Constants.*;

public class TerritoryStrategy implements Strategy {
	private Watchdog watchdog;
	public int maxitr;
	public int evaluations;
	public int ab_runs;
	public int cutoffs;

	static final int K1= 55;
	static final int K2= 194;
	static final int K3= 3;

	public String toString() {
		return "Territory";
	}

	@Override
	public Move nextMove(Move present, final Grid grid, final int forPlayer, final int maxDepth, final Watchdog dog) {
		if (watchdog==null) this.watchdog= new Watchdog();

		//TODO: find a way to keep a copy of the latest present evaluation before timeout
		Grid g= grid.copy();
		
		if(DEBUG) {
			System.out.println("Territory mode P"+forPlayer+" ("+Grid.getX(g.head(forPlayer))+","+Grid.getY(g.head(forPlayer))+")");
		}
		
		int itr;
		Move best= null;
		evaluations= 0;
		ab_runs= 0;
		cutoffs= 0;

		try {
			for (itr= 1; itr<=maxDepth; itr++) {
				if(DEBUG) {
					System.out.println("Depth "+itr);
				}
				Move m= _alphabeta(g, forPlayer/*, forPlayer*/, MIN, MAX, (2*itr)-1, present, dog,"");
				best= m;
				maxitr= itr;
				if (present.value==MIN) {
					if(DEBUG) {
						System.out.println("We're going to die !!!");
					}
					// deeper searching is apparently impossible (either because
					// there are
					// no
					// more moves for us or because we don't have any search
					// time left)
					break;
				}
				if (best!=null) {
					if (present.value==MAX) {
						if(DEBUG) {
							System.out.println("Opponent(s) are stuck we win !!!");
						}
						// our opponent cannot move, so we win
						break;
					}
					if(DEBUG) {
						System.out.println("best so far: depth="+itr+" "+best);
					}
				}
//				if (present.value==MAX) {
//					if(DEBUG) {
//						System.out.println("Deeper searching is apparently impossible: "+present.value+"=MIN");
//					}
//					// deeper searching is apparently impossible (either because
//					// there are
//					// no
//					// more moves for us or because we don't have any search
//					// time left)
//					break;
//				}
				dog.check();
			}
		} catch (Timeout t) {
			if(DEBUG) {
				System.out.println("Timeout !!");
			}
		}
		if(true || DEBUG) {
			System.out.println("Territory mode P"+forPlayer+" depth="+maxitr+" Move "+(best==null?"???":best.dir[forPlayer]));
		}
		return best;
	}

	@Override
	public Integer evaluate(final Grid g, final int player) {
		try {
			return evaluate_board(g, player, Watchdog.createInfinite());
		} catch (Timeout t) {
		}
		return null;
	}

	// do an iterative-deepening search on all moves and see if we can find a
	// move
	// sequence that cuts off our opponent
	Move _alphabeta(Grid g, int forPlayer/*, int player*/, int a, int b, int itr, final Move present, final Watchdog dog, String tab) throws Timeout {
		dog.check();

		++ab_runs;
		
		if(DEBUG_TERRITORY_MINIMAX) {
			if(itr>0) System.out.println(tab+"alphabeta(P"+forPlayer+") ["+itr+"] alpha="+a+" beta="+b);
			else      System.out.println(tab+"Evaluation");
		}

		if(!g.alive(forPlayer)) {
			// We're dead !!!
			present.eval= MIN; //g.alive[forPlayer]?Integer.MAX_VALUE:Integer.MIN_VALUE;
			present.value= present.eval; //g.alive[forPlayer]?Integer.MAX_VALUE:Integer.MIN_VALUE;
			if(DEBUG_TERRITORY_MINIMAX) {
//				GridUtils.dump(g);
				System.out.println(tab+"  --> we're dead");
			}
			return null;
		}
		
		if (g.remainingPlayers==1) {
			// No more opponents !!!

			present.eval= MAX; //g.alive[forPlayer]?Integer.MAX_VALUE:Integer.MIN_VALUE;
			present.value= present.eval; //g.alive[forPlayer]?Integer.MAX_VALUE:Integer.MIN_VALUE;
			if(DEBUG_TERRITORY_MINIMAX) {
//				GridUtils.dump(g);
				System.out.println(tab+"  --> no more competitor(s)");
			}
			return null;
		}

		// last iteration?
		if (itr==0) {
			// We're evaluating present if not already done !!
			if (present.eval==null) {
				if(DEBUG_TERRITORY_MINIMAX) {
					System.out.println(tab+"-->evaluating "+present);
				}
				present.eval= evaluate_board(g, forPlayer, dog);
				if(DEBUG_TERRITORY_MINIMAX) {
//					GridUtils.dump(g);
					System.out.println(tab+"-->P"+forPlayer+" ("+Grid.getX(g.head(forPlayer))+","+Grid.getY(g.head(forPlayer))+")");
					System.out.println(tab+"-->evaluate_board="+present.eval);
				}
			}
			present.value= present.eval;
			return null;
		}

		if (present.future.isEmpty()) {
			// No future yet. build it
			if(g.nextPlayer()==forPlayer) {
				int xy=g.head(forPlayer);
				for (Direction d : Direction.ALL) {
					if(!g.empty(xy,d)) continue;
					present.addFuture(new Move(forPlayer, d));
				}
			} else {
////				System.out.println(tab+"  --> Building opponent future of "+present);
//				int xy=g.head(player);
//				for(Direction d: Direction.ALL) {
//					if(!g.empty(xy,d)) continue;
//					Move m= new Move(player, d);
////					System.out.println(tab+"  --> adding "+m);
//					present.addFuture(m);
//				}
//				if(present.future.isEmpty()) {
////					System.out.println(tab+"  --> player P"+player+" will die");
//					present.addFuture(new Move(player, Direction.UP));
//				}

				//int comp= g.playerComponent(forPlayer);
				buildFuture(g.nextPlayer(), forPlayer, g, null, present.future);
			}
			
			if (present.future.isEmpty()) {
				if (g.nextPlayer()==forPlayer) {
					// No future for forPlayer
					present.eval= MIN;
					present.value= MIN;
					if(DEBUG_TERRITORY_MINIMAX) {
						System.out.println(tab+"-->No future for P"+forPlayer);
					}
					return null;
				}
				// opponent(s) will die
				if(DEBUG_TERRITORY_MINIMAX) {
					System.out.println(tab+"-->Opponent will die (P"+g.nextPlayer()+")");
				}
				present.addFuture(new Move(g.nextPlayer(), Direction.UP));
			}
		} else {
			// Sort future
//			Collections.sort(present.future, maximize?Move.BEST_FIRST:Move.BEST_LAST);
//			if(DEBUG_TERRITORY_MINIMAX) {
//				System.out.println(tab+"-->Sorting future "+(maximize?"max first":"min first"));
//			}
			Collections.sort(present.future, Move.BEST_LAST);
			if(DEBUG_TERRITORY_MINIMAX) {
				System.out.println(tab+"-->Sorting future");
			}
		}

		// periodically check timeout. if we do time out, give up, we can't do
		// any
		// more work; whatever we found so far will have to do
//		Move best= null;
//		int bestv;
//		int nextp= g.nextPlayer();
		
		Move minimax=null;
		int  minimaxv= MIN;
		
		for (Move m : present.future) {
			// move player
			try {
				m.move(g);
				_alphabeta(g, forPlayer/*, nextp*/, -b, -a, itr-1, m, dog, tab+"  ");
			} finally {
				m.unmove(g);
			}
			dog.check();
			m.value=m.value;
			int val= m.value;
			if (minimax==null || val>minimaxv) {
				minimax= m;
				minimaxv=val;
				if(minimaxv>a) {
					a=minimaxv;
					if (a>=b) {
						// beta cutoff
						if(DEBUG_TERRITORY_MINIMAX) {
							System.out.println(tab+"-->beta cutoff");
						}	
						break;
					}
				}
			}

		}
		present.value= minimaxv;
		if(DEBUG_TERRITORY_MINIMAX) {
			System.out.println(tab+"alphabeta(P"+forPlayer+") ["+itr+"] res="+minimax);
		}	
		return minimax;
	}

	private void buildFuture(int player, int forPlayer, Grid g, Move present, List<Move> future) {
		if(player==forPlayer) {
			if(present!=null) {
//				System.out.println("Adding future "+present);
				future.add(present);
			}
			return;
		}
		int xy=g.head(player);
		int count=0;
		Move m;
		for (Direction d : Direction.ALL) {
			if(!g.empty(xy,d)) continue;
			++count;
			m= new Move(present).append(player, d);
			buildFuture(g.nextPlayer(player), forPlayer, g, m, future);
		}
		if(count==0) {
			// Player is going to die
//			System.out.println("Killing P"+player);
			m= new Move(present).append(player, Direction.UP);
			buildFuture(g.nextPlayer(player), forPlayer, g, m, future);
		}
	}
	
	
	private int evaluate_board(final Grid g, final int player, final Watchdog dog) throws Timeout {

		// remove players from the board when evaluating connected components,
		// because if a player is separating components he still gets to choose
		// which
		// one to move into.
		g.calculateComponents(); // compute present's components

		if(DEBUG_TERRITORY) {
			GridUtils.dumpComponents(g);
		}
		
		++evaluations;

		// follow the maximum territory gain strategy until we partition
		// space or crash
		int comp= g.playerComponent(player);
		int p;
		for (p= g.nextPlayer(player); p!=player; p= g.nextPlayer(p)) {
			if (comp==g.playerComponent(p)) {
				// We have to fight for territory
				if(DEBUG_TERRITORY) {
					System.out.println("Need to fight for territory");
				}
				int v= evaluate_territory(g, player);
				return v;
			}
		}

		// Build articulated space for all players
		g.resetArticulations();
		int v;
		int ff0;
		int ffmax;
		int maxp;
		try {
		g.hideHeads();
		p= player;
		do {
			g.calculateArticulations(g.head(p));
		} while ((p= g.nextPlayer(p))!=player);

		// since each bot is in a separate component by definition here, it's OK
		// to
		// destructively update cp for floodfill() TODO ???

		// now ideally we would separate regions by articulation vertices and
		// then
		// find the maximum traversable area.
		Space ccount0= max_articulated_space(g, g.head(player));
		ff0= ccount0.fillable(Color.at(g.head(player)));
		ffmax= MIN;
		maxp= player;
		for (p= g.nextPlayer(player); p!=player; p= g.nextPlayer(p)) {
			Space ccountp= max_articulated_space(g, g.head(p));
			int ffp= ccountp.fillable(Color.at(g.head(p)));
			if (ffp>ffmax) {
				ffmax= ffp;
				maxp= p;
			}
			dog.check();
		}
		if(maxp==player) {
			if(DEBUG) {
				System.out.println("OUPS !!! What the fuck !!!");
			}
		}
		v= 1000*(ff0-ffmax);
		} finally {
			g.restoreHeads();
		}
		// if our estimate is really close, try some searching
		if (v!=0&&Math.abs(v)<=30000) {
			Move present= new Move();
			Grid g0= g.copy();
			SurvivalStrategy.spacefill(present, g0, player, 3, dog);
			ff0= present.value;
			present.reset();
			g0= g.copy();
			SurvivalStrategy.spacefill(present, g0, maxp, 3, dog);
			ffmax= present.value;
			v= 10000*(ff0-ffmax);
		}
		return v;
	}

	private int evaluate_territory(final Grid g, int player) {
		g.resetTerritory();
		int p= player;
		int comp= g.playerComponent(player);
		// Build territory for all player in our component
		do {
			if (comp!=g.playerComponent(p)) continue;
			g.calculateTerritory(g.head(p), p);
		} while ((p= g.nextPlayer(p))!=player);

		if(DEBUG_TERRITORY) {
			GridUtils.dumpTerritory(g);
		}

		// Build articulations for all player in our component
		g.resetArticulations();
		g.hideHeads();
		p= player;
		do {
			if (comp!=g.playerComponent(p)) continue;
			g.calculateArticulations(g.head(p));
		} while ((p= g.nextPlayer(p))!=player);

		if(DEBUG_TERRITORY) {
			GridUtils.dumpArticulations(g);
		}

		// Compute remaining space
		Space ccount0= max_articulated_space(g, g.head(player), g.territory);
		int nodecount= K1*(ccount0.front+ccount0.fillable(Color.at(g.head(player))))+K2*ccount0.edges;
		if(DEBUG_TERRITORY) {
			System.out.println("ccount("+player+")="+ccount0);
			System.out.println("nodecount("+player+")="+nodecount);
		}
		int ncmax=0;
		for (p= g.nextPlayer(player); p!=player; p= g.nextPlayer(p)) {
			if (comp!=g.playerComponent(p)) continue;
			Space ccount1= max_articulated_space(g, g.head(p), g.territory);
			int nc1= K1*(ccount1.front+ccount1.fillable(Color.at(g.head(p))))+K2*ccount1.edges;
			if(nc1>ncmax) ncmax= nc1;
			if(DEBUG_TERRITORY) {
				System.out.println("ccount("+p+")="+ccount1);
				System.out.println("nodecount("+p+")="+nc1);
			}
		}
		nodecount-= ncmax;
		
		if(DEBUG_TERRITORY) {
			System.out.println("nodecount(gain)="+nodecount);
		}
		g.restoreHeads();

		return nodecount;
	}

	// this assumes the space is separated into a DAG of chambers
	// if cycles or bidirectional openings really do exist, then we just get a
	// bad
	// estimate :/
	static Space max_articulated_space(Grid g, int v) {
		return max_articulated_space(g, v, null);
	}

	static Space max_articulated_space(Grid g, int v, int[] territory) {
		if(DEBUG_TERRITORY) {
			System.out.println("max_articulated_space"+Grid.toXYString(v));
		}
		return max_articulated_space(g, v, territory, territory==null?16<<16:territory[v]&0xFF0000, "");
	}

	static Space max_articulated_space(Grid g, int v, int[] territory, int pID, String tab) {
		List<Integer> exits= new ArrayList<Integer>(100);
		Space space= new Space();
		explore_space(space, g, exits, v, territory, pID);

		if(DEBUG_TERRITORY) {
			System.out.print(tab+"Explore_space: "+Grid.toXYString(v)/*+": "+space*/);
			System.out.print("  exits: ");
			for(int xy: exits) System.out.print(Grid.toXYString(xy)+" ");
			System.out.println();
		}
		
		Space maxspace= new Space(space);
		int maxsteps= 0;
		Color entrancecolor= Color.at(v);
		final int[] localsteps= { new Space(space.red, space.black+1, 0, 0).fillable(entrancecolor),
				new Space(space.red+1, space.black, 0, 0).fillable(entrancecolor) };
		for (int i= 0; i<exits.size(); i++) {
			Color exitcolor= Color.at(exits.get(i));
			// space includes our entrance but not our exit node
			Space child= max_articulated_space(g, exits.get(i), territory, pID, tab+"__");
			// child includes our exit node
			int steps= child.fillable(exitcolor);
			if (child.front==0) steps+= localsteps[exitcolor.id];
			else steps+= (territory[exits.get(i)]&0xFFFF)-1;
			// now we need to figure out how to connect spaces via colored
			// articulation vertices
			// exits[i] gets counted in the child space
			// fprintf(stderr, "space@%d,%d exit #%d steps=%d %s\n", v.x, v.y,
			// i,
			// steps, steps > maxsteps ? "new max" : "");

//			if(DEBUG_TERRITORY) {
//				System.out.println(tab+"space"+Grid.toXYString(v)+" exit"+Grid.toXYString(exits.get(i))+" steps="+steps+(steps>maxsteps?" new max":""));
//			}
			if (steps>maxsteps) {
				maxsteps= steps;
				if (child.front==0) {
					maxspace= child.add(space);
				} else {
					maxspace= child;
				}
			}
		}
		if(DEBUG_TERRITORY) {
			System.out.println(tab+"space"+Grid.toXYString(v)+" steps="+maxsteps);
		}
//		if(DEBUG_TERRITORY) {
//			System.out.println("maxspace="+maxspace);
//		}
		
		return maxspace;
	}

	// returns the maximum "weight" of connected reachable components: we find
	// the
	// "region" bounded by all articulation points, traverse each adjacent
	// region
	// recursively, and return the maximum traversable area
	static void explore_space(Space result, Grid g, List<Integer> exits, int v, int[] territory, int pID) {
		
		// Rework pID marking
		// Change nodecount computation to compute the smallest dif instead of the cumulated diff !!
		
		if ((g.num[v]&pID)==pID) return; // redundant; already explored
		if (Color.at(v)==Color.RED) ++result.red;
		else ++result.black;
		g.num[v]|= pID; // mark with pID
		if (g.articd[v]!=0) {
			// we're an articulation vertex; nothing to do but populate the
			// exits
			for (Direction d : Direction.ALL) {
				int w= d.move(v);
				if (g.grid[w]!=0) continue;
				result.edges++;
				if (territory!=null&&(territory[w]&0xFF0000)!=pID) {
					result.front= 1;
					continue;
				}
				if ((g.num[w]&pID)==pID) continue; // use 'num' from articulation
											// vertex pass to mark nodes
											// used
				if(!exits.contains(w))	exits.add(w);
			}
		} else {
			// this is a non-articulation vertex
			for (Direction d : Direction.ALL) {
				int w= d.move(v);
				if (g.grid[w]!=0) continue;
				result.edges++;

				// filter out nodes not in our voronoi region
				if (territory!=null&&(territory[w]&0xFF0000)!=pID) {
					result.front= 1;
					continue;
				}

				if ((g.num[w]&pID)==pID) continue; // use 'num' from articulation
											// vertex pass to mark nodes
											// used
				if (g.articd[w]!=0) { // is this vertex articulated? then
										// add it as an exit and don't
										// traverse it yet
					if(!exits.contains(w))	exits.add(w);
					
				} else {
					Space s= new Space();
					explore_space(s, g, exits, w, territory, pID);
					result.add(s);
				}
			}
		}
	}

	static class Space {
		int red, black, edges, front;

		Space() {
		}

		Space(Space o) {
			red= o.red;
			black= o.black;
			edges= o.edges;
			front= o.front;
		}

		Space(int r, int b, int e, int f) {
			red= r;
			black= b;
			edges= e;
			front= f;
		}

		Space add(Space o) {
			red+= o.red;
			black+= o.black;
			edges+= o.edges;
			front+= o.front;
			return this;
		}

		int fillable(Color startcolor) {
			if (startcolor==Color.RED) {
				// start on red? then moves are black-red-black-red-black (2
				// red, 3
				// black: 5; 3 red 3 black: 6; 4 red 3 black
				return 2*(red-1<black?red-1:black)+(black>=red?1:0);
			} else {
				// moves are red-black-red-black-red
				return 2*(red<black-1?red:black-1)+(red>=black?1:0);
			}
		}

		public String toString() {
			return "red="+red+" black="+black+" edges="+edges+" front="+front;
		}
		
	};

}
