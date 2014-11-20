import java.io.File;
import java.io.FileReader;
import java.io.IOException;


public class Grid2Game {
	public static void main(String[] args) throws IOException {
		Game game= GameUtils.loadGameV0(new FileReader("16134085.tron"));

		GameUtils.store(game, new File("16134085.game"));
		
		
		
		
		
	}
}
