

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.InputMismatchException;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Scanner;

public class Utils {

	static public void store(Grid g, String file) throws FileNotFoundException {
		store(g, new PrintStream(new FileOutputStream(file)));
	}

	static public void store(Grid g, File file) throws FileNotFoundException {
		store(g, new PrintStream(new FileOutputStream(file)));
	}

	static public void store(Grid g, PrintStream out) {

		out.println("GRID");
		for (int y= 0; y<Grid.PLAYGROUND_HEIGHT; ++y) {
			for (int x= 0; x<Grid.PLAYGROUND_WIDTH; ++x) {
				int xy= Grid.getXY(x+1, y+1);
				if (g.grid[xy]==0||getLightCycle(g, xy)>=0) out.print(".");
				else out.print("X");

			}
			out.println();
		}
		out.println("PLAYERS "+g.nbPlayers);
		out.println("CURRENT "+g.player);
		out.println("MOVES "+g.moves.toString());
		List<Iterator<Integer>> cyclesIt= new ArrayList<>(g.nbPlayers);
		for (int p= 0; p<g.nbPlayers; ++p) {
			if(g.alive[p]) out.println("P"+p+" "+g.cycles.get(p).toString());
		}
		out.flush();

	}

	static public void dump(Grid g) {
		dump(g, System.out);
	}

	static public void dump(Grid g, PrintStream out) {

		out.println("GRID moves="+g.moves.size());
		for (int y= 0; y<Grid.PLAYGROUND_HEIGHT; ++y) {
			for (int x= 0; x<Grid.PLAYGROUND_WIDTH; ++x) {
				int xy= Grid.getXY(x+1, y+1);
				int p= getLightCycle(g, xy);
				if (p>=0) {
					if (xy==g.cycles.get(p).getLast()) out.print("o");
					else out.print(p);
				} else if (g.grid[xy]==0) out.print(".");
				else out.print("X");

			}
			out.println();
		}
		out.flush();
	}

//	final public void dump(PrintStream out) {
//
//		out.println("GRID");
//		for (int y= 0; y<Grid.PLAYGROUND_HEIGHT; ++y) {
//			for (int x= 0; x<Grid.PLAYGROUND_WIDTH; ++x) {
//				int xy= Grid.getXY(x+1, y+1);
//				int p= getLightCycle(xy);
//				if (p>=0) {
//					if (xy==cycles.get(p).getLast()) out.print("o");
//					else out.print(p);
//				} else if (grid[xy]==0) out.print(".");
//				else out.print("X");
//
//			}
//			out.println();
//		}
//		out.println("nbPlayers "+nbPlayers);
//		out.println("remaining "+remainingPlayers);
//		out.println("heads "+Arrays.toString(head));
//		out.println("alive "+Arrays.toString(alive));
//		out.println("moves "+moves.toString());
//		for(int p=0; p<nbPlayers; ++p) {
//			out.println("P"+p+" "+cycles.get(p).toString());
//		}
////		List<Iterator<Integer>> itrs= new LinkedList<Iterator<Integer>>();
////		int[] xy0= new int[nbPlayers];
////		for(int p=0; p<nbPlayers; ++p) {
////			xy0[p]= cycles.get(p).getFirst();
////			itrs.add(cycles.get(p).iterator());
////		}
////		for(int p:moves) {
////			Iterator<Integer> itr= itrs.get(p);
////			if(itr.hasNext()) {
////				int xy1= itr.next();
////				Direction d= Direction.fromTo(xy0[p], xy1);
////				out.println("P"+p+" "+Grid.toXYString(xy1)+" "+d);
////				xy0[p]= xy1;
////			}
////			else out.println("Weird !!!: missing move for P"+p);
////		}
//	}
//	private int getLightCycle(int xy) {
//		for (int p= 0; p<nbPlayers; ++p) {
//			if (alive[p]&&cycles.get(p).contains(xy)) return p;
//		}
//		return -1;
//	}

	
	
	
	
	
	
	
	
	static public void dumpComponents(Grid g) {
		dumpComponents(g, System.out);
	}

	static public void dumpComponents(Grid g, PrintStream out) {
		out.println("COMPONENTS");
		for (int y= 0; y<Grid.PLAYGROUND_HEIGHT; ++y) {
			for (int x= 0; x<Grid.PLAYGROUND_WIDTH; ++x) {
				int xy= Grid.getXY(x+1, y+1);
				out.print(g.components[xy]);
				out.print(" ");

			}
			out.println();
		}
		out.flush();
	}

	static public void dumpTerritory(Grid g) {
		dumpTerritory(g, System.out);
	}

	static public void dumpTerritory(Grid g, PrintStream out) {
		out.println("TERRITORY");
		for (int y= 0; y<Grid.PLAYGROUND_HEIGHT; ++y) {
			for (int x= 0; x<Grid.PLAYGROUND_WIDTH; ++x) {
				int xy= Grid.getXY(x+1, y+1);
				if (g.territory[xy]==Integer.MAX_VALUE) out.print(".");
				else out.print(g.territory[xy]>>12);
			}
			out.println();
		}
		out.flush();
	}

	static public void dumpArticulations(Grid g) {
		dumpArticulations(g, System.out);
	}

	static public void dumpArticulations(Grid g, PrintStream out) {
		out.println("ARTICULATIONS");
		for (int y= 0; y<Grid.PLAYGROUND_HEIGHT; ++y) {
			for (int x= 0; x<Grid.PLAYGROUND_WIDTH; ++x) {
				int xy= Grid.getXY(x+1, y+1);
				if (g.articd[xy]==0) {
					if (g.grid[xy]==1) out.print("X");
					else out.print(".");
				} else {
					out.print("@");
				}
			}
			out.println();
		}
		out.flush();
	}

	static public void dumpNum(Grid g) {
		dumpNum(g, System.out);
	}

	static public void dumpNum(Grid g, PrintStream out) {
		out.println("NUM");
		for (int y= 0; y<Grid.PLAYGROUND_HEIGHT; ++y) {
			for (int x= 0; x<Grid.PLAYGROUND_WIDTH; ++x) {
				int xy= Grid.getXY(x+1, y+1);

				out.print(g.num[xy]>>12);
//				out.print(String.format("%02d ", g.num[xy]>>12));
			}
			out.println();
		}
		out.flush();
	}

	private static int getLightCycle(Grid g, int xy) {
		for (int p= 0; p<g.nbPlayers; ++p) {
			if (g.alive[p]&&g.cycles.get(p).contains(xy)) return p;
		}
		return -1;
	}

	static public Grid loadGrid(String dump) throws FileNotFoundException {
		return loadGrid(new Scanner(dump));
	}

	static public Grid loadGrid(File file) throws FileNotFoundException {
		return loadGrid(new Scanner(file));
	}

	static public Grid loadGrid(InputStream in) {
		return loadGrid(new Scanner(in));
	}
	static public Grid loadGrid(Scanner in) {

		try {
			String line= in.next("GRID");
			in.nextLine();
			Grid g= new Grid();
			for (int y= 0; y<Grid.PLAYGROUND_HEIGHT; ++y) {
				line= in.nextLine();
				for (int x= 0; x<Grid.PLAYGROUND_WIDTH; ++x) {
					int xy= Grid.getXY(x+1, y+1);
					char c= line.charAt(x);
					if (c=='.') g.grid[xy]= 0;
					else g.grid[xy]= 1;
				}
			}

			line= in.next("PLAYERS");
			int p= in.nextInt();
			g.setNbPlayers(p);
			in.nextLine();
			
			if(in.hasNext("CURRENT")) {
				// New format
				line= in.next("CURRENT");
				p= in.nextInt();
				g.player=p;
				in.nextLine();
				in.next("MOVES");
				in.useDelimiter(" \\[|, |]");
				while(in.hasNextInt()) {
					g.moves.add(in.nextInt());
				}
				in.reset();
				in.nextLine();
				for(p=0; p<g.nbPlayers; ++p) {
					LinkedList<Integer> cycle= g.cycles.get(p);
					in.next("P"+p);
					in.useDelimiter(" \\[|, |]");
					while(in.hasNextInt()) {
						final int xy=in.nextInt();
						g.head[p]=xy;
						g.grid[xy]=1;
						cycle.add(xy);
					}
					in.reset();
					in.nextLine();
				}
			} else {
				// Legacy format
				while (in.hasNext()) {
					p= in.nextInt();
					int x= in.nextInt();
					int y= in.nextInt();
					in.nextLine();
					int xy= Grid.getXY(x+1, y+1);
					g.move(p, xy);
				}
			}
			return g;
		} catch (InputMismatchException e) {
			e.printStackTrace();
		}
		return null;
	}

	static public short[] loadComponents(InputStream in) {
		return loadComponents(new Scanner(in));
	}
	static public short[] loadComponents(Scanner in) {
		short[] components= new short[Grid.AREA];
		
		try {
			in.next("COMPONENTS");
			in.nextLine();
			for (int y= 0; y<Grid.PLAYGROUND_HEIGHT; ++y) {
				for (int x= 0; x<Grid.PLAYGROUND_WIDTH; ++x) {
					int xy= Grid.getXY(x+1, y+1);
					short c= in.nextShort();
					components[xy]= c;
				}
				in.nextLine();
			}
			return components;
		} catch (InputMismatchException e) {
			e.printStackTrace();
		}
		return null;
	}
	
	
	
//	static public Game loadGame(Scanner in) {
//
//		try {
//			String line= in.next("GRID");
//			in.nextLine();
//			Game g= new Game();
//			for (int y= 0; y<Grid.PLAYGROUND_HEIGHT; ++y) {
//				line= in.nextLine();
//				for (int x= 0; x<Grid.PLAYGROUND_WIDTH; ++x) {
//					int xy= Grid.getXY(x+1, y+1);
//					char c= line.charAt(x);
//					if (c=='.') g.grid[xy]= 0;
//					else g.grid[xy]= 1;
//				}
//			}
//
//			line= in.next("PLAYERS");
//			int p= in.nextInt();
//			g.setNbPlayers(p);
//			in.nextLine();
//			while (in.hasNext()) {
//				p= in.nextInt();
//				int x= in.nextInt();
//				int y= in.nextInt();
//				in.nextLine();
//				int xy= Grid.getXY(x+1, y+1);
//				g.move(p, xy);
//			}
//			
//			
//			
//			return g;
//		} catch (InputMismatchException e) {
//			e.printStackTrace();
//		}
//		return null;
//	}
}
