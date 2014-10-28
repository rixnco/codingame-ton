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

}
