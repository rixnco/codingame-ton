import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.util.LinkedList;

import javax.swing.JPanel;

import tron.Grid;



@SuppressWarnings("serial")
public class BoardPanel extends JPanel {

	private Grid   state;

	private boolean showTerritory=true;

	public BoardPanel() {
		setPreferredSize(new Dimension(640,440));
	}


	public void setGrid(Grid state) {
		this.state= state;
		repaint();
	}

	public void showTerritory(boolean show) {
		showTerritory=show;
	}
	
	
	Color[] playerColor= new Color[] { new Color(0xFF0000), new Color(0x0000FF), new Color(0x00FF00), new Color(0xFF8000) };

	Color[] territoryColor= new Color[] { new Color(0xFF8080), new Color(0x8080FF), new Color(0x80FF80), new Color(0xFFC080) };
	
	private int cw;
	
	@Override
	protected void paintComponent(Graphics g) {
		super.paintComponent(g);
		Graphics2D g2= (Graphics2D)g;
		
		int width= getWidth();
		int height= getHeight();
		
		cw= width/Grid.WIDTH;
		if(cw>height/Grid.HEIGHT) cw=height/Grid.HEIGHT;
		g.translate((width-cw*Grid.WIDTH)/2, (height-cw*Grid.HEIGHT)/2);
		
		Color c;
		
		// Draw grid
		if(state==null) {
			for(int y=0; y<Grid.HEIGHT; ++y) {
				for(int x=0; x<Grid.WIDTH; ++x) {
					if(x==0 || x==Grid.WIDTH-1 || y==0 || y==Grid.HEIGHT-1) {
						g2.setColor(Color.BLACK);
					} else {
						g2.setColor(Color.LIGHT_GRAY);
					}
					g2.fillRect(x*cw, y*cw, cw, cw);
					g2.setColor(Color.BLACK);
					g2.drawRect(x*cw, y*cw, cw, cw);
				}
			}
		} else {
			for(int y=0; y<Grid.HEIGHT; ++y) {
				for(int x=0; x<Grid.WIDTH; ++x) {
					int xy= y*Grid.WIDTH+x;
					if(state.grid[xy]==1) {
						c= Color.BLACK;
						for(int p=0; p<state.nbPlayers; ++p) {
							if(state.alive(p) && state.cycles.get(p).contains(xy)) {
								c= Color.LIGHT_GRAY;
								break;
							}
						}
					} else {
						c=Color.LIGHT_GRAY;
					}
					g2.setColor(c);
					g2.fillRect(x*cw, y*cw, cw, cw);
					g2.setColor(Color.BLACK);
					g2.drawRect(x*cw, y*cw, cw, cw);
				}
			}
		}
		
		paintTerritory(g2);
		paintLightCycles(g2);
		
	}


	private void paintTerritory(Graphics2D g2) {
		if(state==null || state.dirtyTerritory || !showTerritory) return;
		
		for(int y=0; y<Grid.HEIGHT; ++y) {
			for(int x=0; x<Grid.WIDTH; ++x) {
				int xy= y*Grid.WIDTH+x;
				
				int val= state.territory[xy];
				Color c= getTerritoryColor(val);
				if(c!=null) {
					g2.setColor(c);
					g2.fillRect(x*cw+1, y*cw+1, cw-2, cw-2);
				}
			}
		}
		
	}	

	private Color getTerritoryColor(int val) {
		int p= val>>16;
		float d= 30-(val&0xFFFF);
		if(d<0) d=0;
		Color c;
		switch(p) {
		case 1: c= playerColor[0]; break;
		case 2: c= playerColor[1]; break;
		case 4: c= playerColor[2]; break;
		case 8: c= playerColor[3]; break;
		default: return null;
		}
		
		float ratio=(float) (0.1 + (d/60));
		float[] hsb= new float[3];
		Color.RGBtoHSB(c.getRed(), c.getGreen(), c.getBlue(), hsb);
		hsb[1]*=ratio;
		c= new Color(Color.HSBtoRGB(hsb[0], hsb[1], hsb[2]));
		return c;
	}
		

	private void paintLightCycles(Graphics2D g) {
		if(state==null) return;
		int dw= cw/2;
		// Draw lights
		BasicStroke lightStroke= new BasicStroke(dw, BasicStroke.CAP_ROUND,  BasicStroke.JOIN_ROUND );
		BasicStroke headStroke= new BasicStroke(2);


		for(int p=0; p<state.nbPlayers; ++p) {
			if(state.alive(p)) {
				
				LinkedList<Integer> light= state.cycles.get(p);
				if(light!=null && !light.isEmpty()) {
					Path2D.Double path= new Path2D.Double();
					boolean first=true;
					for(int xy: light) {
						if(first) path.moveTo(Grid.getX(xy)*cw+dw, Grid.getY(xy)*cw+dw);
						else path.lineTo(Grid.getX(xy)*cw+dw, Grid.getY(xy)*cw+dw);
						first=false;
					}
					g.setStroke(lightStroke);
					g.setColor(playerColor[p]);
					g.draw(path);
					int xyhead= light.getLast();
					Ellipse2D.Double head= new Ellipse2D.Double(Grid.getX(xyhead)*cw, Grid.getY(xyhead)*cw, cw, cw);
					g.setStroke(headStroke);
					g.fill(head);
					
					if(p== state.player) {
						head= new Ellipse2D.Double(Grid.getX(xyhead)*cw+2, Grid.getY(xyhead)*cw+2, cw-4, cw-4);
						g.setColor(Color.ORANGE);
						g.fill(head);
					}
				
				}
			}
		}
	}
	
	
}
