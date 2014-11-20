import java.io.File;
import java.io.FileFilter;
import java.io.FileReader;
import java.io.IOException;


public class Tron2Game {
	static final FileFilter filter= new FileFilter() {
		@Override
		public boolean accept(File pathname) {
			if(pathname.isDirectory()) return true;
			if(pathname.getName().endsWith(".tron")) return true;
			return false;
		}
		
	};
	
	
	
	public static void main(String[] args) throws IOException {
		if(args.length<1) throw new IllegalArgumentException("Missing parameter");
		
		for(String p: args) {
			File f= new File(p);
			convert(f);
		}
	}
		
	static private void convert(File f) throws IOException {
		if(f.isFile()) {
			try {
				System.out.print("Converting "+f);
				Game game;
				try {
					// Check old json format
					game= GameUtils.loadGameV0(new FileReader(f));
				} catch(IOException ex) {
					// Check grid format old or new
					Grid g= Utils.loadGrid(f);
					game= new Game(g, null);
						
				}
				GameUtils.store(game, f);
				System.out.println("  ...OK");
			} catch(IOException ex) {
				System.out.println("  ...FAILED");
			}
		} else if(f.isDirectory()) {
			File[] children= f.listFiles(filter);
			for(File c:children) {
				convert(c);
			}
		}
	}
		
}
