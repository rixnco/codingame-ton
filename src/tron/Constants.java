package tron;

import java.io.File;


public class Constants {
	// TODO:
	// - remove tryfinally (useless as we're working on a copy of the original grid
	// - replace minimax with negamax
	// - add processing of all opponents in minamax
	// - remove tab string
	
	
//	static final File DEFAULT_GAME=new File("./grids/game-20141023-041234.tron");
	public static final File DEFAULT_GAME=new File("./grids/default-tron-3P.tron");
	
	public static final boolean DEBUG= false;

	public static final boolean DEBUG_GRID= false;

	public static final boolean DEBUG_TERRITORY= false;

	public static final boolean DEBUG_TERRITORY_MINIMAX= true;

	public static final int MAX= 100000000;
	
	public static final int MIN= -MAX;
	
}
