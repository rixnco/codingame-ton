
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.util.LinkedList;

import javax.swing.JPanel;



@SuppressWarnings("serial")
public class BoardPanel extends JPanel {

	private Grid   state;

	private boolean showTerritory=true;
	private boolean showArticulations=true;

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
	
	public int getCycleX(int x) {
		if(x<ox+cw) return -1;
		if(x>ox+Grid.WIDTH*cw) return -1;
		return (x-ox)/cw;
	}
	public int getCycleY(int y) {
		if(y<oy+cw) return -1;
		if(y>oy+Grid.HEIGHT*cw) return -1;
		return (y-oy)/cw;
	}
	
	static final Color[] playerColor= new Color[] { new Color(0xFF0000), new Color(0x0000FF), new Color(0x00FF00), new Color(0xFF8000) };

	static final Color[] territoryColor= new Color[] { new Color(0xFF8080), new Color(0x8080FF), new Color(0x80FF80), new Color(0xFFC080) };
	
	private int cw;
	private int ox;
	private int oy;
	
	@Override
	protected void paintComponent(Graphics g) {
		super.paintComponent(g);
		Graphics2D g2= (Graphics2D)g;
		
		int width= getWidth();
		int height= getHeight();
		
		cw= width/Grid.WIDTH;
		if(cw>height/Grid.HEIGHT) cw=height/Grid.HEIGHT;
		ox= (width-cw*Grid.WIDTH)/2;
		oy= (height-cw*Grid.HEIGHT)/2;
		g.translate(ox, oy);
		
		Color c;
		
		// Draw grid
		if(state==null) {
			for(int y=0; y<Grid.HEIGHT; ++y) {
				for(int x=0; x<Grid.WIDTH; ++x) {
					if(x==0 || x==Grid.WIDTH-1 || y==0 || y==Grid.HEIGHT-1) {
						g2.setColor(Color.BLACK);
					} else {
						g2.setColor( getColor(Color.LIGHT_GRAY, Cell.at(x, y)==Cell.BLACK?0.90f:1f));
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
							if(state.alive[p] && state.cycles.get(p).contains(xy)) {
								c= getColor(Color.LIGHT_GRAY, Cell.at(x, y)==Cell.BLACK?0.90f:1f);
								break;
							}
						}
					} else {
						c= getColor(Color.LIGHT_GRAY, Cell.at(x, y)==Cell.BLACK?0.90f:1f);
					}
					g2.setColor(c);
					g2.fillRect(x*cw, y*cw, cw, cw);
					g2.setColor(Color.BLACK);
					g2.drawRect(x*cw, y*cw, cw, cw);
				}
			}
		}

		g2.setColor(Color.DARK_GRAY);
		g2.setStroke(new BasicStroke(2));
		for(int y=1; y<Grid.HEIGHT; y+=3) {
			g2.drawLine(cw, y*cw, Grid.WIDTH*cw-cw, y*cw);
		}
		for(int x=1; x<Grid.WIDTH; x+=3) {
			g2.drawLine(x*cw, cw, x*cw, Grid.HEIGHT*cw-cw);
		}
		g2.setStroke(new BasicStroke());
		
		
		
		paintTerritory(g2);
		paintArticulations(g2);
		paintLightCycles(g2);
		
	}

	private void paintArticulations(Graphics2D g2) {
		if(state==null || state.dirtyArticulations || !showArticulations) return;
		
		for(int y=0; y<Grid.HEIGHT; ++y) {
			for(int x=0; x<Grid.WIDTH; ++x) {
				int xy= y*Grid.WIDTH+x;
				
				int val= state.articd[xy];
				if(val!=0) {
					g2.setColor(Color.BLACK);
					g2.fillOval(x*cw+3, y*cw+3, cw-6, cw-6);
				}
			}
		}
		
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
		int pID= (val>>12)&0xF;
		float d= 30-(val&0x0FFF);
		if(d<0) d=0;
		Color c;
		switch(pID) {
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
		int rgba= Color.HSBtoRGB(hsb[0], hsb[1], hsb[2]);
		rgba=(rgba&0xFFFFFF)|(127<<24);
		c= new Color(rgba, true);
		int t= c.getAlpha();
		return c;
	}

	private Color getColor(Color c, float r) {
		float[] hsb= new float[3];
		Color.RGBtoHSB(c.getRed(), c.getGreen(), c.getBlue(), hsb);
		hsb[2]*=r;
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
			if(state.alive[p]) {
				
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
