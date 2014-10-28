package tron;


import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;


public class Grid {

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
	public int[]   components= new int[AREA];
	public int[]   cedges, red, black;
	public boolean dirtyComponents=true;
	
	// Territory
	public boolean dirtyTerritory=true;
	public int[] territory= new int[Grid.AREA];

	// Articulations
	public boolean dirtyArticulations=true;
	 public int   counter=0;
	 public int[] low= new int[Grid.AREA];
	 public int[] num= new int[Grid.AREA];
	 public int[] articd= new int[Grid.AREA];

	
	 public Grid() {
		 this(4);
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
	}

	public void setNbPlayers(int nbPlayers) {
		this.nbPlayers= nbPlayers;
		this.remainingPlayers= nbPlayers;
		for (int t= 0; t<nbPlayers; ++t) {
			alive[t]= true;
		}
		for (int p= 0; p<nbPlayers; ++p) {
			cycles.add(new LinkedList<Integer>());
		}
		nbMoves=0;
		player=-1;
	}
	
	
	protected Grid(Grid src) {
		// Copy grid state
		System.arraycopy(src.grid, 0, this.grid, 0, AREA);

		// Copy player's state
		nbPlayers= src.nbPlayers;
		remainingPlayers= src.remainingPlayers;
		System.arraycopy(src.alive, 0, this.alive, 0, 4);
		System.arraycopy(src.head, 0, this.head, 0, 4);
		player= src.player;

		for (int p= 0; p<nbPlayers; ++p) {
			cycles.add(new LinkedList<Integer>(src.cycles.get(p)));
		}
		nbMoves= src.nbMoves;
		moves.addAll(src.moves);
		
		// Components
		if(!src.dirtyComponents) {
			dirtyComponents=false;
			System.arraycopy(src.components, 0, this.components, 0, AREA);
			cedges= new int[src.cedges.length];
			System.arraycopy(src.cedges, 0, this.cedges, 0, src.cedges.length);
			red= new int[src.red.length];
			System.arraycopy(src.red, 0, this.red, 0, src.red.length);
			black= new int[src.black.length];
			System.arraycopy(src.black, 0, this.black, 0, src.black.length);
		}
		
		// Articulations
		if(!src.dirtyArticulations) {
			dirtyArticulations=false;
			this.counter= src.counter;
			System.arraycopy(src.num, 0, this.num, 0, Grid.AREA);
			System.arraycopy(src.low, 0, this.low, 0, Grid.AREA);
			System.arraycopy(src.articd, 0, this.articd, 0, Grid.AREA);
		}		
		// Territory
		if(!src.dirtyTerritory) {
			dirtyTerritory=false;
			System.arraycopy(src.territory, 0, this.territory, 0, Grid.AREA);
		}
	}

	public Grid copy() {
		return new Grid(this);
	}

	public int nextPlayer() {
		if (remainingPlayers<=0) return -1;
		int p= (this.player+1)%nbPlayers;
		while (!alive[p])
			p= (p+1)%nbPlayers;
		return p;
	}

	public int nextPlayer(int p) {
		if (remainingPlayers<=0) return -1;
		p= (p+1)%nbPlayers;
		while (!alive[p])
			p= (p+1)%nbPlayers;
		return p;
	}

	public int previousPlayer() {
		if (remainingPlayers<=0) return -1;
		int p= (this.player-1)%nbPlayers;
		while (!alive[p])
			p= (p-1)%nbPlayers;
		return p;
	}

	public int previousPlayer(int p) {
		if (remainingPlayers<=1) return -1;
		p= (p+nbPlayers-1)%nbPlayers;
		while (!alive[p])
			p= (p+nbPlayers-1)%nbPlayers;
		return p;
	}

	private void killPlayer(final int p) {
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

	private void resurectPlayer(final int p) {
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

	public boolean empty(int xy, final Direction d) {
		return grid[xy+d.step]==0;
	}
	
	public boolean move(final int p, final int xy) {
		if (!alive[p]) return false;

		player=p;
		moves.add(p);
		++nbMoves;

		if (grid[xy]==0) {
			grid[xy]= 1;
			head[p]= xy;
			cycles.get(p).add(xy);
			addToComponents(xy);
			if(Constants.DEBUG_GRID) {
				System.out.println("move P"+player+" ("+Grid.getX(xy)+","+Grid.getY(xy)+")");
				Utils.dump(this);
			}
		} else {
			killPlayer(p);
			if(Constants.DEBUG_GRID) {
				System.out.println("move P"+player+" ("+Grid.getX(xy)+","+Grid.getY(xy)+") -> killed");
				Utils.dump(this);
			}
		}

		dirtyArticulations=true;
		dirtyTerritory=true;
		
		return alive[p];
	}

	public boolean move(final int p, final Direction d) {
		return move(p, head[p]+d.step);
	}

	public boolean unmove() {
		if(nbMoves==0) return false;
		
		if(!alive[player]) {
			if(Constants.DEBUG_GRID) {
				System.out.println("unmove P"+player+" ("+Grid.getX(head[player])+","+Grid.getY(head[player])+") -> resurected");
				Utils.dump(this);
			}
			resurectPlayer(player);
		} else {
			int xy= head[player];
			grid[xy]= 0;
			removeFromComponents(xy);
			cycles.get(player).removeLast();
			head[player]= cycles.get(player).isEmpty()?-1:cycles.get(player).getLast();
			if(Constants.DEBUG_GRID) {
				System.out.println("unmove P"+player+" ("+Grid.getX(head[player])+","+Grid.getY(head[player])+")");
				Utils.dump(this);
			}
		}

		moves.removeLast();
		player=(moves.isEmpty()?-1:moves.getLast());
		--nbMoves;
		dirtyArticulations=true;
		dirtyTerritory=true;
		return true;
	}
	
	public void hideHeads() {
		for(int p=0; p<nbPlayers; ++p) {
			if(alive[p]) grid[head[p]]=0;
		}
	}

	public void restoreHeads() {
		for(int p=0; p<nbPlayers; ++p) {
			if(alive[p]) grid[head[p]]=1;
		}
	}
	

	final public boolean alive(final int p) {
		return alive[p];
	}

	final public boolean empty(final int idx) {
		return grid[idx]==0;
	}

	static public final int getX(final int xy) {
		return xy%WIDTH;
	}
	
	static public final String toXYString(int xy) {
		return "("+getX(xy)+","+getY(xy)+")";
	}
	
	static public final int getXY(int x, int y) {
		return y*WIDTH+x;
	}

	static public int getY(final int xy) {
		return xy/WIDTH;
	}

	public final int head(final int p) {
		return (alive[p]?head[p]:0);
	}

	public int degree(final int idx) {
		return 4-grid[idx+Direction.LEFT.step]-grid[idx+Direction.RIGHT.step]-grid[idx+Direction.UP.step]-grid[idx+Direction.DOWN.step];
	}

	public byte isArticulation(final int idx) {
		return articulation_map[neighborsMask(idx)];
	}

	private int neighborsMask(final int idx) {
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
	public void resetComponents() {
		dirtyComponents=true;
	}
	public void calculateComponents(boolean force) {
		if(force) dirtyComponents=true;
		calculateComponents();
	}
	public void calculateComponents() {
		if(!dirtyComponents) return;

		// Remove player's head before computing connected components
		hideHeads();
		
		List<Integer> equiv= new ArrayList<Integer>();
		Arrays.fill(components, 0);
		equiv.add(0);
		int group=1;
		int idx= FIRST;
		for(idx= FIRST; idx<=LAST; ++idx) {
			if(grid[idx]!=0) continue;
			int up= equiv.get(components[idx+Direction.UP.step]);
			int left= equiv.get(components[idx+Direction.LEFT.step]);
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
		cedges= new int[equiv.size()];
		red= new int[equiv.size()];
		black= new int[equiv.size()];
		idx= FIRST;
		for(int y=1; y<HEIGHT-1; ++y, idx+=2) {
			for(int x=1; x<WIDTH-1; ++x, ++idx) {
				int e= equiv.get(components[idx]);
				components[idx]=e;
				cedges[e]+=degree(idx);
				if(Color.at(x,y)==Color.RED) ++red[e]; else ++black[e];
			}
		}
		
		// Restore player's head after computing connected components
		restoreHeads();
		dirtyComponents=false;
	}
	
	public void removeFromComponents(int idx) {
		removeFromComponents(idx, false);
	}
	public void removeFromComponents(int idx, boolean forceRecalc) {
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
		      if(Color.at(idx)==Color.RED) --red[components[idx]]; else --black[components[idx]];
		 }
	}

	public void addToComponents(int idx) {
		addToComponents(idx,false);
	}
	public void addToComponents(int idx, boolean forceRecalc) {
		if(dirtyComponents) {
			if(forceRecalc) calculateComponents();
			return;
		}
		for (Direction d : Direction.ALL) {
			int r = d.move(idx);
			if (grid[r] != 0)	continue;
			if (components[idx] != 0 && components[idx] != components[r]) {
				dirtyComponents=true;
				if(forceRecalc) calculateComponents();
				return;
			}
			components[idx] = components[r];
		}
		cedges[components[idx]] += 2 * degree(idx);
		if (Color.at(idx)==Color.RED) ++red[components[idx]]; else ++black[components[idx]];
	}
	
	private void mergeEquiv(List<Integer> equiv, int oldGroup, int newGroup) {
		for(int t=0; t<equiv.size(); ++t) {
			if(equiv.get(t)==oldGroup) equiv.set(t, newGroup);
		}
	}

	
	public int playerComponent(int p) {
		if(!alive[p]) return -1;
		return components[head[p]];
	}

	  // number of fillable squares starting from idx (not including idx)
	public int fillableAreaAt(int idx) { 
		calculateComponents();
		return fillableArea(components[idx], Color.at(idx)); 
	}
	
	public int getPlayerFillableArea(int p) {
		if(!alive[p]) return 0;
		return fillableAreaAt(head[p]);
	}
	
	public int connectedValueAt(int idx) { 
		calculateComponents();
		return cedges[components[idx]]; 
	}
	
	
	  // number of fillable squares in area when starting on 'startcolor' (assuming starting point is not included)
	private int fillableArea(int component, Color startColor) {
		  if(startColor==Color.RED) { // start on red?  then moves are black-red-black-red-black (2 red, 3 black: 5; 3 red 3 black: 6; 4 red 3 black
			    return 2*(red[component]-1<black[component]? red[component]-1:black[component]) +
			      (black[component] >= red[component] ? 1 : 0);
			  } else { // moves are red-black-red-black-red
			    return 2*(red[component]<black[component]-1? red[component]:black[component]-1) +
			      (red[component] >= black[component] ? 1 : 0);
			  }
	  }
	  
	
	//	Articulations
	public void resetArticulations() {
		counter=0;
		Arrays.fill(low, 0);
		Arrays.fill(num, 0);
		Arrays.fill(articd, 0);
		dirtyArticulations=true;
	}

	// calculate articulation vertices within our voronoi region
	// algorithm taken from http://www.eecs.wsu.edu/~holder/courses/CptS223/spr08/slides/graphapps.pdf
	// DFS traversal of graph
	public int calculateArticulations(int xy) {
		return calculateArticulations(xy, null, 0, -1);
	}
	public int calculateArticulations(int xy, int[] territory) {
		return calculateArticulations(xy, territory, territory==null?0:territory[xy]&0xFF0000, -1);
	}
	
	private int calculateArticulations(int xy, int[] territory, int playerID, int parent)
	{
		dirtyArticulations=false;
	  int nodenum = ++counter;
	  low[xy] = num[xy] = nodenum; // rule 1
	  int children=0;
	  int count=0;
	  for(Direction d: Direction.ALL) {
	    int xyd = d.move(xy);
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
	public void resetTerritory() {
		Arrays.fill(territory, Short.MAX_VALUE);
		dirtyTerritory=true;
	}
	
	public void calculateTerritories() {
		resetTerritory();
		for(int p=0; p<nbPlayers; ++p) {
			if(alive[p]) calculateTerritory(head[p], p);
		}
	}
	public void calculateTerritory(int xy, int player) {
		assert(xy>=Grid.FIRST && xy<=Grid.LAST);
		dirtyTerritory=false;

		ArrayDeque<Integer> current= new ArrayDeque<Integer>(100);
		ArrayDeque<Integer> next= new ArrayDeque<Integer>(100);
		ArrayDeque<Integer> temp;
		
		int pID= 1<<(16+player);
		
		next.add(xy);
		territory[xy]=pID;
		int dist=0;
		do {
			++dist;
			temp=current;
			current= next;
			next=temp;
			
			while(!current.isEmpty()) {
				xy= current.poll();
				assert(territory[xy]==dist);
				for(Direction d: Direction.ALL) {
					int xyn= d.move(xy);
					if(grid[xyn]!=0) continue;
					int pdist= territory[xyn]&0xFFFF;
					if(pdist>dist) {
						// player's territory
						territory[xyn]=dist|pID;
						next.add(xyn);
					} else if(pdist==dist ) {
						// battlefront
						territory[xyn]|= pID;
					}
				}
			}
			
		} while(!next.isEmpty());
		assert(current.isEmpty());
		assert(next.isEmpty());
	}
	
	
	static public enum Color {
		RED(0), BLACK(1);

		final int id;
		private Color(int id) {
			this.id=id;
		}
		
		static public Color at(final int idx) {
			return at(Grid.getX(idx), Grid.getY(idx));
		}
		
		static public Color at(final int x, final int y) {
			return ((x ^ y) & 1)==1?RED:BLACK;
		}
	}
	
	
	static public enum Direction {
		NONE(0), LEFT(-1), RIGHT(1), UP(-Grid.WIDTH), DOWN(Grid.WIDTH);

		static final Direction[] ALL= new Direction[] { LEFT, UP, RIGHT, DOWN };

		public final int step;

		private Direction(int delta) {
			this.step= delta;
		}

		public int move(int xy) {
			return xy==0?0:xy+step;
		}

		static Direction fromTo(int xy0, int xy1) {
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

		public static Direction parse(String line) {
			if(line.equals("UP")) return UP;
			else if(line.equals("DOWN")) return DOWN;
			else if(line.equals("LEFT")) return LEFT;
			else if(line.equals("RIGHT")) return RIGHT;
			return NONE;
		}
	}
}
