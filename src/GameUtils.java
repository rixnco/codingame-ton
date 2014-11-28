

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.PushbackReader;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

public class GameUtils {

	static public void store(Game game, String file) throws IOException {
		store(game, new PrintWriter(new FileOutputStream(file)));
	}
	
	static public void store(Game game, File file) throws IOException {
		store(game, new PrintWriter(new FileOutputStream(file)));
	}
	
	static public void store(Game game, PrintStream out) throws IOException {
		store(game, new PrintWriter(out));
	}	
	
	static public void store(Game game, PrintWriter out) throws IOException {
		store(game.grid, game.playerAgents, out);
	}

	
	@SuppressWarnings("unchecked")
	static private void store(Grid g, List<PlayerAgent> players, PrintWriter out) throws IOException {
		
		JSONObject game= new JSONObject();
		game.put("version", 1);
		game.put("nbPlayers", g.nbPlayers);
		game.put("current", g.player);
		JSONArray jsonMoves=new JSONArray();
		
		List<Iterator<Integer>> itrs= new ArrayList<>(4);
		for(int p=0; p<g.nbPlayers; ++p) {
			itrs.add( g.cycles.get(p).iterator() );
		}
		for(int m:g.moves) {
			int xy= itrs.get(m).next();
			JSONArray move= new JSONArray();
			move.add(m);
			move.add(Grid.getX(xy)-1);
			move.add(Grid.getY(xy)-1);
			jsonMoves.add(move);
		}
		game.put("moves", jsonMoves);

		for(Iterator<Integer> itr: itrs) assert(!itr.hasNext());
		
		JSONArray jsonPlayers= new JSONArray();
		for(int p=0; p<g.nbPlayers; ++p) {
			JSONObject jsonPlayer= new JSONObject();
			 if(players!=null && players.size()>p) {
				PlayerAgent player= players.get(p);
				jsonPlayer.put("name", player.name);
				jsonPlayer.put("rank", player.rank);
				jsonPlayer.put("score", player.score);
				jsonPlayer.put("language", player.language);
			} else {
				jsonPlayer.put("name", "P"+p);
				jsonPlayer.put("rank", 0);
				jsonPlayer.put("score", 0.0);
				jsonPlayer.put("language", "Java");
				
			}
			jsonPlayers.add(jsonPlayer);
		}
		game.put("players", jsonPlayers);

		StringBuffer line;
		JSONArray jsonGrid= new JSONArray();
		for (int y= 0; y<Grid.PLAYGROUND_HEIGHT; ++y) {
			line= new StringBuffer();
			for (int x= 0; x<Grid.PLAYGROUND_WIDTH; ++x) {
				int xy= Grid.getXY(x+1, y+1);
				if (g.grid[xy]==0||getLightCycle(g, xy)>=0) line.append(".");
				else line.append("X");
			}
			jsonGrid.add(line.toString());
		}
		game.put("grid", jsonGrid);

		game.writeJSONString(out);
		
		out.flush();
	}
	
	@SuppressWarnings("unchecked")
	static private void storeV0(Grid g, List<PlayerAgent> players, PrintWriter out) throws IOException {
		
		JSONObject game= new JSONObject();
		game.put("nbPlayers", g.nbPlayers);
		game.put("current", g.player);
		game.put("moves", new JSONArray(g.moves));
		JSONArray jsonPlayers= new JSONArray();
		for(int p=0; p<g.nbPlayers; ++p) {
			JSONObject jsonPlayer= new JSONObject();
			if(players!=null && players.size()>p) {
				PlayerAgent player= players.get(p);
				jsonPlayer.put("name", player.name);
				jsonPlayer.put("rank", player.rank);
				jsonPlayer.put("score", player.score);
				jsonPlayer.put("language", player.language);
			}
			jsonPlayer.put("alive", g.alive[p]);
			jsonPlayer.put("cycles", new JSONArray(g.cycles.get(p)));
			
			jsonPlayers.add(jsonPlayer);
		}
		game.put("players", jsonPlayers);
		StringWriter grid= new StringWriter();
//		out.println("GRID");
		for (int y= 0; y<Grid.PLAYGROUND_HEIGHT; ++y) {
			for (int x= 0; x<Grid.PLAYGROUND_WIDTH; ++x) {
				int xy= Grid.getXY(x+1, y+1);
				if (g.grid[xy]==0||getLightCycle(g, xy)>=0) grid.append(".");
				else grid.append("X");

			}
			grid.append("\n");
		}
		grid.flush();
		game.put("grid", grid.toString());

		game.writeJSONString(out);
		
		
	}
	private static int getLightCycle(Grid g, int xy) {
		for (int p= 0; p<g.nbPlayers; ++p) {
			if (g.alive[p]&&g.cycles.get(p).contains(xy)) return p;
		}
		return -1;
	}

	static public Game loadGame(String dump) throws IOException  {
		return loadGame(new StringReader(dump));
	}
	static public Game loadGame(File file) throws IOException  {
		return loadGame(new FileReader(file));
	}
	static public Game loadGame(InputStream in) throws IOException {
		return loadGame(new InputStreamReader(in));
	}	
	static public Game loadGameV0(Reader in) throws IOException {
		JSONParser parser= new JSONParser();
		JSONObject jsonGame;
		PushbackReader bin= new PushbackReader(in);
		int c;
		// Skip header comments
		while((c=bin.read())=='#') {
			while((c=bin.read())!='\r' && c!='\n');
			while((c=bin.read())=='\r' || c=='\n');
			if(c!=-1) bin.unread(c);
		}
		if(c!=-1) bin.unread(c);
		
		try {
			jsonGame= (JSONObject) parser.parse(bin);
		} catch (ParseException e) {
			throw new IOException("Failed to parse input", e);
		}
		
		if(jsonGame.containsKey("version")) throw new IOException("Invalid format");
		
		Grid g= new Grid(((Long)jsonGame.get("nbPlayers")).intValue());
		g.player= ((Long) jsonGame.get("current")).intValue();
		Object o= jsonGame.get("moves");
		if(!(o instanceof JSONArray) || ((JSONArray)o).size()==0) throw new IOException("Invalid format");
		o= ((JSONArray)o).get(0);
		if(!(o instanceof Long)) throw new IOException("Invalid format");
		for(long p: (Collection<Long>)jsonGame.get("moves")) {
			g.moves.add((int)p);
		}
		g.nbMoves= g.moves.size();
		List<PlayerAgent> playersInfo= new ArrayList<>(g.nbPlayers);
		
		JSONArray players= (JSONArray)jsonGame.get("players");
		for(int p=0; p<g.nbPlayers; ++p) {
			PlayerAgent playerInfo= new PlayerAgent();
			JSONObject player= (JSONObject) players.get(p);
			g.alive[p]= (Boolean) player.get("alive");
			g.remainingPlayers-=(g.alive[p]?0:1);
			List<Integer> cycles=g.cycles.get(p); 
			for(long xy: (Collection<Long>)player.get("cycles")) {
				g.head[p]= (int)xy;
				if(g.alive[p]) g.grid[(int)xy]= 1;
				cycles.add((int)xy);
			}
			playerInfo.name= player.containsKey("name")?(String)player.get("name"):"Player"+p;
			playerInfo.rank= player.containsKey("rank")?((Long)player.get("rank")).intValue():0;
			playerInfo.score= player.containsKey("score")?((Double)player.get("score")).floatValue():0;
			playerInfo.language= player.containsKey("language")?(String)player.get("language"):"Java";
			playersInfo.add(playerInfo);
		}
		
		
		return new Game(g,playersInfo);
	}
	
	static public Game loadGame(Reader in) throws IOException {
		JSONParser parser= new JSONParser();
		JSONObject jsonGame;
		PushbackReader bin= new PushbackReader(in);
		int c;
		// Skip header comments
		while((c=bin.read())=='#') {
			while((c=bin.read())!='\r' && c!='\n');
			while((c=bin.read())=='\r' || c=='\n');
			if(c!=-1) bin.unread(c);
		}
		if(c!=-1) bin.unread(c);
		
		try {
			jsonGame= (JSONObject) parser.parse(bin);
		} catch (ParseException e) {
			throw new IOException("Failed to parse input", e);
		}
		
		Grid g= new Grid();
		g.setNbPlayers(((Long)jsonGame.get("nbPlayers")).intValue());
		g.player= ((Long) jsonGame.get("current")).intValue();
		for(JSONArray move: (Collection<JSONArray>)jsonGame.get("moves")) {
			int p= ((Long)move.get(0)).intValue();
			int x= ((Long)move.get(1)).intValue();
			int y= ((Long)move.get(2)).intValue();
			g.move(p, Grid.getXY(x+1,y+1));
		}

		List<PlayerAgent> playersInfo= new ArrayList<>(g.nbPlayers);
		JSONArray jsonPlayers= (JSONArray)jsonGame.get("players");
		for(int p=0; p<g.nbPlayers; ++p) {
			String name;
			int rank;
			float score;
			String language;
			
			if(jsonPlayers==null|| jsonPlayers.size()<p) {
				name="P"+p;
				rank=0;
				score=0;
				language="Java";
			}
			else {
				JSONObject jsonPlayer= (JSONObject) jsonPlayers.get(p);
				name= (String)jsonPlayer.get("name");
				rank= ((Long)jsonPlayer.get("rank")).intValue();
				score= ((Double)jsonPlayer.get("score")).floatValue();
				language= (String)jsonPlayer.get("language");
			}
			

			PlayerAgent playerInfo= new PlayerAgent();
			playerInfo.name= name;
			playerInfo.rank= rank;
			playerInfo.score= score;
			playerInfo.language= language;
			playersInfo.add(playerInfo);
		}
		
		
		return new Game(g,playersInfo);
	}
	
}
