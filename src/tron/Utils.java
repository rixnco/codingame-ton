package tron;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.InputMismatchException;
import java.util.Iterator;
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
		Iterator<Integer> movesIt= g.moves.iterator();
		List<Iterator<Integer>> cyclesIt= new ArrayList<>(g.nbPlayers);
		for (int p= 0; p<g.nbPlayers; ++p) {
			cyclesIt.add(g.cycles.get(p).iterator());
		}

		while (movesIt.hasNext()) {
			int p= movesIt.next();
			int xy= cyclesIt.get(p).next();
			int x= Grid.getX(xy)-1;
			int y= Grid.getY(xy)-1;
			out.println(p+" "+x+" "+y);
		}

		assert (movesIt.hasNext()==false);
		for (int p= 0; p<g.nbPlayers; ++p) {
			assert (cyclesIt.get(p).hasNext()==false);
		}

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
	}

	static public void dumpComponents(Grid g) {
		dumpComponents(g, System.out);
	}

	static public void dumpComponents(Grid g, PrintStream out) {
		out.println("COMPONENTS");
		for (int y= 0; y<Grid.PLAYGROUND_HEIGHT; ++y) {
			for (int x= 0; x<Grid.PLAYGROUND_WIDTH; ++x) {
				int xy= Grid.getXY(x+1, y+1);
				out.print(g.components[xy]);

			}
			out.println();
		}
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
				else out.print(g.territory[xy]>>16);
			}
			out.println();
		}
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
	}

	static public void dumpNum(Grid g) {
		dumpNum(g, System.out);
	}

	static public void dumpNum(Grid g, PrintStream out) {
		out.println("NUM");
		for (int y= 0; y<Grid.PLAYGROUND_HEIGHT; ++y) {
			for (int x= 0; x<Grid.PLAYGROUND_WIDTH; ++x) {
				int xy= Grid.getXY(x+1, y+1);

				out.print(String.format("%02d ", g.num[xy]));
			}
			out.println();
		}
	}

	private static int getLightCycle(Grid g, int xy) {
		for (int p= 0; p<g.nbPlayers; ++p) {
			if (g.alive(p)&&g.cycles.get(p).contains(xy)) return p;
		}
		return -1;
	}

	static public Grid loadGrid(String dump) throws FileNotFoundException {
		return loadGrid(new Scanner(dump));
	}

	static public Grid loadGrid(File file) throws FileNotFoundException {
		return loadGrid(new Scanner(file));
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
			while (in.hasNext()) {
				p= in.nextInt();
				int x= in.nextInt();
				int y= in.nextInt();
				in.nextLine();
				int xy= Grid.getXY(x+1, y+1);
				g.move(p, xy);
			}
			return g;
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
