package tron;


public interface Strategy {
	public Move nextMove(final Move present, final Grid g, final int player, int maxDepth, Watchdog watchdog);
	public Integer evaluate(final Grid g, final int player);
	
}
