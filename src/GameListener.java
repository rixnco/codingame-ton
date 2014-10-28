import tron.Grid;


public interface GameListener {

	void gameInitialized(Grid state);

	void playerMoved(Grid state, int p);

	void gameTerminated(Grid state);

}
