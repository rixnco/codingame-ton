package tron;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
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
		
		LinkedList<Integer> opponents= new LinkedList<>();
		g.calculateComponents(true);
		int c= g.playerComponent(forPlayer);
		for(int p= g.nextPlayer(forPlayer); p!=forPlayer; p=g.nextPlayer(p)) {
			if(c==g.playerComponent(p)) opponents.add(p);
		}

		if(DEBUG) {
			System.out.println("Territory mode P"+forPlayer+" against "+opponents.toString());
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
				Move m= _alphabeta(g, forPlayer, opponents, MIN, MAX, (2*itr)-1, present, dog,"");
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
		if(DEBUG) {
			System.out.println("Territory mode P"+forPlayer+" depth="+maxitr+" Move "+(best==null?"???":best.dir[forPlayer]));
		}
		return best;
	}

	@Override
	public Integer evaluate(final Grid g, final int player) {
		LinkedList<Integer> opponents= new LinkedList<>();
		g.calculateComponents(true);
		int c= g.playerComponent(player);
		for(int p= g.nextPlayer(player); p!=player; p=g.nextPlayer(p)) {
			if(c==g.playerComponent(p)) opponents.add(p);
		}

		try {
			return evaluate_board(g, player, opponents, Watchdog.createInfinite());
		} catch (Timeout t) {
		}
		return null;
	}

	// do an iterative-deepening search on all moves and see if we can find a
	// move
	// sequence that cuts off our opponent
	Move _alphabeta(Grid g, int forPlayer, LinkedList<Integer> opponents, int a, int b, int itr, final Move present, final Watchdog dog, String tab) throws Timeout {
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

		
		for(int p: opponents) {
			if (!g.alive(p)) {
				// At least one opponent died!!!
	
				present.eval= MAX; 
				present.value= present.eval; 
				if(DEBUG_TERRITORY_MINIMAX) {
	//				GridUtils.dump(g);
					System.out.println(tab+"  --> At least one opponent died");
				}
				return null;
			}
		}
		
		// last iteration?
		if (itr==0) {
			// We're evaluating present if not already done !!
			if (present.eval==null) {
				if(DEBUG_TERRITORY_MINIMAX) {
					System.out.println(tab+"-->evaluating "+present);
				}
				present.eval= evaluate_board(g, forPlayer, opponents, dog);
				if(DEBUG_TERRITORY_MINIMAX) {
//					GridUtils.dump(g);
					System.out.println(tab+"-->P"+forPlayer+" ("+Grid.getX(g.head(forPlayer))+","+Grid.getY(g.head(forPlayer))+")");
					System.out.println(tab+"-->evaluate_board="+present.eval);
				}
			}
			present.value= present.eval;
			return null;
		}

		boolean maximize=(itr&1)==1;

		if (present.future.isEmpty()) {
			// No future yet. build it
			if(maximize) {
				int xy=g.head(forPlayer);
				for (Direction d : Direction.ALL) {
					if(!g.empty(xy,d)) continue;
					present.addFuture(new Move(forPlayer, d));
				}
				if(present.future.isEmpty()) {
					// NO FUTURE FOR MAX PLAYER !!!
					present.eval=MIN;
					present.value= MIN;
					if(DEBUG_TERRITORY_MINIMAX) {
						System.out.println(tab+"-->No future for P"+forPlayer);
					}
					return null;
				}
			} else {

				present.future= buildFuture(opponents, g);
					
				if(present.future.isEmpty()) {
					// NO FUTURE FOR ONE OPPONENT !!!
					present.eval=MAX;
					present.value= MAX;
					if(DEBUG_TERRITORY_MINIMAX) {
						System.out.println(tab+"-->No future for one opponent");
					}
					return null;
				}
				
			}
		}
		
		// Sort Future
		Collections.sort(present.future, maximize?Move.BEST_FIRST:Move.BEST_LAST);
		if(DEBUG_TERRITORY_MINIMAX) {
			System.out.println(tab+"-->Sorting future");
		}

		Move minimax=null;
		int  minimaxv= maximize?MAX:MIN;
		
		for (Move m : present.future) {
			// move player
			m.move(g);
			_alphabeta(g, forPlayer, opponents, a, b, itr-1, m, dog, tab+"  ");
			m.unmove(g);
			dog.check();
			if(maximize) {
				// Maximizing 
				
				if(minimax==null || minimaxv<=m.value) {
					if(minimax!=null && minimaxv==m.value) {
						// Choose move with lowest degree
						int head= g.head(forPlayer);
						if(g.degree(minimax.dir[forPlayer].move(head))>g.degree(m.dir[forPlayer].move(head))) {
							minimax= m;
						}
					} else {
						minimax=m;
						minimaxv= m.value;
					}
				}
				if(minimaxv>=b) {
					// Beta pruning
					break;
				}
				a= a>minimaxv?a:minimaxv;
			} else {
				// Minimizing
				if(minimax==null || minimaxv>m.value) {
					minimax=m;
					minimaxv= m.value;
				}
				if(minimaxv<=a) {
					// Alpha pruning
					break;
				}
				b= b<minimaxv?b:minimaxv;
				
			}

		}
		present.value= minimaxv;
		if(DEBUG_TERRITORY_MINIMAX) {
			System.out.println(tab+"alphabeta(P"+forPlayer+") ["+itr+"] res="+minimax);
		}	
		return minimax;
	}

	private LinkedList<Move> buildFuture(LinkedList<Integer> opponents, Grid g) {
		
		LinkedList<Move> present= new LinkedList<>();
		LinkedList<Move> future= new LinkedList<>();
		LinkedList<Move> tmp;
		
		future.add(new Move());
		for(int p:opponents) {
			tmp=present;
			present=future;
			future=tmp;
			future.clear();
			
			int count=0;
			for(Move m: present) {
				m.move(g);
				int head= g.head(p);
				for(Direction d:Direction.ALL) {
					if(!g.empty(head, d)) continue;
					++count;
					future.add(new Move(m).append(p,d));
				}
				m.unmove(g);
			}
			if(count==0) {
				// Opponent p will die anyway
				future.clear();
				return future;
			}
		}
		
		return future;
	}
	
	
	private int evaluate_board(final Grid g, final int player, LinkedList<Integer> opponents, final Watchdog dog) throws Timeout {
		// remove players from the board when evaluating connected components,
		// because if a player is separating components he still gets to choose
		// which
		// one to move into.
		g.calculateComponents(true); // compute present's components

		if(DEBUG_TERRITORY) {
			Utils.dumpComponents(g);
		}
		
		++evaluations;

		if(opponents.size()>0) {
			if(DEBUG_TERRITORY) {
				System.out.println("Need to fight for territory");
			}
			int v= evaluate_territory(g, player, opponents);
			return v;
		}
		
		if(DEBUG_TERRITORY) {
			System.out.println("Need to fill in our territory");
		}
		// Build articulated space for all players
		g.resetArticulations();
		int v;
		int ff0;
		int ffmax;
		int maxp;
		g.hideHeads();
		int p= player;
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
		g.restoreHeads();

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

	private int evaluate_territory(final Grid g, int player, LinkedList<Integer> opponents) {
		g.resetTerritory();
		
		g.calculateTerritory(g.head(player), player);
		for(int p:opponents) {
			g.calculateTerritory(g.head(p), p);
		}

		if(DEBUG_TERRITORY) {
			Utils.dumpTerritory(g);
		}

		// Build articulations for all player in our component
		g.resetArticulations();
		g.hideHeads();
		g.calculateArticulations(g.head(player), g.territory);
		for(int p:opponents) {
			g.calculateArticulations(g.head(p), g.territory);
		}

		if(DEBUG_TERRITORY) {
			Utils.dumpArticulations(g);
		}

		// Compute remaining space
		Space ccount0= max_articulated_space(g, g.head(player), g.territory);
		int nodecount= K1*(ccount0.front+ccount0.fillable(Color.at(g.head(player))))+K2*ccount0.edges;
		if(DEBUG_TERRITORY) {
			System.out.println("ccount("+player+")="+ccount0);
			System.out.println("nodecount("+player+")="+nodecount);
		}
		int ncmax=0;
		for(int p: opponents) {
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

			if(DEBUG_TERRITORY) {
				System.out.println(tab+"space"+Grid.toXYString(v)+" exit"+Grid.toXYString(exits.get(i))+" steps="+steps+(steps>maxsteps?" new max":""));
			}
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
