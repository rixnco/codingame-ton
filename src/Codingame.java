import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;


public class Codingame {
	static final SimpleDateFormat formatter= new SimpleDateFormat("yyyyMMdd-hhmmss");
	
	
	public static void main(String[] args) throws IOException, ParseException {
		String sessionID;
		File dir;
		
		if(args.length>0) sessionID=args[0];
		else sessionID="60207505";
		if(args.length>1) dir=new File(args[1]);
		else dir=new File("grids/"+sessionID+"-"+formatter.format(new Date()));			
		
		dir.mkdirs();
		
		ExecutorService executor= Executors.newFixedThreadPool(15);
		downloadGames(sessionID, dir, executor);
		
	}
	
	static public void downloadGames(String sessionId, File dir, ExecutorService executor) throws IOException, ParseException {
		URL url=new URL("http://www.codingame.com/services/gamesPlayersRankingRemoteService/findAllByTestSessionId");
		HttpURLConnection con = (HttpURLConnection) url.openConnection();
		
		con.setRequestMethod("POST");
		
		String postContent= "["+sessionId+"]";
		
		con.setDoOutput(true);
		DataOutputStream wr = new DataOutputStream(con.getOutputStream());
		wr.writeBytes(postContent);
		wr.flush();
		wr.close();
		
		int responseCode = con.getResponseCode();
		if(responseCode!=200) throw new IOException("Failed to connect to CodinGame.com: "+responseCode);

		
		BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
		String inputLine;
		StringBuffer response = new StringBuffer();
 
		while ((inputLine = in.readLine()) != null) {
			response.append(inputLine);
		}
		in.close();
		
		JSONParser parser= new JSONParser();
		JSONObject resp= (JSONObject) parser.parse(response.toString());

		JSONArray games= (JSONArray) resp.get("success");
		for(Object o:games) {
			JSONObject g= (JSONObject)o;
			String gameId=g.get("gameId").toString();
			executor.execute(new Worker(gameId, dir));
		}
		
		
	}
	
	static class Worker implements Runnable {
		File dir;
		String gameId;
		public Worker(String gameId, File dir) {
			this.gameId= gameId;
			this.dir= dir;
		}
		public void run() {
			try {
				System.out.println("Downloading game: "+gameId);
				Game g= downloadGame(gameId);

				try {
					System.out.println("Storing game    : "+gameId);
					GameUtils.store(g, new File(dir, gameId+".tron"));
					System.out.println("Game stored     : "+gameId);
				} catch(IOException ex) {
					System.err.println("Failed to store game "+gameId);
				}
				
			} catch (IOException e) {
				System.err.println("Failed to download game "+gameId);
			} catch (ParseException e) {
				System.err.println("Failed to download game "+gameId);
			}
		}
	}
	
	
	static public Game downloadGame(String gameId) throws IOException, ParseException {
		URL url=new URL("http://www.codingame.com/services/gameResultRemoteService/findInformationByIdAndSaveGameV2");
		HttpURLConnection con = (HttpURLConnection) url.openConnection();
		
		con.setRequestMethod("POST");
		
		String postContent= "["+gameId+",-999]";
		
		con.setDoOutput(true);
		DataOutputStream wr = new DataOutputStream(con.getOutputStream());
		wr.writeBytes(postContent);
		wr.flush();
		wr.close();
		
		int responseCode = con.getResponseCode();
		if(responseCode!=200) throw new IOException("Failed to connect to CodinGame.com: "+responseCode);
		BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
		String inputLine;
		StringBuffer response = new StringBuffer();
 
		while ((inputLine = in.readLine()) != null) {
			response.append(inputLine);
		}
		in.close();
		
		JSONParser parser= new JSONParser();
		JSONObject resp= (JSONObject) parser.parse(response.toString());
		JSONArray players= (JSONArray) ((JSONObject) resp.get("success")).get("playersAgents");
		//		"agentId": 31988,
		//		"candidateId": 307499,
		//		"campaignId": 6933,
		//		"playerName": "Alexandre",
		//		"programmingLanguageId": "Java",
		//		"score": 35.9742537605519,
		//		"creationTime": 1401993065150,
		//		"valid": true,
		//		"questionId": 13083,
		//		"rank": 79,
		//		"gamesPlayed": 100,
		//		"progress": "EQUAL"		
		
		int nbPlayers= players.size();
		
		
		
		JSONObject gameResult= (JSONObject)((JSONObject) resp.get("success")).get("gameResult");
		JSONArray positions= (JSONArray)gameResult.get("positions");
		JSONArray infos= (JSONArray)gameResult.get("positions");
		JSONArray views= (JSONArray)gameResult.get("views");
		JSONArray errors= (JSONArray)gameResult.get("errors");
		JSONArray scores= (JSONArray)gameResult.get("scores");
		JSONArray ids= (JSONArray)gameResult.get("ids");
		JSONArray outputs= (JSONArray)gameResult.get("outputs");
		String uinputs= (String) gameResult.get("uinputs");
		
		List<PlayerAgent> playersInfo= new ArrayList<PlayerAgent>(players.size());
		
		for(int p=0; p<nbPlayers; ++p) {
			JSONObject playerAgent= (JSONObject) players.get(p);
			PlayerAgent player= new PlayerAgent();
			player.name= (String) playerAgent.get("playerName");
			Object o=playerAgent.get("rank");
			player.rank= o==null?0:((Long)o).intValue();
			o=playerAgent.get("score");
			player.score= o==null?0:((Double) o).floatValue();
			o=playerAgent.get("programmingLanguageId");
			player.language= o==null?"--":o.toString();
			playersInfo.add(player);
		}
		Grid grid= new Grid(nbPlayers);
		for(Object o :views) {
			String entry= (String)o;
			if(entry.startsWith("KEY_FRAME")) processKeyFrame(entry, grid);
		}
		
		return new Game(grid, playersInfo);
	}

	
	static void processKeyFrame(String keyFrame, Grid grid) {
		@SuppressWarnings("resource")
		Scanner	frame= new Scanner(keyFrame);
		frame.nextLine();
		while(frame.hasNextInt()) {
			int p= frame.nextInt(); frame.nextLine();
			int nbMoves= frame.nextInt(); frame.nextLine();
			if(nbMoves>0) {
				for(int m=1; m<=nbMoves; ++m) {
					int x= 1+frame.nextInt();
					int y= 1+frame.nextInt();
					if(m==nbMoves) {
						grid.move(p, y*32+x);
					}
					frame.nextLine();
				}
			} else if(grid.alive[p]==true) {
				// Kill player
				grid.move(p, Direction.NONE);
			}
			
		}
		
	}
	
}
