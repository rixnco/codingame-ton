import java.util.*;
import java.io.*;
final class Player {
	public static final int TIMEOUT=90;
	public static final int DEPTH=100;
	static final int DEBUG=1;
	final Scanner in;
    final PrintStream out;
	final PrintStream log;
    Move present=null;
    final Grid grid= new Grid();
    final Grid tmp= new Grid();
	final Timer timer= new Timer();
    Watchdog dog;
    static public void main(String[] args) {
    	new Player().play();
    }
    public Player() {
    	this(System.in, System.out, System.err);
    }
    public Player(InputStream in, PrintStream out, PrintStream log) {
    	this.in= new Scanner(in);
    	this.out=out;
    	this.log=log;
    }
    final public void play() {
        // game loop
        int N = in.nextInt(); // total number of players (2 to 4).
        int P = in.nextInt(); // your player number (0 to 3).
        in.nextLine();
        grid.setNbPlayers(N);
        int[] XY0= new int[N];
        int[] XY1= new int[N];
        boolean[] alive= new boolean[] { 0<N, 1<N, 2<N, 3<N };
        boolean firstRound=true;
        Move current;
        while (true) {
        	System.arraycopy(XY1, 0, XY0, 0, N);
        	current= Move.get(0);
            for (int i = 0; i < N; i++) {
                final int X0 = in.nextInt()+1; // starting X coordinate of lightcycle (or 0)
                final int Y0 = in.nextInt()+1; // starting Y coordinate of lightcycle (or 0)
                final int X1 = in.nextInt()+1; // starting X coordinate of lightcycle (can be the same as X0 if you play before this player)
                final int Y1 = in.nextInt()+1; // starting Y coordinate of lightcycle (can be the same as Y0 if you play before this player)
                in.nextLine();
                int p= (i-P+N)%N;
                if(firstRound) { // first round
                	grid.move(p, Grid.getXY(X0,Y0));
                    XY0[p]= Grid.getXY(X0,Y0);
                }
                if(X0==0 && alive[p]) { // Suicide player
                	grid.move(p,0);
                	continue;
                }
	            XY1[p]= Grid.getXY(X1, Y1);
	            Direction d= Direction.fromTo(XY0[p], XY1[p]);
	            if(d!=Direction.NONE) current.append(p, d);
            }
        	firstRound=false;
    		dog= new Watchdog();
      		timer.schedule(dog, TIMEOUT);
      		// Apply move
      		current.move(grid);
      		if(present!=null && Arrays.equals(alive, grid.alive)) {
      			// No player died
      			Move evaluated=null;
      			switch(present.strategy){ 
      			case TERRITORY:
	      			// Try to find if we've already evaluated this position
	      			for(Iterator<Move> it= present.future.iterator(); it.hasNext(); ) {
	      				Move f= it.next();
	      				if(!f.match(current)) continue;
	      				evaluated=f;
	      				it.remove();
	      				break;
	      			}
	      			break;
      			case SURVIVAL:
      				evaluated=present;
      				present=null;
      				break;
      			}
      			if(evaluated!=null) {
      				current.dispose(false);
      				current=evaluated;
      			}
      		} 
  			if(present!=null) present.dispose(false);
  			present=current;
        	tmp.copy(grid);
        	Move best= IA.nextMove(present, tmp, 0, DEPTH, dog);
        	if(best!=null) {
	   			out.println(best.dir[0]); 
	        	out.flush();	
        	} else {
    			out.println("Your father was the Creator, Sam! Make it there alive and he'll find you...");
            	out.flush();	
    			break;
        	}
    		// Do some house cleaning
    		dog.cancel();
			present.future.remove(best);
    		present.dispose();
			present=best;
    		Move.flushPending();
			// Next turn
	        in.nextInt(); // total number of players (2 to 4).
	        in.nextInt(); // your player number (0 to 3).
	        in.nextLine();
		}
	}
}
final class IA {
	// Entry point
	final static public Move nextMove(final Move present, final Grid grid, final int forPlayer, final int maxDepth, Watchdog dog) {
		// original grid must have been copied before calling this method as it will be messed up during processing
		grid.hideHeads();
		grid.calculateComponents(true);
		grid.restoreHeads();
		
		int cp= grid.playerComponent(forPlayer);

		LinkedList<Integer> opponents= new LinkedList<>();
		for (int p= grid.nextPlayer(forPlayer); p!=forPlayer; p= grid.nextPlayer(p)) {
			if(cp==grid.playerComponent(p)) opponents.add(p); 
		}
		Move m;
		if(opponents.isEmpty()) {
			m= nextMoveSurvival(present, grid, forPlayer, maxDepth, dog);
		} else if(opponents.size()==1 ){
			m= nextMoveTerritory(present, grid, forPlayer, opponents, maxDepth, dog);
		} else {
			m= nextMoveFight(present, grid, forPlayer, opponents, maxDepth, dog);
		}
		return m;
	}
	// Fight Strategy
	public static int maxn_runs;
	public static int maxn_evals;
	// Territory Strategy
	public static int ab_runs;
	public static int ab_evals;
	public static int ab_cutoffs;
	static final int K1= 5; //55;
	static final int K2= 19; //194;
	public static int maxitr=0;
	final static public Move nextMoveFight(Move present, final Grid grid, final int forPlayer, final LinkedList<Integer> opponents, final int maxDepth, final Watchdog dog) {
		// original grid must have been copied before calling this method as it will be messed up during processing
		boolean dirty=present.strategy!=Strategy.FIGHT;
		opponents.addFirst(forPlayer);
		dirty|= (present.opponents!=null && !opponents.equals(present.opponents));
		
		if(dirty) {
			present.reset(false);
			present.strategy= Strategy.FIGHT;
			present.player= forPlayer;
		}
		present.opponents= opponents;
		int itr;
		Move best= null;
		ab_evals= 0;
		maxn_runs= 0;
		try {
			for (itr= 0; itr<maxDepth; itr++) {
				Move m= maxn(present, forPlayer, opponents, grid, itr*opponents.size(), dog);
				maxitr= (itr*opponents.size())+1;
				if(best!=null) best.dispose(false);
				best= Move.get(m);  // Deep copy of best eval so far TODO is it a good idea ???
//				best= m;  // Use best as is. It may be modified by next itr search, but sorting should take care of that
				present.depth=maxitr;		
				dog.check();
			}
		} catch (Timeout t) {
		}
		return best;
	}
	final static Move maxn(final Move present, final int forPlayer, final List<Integer> players, final Grid g, int itr, final Watchdog dog) throws Timeout {
		dog.check();
		final int nbPlayers=players.size();
		++maxn_runs;
		// last iteration?
		if (itr<0) {
			// We're evaluating present if not already done !!
			if (present.eval==Move.NaN) {
				present.eval= evaluate_maxn(g, forPlayer, players, dog);
				present.degree= g.alive[forPlayer]?g.degree(g.head[forPlayer]):0;
			}
			present.value= present.eval;
			present.strategy= Strategy.FIGHT;
			present.depth=itr;
			present.opponents=players;
			return null;
		}
		int player= players.get((nbPlayers-(itr%nbPlayers))%nbPlayers);
		if (present.future.isEmpty()) { // No future yet. build it
			int xy=g.head[player];
			for (Direction d : Direction.ALL) {
				if(g.grid[xy+d.step]!=0) continue;
				present.future.add(Move.get(player, d));
			}
			if(present.future.isEmpty()) {
				present.future.add(Move.get(player, Direction.NONE));
			}
		}
		Move best=null;
		for (Move m : present.future) {
			m.move(g);
			maxn(m, forPlayer, players, g, itr-1, dog);
			m.unmove(g);
			dog.check();
			if(best==null || BEST_FIGHT_FIRST.compare(m, best)<0) {
				best= m;
			}
		}
		present.value= best.value;
		present.strategy= Strategy.FIGHT;
		present.depth=itr;
		present.opponents=players;
		return best;
	}
 	final static long evaluate_maxn(final Grid g, final int player, final List<Integer> opponents, final Watchdog dog) throws Timeout {
 		++maxn_evals;
		g.resetTerritory();
		int p;
		for(int t=1; t<opponents.size(); ++t) {
			p=opponents.get(t);
			if(g.alive[p])g.calculateTerritory(p);
		}
		if(g.alive[player])g.calculateTerritory(player);
		// Build articulations for all player in our component
		g.resetArticulations();
		for(int t=1; t<opponents.size(); ++t) {
			p=opponents.get(t);
			if(!g.alive[p]) continue;
			g.hideHead(p);
			g.calculateArticulations(g.head[p], g.territory);
			g.restoreHead(p);
		}
		if(g.alive[player]) {
			g.hideHead(player);
			g.calculateArticulations(g.head[player], g.territory);
			g.restoreHead(player);
		}
		// Compute remaining space
		long res=0;
		Space ccount;
		long nodecount;
		for(int t=0; t<opponents.size(); ++t) {
			p=opponents.get(t);
			if(!g.alive[p]) continue;
			ccount= max_articulated_space(g, g.head[p], g.territory);
			dog.check();
			nodecount= K1*(ccount.front+ccount.fillable(Cell.at(g.head[p])))+K2*ccount.edges; //ccount.fillable(Cell.at(g.head[p])); //
			res|= nodecount<<(16*p);
		}
//		if(g.alive[player]) {
//			ccount= max_articulated_space(g, g.head[player], g.territory);
//			dog.check();
//			nodecount= K1*(ccount.front+ccount.fillable(Cell.at(g.head[player])))+K2*ccount.edges;//ccount.fillable(Cell.at(g.head[player])); //
//			res|= nodecount<<(16*player);
//		}
		return res;
	}
	final static public Move nextMoveTerritory(Move present, final Grid grid, final int forPlayer, final List<Integer> opponents, final int maxDepth, final Watchdog dog) {
		// original grid must have been copied before calling this method as it will be messed up during processing
		boolean dirty=present.strategy!=Strategy.TERRITORY;
		dirty|= (present.opponents!=null && !opponents.equals(present.opponents));
		if(dirty) {
			present.reset(false);
			present.strategy= Strategy.TERRITORY;
			present.player= forPlayer;
			present.opponents=opponents;
		}
		int itr;
		Move best= null;
		ab_evals= 0;
		ab_runs= 0;
		ab_cutoffs= 0;
		try {
			for (itr= 1; itr<=maxDepth; itr++) {
				Move m= alphabeta(grid, forPlayer, opponents, Move.MIN, Move.MAX, (2*itr)-1, present, dog);
				maxitr= 2*itr-1;
				best= m;  // Use best as is. It may be modified by next itr search, but sorting should take care of that
				if (present.value==Move.MIN) {
					// deeper searching is apparently impossible (either because
					// there are no more moves for us
					break;
				}
				if (best!=null) {
					if (present.value==Move.MAX) {
						// our opponent cannot move, so we win
						break;
					}
				}
				dog.check();
			}
		} catch (Timeout t) {
		}
		present.depth=maxitr;		
		present.opponents= opponents;
		return best;
	}
	// do an iterative-deepening search on all moves and see if we can find a move sequence that cuts off our opponent
	final static Move alphabeta(final Grid g, final int forPlayer, final List<Integer> opponents, long a, long b, int itr, final Move present, final Watchdog dog) throws Timeout {
		final int opponent=opponents.get(0);
		dog.check();
		++ab_runs;
		// last iteration?
		if (itr==0) {
			// We're evaluating present if not already done !!
			if (present.eval==Move.NaN) {
				present.degree= g.degree(g.head[forPlayer]);
				present.eval= evaluate_alphabeta(g, forPlayer, opponent, dog);
			}
			present.value= present.eval;
			present.strategy= Strategy.TERRITORY;
			present.depth=itr;
			present.opponents=opponents;
			return null;
		}
		boolean maximize=(itr&1)==1;
		if (present.future.isEmpty()) {
			// No future yet. build it
			if(maximize) {
				int xy=g.head[forPlayer];
				for (Direction d : Direction.ALL) {
					if(g.grid[xy+d.step]!=0) continue;
					present.future.add(Move.get(forPlayer, d));
				}
				if(present.future.isEmpty()) {
					// NO FUTURE FOR MAX PLAYER !!!
					present.eval=Move.MIN;
					present.value=Move.MIN;
					present.strategy= Strategy.TERRITORY;
					present.depth=itr;
					present.opponents=opponents;
					return null;
				}
			} else {
				int xy=g.head[opponent];
				for (Direction d : Direction.ALL) {
					if(g.grid[xy+d.step]!=0) continue;
					present.future.add(Move.get(opponent, d));
				}
				if(present.future.isEmpty()) {
					// NO FUTURE FOR ONE OPPONENT !!!
					present.eval=Move.MAX;
					present.value=Move.MAX;
					present.strategy= Strategy.TERRITORY;
					present.depth=itr;
					present.opponents=opponents;
					return null;
				}
			}
		}
		// Sort Future
		Collections.sort(present.future, maximize?BEST_TERRITORY_FIRST:BEST_TERRITORY_LAST);
		Move minimax=null;
		long  minimaxv= maximize?Move.MIN:Move.MAX;
		
		for (Move m : present.future) {
			// move player
			m.move(g);
			alphabeta(g, forPlayer, opponents, a, b, itr-1, m, dog);
			m.unmove(g);
			dog.check();
			if(maximize) { // Maximizing 
				if(minimax==null || BEST_TERRITORY_FIRST.compare(minimax, m)>0) {
					minimax= m;
					minimaxv=minimax.value;
				}
				if(minimaxv>=b) {
					// Beta pruning
					++ab_cutoffs;
					break;
				}
				a= a>minimaxv?a:minimaxv;
			} else { // Minimizing
				if(minimax==null || BEST_TERRITORY_LAST.compare(minimax, m)>0) {
					minimax=m;
					minimaxv= m.value;
				}
				if(minimaxv<=a) {
					// Alpha pruning
					++ab_cutoffs;
					break;
				}
				b= b<minimaxv?b:minimaxv;
			}
		}
		present.value= minimaxv;
		present.strategy= Strategy.TERRITORY;
		present.depth=itr;
		present.opponents=opponents;
		return minimax;
	}
	
	final static Grid spacefill_tmp= new Grid();
	final static public int evaluate_alphabeta(final Grid g, final int player, final int opponent, final Watchdog dog) throws Timeout {

		++ab_evals;
		g.hideHeads();
		g.calculateComponents(true); // compute present's components
		g.restoreHeads();
		
		int comp= g.playerComponent(player);
		if(comp==g.playerComponent(opponent)) {
			return evaluate_territory(g, player, opponent, dog);
		}
		// 2 players are in separated space ==> Build articulated space for all players
		int v;
		int ff0,ff1;
			
		g.resetArticulations();
		g.hideHeads();
		g.calculateArticulations(g.head[opponent]);
		g.calculateArticulations(g.head[player]);
		g.restoreHeads();

		Space ccount0= max_articulated_space(g, g.head[player]);
		ff0= ccount0.fillable(Cell.at(g.head[player]));
		Space ccountp= max_articulated_space(g, g.head[opponent]);
		ff1= ccountp.fillable(Cell.at(g.head[opponent]));
		dog.check();
		v= 10000*(ff0-ff1);
		// if our estimate is really close, try some searching
		if (v!=0&&Math.abs(v)<=30000) {
			Move present= Move.get();
			spacefill_tmp.copy(g, false);
			spacefill(present, spacefill_tmp, player, 3, dog);
			ff0= (int)present.value;
			dog.check();
			present.reset();
			spacefill_tmp.copy(g,false);
			spacefill(present, spacefill_tmp, opponent, 3, dog);
			ff1= (int)present.value;
			v= 10000*(ff0-ff1);
			present.dispose(false);
		}
		return v;
	}
	
 	final static int evaluate_territory(final Grid g, final int player, final int opponent, final Watchdog dog) throws Timeout {
		g.resetTerritory();
		g.calculateTerritory(opponent);
		g.calculateTerritory(player);
		// Build articulations for all player in our component
		g.resetArticulations();
		g.hideHeads();
		g.calculateArticulations(g.head[opponent]/*, g.territory*/);
		g.calculateArticulations(g.head[player]/*, g.territory*/);
		g.restoreHeads();
		// Compute remaining space
		Space ccount0= max_articulated_space(g, g.head[player], g.territory);
		dog.check();
		int nodecount0= ccount0.fillable(Cell.at(g.head[player])); //K1*(ccount0.front+ccount0.fillable(Cell.at(g.head[player])))+K2*ccount0.edges;//
		Space ccount1= max_articulated_space(g, g.head[opponent], g.territory);
		dog.check();
		int nodecount1= ccount1.fillable(Cell.at(g.head[opponent])); //K1*(ccount1.front+ccount1.fillable(Cell.at(g.head[p])))+K2*ccount1.edges; //
		return nodecount0-nodecount1;
	}
	final static Space max_articulated_space(Grid g, int v) {
		return max_articulated_space(g, v, null);
	}
	final static Space max_articulated_space(Grid g, int v, short[] territory) {
		return max_articulated_space(g, v, territory, (short)(territory==null?0xF000:territory[v]&0xF000));
	}
	final static Space max_articulated_space(final Grid g, final int xy, final short[] territory, final short pID) {
		List<Integer> exits= new ArrayList<Integer>();
		Space space= exploreSpace(g, xy, exits, territory, pID);
		Cell entranceCell= Cell.at(xy);
		Space maxspace= new Space(space);
		int maxsteps= space.fillable(entranceCell);
		++space.black;
		final int localBlack= space.fillable(entranceCell);
		--space.black; ++space.red;
		final int localRed= space.fillable(entranceCell);
		--space.red;
		final int[] localSteps= { localBlack, localRed };
		for (int xye:exits) {
			Cell exitCell= Cell.at(xye);
			// space includes our entrance but not our exit node
			Space child= max_articulated_space(g, xye, territory, pID);
			// child includes our exit node
			int steps= child.fillable(exitCell);
			// direct steps
			int directSteps=territory==null?0:(territory[xye]&0x0FFF)-(territory[xy]&0x0FFF);
			
			if (child.front==0) steps+= localSteps[exitCell.id];
			else steps+= directSteps;
			if (steps>maxsteps) {
				maxsteps= steps;
				if (child.front==0) {
					maxspace= child.add(space);
				} else {
					maxspace= child.add(entranceCell, directSteps);
				}
			}
		}
		//System.out.println("maxSpace"+Grid.toXYString(xy)+"="+maxspace);
		return maxspace;
	}
	final static void explore_space(final Space result, final Grid g, final List<Integer> exits, final int v, final int[] territory, final int pID) {
		if ((g.num[v]&pID)==pID) return; // redundant; already explored
		if (Cell.at(v)==Cell.RED) ++result.red;
		else ++result.black;
		g.num[v]|= pID; // mark with pID
		if (g.articd[v]!=0) {
			// we're an articulation vertex; nothing to do but populate the exits
			for (Direction d : Direction.ALL) {
				int w= v+d.step;
				if (g.grid[w]!=0) continue;
				++result.edges;
				if (territory!=null&&(territory[w]&pID)!=pID) {
					result.front=1;
					continue;
				}
				if ((g.num[w]&pID)==pID) continue; // use 'num' from articulation vertex pass to mark nodes used
				if(!exits.contains(w))	exits.add(w);
			}
		} else {
			// this is a non-articulation vertex
			for (Direction d : Direction.ALL) {
				int w= v+d.step;
				if (g.grid[w]!=0) continue;
				++result.edges;
				// filter out nodes not in our territory region
				if (territory!=null&&(territory[w]&pID)!=pID) {
					result.front=1;
					continue;
				}
				if ((g.num[w]&pID)==pID) continue; // use 'num' from articulation vertex pass to mark nodes used
				if (g.articd[w]!=0) { // is this vertex articulated? thenadd it as an exit and don't traverse it yet
					if(!exits.contains(w))	exits.add(w);
				} else {
					Space s= new Space();
					explore_space(s, g, exits, w, territory, pID);
					result.add(s);
				}
			}
		}
	}
	static private ArrayDeque<Integer> current= new ArrayDeque<Integer>(40);
	static private ArrayDeque<Integer> next= new ArrayDeque<Integer>(40);
	static private ArrayDeque<Integer> tmp;
	final static Space exploreSpace(final Grid g, int xy, final List<Integer> exits, final short[] territory, final short pID) {
		// Rework pID marking
		Space s= new Space();
		current.clear();
		next.clear();
		next.add(xy);
		do {
			tmp= current;
			current=next;
			next=tmp;
			while(!current.isEmpty()) {
				xy=current.poll();
				if((g.num[xy]&pID)==pID) continue;
				s.add(xy);	       // Add cell
				g.num[xy]|=pID;    // Mark processed;
				if(g.articd[xy]!=0) {
					// Articulation => Populate exits
					for(Direction d: Direction.ALL) {
						int w=xy+d.step;
						if(g.grid[w]!=0) continue;  // Occupied
						++s.edges;
						if((g.num[w]&pID)!=0) continue; // Already explored
						if (territory!=null && (territory[w]&pID)!=pID) {
							// Battlefront => count it, mark it and skip
							++s.front;
							continue;
						}
						if(!exits.contains(w)) exits.add(w);
					}
				} else {
					// Not an articulation => explore
					for (Direction d : Direction.ALL) {
						int w= xy+d.step;
						if (g.grid[w]!=0) continue;		// Occupied
						++s.edges;						// Count edges
						if ((g.num[w]&pID)!=0) continue; // Already explored
						if (territory!=null&&(territory[w]&pID)!=pID) { // Battlefront => count it, and skip
							++s.front;
							continue;
						}
						if (g.articd[w]!=0) { // is this vertex articulated? then add it as an exit and don't traverse it yet
							if(!exits.contains(w)) exits.add(w);
						} else { // Add it for processing
							next.add(w);
						}
					}
				}
			}
		} while(!next.isEmpty());
		return s;
	}
	final static Space exploreSpace1(final Grid g, final int xy, final List<Integer> exits, final short[] territory, final short pID) {
		Space s= new Space();
		if ((g.num[xy]&pID)==pID) return s; // redundant; already explored
		s.add(xy);
		g.num[xy]|= pID; // mark with pID
		if (g.articd[xy]!=0) {
			// we're an articulation vertex; nothing to do but populate the
			// exits
			for (Direction d : Direction.ALL) {
				int w= xy+d.step;
				if (g.grid[w]!=0) continue;
				++s.edges;
				if ((g.num[w]&pID)==pID) continue; // use 'num' from articulation vertex pass to mark nodes used
				if (territory!=null&&(territory[w]&pID)!=pID) {
					++s.front;
					g.num[w]|=pID;
					continue;
				}
				exits.add(w);
			}
		} else {
			// this is a non-articulation vertex
			for (Direction d : Direction.ALL) {
				int w= xy+d.step;
				if (g.grid[w]!=0) continue;
				++s.edges;
				if ((g.num[w]&pID)==pID) continue; // use 'num' from articulation vertex pass to mark nodes used
				// filter out nodes not in our territory region
				if (territory!=null&&(territory[w]&pID)!=pID) {
					++s.front;
					g.num[w]|=pID;
					continue;
				}
				if (g.articd[w]!=0) { // is this vertex articulated? then add it as an exit and don't traverse it yet
					exits.add(w);
				} else {
					s.add(exploreSpace1(g, w, exits, territory, pID));
				}
			}
		}
		return s;
	}
	// Survival Strategy 
	final static public Move nextMoveSurvival(Move present, final Grid grid, final int player, final int maxDepth, final Watchdog dog) {
		// original grid must have been copied before calling this method as it will be messed up during processing
		int itr;
		int head= grid.head[player];
		int area= grid.fillableAreaAt(head);
		if(present.strategy!=Strategy.SURVIVAL) {
			present.reset(false);
			present.player= player;
			present.strategy= Strategy.SURVIVAL;
		}
		int maxitr=0;
		Move best= null;
		try {
		for (itr= 1; itr<=maxDepth; itr++) {
			if (dog.panic) break;
			Move m= spacefill(present, grid, player, itr, dog);
			maxitr= itr;
			if(m==null) continue;
			if (best==null || BEST_SURVIVAL_FIRST.compare(m,best)<0) {
				if(best!=null) best.dispose(false);
				best= Move.get(m);  // Deep copy of best eval so far TODO is it a good idea ???
			}
			if (best.value<=itr) {
				break; // we can't possibly search any deeper TODO ???
			}
			if (best.value>=area) {
				break; // solved!
			}
		}
		} catch(Timeout t) {
		}
		present.depth=maxitr;
		return best;
	}
	final static Move spacefill(final Move present, final Grid g, final int player, final int itr, final Watchdog dog) throws Timeout {
		dog.check();
		final int head= g.head[player];
		int degree=g.degree(head); 
		if (degree==0) {
			present.eval= 0;
			present.value= 0;
			present.strategy=Strategy.SURVIVAL;
			present.depth=itr;
			return null;
		}
		if (itr==0) {
			// We're evaluating present
			if(present.eval==Move.NaN) {
				present.degree= degree;
				present.eval= floodfill(g, head, dog);
			}
			present.depth=itr;
			present.value=present.eval;
			present.strategy=Strategy.SURVIVAL;
			return null;
		}
		if (present.future.size()==0) {
			// present has never been evaluated ==> populate future
			for (Direction d : Direction.ALL) {
				if(g.grid[head+d.step]!=0) continue;
				present.future.add(Move.get(player, d));
			}
		}
		Collections.sort(present.future, BEST_SURVIVAL_FIRST);		
		int spacesleft= g.fillableAreaAt(head);
		Move bestf= null;
		for (Move f : present.future) {
			f.move(g);
			spacefill(f, g, player, itr-1, dog);
			f.unmove(g);
			if (bestf==null || BEST_SURVIVAL_FIRST.compare(f, bestf)<0) {
				bestf= f;
			}
			if (bestf.value==spacesleft) break; // we solved it!
			dog.check();
		}
		assert(bestf!=null);
		present.value= (bestf==null?0:bestf.value);
		present.depth=itr;
		return bestf;
	}
	final static public int floodfill(final Grid g, final int xy, final Watchdog dog) throws Timeout {
		// flood fill heuristic: choose to remove as few edges from the graph as
		// possible (in other words, move onto the square with the lowest degree)
		dog.check();		
		int bestv= 0;
		int b= xy;
		for (Direction d : Direction.ALL) {
			int xyd= xy+d.step;
			if (g.grid[xyd]!=0) continue;
			int v= g.connectedValueAt(xyd)+g.fillableAreaAt(xyd)-2*g.degree(xyd)-4*g.isArticulation(xyd);
			if (v>bestv) {
				bestv= v;
				b= xyd;
			}
		}
		if (bestv==0) return 0;
		int a;
		g.grid[b]= 1; g.removeFromComponents(b);
		a= 1+floodfill(g, b, dog);
		g.grid[b]= 0; g.addToComponents(b);
		return a;
	}	
	static final public Comparator<Move> BEST_SURVIVAL_FIRST= new Comparator<Move>()  {
		@Override
		final public int compare(Move o1, Move o2) {
			// Compare values
			if(o1.value!=Move.NaN || o2.value!=Move.NaN) {
				if(o1.value==Move.NaN) return 1; // put not eval'd last
				if(o2.value==Move.NaN) return -1; // put not eval'd last
			}
			if(o1.value!=Move.NaN && o2.value!=Move.NaN) {
				if(o1.value>o2.value) return -1;
				if(o1.value<o2.value) return 1;
			}
			// --> Compare degree
			if(o1.degree==-1 && o2.degree==-1) return 0;
			if(o1.degree==-1) return 1;
			if(o2.degree==-1) return -1;
			if(o1.degree<o2.degree) return -1;  
			if(o1.degree>o2.degree) return 1;  
			return 0;
		}
	};	
	static final public Comparator<Move> BEST_TERRITORY_FIRST= new Comparator<Move>()  {
		@Override
		final public int compare(Move o1, Move o2) {
			// Compare values
			if(o1.value!=Move.NaN || o2.value!=Move.NaN) {
				if(o1.value==Move.NaN) return 1; // put not eval'd last
				if(o2.value==Move.NaN) return -1; // put not eval'd last
			}
			if(o1.value!=Move.NaN && o2.value!=Move.NaN) {
				if(o1.value>o2.value) return -1;
				if(o1.value<o2.value) return 1;
			}
			// --> Compare degree
			if(o1.degree==-1 && o2.degree==-1) return 0;
			if(o1.degree==-1) return 1;
			if(o2.degree==-1) return -1;
			if(o1.degree<o2.degree) return -1;  
			if(o1.degree>o2.degree) return 1;  
			return 0;
		}
	};
	static final public Comparator<Move> BEST_TERRITORY_LAST= new Comparator<Move>()  {
		@Override
		final public int compare(Move o1, Move o2) {
			// Compare values
			if(o1.value!=Move.NaN || o2.value!=Move.NaN) {
				if(o1.value==Move.NaN) return 1; // put null last
				if(o2.value==Move.NaN) return -1; // put null last
			}
			if(o1.value!=Move.NaN && o2.value!=Move.NaN) {
				if(o1.value>o2.value) return 1;
				if(o1.value<o2.value) return -1;
			}
			// o1.value and o2.value are null
			// --> compare degree
			if(o1.degree==-1 && o2.degree==-1) return 0;
			if(o1.degree==-1) return 1;
			if(o2.degree==-1) return -1;
			if(o1.degree<o2.degree) return  1;  
			if(o1.degree>o2.degree) return -1;  
			return 0;
		}
	};
	static final public Comparator<Move> BEST_FIGHT_FIRST= new Comparator<Move>()  {
		@Override
		final public int compare(Move o1, Move o2) {
			// Compare values
			if(o1.value!=Move.NaN || o2.value!=Move.NaN) {
				if(o1.value==Move.NaN) return 1; // put not eval'd last
				if(o2.value==Move.NaN) return -1; // put not eval'd last
			}			
			long v1= (o1.value>>(16*o1.player))&0xFFFF;
			long v2= (o2.value>>(16*o2.player))&0xFFFF;	
			if(v1>v2) return -1;
			if(v1<v2) return 1;

			v1=v2=0;
			for(int p=0; p<4; ++p) {
				v1+= ((o1.value>>(16*p))&0xFFFF);
				v2+= ((o2.value>>(16*p))&0xFFFF);
			}
			if(v1<v2) return -1;
			if(v1>v2) return 1;
			
//			long min1=Long.MAX_VALUE;
//			long min2=Long.MAX_VALUE;
//			for(int p=0; p<4; ++p) {
//				if(p==o1.player) continue;  // ignore our value
//				v1= (o1.value>>(16*p))&0xFFFF;
//				if(v1==0) continue;   // ignore non eval'd players ==> TODO Change this value to -1 / 0xFFFF
//				if(min1>v1) min1=v1;
//				
//				v2= (o2.value>>(16*p))&0xFFFF;
//				if(min2>v2) min2=v2;
//			}
//			if(min1<min2) return -1;
//			if(min1>min2) return 1;
			return 0;
		}
	};
}
final class Grid {
	static public final int PLAYGROUND_WIDTH= 30;
	static public final int PLAYGROUND_HEIGHT= 20;
	static public final int WIDTH= PLAYGROUND_WIDTH+2;
	static public final int HEIGHT= PLAYGROUND_HEIGHT+2;
	static public final int AREA= WIDTH*HEIGHT;
	static public final int FIRST= WIDTH+1;
	static public final int LAST= AREA-WIDTH-1;
	// Grid state
	public byte[] grid= new byte[AREA];
	// Players state
	public int nbPlayers;
	public int remainingPlayers;
	public boolean[] alive= new boolean[] { false, false, false, false };
	public int[] head= new int[4];
	public int player= -1;
	public int nbMoves;
	public LinkedList<LinkedList<Integer>> cycles= new LinkedList<>();
	public LinkedList<Integer> moves= new LinkedList<>();
	// Components
	public boolean   dirtyComponents=true;
	public short[]   components= new short[AREA];
	public short[]   cedges, red, black;
	// Territory
	public boolean dirtyTerritory=true;
	public short[] territory= new short[Grid.AREA];
	// Articulations
	public boolean dirtyArticulations=true;
	 public short   artiCounter=0;
	 public short[] low= new short[AREA];
	 public short[] num= new short[AREA];
	 public short[] articd= new short[AREA];
    // Statistics
    static int counter=0;
	public Grid() {
        this(0);
	}
	public Grid(int nbPlayers) {
		for (int x= 0; x<WIDTH; ++x) {
			grid[x]= 0x1;
			grid[(HEIGHT-1)*WIDTH+x]= 0x1;
		}
		for (int y= 0; y<HEIGHT; ++y) {
			grid[y*WIDTH]= 0x1;
			grid[y*WIDTH+WIDTH-1]= 0x1;
		}
		setNbPlayers(nbPlayers);
		++counter;
	}
	protected Grid(Grid src) {
	    copy(src);
		++counter;
	}
	final public Grid copy() {
		return new Grid(this);
    }
	final public void copy(Grid src) {
    	copy(src,false);
    }
	final public void copy(Grid src, boolean all) {
       // Copy grid state
		System.arraycopy(src.grid, 0, this.grid, 0, AREA);
		// Copy player's state
		nbPlayers= src.nbPlayers;
		remainingPlayers= src.remainingPlayers;
		System.arraycopy(src.alive, 0, this.alive, 0, 4);
		System.arraycopy(src.head, 0, this.head, 0, 4);
		player= src.player;
		cycles.clear();
		for (int p= 0; p<nbPlayers; ++p) {
			cycles.add(new LinkedList<Integer>(src.cycles.get(p)));
		}
		nbMoves= src.nbMoves;
		moves.clear();
		moves.addAll(src.moves);
		// Components
		dirtyComponents=!all || src.dirtyComponents;
		if(!dirtyComponents) {
			System.arraycopy(src.components, 0, this.components, 0, AREA);
			cedges= new short[src.cedges.length];
			System.arraycopy(src.cedges, 0, this.cedges, 0, src.cedges.length);
			red= new short[src.red.length];
			System.arraycopy(src.red, 0, this.red, 0, src.red.length);
			black= new short[src.black.length];
			System.arraycopy(src.black, 0, this.black, 0, src.black.length);
		}
		// Articulations
		dirtyArticulations= !all || src.dirtyArticulations;
		if(!dirtyArticulations) {
			this.artiCounter= src.artiCounter;
			System.arraycopy(src.num, 0, this.num, 0, Grid.AREA);
			System.arraycopy(src.low, 0, this.low, 0, Grid.AREA);
			System.arraycopy(src.articd, 0, this.articd, 0, Grid.AREA);
		}		
		// Territory
		dirtyTerritory= !all || src.dirtyTerritory;
		if(!dirtyTerritory) {
			System.arraycopy(src.territory, 0, this.territory, 0, Grid.AREA);
		}
    }
	final public void setNbPlayers(int nbPlayers) {
    	// We should maybe undo all previous player's moves !!!
		this.nbPlayers= nbPlayers;
		this.remainingPlayers= nbPlayers;
		for (int t= 0; t<4; ++t) {
			alive[t]= t<nbPlayers;
		}
		cycles.clear();
		for (int p= 0; p<nbPlayers; ++p) {
			cycles.add(new LinkedList<Integer>());
		}
		nbMoves=0;
		dirtyComponents=true;
		dirtyArticulations=true;
		dirtyTerritory=true;
    }   
	final public int nextPlayer() {
		return nextPlayer(player);
	}
	final public int nextPlayer(int p) {
		if (remainingPlayers<1) return -1;
		p= (p+1)%nbPlayers;
		while (!alive[p]) p= (p+1)%nbPlayers;
		return p;
	}
	final private void killPlayer(final int p) {
		if (alive[p]) {
			alive[p]= false;
			head[p]= 0;
			--remainingPlayers;
			for (int idx : cycles.get(p)) {
				grid[idx]= 0;
			}
			dirtyComponents=true;
		}
	}
	final private void resurectPlayer(final int p) {
		if (!alive[p]) {
			alive[p]= true;
			for (int idx : cycles.get(p)) {
				grid[idx]= 1;
			}
			head[p]= cycles.get(p).getLast();
			++remainingPlayers;
			dirtyComponents=true;
		}
	}
	final public boolean move(final int p, final int xy) {
		if (!alive[p]) return false;
		player=p;
		if (grid[xy]==0) {
			grid[xy]= 1;
			head[p]= xy;
			addToComponents(xy);
		} else {
			killPlayer(p);
		}
		moves.add(p);
		cycles.get(p).add(xy);
		++nbMoves;
		dirtyArticulations=true;
		dirtyTerritory=true;		
		return alive[p];
	}
	final public boolean move(final int p, final Direction d) {
		return move(p, head[p]+d.step);
	}
	final public boolean unmove() {
		if(nbMoves==0) return false;
		moves.removeLast();
		--nbMoves;
		cycles.get(player).removeLast();
		if(!alive[player]) {
			resurectPlayer(player);
		} else {
			int xy= head[player];
			grid[xy]= 0;
			removeFromComponents(xy);
			head[player]= cycles.get(player).isEmpty()?-1:cycles.get(player).getLast();
		}
		player=(moves.isEmpty()?-1:moves.getLast());
		dirtyArticulations=true;
		dirtyTerritory=true;
		return true;
	}
	final public void hideHeads() {
		for(int p=0; p<nbPlayers; ++p) {
			if(alive[p]) grid[head[p]]=0;
		}
	}
	final public void restoreHeads() {
		for(int p=0; p<nbPlayers; ++p) {
			if(alive[p]) grid[head[p]]=1;
		}
	}
	final public void hideHead(int p) {
		if(alive[p]) grid[head[p]]=0;
	}
	final public void restoreHead(int p) {
		if(alive[p]) grid[head[p]]=1;
	}
	final public boolean empty(final int idx) {
		return grid[idx]==0;
	}
	static public final int getX(final int xy) {
		return xy&0x1F;
	}
	static public final String toXYString(int xy) {
		return "("+getX(xy)+","+getY(xy)+")";
	}
	static public final int getXY(int x, int y) {
		return (y<<5)|x;
	}
	static public final int getY(final int xy) {
		return xy>>5;
	}
	final public int degree(final int idx) {
		return 4-grid[idx+Direction.LEFT.step]-grid[idx+Direction.RIGHT.step]-grid[idx+Direction.UP.step]-grid[idx+Direction.DOWN.step];
	}
	final public byte isArticulation(final int idx) {
		return articulation_map[neighborsMask(idx)];
	}
	final private int neighborsMask(final int idx) {
		return (grid[idx+Direction.UP.step+Direction.LEFT.step])|
				(grid[idx+Direction.UP.step])<<1|
				(grid[idx+Direction.UP.step+Direction.RIGHT.step])<<2|
				(grid[idx+Direction.RIGHT.step])<<3|
				(grid[idx+Direction.DOWN.step+Direction.RIGHT.step])<<4|
				(grid[idx+Direction.DOWN.step])<<5|
				(grid[idx+Direction.DOWN.step+Direction.LEFT.step])<<6|
				(grid[idx+Direction.LEFT.step])<<7;
	}
	private static final byte[] articulation_map= new byte[] { 0, 0, 0, 0, 0, 1, 0, 0, 0, 1, 0, 0, 0, 1, 0, 0, 0, 1, 1, 1, 1, 1, 1, 1, 0, 1, 0, 0, 0, 1, 0, 0, 0, 1, 1, 1, 1, 1, 1, 1,
			0, 1, 0, 0, 0, 1, 0, 0, 0, 1, 1, 1, 1, 1, 1, 1, 0, 1, 0, 0, 0, 1, 0, 0, 0, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,
			1, 1, 1, 1, 1, 0, 1, 1, 1, 1, 1, 1, 1, 0, 1, 0, 0, 0, 1, 0, 0, 0, 1, 1, 1, 1, 1, 1, 1, 0, 1, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 1, 1, 0, 0, 1, 1, 0, 0, 1, 1,
			0, 0, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 0, 0, 1, 1, 0, 0, 0, 0, 0, 0, 1, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
			0, 0, 0, 1, 1, 0, 0, 1, 1, 0, 0, 1, 1, 0, 0, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 0, 0, 1, 1, 0, 0, 0, 0, 0, 0, 1, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
			1, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 };
	
	// Components
	final public void resetComponents() {
		dirtyComponents=true;
	}
	final public void calculateComponents(final boolean force) {
		if(force) dirtyComponents=true;
		calculateComponents();
	}
	final public void calculateComponents() {
		if(!dirtyComponents) return;

		List<Short> equiv= new ArrayList<>();
		Arrays.fill(components, (short)0);
		equiv.add((short)0);
		short group=1;
		int idx= FIRST;
		for(idx= FIRST; idx<=LAST; ++idx) {
			if(grid[idx]!=0) continue;
			short up= equiv.get(components[idx+Direction.UP.step]);
			short left= equiv.get(components[idx+Direction.LEFT.step]);
			if(up==0 && left==0) {
				// new component;
				equiv.add(group);
				components[idx]=group++;
			} else if(up==left) {
				// same component
				components[idx]=up;
			} else {
				// Join group
				if(left==0 || (up!=0 && up< left)) {
					components[idx]=up;
					if(left!=0) mergeEquiv(equiv, left, up);
				} else {
					components[idx]= left;
					if(up!=0) mergeEquiv(equiv, up, left);
				}
			}
		}
		cedges= new short[equiv.size()];
		red= new short[equiv.size()];
		black= new short[equiv.size()];
		idx= FIRST;
		for(int y=1; y<HEIGHT-1; ++y, idx+=2) {
			for(int x=1; x<WIDTH-1; ++x, ++idx) {
				short e= equiv.get(components[idx]);
				components[idx]=e;
				cedges[e]+=degree(idx);
				if(Cell.at(x,y)==Cell.RED) ++red[e]; else ++black[e];
			}
		}
		
		dirtyComponents=false;
	}	
	final public void removeFromComponents(int idx) {
		removeFromComponents(idx, false);
	}
	final public void removeFromComponents(int idx, boolean forceRecalc) {
		if(dirtyComponents) {
			if(forceRecalc) calculateComponents();
			return;
		}
		components[idx] = 0;
		 
		 if(isArticulation(idx)==1) {
		      dirtyComponents=true;
		      if(forceRecalc) calculateComponents();
		 } else {
		     cedges[components[idx]] -= 2*degree(idx);
		      if(Cell.at(idx)==Cell.RED) --red[components[idx]]; else --black[components[idx]];
		 }
	}
	final public void addToComponents(int idx) {
		addToComponents(idx,false);
	}
	final public void addToComponents(int idx, boolean forceRecalc) {
		if(dirtyComponents) {
			if(forceRecalc) calculateComponents();
			return;
		}
		for (Direction d : Direction.ALL) {
			int r = idx+d.step;
			if (grid[r] != 0)	continue;
			if (components[idx] != 0 && components[idx] != components[r]) {
				dirtyComponents=true;
				if(forceRecalc) calculateComponents();
				return;
			}
			components[idx] = components[r];
		}
		cedges[components[idx]] += 2 * degree(idx);
		if (Cell.at(idx)==Cell.RED) ++red[components[idx]]; else ++black[components[idx]];
	}	
	final private void mergeEquiv(List<Short> equiv, short oldGroup, short newGroup) {
		for(int t=0; t<equiv.size(); ++t) {
			if(equiv.get(t)==oldGroup) equiv.set(t, newGroup);
		}
	}	
	final public int playerComponent(int p) {
		if(!alive[p]) return -1;
		return components[head[p]];
	}
	  // number of fillable squares starting from idx (not including idx)
	final public int fillableAreaAt(int idx) { 
		calculateComponents();
		return fillableArea(components[idx], Cell.at(idx)); 
	}	
	final public int connectedValueAt(int idx) { 
		calculateComponents();
		return cedges[components[idx]]; 
	}
	  // number of fillable squares in area when starting on 'startcolor' (assuming starting point is not included)
	final private int fillableArea(int component, Cell startColor) {
		  if(startColor==Cell.RED) { // start on red?  then moves are black-red-black-red-black (2 red, 3 black: 5; 3 red 3 black: 6; 4 red 3 black
			    return 2*(red[component]-1<black[component]? red[component]-1:black[component]) +
			      (black[component] >= red[component] ? 1 : 0);
			  } else { // moves are red-black-red-black-red
			    return 2*(red[component]<black[component]-1? red[component]:black[component]-1) +
			      (red[component] >= black[component] ? 1 : 0);
			  }
	  }	
	//	Articulations
	final public void resetArticulations() {
		artiCounter=0;
		Arrays.fill(low, (short)0);
		Arrays.fill(num, (short)0);
		Arrays.fill(articd, (short)0);
		dirtyArticulations=true;
	}
	final public int calculateArticulations(int xy) {
		return calculateArticulations(xy, null, (short)0, -1);
	}
	final public int calculateArticulations(int xy, short[] territory) {
		return calculateArticulations(xy, territory, (short)(territory==null?0:territory[xy]&0xF000), -1);
	}	
	final private int calculateArticulations(int xy, short[] territory, short playerID, int parent) {
		dirtyArticulations=false;
	  short nodenum = ++artiCounter;
	  low[xy] = num[xy] = nodenum; // rule 1
	  int children=0;
	  int count=0;
	  for(Direction d: Direction.ALL) {
	    int xyd= xy+d.step;
	    if(grid[xyd]!=0) continue;
	    
	    if(territory!=null && (territory[xyd]&playerID) != playerID) continue; // filter out nodes not in our voronoi region
	    if(num[xyd]==0) { // forward edge
	      children++;
	      count += calculateArticulations(xyd, territory, playerID, nodenum);
	      if(low[xyd] >= nodenum && parent != -1) {
	        articd[xy] = 1;
	        count++;
	      }
	      if(low[xyd] < low[xy]) low[xy] = low[xyd];   // rule 3
	    } else {
	      if(num[xyd] < nodenum) { // back edge
	        if(num[xyd] < low[xy]) low[xy] = num[xyd]; // rule 2
	      }
	    }
	  }
	  if(parent == -1 && children > 1) {
	    count++;
	    articd[xy] = 1;
	  }
	  return count;
	}	
	// Territory
	final public void resetTerritory() {
		Arrays.fill(territory, (short)(0x0FFF));
		dirtyTerritory=true;
	}
	final public void calculateTerritory(int p) {
		if(alive[p]) calculateTerritory(head[p], p);
	}
	static ArrayDeque<Integer> current= new ArrayDeque<Integer>(10);
	static ArrayDeque<Integer> next= new ArrayDeque<Integer>(10);
	static ArrayDeque<Integer> temp;
	final public void calculateTerritory(int xy, int player) {
		dirtyTerritory=false;

		short pID= (short) (1<<(12+player));
		
		next.add(xy);
		territory[xy]=pID;
		short dist=0;
		do {
			++dist;
			temp=current;
			current= next;
			next=temp;
			
			while(!current.isEmpty()) {
				xy= current.poll();
				for(Direction d: Direction.ALL) {
					int xyn= xy+d.step;
					if(grid[xyn]!=0) continue;
					int pdist= territory[xyn]&0x0FFF;
					if(pdist>dist) {
						// player's territory
						territory[xyn]=(short) (pID|dist);
						next.add(xyn);
					}
				}
			}
			
		} while(!next.isEmpty());
	}
}
enum Cell {
	BLACK(0), RED(1);

	final int id;
	private Cell(int id) {
		this.id=id;
	}
	
	static final public Cell at(final int idx) {
		return ((idx ^ (idx>>5)) & 1)==1?RED:BLACK;
	}
	
	static final public Cell at(final int x, final int y) {
		return ((x ^ y) & 1)==1?RED:BLACK;
	}
}
enum Direction {
	NONE(0), LEFT(-1), RIGHT(1), UP(-Grid.WIDTH), DOWN(Grid.WIDTH), ANY(0);
	static final Direction[] ALL= new Direction[] { LEFT, UP, RIGHT, DOWN };
	public final int step;
	private Direction(int delta) {
		this.step= delta;
	}
	static final Direction fromTo(int xy0, int xy1) {
		switch (xy1-xy0) {
		case -1:
			return LEFT;
		case 1:
			return RIGHT;
		case Grid.WIDTH:
			return DOWN;
		case -Grid.WIDTH:
			return UP;
		}
		return NONE;
	}
	static final Direction parse(String line) {
		if(line.equals("UP")) return UP;
		else if(line.equals("DOWN")) return DOWN;
		else if(line.equals("LEFT")) return LEFT;
		else if(line.equals("RIGHT")) return RIGHT;
		return NONE;
	}
}
enum Strategy {
	NONE,FIGHT,TERRITORY,SURVIVAL,ADAPTATIVE;
}
final class Move {
	static public final long NaN=Long.MAX_VALUE;
	static public final long MAX=NaN-1;
	static public final long MIN=-MAX;	
	public int 		   player=-1;
	public Direction[] dir= new Direction[] { Direction.ANY,Direction.ANY,Direction.ANY,Direction.ANY};
	public long eval= NaN;
	public long value= NaN;
	public int degree= -1;
	public int depth= 1;
	public Strategy strategy=Strategy.NONE;
	public MoveList future= new MoveList();
	public List<Integer> opponents;	
	private Move() {
	}
	private Move(int player, Direction dir) {
		this.player=player;
		this.dir[player]=dir;
	}
	final public int size() {
		int s= future.size();
		for(Move f:future) s+=f.size();
		return s;
	}
	final public void dispose() {
		dispose(true);
	}
	final public void dispose(boolean immediate) {
		if(immediate) {
			reset();
			synchronized(availablePool) {
			availablePool.add(this);
			}
		} else {
			pendingPool.add(this);
		}
	}	
	final public Move append(int player, Direction d) {
		this.player=this.player==-1?player:this.player;
		dir[player]=d;
		return this;
	}	
	final public boolean match(Move other) {
		for(int p=0; p<4; ++p) {
			if(dir[p]==Direction.ANY || other.dir[p]==Direction.ANY) continue;
			if(dir[p]!=other.dir[p]) return false;
		}
		return true;
	}	
	final public Grid move(Grid g) {
		if(player!=-1) {
			int p=player;
			do{
				if(dir[p]!=Direction.ANY) g.move(p, dir[p]);
			} while((p=(++p)%4)!=player);
		}
		return g;
	}
	final public Grid unmove(Grid g) {
		if(player!=-1) {
			for(int p=0; p<4; ++p) {
				// We assume that the previous move was this one
				if(dir[p]!=Direction.ANY) g.unmove();
			}
		}
		return g;
	}	
	final public void reset() {
		reset(true);
	}
	final public void reset(boolean immediate) {
		player=-1;
		for(int p=0; p<4; ++p) dir[p]= Direction.ANY;
		eval=NaN;
		value= NaN;
		degree=-1;
		depth=1;
		strategy=Strategy.NONE;
		if(immediate) future.clear();
		else while(!future.isEmpty()) future.remove().dispose(false);
		opponents=null;
	}	
	final public String toString() {
		if(player==-1) return "--";
		StringBuffer bf= new StringBuffer();
		boolean first=true;
		int p=player;
		do {
			if(dir[p%4].step!=0) {
				bf.append(first?"P":" P").append(p%4).append("-").append(dir[p%4]);
				first=false;
			}
		} while((++p%4)!=player);
		if(first) bf.append("--");
		else bf.append(" [ "+(eval==NaN?"-":eval)+" / "+(value==NaN?"-":value)+" ]");
		return bf.toString();
	}
	// Moves pool !!!
	static MoveList availablePool= new MoveList();
	static MoveList pendingPool= new MoveList();
	static final public Move get(Move src)  {
		Move m= get();
		System.arraycopy(src.dir, 0, m.dir, 0, 4);
		m.player=src.player;
		m.eval= src.eval;
		m.value= src.value;
		m.degree= src.degree;
		m.strategy= src.strategy;
		m.depth= src.depth;
		m.opponents= src.opponents;
		for(Move f: src.future) {
			m.future.add(Move.get(f));
		}
		return m;
	}
	static final public Move get(int player)  {
		return get(player,Direction.ANY);
	}
	static final public Move get(int player, Direction d)  {
		Move m= get();
		m.dir[player]=d;
		m.player=player;
		return m;
	}
	static final public Move get(int player, Direction[] dir)  {
		Move m= get();
		m.player=player;
		System.arraycopy(dir, 0, m.dir, 0, 4);
		return m;
	}
	static final public Move get() {
		synchronized(availablePool) {
		Move m;
		if(availablePool.isEmpty()) {
			m=new Move();
		} else {
			m=availablePool.remove();
		}
		return m;
		}
	}
	static public void flushPending() {
		synchronized(availablePool)  {
	    pendingPool.clear();
		}
	}
}
final class MoveList extends LinkedList<Move> {
	public final void clear() {
		while(!isEmpty()) remove().dispose();
	}
}
class Watchdog extends TimerTask {
	static public Watchdog getInfinite() {
		return new Watchdog() {
			@Override
			public void run() {}
			@Override
			public void check() {}
		};
	}
	static final public Watchdog getDefault() {
		return new Watchdog();
	}
	
	static final Timeout timeout= new Timeout();
	public boolean panic=false;
	@Override
	public void run() {
		panic=true;
	}
	public void check() throws Timeout {
		if(panic) throw timeout;
	}
}
final class Timeout extends Exception {
}
final class Space {
	int red, black, edges, front;
	Space() {}
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
	final Space add(Space o) {
		red+= o.red;
		black+= o.black;
		edges+= o.edges;
		front+= o.front;
		return this;
	}
	final Space add(int xy) {
		if(Cell.at(xy)==Cell.RED) 
			++red; 
		else 
			++black;
		return this;
	}
	final Space add(Cell startcell, int length) {
		final int half= length>>1;
		if (startcell==Cell.RED) {
			red+=length-half;
			black+=half;
		} else {
			black+=length-half;
			red+=half;
		}
		return this;
	}
	final int fillable(Cell startcell) {
		if(red==0 && black==0) return 0;
		if (startcell==Cell.RED) return 2*(red-1<black?red-1:black)+(black>=red?1:0);
		else return 2*(red<black-1?red:black-1)+(red>=black?1:0);
	}
}