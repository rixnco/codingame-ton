import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
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
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;



public class CodingameRaw {
	static final SimpleDateFormat formatter= new SimpleDateFormat("yyyyMMdd-HHmmss");
	
	
	public static void main(String[] args) throws IOException, ParseException {
		String sessionId;
		String userId;
		File dir;
		
		if(args.length>0) sessionId=args[0];
		else sessionId="602075";
		if(args.length>1) userId=args[1];
		else userId="448075";			
		if(args.length>2) dir=new File(args[2]);
		else dir=new File("grids-raw/"+sessionId+"-"+formatter.format(new Date()));			
		
		dir.mkdirs();
		
		ExecutorService executor= Executors.newFixedThreadPool(20);
		downloadGames(sessionId, userId, dir, executor);
		
	}
	
	static public void downloadGames(String sessionId, String userId, File dir, ExecutorService executor) throws IOException, ParseException {
		URL url=new URL("http://ide.codingame.com/services/gamesPlayersRankingRemoteService/findAllByTestSessionId");
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
		LinkedList<Worker> workers= new LinkedList<>();
		for(Object o:games) {
			JSONObject g= (JSONObject)o;
			String gameId=g.get("gameId").toString();
			workers.add(new Worker(gameId, userId, dir));
		}
		total= workers.size();
		count=0;
		try {
			executor.invokeAll(workers);
		} catch (InterruptedException e) {
		}
		executor.shutdown();
	}

	static Object lock= new Object();
	static int count;
	static int total;
	
	static class Worker implements Callable<Void> {
		File dir;
		String gameId;
		String userId;
		public Worker(String gameId, String userId, File dir) {
			this.gameId= gameId;
			this.userId= userId;
			this.dir= dir;
		}
		public Void call() throws Exception {
			try {
//				System.out.println("Downloading raw game: "+gameId);
				JSONObject game= downloadGame(gameId,userId);

				try {
//					System.out.println("Storing raw game    : "+gameId);
					FileWriter writer= new FileWriter(new File(dir, gameId+".json"));
					try {
					game.writeJSONString(writer);
					} finally {
						writer.close();
					}
					synchronized(lock) {
						++count;
						System.out.println("Downloaded : "+gameId+"  "+count+"/"+total);
					}
				} catch(IOException ex) {
					System.err.println("Failed to store raw game "+gameId);
				}
				
			} catch (IOException e) {
				System.err.println("Failed to download raw game "+gameId);
			} catch (ParseException e) {
				System.err.println("Failed to download raw game "+gameId);
			}
			return null;
		}
	}
	
	
	static public JSONObject downloadGame(String gameId, String userId) throws IOException, ParseException {
		URL url=new URL("http://www.codingame.com/services/gameResultRemoteService/findInformationByIdAndSaveGameV2");
		HttpURLConnection con = (HttpURLConnection) url.openConnection();
		
		con.setRequestMethod("POST");
		String postContent= "["+gameId+","+userId+"]";
		
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
		JSONObject success= (JSONObject) resp.get("success");
		success.remove("viewer");
		success.remove("gamebox");
		success.remove("shareable");
		return success;
	}
	
	static public Game json2Game(JSONObject rawGame) throws IOException, ParseException {

		
		
		JSONArray players= (JSONArray) rawGame.get("playersAgents");
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
		
		JSONObject gameResult= (JSONObject)rawGame.get("gameResult");
		JSONArray positions= (JSONArray)gameResult.get("positions");
		JSONArray infos= (JSONArray)gameResult.get("infos");
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
