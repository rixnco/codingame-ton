import java.awt.Dimension;
import java.awt.EventQueue;

import javax.swing.JFrame;
import javax.swing.JSplitPane;

import java.awt.BorderLayout;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JFileChooser;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.JTextPane;
import javax.swing.JButton;
import javax.swing.JSlider;
import javax.swing.JMenuBar;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.KeyStroke;
import javax.swing.SpinnerNumberModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.filechooser.FileFilter;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.MutableTreeNode;
import javax.swing.tree.TreePath;

import tron.Constants;
import tron.Grid.Direction;
import tron.TerritoryStrategy;
import tron.Grid;
import tron.GridUtils;
import tron.Move;
import tron.Strategy;
import tron.SurvivalStrategy;
import tron.TronStrategy;
import tron.Watchdog;

import java.awt.event.KeyEvent;
import java.awt.event.InputEvent;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.LinkedList;
import java.util.List;
import java.util.Timer;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;

import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JSpinner;
import javax.swing.JToggleButton;


@SuppressWarnings("serial")
public class TronGUI {
	

	static private class ExtendedMove extends Move {
		public Strategy strategy;
		public Integer depth;
		public Integer timeout;
	
		public ExtendedMove() {
		}
		public ExtendedMove(int player, Direction d) {
			super(player,d);
		}
		public ExtendedMove(ExtendedMove m) {
			super(m);
			this.strategy= m.strategy;
			this.depth= m.depth;
			this.timeout= m.timeout;
			this.grid= m.grid==null?null:m.grid.copy();
		}
		public ExtendedMove(Move m, Strategy strategy, Integer depth, Integer timeout) {
			super(m);
			this.strategy=strategy;
			this.depth= depth;
			this.timeout= timeout;
			this.grid= m.grid==null?null:m.grid.copy();
		}
	}
	
	
	private LinkedList<ExtendedMove> gameStates= new LinkedList<>();

	private JFrame frame;
	private BoardPanel boardPanel;
	private JButton firstBtn;
	private JButton previousBtn;
	private JSlider gameStateSlider;
	private JButton nextBtn;
	private JButton lastBtn;
	private JTree moveTree;
	private JTextPane logPane;
	private JPanel controlPanel;


	private Action gameFirstAction= new GameFirstAction("<<");
	private Action gamePreviousAction= new GamePreviousAction("<");
	private Action gameNextAction= new GameNextAction(">");
	private Action gameLastAction= new GameLastAction(">>");
	private Action gamePlayAction= new GamePlayAction("go");
	private Action gameResetAction= new GameResetAction("reset");
	
	private int gameStateIdx=-1;
	private boolean gameOver=true;

	private boolean processing=false;
	
	private ExecutorService executor= Executors.newSingleThreadExecutor(new ThreadFactory() {
		@Override
		public Thread newThread(Runnable r) {
			Thread th= new Thread(r, "GameRunner");
			th.setPriority(Thread.MAX_PRIORITY);
			return th;
		}
	});
	private JComboBox<Strategy> strategyCombo;
	private JSpinner depthSpinner;
	private JSpinner timeoutSpinner;
	private JToggleButton gamePlayBtn;

	private boolean playing;
	/**
	 * Launch the application.
	 */
	public static void main(String[] args) {
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					TronGUI window= new TronGUI();
					window.frame.pack();
					window.frame.setVisible(true);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
	}

	/**
	 * Create the application.
	 */
	public TronGUI() {
		initialize();
	}

	/**
	 * Initialize the contents of the frame.
	 */
	private void initialize() {
		frame= new JFrame();
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		
		JSplitPane mainSplitPane = new JSplitPane();
		mainSplitPane.setResizeWeight(1.0);
		mainSplitPane.setOneTouchExpandable(true);
		mainSplitPane.setOrientation(JSplitPane.VERTICAL_SPLIT);
		frame.getContentPane().add(mainSplitPane, BorderLayout.CENTER);
		
		JSplitPane gameSplitPane = new JSplitPane();
		gameSplitPane.setResizeWeight(0.5);
		gameSplitPane.setOneTouchExpandable(true);
		mainSplitPane.setLeftComponent(gameSplitPane);
		
		JPanel gamePanel = new JPanel();
		gameSplitPane.setRightComponent(gamePanel);
		gamePanel.setLayout(new BorderLayout(0, 0));
		
		boardPanel= new BoardPanel();
		gamePanel.add(boardPanel, BorderLayout.CENTER);
		
		controlPanel = new JPanel();
		controlPanel.setEnabled(false);
		gamePanel.add(controlPanel, BorderLayout.SOUTH);
		
		gameFirstAction.setEnabled(false);
		
		JButton btnReset = new JButton("reset");
		btnReset.setAction(gameResetAction);
		controlPanel.add(btnReset);
		firstBtn = new JButton("<<");
		firstBtn.setAction(gameFirstAction);
		controlPanel.add(firstBtn);
		
		gamePreviousAction.setEnabled(false);
		previousBtn = new JButton("<");
		previousBtn.setAction(gamePreviousAction);
		controlPanel.add(previousBtn);
		
		gameStateSlider = new JSlider();
		gameStateSlider.setEnabled(false);
		gameStateSlider.setMinimumSize(new Dimension(400,30));
		gameStateSlider.setPreferredSize(new Dimension(400,30));
		gameStateSlider.setMinimum(0);
		gameStateSlider.setSnapToTicks(true);
		gameStateSlider.setMaximum(0);
		gameStateSlider.setMajorTickSpacing(10);
		gameStateSlider.setMinorTickSpacing(1);
		gameStateSlider.addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(ChangeEvent e) {
				int idx = gameStateSlider.getValue();
				if (idx >= 0 && idx < gameStates.size()) {
					setCurrentGameState(idx);
				}
			}
		});
		controlPanel.add(gameStateSlider);
		
		gameNextAction.setEnabled(false);
		nextBtn = new JButton(">");
		nextBtn.setAction(gameNextAction);
		controlPanel.add(nextBtn);
		
		gameLastAction.setEnabled(false);
		lastBtn = new JButton(">>");
		lastBtn.setAction(gameLastAction);
		controlPanel.add(lastBtn);
		
		gamePlayBtn = new JToggleButton("play");
		gamePlayBtn.setAction(gamePlayAction);
		controlPanel.add(gamePlayBtn);
		
		JPanel panel = new JPanel();
		gamePanel.add(panel, BorderLayout.NORTH);
		
		JLabel lblStrategy = new JLabel("Strategy");
		panel.add(lblStrategy);
		
		strategyCombo = new JComboBox<>();
		strategyCombo.setEnabled(false);
		strategyCombo.addItem(new TerritoryStrategy());
		strategyCombo.addItem(new SurvivalStrategy());
		strategyCombo.addItem(new TronStrategy());
		strategyCombo.setSelectedIndex(2);
		panel.add(strategyCombo);
		
		JLabel lblDepth = new JLabel("depth");
		panel.add(lblDepth);
		
		depthSpinner = new JSpinner(new SpinnerNumberModel(10,1,1000,1));
		depthSpinner.setEnabled(false);
		panel.add(depthSpinner);
		
		JLabel lblTimeout = new JLabel("timeout");
		panel.add(lblTimeout);
		
		timeoutSpinner = new JSpinner(new SpinnerNumberModel(100,0,10000,100));
		timeoutSpinner.setEnabled(false);
		panel.add(timeoutSpinner);
		
		JSplitPane moveSplitPane = new JSplitPane();
		moveSplitPane.setResizeWeight(1.0);
		moveSplitPane.setOrientation(JSplitPane.VERTICAL_SPLIT);
		moveSplitPane.setOneTouchExpandable(true);
		gameSplitPane.setLeftComponent(moveSplitPane);
		
		JScrollPane moveTreeScrollPane = new JScrollPane();
		moveSplitPane.setLeftComponent(moveTreeScrollPane);
		
		moveTree = new JTree();
		moveTree.setModel(null);
		moveTree.setEditable(false);
		moveTree.setPreferredSize(new Dimension(200,500));
		moveTree.addTreeSelectionListener(new TreeSelectionListener() {

			@Override
			public void valueChanged(TreeSelectionEvent e) {
				TreePath path = e.getPath();
				if (path == null) return;
				
				ExtendedMove root= (ExtendedMove) ((DefaultMutableTreeNode)path.getPathComponent(0)).getUserObject();
				Move selected= (Move) ((DefaultMutableTreeNode)path.getLastPathComponent()).getUserObject();
				if(selected.grid==null) {
										
					Object[] p= ((DefaultMutableTreeNode)path.getLastPathComponent()).getUserObjectPath();
					Grid g=null;
					Move m=null;
					for(Object o: p) {
						m=(Move)o;
						if(g==null && m.grid!=null) { g= m.grid.copy(); continue; }
						if(g!=null) m.move(g);
					}
					selected.grid=g;
				}

				if(selected.grid.remainingPlayers>1 && root.strategy!=null) {
					//selected.eval=root.strategy.evaluate(selected.grid, selected.grid.player);
					selected.grid.calculateTerritories();
					
				}
				boardPanel.setGrid(selected.grid);
			}

		});
		
		
		moveTreeScrollPane.setViewportView(moveTree);
		
		JScrollPane moveDetailScrollPane = new JScrollPane();
		moveSplitPane.setRightComponent(moveDetailScrollPane);
		
		JScrollPane logScrollPane = new JScrollPane();
		logScrollPane.setPreferredSize(new Dimension(10, 10));
		mainSplitPane.setRightComponent(logScrollPane);
		
		logPane = new JTextPane();
		logPane.setEditable(false);
		logScrollPane.setViewportView(logPane);
		
		
		JMenuBar menuBar = new JMenuBar();
		frame.setJMenuBar(menuBar);
		
		JMenu mnGame = new JMenu("Game");
		menuBar.add(mnGame);
		
		JMenuItem mntmLoad = new JMenuItem("Load");
		mntmLoad.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				JFileChooser fc= new JFileChooser(".");
				fc.setDialogTitle("Load Game");
				
				FileFilter tronFilter= new FileFilter() {
					@Override
					public boolean accept(File f) {
						if(f.isDirectory()) return true;
						if(f.getName().endsWith(".tron")) return true;
						return false;
					}

					@Override
					public String getDescription() {
						return "Tron files";
					}
					
				};
				
				fc.addChoosableFileFilter(tronFilter);
				fc.setFileFilter(tronFilter);
				int res= fc.showOpenDialog(frame);
				if(res== JFileChooser.APPROVE_OPTION) {
					loadGame(fc.getSelectedFile());
				}
			}
		});
		mntmLoad.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_L, InputEvent.CTRL_MASK));
		mnGame.add(mntmLoad);
		
		loadGame(Constants.DEFAULT_GAME);
	}
	
	
	private void loadGame(File file) {
		if(file==null) return;
		try {
			Grid g= GridUtils.load(file);
			
			initGame(g);
			
		} catch (FileNotFoundException e) {
		}
	}

	
	private void initGame(Grid g) {
		ExtendedMove m= new ExtendedMove();
		m.grid= g;
		gameStates.clear();
		gameStates.add(m);
		gameStateIdx=-1;
		setCurrentGameState(0);
	}

	private MutableTreeNode toNode(Move m) {
		DefaultMutableTreeNode node = new DefaultMutableTreeNode(m);
		if (m.future != null) {
			for (Move f : m.future) {
				node.add(toNode(f));
			}
		}
		return node;
	}
	
	
	private void update() {
		int lastGameStateIdx= gameStates.size()-1;
		
		gameFirstAction.setEnabled(!processing && gameStateIdx>0);
		gamePreviousAction.setEnabled(!processing && gameStateIdx>0);
		gameStateSlider.setEnabled(!processing && gameStateIdx>=0);
		gameNextAction.setEnabled(!processing && !(gameOver && gameStateIdx==lastGameStateIdx));
		gameLastAction.setEnabled(!processing && gameStateIdx<lastGameStateIdx);
		gamePlayAction.setEnabled(!gameOver && gameStateIdx==lastGameStateIdx);
		gameResetAction.setEnabled(!processing && gameStateIdx>=0);
		moveTree.setEnabled(!processing);
		
		strategyCombo.setEnabled(!gameOver && !processing && gameStateIdx==lastGameStateIdx);
		depthSpinner.setEnabled(!gameOver && !processing && gameStateIdx==lastGameStateIdx);
		timeoutSpinner.setEnabled(!gameOver && !processing && gameStateIdx==lastGameStateIdx);
	}
	
	private void setCurrentGameState(int idx) {
		if(idx<0 || idx>gameStates.size()) return;
		if(idx==gameStateIdx) return;
		
		if(idx==gameStates.size()) {
			processing=true;
			update();
			ExtendedMove m= gameStates.getLast();
			m.strategy= (Strategy) strategyCombo.getSelectedItem();
			m.timeout= (Integer) timeoutSpinner.getValue();
			m.depth= (Integer) depthSpinner.getValue();
			executor.execute(new GameRunner(m));
			
		} else {
			ExtendedMove m= gameStates.get(idx);

			if(!playing) {
				gameStateIdx=idx;
				gameStateSlider.setMaximum(gameStates.size()-1);
				gameStateSlider.setValue(idx);
				
				if(m.strategy!=null) strategyCombo.setSelectedItem(m.strategy);
				if(m.depth!=null) depthSpinner.setValue((Integer)m.depth);
				if(m.timeout!=null) timeoutSpinner.setValue((Integer)m.timeout);
				
				MutableTreeNode root= toNode(m);
				DefaultTreeModel model = new DefaultTreeModel(root);
				moveTree.setModel(model);
				moveTree.setSelectionPath(new TreePath(root));
				gameOver= m.grid.remainingPlayers<=1;
				update();
			} else if(processing) {
				moveTree.setModel(null);
				boardPanel.setGrid(m.grid);
			}
		}
	}
	
	private void gameMoveFirst() {
		setCurrentGameState(0);
	}
	
	private void gameMovePrevious() {
		setCurrentGameState(gameStateIdx-1);
	}
	
	private void gameMoveNext() {
		setCurrentGameState(gameStateIdx+1);
	}
	
	private void gameMoveLast() {
		setCurrentGameState(gameStates.size()-1);
	}
	
	private void gameStart() {
		gamePlayAction.putValue("NAME", "stop");
		gamePlayAction.putValue(Action.SELECTED_KEY, true);
		gamePlayAction.putValue(Action.ACTION_COMMAND_KEY, "stop");
		playing=true;
		processing=true;
		update();
		
		ExtendedMove m= gameStates.getLast();
		m.strategy= (Strategy) strategyCombo.getSelectedItem();
		m.timeout= (Integer) timeoutSpinner.getValue();
		m.depth= (Integer) depthSpinner.getValue();
		executor.execute(new GameRunner(m));
	}
	
	private void gameStop() {
		gamePlayAction.putValue("NAME", "go");
		gamePlayAction.putValue(Action.SELECTED_KEY, false);
		gamePlayAction.putValue(Action.ACTION_COMMAND_KEY, "start");
		playing=false;
		update();
	}
	
	private void gameReset() {
		if(gameStates.size()<1) return;
		ExtendedMove m= gameStates.getFirst();
		initGame(m.grid);
	}
	
	private abstract class GameAction extends AbstractAction {
		public GameAction(String name) {
			super(name);
		}
	}
	private class GameFirstAction extends GameAction {
		public GameFirstAction(String name) {
			super(name);
		}
		@Override
		public void actionPerformed(ActionEvent e) {
			gameMoveFirst();
		}
	}
	private class GamePreviousAction extends GameAction {
		public GamePreviousAction(String name) {
			super(name);
		}
		@Override
		public void actionPerformed(ActionEvent e) {
			gameMovePrevious();
		}
	}
	private class GameNextAction extends GameAction {
		public GameNextAction(String name) {
			super(name);
		}
		@Override
		public void actionPerformed(ActionEvent e) {
			gameMoveNext();
		}
	}
	private class GameLastAction extends GameAction {
		public GameLastAction(String name) {
			super(name);
		}
		@Override
		public void actionPerformed(ActionEvent e) {
			gameMoveLast();
		}
	}
	private class GameResetAction extends GameAction {
		public GameResetAction(String name) {
			super(name);
		}
		@Override
		public void actionPerformed(ActionEvent e) {
			gameReset();
		}
	}
	private class GamePlayAction extends GameAction {
		public GamePlayAction(String name) {
			super(name);
		}
		@Override
		public void actionPerformed(ActionEvent e) {
			String action= e.getActionCommand();
			if("stop".equals(action)) {
				playing=false;
			} else {
				gameStart();
			}
		}
	}
	
	private class GameRunner implements Runnable {

		private ExtendedMove move;
		public GameRunner(ExtendedMove m) {
			this.move = m;
		}
		
		@Override
		public void run() {
			int timeout= move.timeout;
			Strategy strategy= move.strategy;
			int depth= move.depth;
			
			Timer t= new Timer();
			ExtendedMove present=move;
			Move tmp;
			try {
			do {
				
				Watchdog dog= timeout==0?Watchdog.createInfinite():Watchdog.createDefault();
				t.schedule(dog, timeout);
				tmp= strategy.nextMove(present, present.grid, present.grid.nextPlayer(), depth, dog);
				dog.cancel();
				if(tmp!=null) {
					if(tmp.grid==null) {
						tmp.grid=present.grid.copy();
						tmp.move(tmp.grid);
					}
					present= new ExtendedMove(tmp, strategy, depth, timeout);
					present.eval=null;
					present.value=null;
					present.future.clear();
					gameStates.add(present);
					if(playing) {
						EventQueue.invokeLater(new Runnable() {
							public void run() {
								gameMoveLast();
							}
						});
					}
				} else {
					// Suicide
					Grid g= present.grid.copy();
					present= new ExtendedMove(g.nextPlayer(), Direction.UP);
					present.eval=null;
					present.value=null;
					present.future.clear();
					present.move(g);
					present.grid=g;
					gameStates.add(present);
				}
			} while(playing && present.grid.remainingPlayers>1);
		} catch(Throwable th) {
			th.printStackTrace();
		}
			EventQueue.invokeLater(new Runnable() {
				public void run() {
					processing=false;
					gameStop();
					gameMoveLast();
				}
			});
		}
	}
	

}
