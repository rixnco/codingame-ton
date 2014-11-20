import java.util.List;


public class Game {
	public Game() {
	}
	public Game(Grid g, List<PlayerInfo> playersInfo) {
		this.grid=g;
		this.playersInfo= playersInfo;
	}
	public Grid grid;
	public List<PlayerInfo> playersInfo;
}
