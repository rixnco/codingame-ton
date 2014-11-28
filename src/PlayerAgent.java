

interface PlayerExecutor {
	Move nextMove(Grid g, int player);
}

public class PlayerAgent implements PlayerExecutor {
	public String name;
	public int    rank;
	public float  score;
	public String language;

	public PlayerExecutor executor; 
	
	
	public PlayerAgent() {}
	public PlayerAgent(String name) {
		this.name= name;
		this.language="Java";
	}
	
	public Move nextMove(Grid g, int player) {
		if(executor==null) return null;
		return executor.nextMove(g, player);
	}
	
	
}
