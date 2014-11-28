import java.util.List;


public class Game {
	public Game() {
	}
	public Game(Grid g, List<PlayerAgent> playerAgents) {
		this.grid=g;
		this.playerAgents= playerAgents;
	}
	public Grid grid;
	public List<PlayerAgent> playerAgents;
}
