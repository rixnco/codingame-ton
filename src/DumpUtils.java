

import java.io.PrintStream;
import java.io.PrintWriter;

public class DumpUtils {

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

	static public void dump(Grid g, PrintWriter out) {

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

}
