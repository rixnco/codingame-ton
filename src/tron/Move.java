package tron;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedList;

import tron.Grid.Direction;

public class Move {
	
	public int 		 player=-1;
	public Direction[] dir= new Direction[] { Direction.NONE,Direction.NONE,Direction.NONE,Direction.NONE};
	public Integer eval= null;
	public Integer value= null;
	public Grid grid;

	public LinkedList<Move> future= new LinkedList<Move>();
	
	public Move() {
	}

	public Move(Move src) {
		if(src==null) return;
		player=src.player;
		System.arraycopy(src.dir, 0, dir, 0, 4);
		eval= src.eval;
		value= src.value;
//		grid= src.grid==null?null:src.grid.copy();
		future.addAll(src.future);
	}

//	public Move(Grid g) {
//		grid= g;
//	}

	public Move(int player, Direction dir) {
		this.player=player;
		this.dir[player]=dir;
	}
	
//	public Move(int player, Direction dir, int value) {
//		this.player= player;
//		this.dir=dir;
//		this.value= eval;
//	}

	public Move copy() {
		Move r= new Move(this);
		return r;
	}
	
	public Move append(int player, Direction d) {
		if(dir[player]!=Direction.NONE) return this; // Should we throw an exception ??

		this.player=this.player==-1?player:this.player;
		dir[player]=d;
		return this;
	}
	
	
	public Grid move(Grid g) {
		if(player!=-1) {
			int p=player;
			do{
				if(dir[p]!=Direction.NONE) g.move(p, dir[p]);
			} while((p=++p%4)!=player);
		}
		return g;
	}
	
	public Grid unmove(Grid g) {
		if(player!=-1) {
			for(int p=0; p<4; ++p) {
				// We assume that the previous move was this one
				if(dir[p]!=Direction.NONE) g.unmove();
			}
		}
		return g;
	}
	
//	public int nextPlayer() {
//		return grid==null?-1:grid.nextPlayer();
//	}
//	
//	public int previousPlayer() {
//		return grid==null?-1:grid.previousPlayer();
//	}
	
	public void reset() {
		Arrays.fill(dir, Direction.NONE);
		eval=null;
		value= null;
//		grid= null;
		future.clear();
	}
	
	public void addFuture(Move f) {
		future.add(f);
	}

	public String toString() {
		StringBuffer bf= new StringBuffer();
		boolean first=true;
		for(int p=0; p<4; ++p) {
			if(dir[p]!=Direction.NONE) {
				bf.append(first?"P":" P").append(p).append("-").append(dir[p]);
				first=false;
			}
		}
		if(first) bf.append("--");
		else bf.append(" [ "+(eval==null?"-":eval)+" / "+(value==null?"-":value)+" ]");
		return bf.toString();
	}
	
	
	static public Comparator<Move> BEST_FIRST= new Comparator<Move>()  {
		@Override
		public int compare(Move o1, Move o2) {
			if(o1.value==null && o2.value==null) return 0;
			if(o1.value==null) return 1; // put null last
			if(o2.value==null) return -1; // put null last
			if(o1.value>o2.value) return 1;
			if(o2.value<o1.value) return -1;
			return 0;
		}
	};
	
	static public Comparator<Move> BEST_LAST= new Comparator<Move>()  {
		@Override
		public int compare(Move o1, Move o2) {
			if(o1.value==null && o2.value==null) return 0;
			if(o1.value==null) return 1; // put null last
			if(o2.value==null) return -1; // put null last
			if(o1.value>o2.value) return -1;
			if(o2.value<o1.value) return 1;
			return 0;
		}
	};
	
}
