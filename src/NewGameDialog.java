import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.util.LinkedList;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.border.EmptyBorder;


public class NewGameDialog extends JDialog implements ActionListener, MouseMotionListener, MouseListener {

	
	/**
	 * 
	 */
	private static final long serialVersionUID= 1L;
	static NewGameDialog dialog;
	static Grid value=null;
	
	public static Grid showDialog(Frame frame, String title) {
		dialog= new NewGameDialog(frame, title);
		dialog.setLocationRelativeTo(frame);
		dialog.setVisible(true);
		return value;
	}
	
	
	
	private Grid g= new Grid();
	BoardPanel boardPanel;
	private int xyo,player;

	private JPopupMenu popup;
	private JButton okButton;
	private JButton cancelButton;

	/**
	 * Launch the application.
	 */
	public static void main(String[] args) {
		try {
			NewGameDialog dialog= new NewGameDialog();
			dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
			dialog.setVisible(true);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Create the dialog.
	 */
	public NewGameDialog() {
		init();
	}

	public NewGameDialog(Frame frame, String title) {
		super(frame, title, true);
		init();
	}
	
	
	private void init() {
		setSize(800, 600);
		getContentPane().setLayout(new BorderLayout());
		{
			JPanel buttonPane= new JPanel();
			buttonPane.setLayout(new FlowLayout(FlowLayout.RIGHT));
			getContentPane().add(buttonPane, BorderLayout.SOUTH);
			{
				okButton= new JButton("OK");
				okButton.setActionCommand("OK");
				okButton.addActionListener(this);
				okButton.setEnabled(false);
				buttonPane.add(okButton);
				getRootPane().setDefaultButton(okButton);
			}
			{
				cancelButton= new JButton("Cancel");
				cancelButton.setActionCommand("Cancel");
				cancelButton.addActionListener(this);
				buttonPane.add(cancelButton);
			}
		}
		{
			boardPanel= new BoardPanel();
			boardPanel.addMouseListener(this);
			boardPanel.addMouseMotionListener(this);
			getContentPane().add(boardPanel, BorderLayout.CENTER);
		}
		{
			popup = new JPopupMenu();
		    JMenuItem menuItem = new JMenuItem("Delete player");
		    menuItem.addActionListener(new ActionListener() {
		    	public void actionPerformed(ActionEvent e) {
		    		if(player!=-1) {
		    			LinkedList<Integer> cycle= g.cycles.remove(player);
		    			for(int xy: cycle) { g.grid[xy]=0; }
		    			for(int p=player; p<g.nbPlayers-1; ++p) {
		    				g.head[p]= g.head[p+1];
		    				g.alive[p]= g.alive[p+1];
		    			}
		    			--g.nbPlayers;
		    			--g.remainingPlayers;
		    			boardPanel.setGrid(g);
		    			
		    			okButton.setEnabled(g.nbPlayers>1);
		    			
		    		}
		    	}
		    });
		    popup.add(menuItem);
		}
		
		pack();
		
	}

	public void actionPerformed(ActionEvent e) {
		if("OK".equals(e.getActionCommand())) {
			NewGameDialog.value=g;
		}
		setVisible(false);
	}

	@Override
	public void mouseClicked(MouseEvent e) {
		if(g.nbPlayers==4) return;
		int x= boardPanel.getCycleX(e.getX());
		int y= boardPanel.getCycleY(e.getY());
		if(x<1 || x>=Grid.WIDTH) return;
		if(y<1 || y>=Grid.HEIGHT) return;
		int xy= Grid.getXY(x, y);
		for(int p=0; p<g.nbPlayers; ++p) {
			if(g.cycles.get(p).contains(xy)) {
				return;
			}
		}
		// New player
		LinkedList<Integer> newPlayer= new LinkedList<>();
		newPlayer.add(xy);
		g.cycles.add(newPlayer);
		g.head[g.nbPlayers]= xy;
		g.alive[g.nbPlayers]=true;
		++g.nbPlayers;
		++g.remainingPlayers;
		boardPanel.setGrid(g);
		okButton.setEnabled(g.nbPlayers>1);
		
	}

	@Override
	public void mousePressed(MouseEvent e) {
		player=-1;
		int x= boardPanel.getCycleX(e.getX());
		int y= boardPanel.getCycleY(e.getY());
		if(x<1 || x>=Grid.WIDTH) return;
		if(y<1 || y>=Grid.HEIGHT) return;
		int xy= Grid.getXY(x, y);
		for(int p=0; p<g.nbPlayers; ++p) {
			if(xy==g.head[p]) {
				player=p;
				xyo=xy;
				g.grid[xy]=1;
			}
		}
	}

	@Override
	public void mouseReleased(MouseEvent e) {
		int x= boardPanel.getCycleX(e.getX());
		int y= boardPanel.getCycleY(e.getY());
		if(x<1 || x>=Grid.WIDTH) return;
		if(y<1 || y>=Grid.HEIGHT) return;
		int xy= Grid.getXY(x, y);
		for(int p=0; p<g.nbPlayers; ++p) {
			if(g.cycles.get(p).contains(xy)) {
				if(e.isPopupTrigger()) {
					popup.show(e.getComponent(), e.getX(), e.getY());
					return;
				}
			}
		}
		
	}

	@Override
	public void mouseEntered(MouseEvent e) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void mouseExited(MouseEvent e) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void mouseDragged(MouseEvent e) {
		if(player==-1) return;
		
		int x= boardPanel.getCycleX(e.getX());
		int y= boardPanel.getCycleY(e.getY());
		if(x<1 || x>=Grid.WIDTH) return;
		if(y<1 || y>=Grid.HEIGHT) return;
		int xy= Grid.getXY(x, y);
		if(Direction.fromTo(xyo, xy)==Direction.NONE) return;
		
		LinkedList<Integer> cycle=g.cycles.get(player); 
		
		if(g.grid[xy]==0) {
			cycle.add(xy);
			g.head[player]=xy;
			xyo=xy;
			g.grid[xy]=1;
			boardPanel.setGrid(g);
		} else if(cycle.size()>1) {
			if(cycle.get(cycle.size()-2)==xy) {
				g.grid[cycle.removeLast()]=0;
				g.head[player]=xy;
				boardPanel.setGrid(g);
				xyo=xy;
			}
		}
		
		
		
	}

	@Override
	public void mouseMoved(MouseEvent e) {
		// TODO Auto-generated method stub
		
	}
	
}
