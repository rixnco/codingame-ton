import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;

import org.json.simple.JSONObject;


public class Database {
	static {
		/* Chargement du driver JDBC pour MySQL */
		try {
		    Class.forName( "com.mysql.jdbc.Driver" );
		} catch ( ClassNotFoundException e ) {
		    /* Gérer les éventuelles erreurs ici. */
		}
	}

	
	
	public static void main(String[] args) throws IOException {
		
//		String gameId= "20812828";
//		
//		JSONObject jsonGame= GameUtils.loadJsonGame(new FileReader("grids-raw/602075-20141204-041620/"+gameId+".json"));
//
//		/* Connexion à la base de données */
//		String url = "jdbc:mysql://localhost:3306/tron";
//		String utilisateur = "tron";
//		String motDePasse = "Jslsmab00";
//		Connection connexion = null;
//		ResultSet rs = null;
//		try {
//		    connexion = DriverManager.getConnection( url, utilisateur, motDePasse );
//
//
//		    /* Création de l'objet gérant les requêtes */
//		    Statement statement = connexion.createStatement();
//		    
//		    statement.execute("INSERT INTO Session SET date='2014-12-05 15:53';", Statement.RETURN_GENERATED_KEYS);
//		    
//		    rs= statement.getGeneratedKeys();
//		    rs.next();
//		    int sessionId= rs.getInt(1);
//		    
//		    Map<String, Integer> userIdMap= new HashMap<>(); 
//		    int nbPlayers= g.playerAgents.size();
//		    statement.execute("INSERT INTO Game SET gameId='"+gameId+"' sessionId='"+sessionId+"' nbPlayers='"+nbPlayers+"';");
//
//		    
//		    int[] userIds= new int[nbPlayers];
//		    for(int p=0; p<g.playerAgents.size(); ++p) {
//		    	PlayerAgent pa= g.playerAgents.get(p);
//		    	Integer userId= userIdMap.get(pa.name);
//		    	if(userId==null) {
//				    statement.execute("INSERT INTO User SET name='"+pa.name+"' language='"+pa.language+"' rank='"+pa.rank+"' score='"+pa.score+"';", Statement.RETURN_GENERATED_KEYS);
//				    rs= statement.getGeneratedKeys();
//				    rs.next();
//				    userId= rs.getInt(1);
//				    userIdMap.put(pa.name, userId);
//		    	}
//		    	
//		    	userIds[p]= userId;
//		    	
//		    	statement.execute("INSERT INTO Player SET userId='"+userId+"' gameId='"+gameId+"' num='"+p+"';");
//		    }
//		    
//		    
//		} catch ( SQLException e ) {
//		    /* Gérer les éventuelles erreurs ici */
//		} finally {
//		    if ( connexion != null )
//		        try {
//		            /* Fermeture de la connexion */
//		            connexion.close();
//		        } catch ( SQLException ignore ) {
//		            /* Si une erreur survient lors de la fermeture, il suffit de l'ignorer. */
//		        }
//		}
//
//		
//		
		
	}
	
	
}
