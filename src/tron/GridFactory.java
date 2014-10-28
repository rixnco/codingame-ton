package tron;

import java.io.IOException;
import java.io.InputStream;

public abstract class GridFactory {
	abstract public Grid create(int nbPlayer);
	
	
	static private GridFactory instance;
	
	static public GridFactory getInstance() {
		if(instance == null) instance= new DefaultFactory();
		return instance;
	}
	
	static public void setDefault(GridFactory instance) {
		GridFactory.instance= instance;
	}
	
	
	static public class DefaultFactory extends GridFactory {
		@Override
		public Grid create(int nbPlayer) {
			return new Grid(nbPlayer);
		}
		
	}

	static public class TemplateFactory extends GridFactory {
		byte[] template= new byte[Grid.AREA];
		
		public TemplateFactory(InputStream in) throws IOException {
				
			try {
			for(int y=0; y<Grid.PLAYGROUND_HEIGHT; ++y) {
				for(int x=0; x<Grid.PLAYGROUND_WIDTH; ++x) {
					int xy= Grid.getXY(x+1, y+1);
				
					int c= in.read();
					while(c!='.' && c!='X' && c!=-1) c= in.read();
					if(c==-1) throw new IOException("Unexpected end of file");
					if(c=='.') template[xy]=0;
					else template[xy]=1;
				}
			}
			} finally {
				if(in!=null) in.close();
			}
		}
		
		public Grid create(int nbPlayer) {
			Grid g= new Grid(nbPlayer);
			System.arraycopy(template, 0, g.grid, 0, Grid.AREA);
			return g;
		}
	}
	
}

