
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Timer;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSlider;
import javax.swing.JSpinner;
import javax.swing.JSplitPane;
import javax.swing.JTextPane;
import javax.swing.JToggleButton;
import javax.swing.JTree;
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

import org.formbuilder.Form;
import org.formbuilder.FormBuilder;
import org.formbuilder.annotations.UIOrder;
import org.formbuilder.annotations.UIReadOnly;
import org.formbuilder.annotations.UITitle;
import org.formbuilder.mapping.beanmapper.SampleBeanMapper;
import org.formbuilder.mapping.beanmapper.SampleContext;

import javax.swing.JTextField;
import javax.swing.BoxLayout;

import java.awt.Component;

import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;

import java.awt.FlowLayout;


@SuppressWarnings("serial")
public class TronGUI {
	public static final File DEFAULT_GAME=new File("./grids/equals-tron-4P.tron");
//	public static final File DEFAULT_GAME=new File("./test2.tron");


	static String format(long eval, Strategy strategy) {
		if(strategy==null) return Long.toString(eval);
		switch(strategy) {
		case FIGHT:
			StringBuffer res= new StringBuffer();
			for(int p=0; p<4; ++p) {
				res.insert(0, " ");
				res.insert(0, Long.toString(eval&0xFFFF));
				eval=eval>>16;
			}
			return res.toString();
		default: 
			return Long.toString(eval);
		}
	}
	
	
	static private class ExtendedMove {
		public Move move;
		public Grid grid;
		public Strategy strategy;
		public Integer depth;
		public Integer timeout;
	
//		public ExtendedMove(Move m) {
//			this.move=m;
//		}
		public ExtendedMove(Move m, Grid g) {
			this.move=m;
			this.grid=g;
		}
		public ExtendedMove(Move m, Grid g, Strategy strategy, int depth, int timeout) {
			this.move=m;
			this.grid=g;
			this.strategy= strategy;
			this.depth= depth;
			this.timeout= timeout;
		}
		
		public String toString() {
			if(move==null) return "--";
			if(move.player==-1) return "--";
			StringBuffer bf= new StringBuffer();
			boolean first=true;
			int p=move.player;
			do {
				if(move.dir[p%4].step!=0) {
					bf.append(first?"P":" P").append(p%4).append("-").append(move.dir[p%4]);
					first=false;
				}
			} while((++p%4)!=move.player);
			if(first) bf.append("--");
			else bf.append(" [ "+(move.eval==Move.NaN?"-":TronGUI.format(move.eval, move.strategy))+" / "+(move.value==Move.NaN?"-":TronGUI.format(move.value,move.strategy))+" ]");
			return bf.toString();

		}
		
		@UITitle("Strategy")
		@UIReadOnly
		@UIOrder(1)
		public String getStrategy() {
			return ""+(move==null?Strategy.NONE:move.strategy);
		}
		@UITitle("Depth")
		@UIReadOnly
		@UIOrder(2)
		public String getDepth() {
			return ""+(move==null?"":move.depth==Move.NaN?"":move.depth);
		}
		@UITitle("Eval")
		@UIReadOnly
		@UIOrder(3)
		public String getEval() {
			if(move==null || move.eval==Move.NaN) return "";
			return TronGUI.format(move.eval, move.strategy);
		}
		@UITitle("Value")
		@UIReadOnly
		@UIOrder(4)
		public String getValue() {
			if(move==null || move.value==Move.NaN) return "";
			switch(move.strategy) {
			case FIGHT:
				StringBuffer res= new StringBuffer();
				long eval= move.value;
				for(int p=0; p<4; ++p) {
					res.insert(0, " ");
					res.insert(0, Long.toString(eval&0xFFFF));
					eval=eval>>16;
				}
				return res.toString();
			default: return Long.toString(move.value);
			}
		}
		@UITitle("Degree")
		@UIReadOnly
		@UIOrder(5)
		public String getDegree() {
			return ""+(move==null?"":move.degree==-1?"":move.degree);
		}
		@UITitle("Opponents")
		@UIReadOnly
		@UIOrder(6)
		public String getOpponents() {
			return ""+(move==null?"":move.opponents==null?"":move.opponents.toString());
		}
		
		
	}
	
	
	private Form<ExtendedMove> form = FormBuilder.map( ExtendedMove.class ).buildForm();
	private LinkedList<ExtendedMove> gameStates= new LinkedList<>();
	private Move[] lastPlayerMove;
	
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
	private ExtendedMove selectedMove=null;
	private Game game;

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
	private JScrollPane moveDetailScrollPane;
	private JTextField heuristicField;
	private JPanel heuristicPanel;
	private JButton heuristicButton;
	private JMenuItem mntmNew;
	private JPanel playersPanel;
	private JPanel gamePanel;
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
		
		gamePanel= new JPanel();
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
		
		JPanel strategyPanel = new JPanel();
		gamePanel.add(strategyPanel, BorderLayout.NORTH);
		
		JLabel lblStrategy = new JLabel("Strategy");
		strategyPanel.add(lblStrategy);
		
		strategyCombo = new JComboBox<>();
		strategyCombo.setEnabled(false);
		strategyCombo.addItem(Strategy.ADAPTATIVE);
		strategyCombo.addItem(Strategy.TERRITORY);
		strategyCombo.addItem(Strategy.SURVIVAL);
		strategyCombo.setSelectedIndex(0);
		strategyPanel.add(strategyCombo);
		
		JLabel lblDepth = new JLabel("depth");
		strategyPanel.add(lblDepth);
		
		depthSpinner = new JSpinner(new SpinnerNumberModel(1,1,1000,1));
		depthSpinner.setEnabled(false);
		strategyPanel.add(depthSpinner);
		
		JLabel lblTimeout = new JLabel("timeout");
		strategyPanel.add(lblTimeout);
		
		timeoutSpinner = new JSpinner(new SpinnerNumberModel(0,0,10000,100));
		timeoutSpinner.setEnabled(false);
		strategyPanel.add(timeoutSpinner);
		
		playersPanel = new JPanel();
		gamePanel.add(playersPanel, BorderLayout.WEST);
		playersPanel.setLayout(new BoxLayout(playersPanel, BoxLayout.Y_AXIS));
		playersPanel.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 10));
		playersPanel.add(Box.createVerticalGlue());
		playersPanel.add(new PlayerInfoPanel());
		playersPanel.add(Box.createVerticalGlue());
		
		
		JSplitPane moveSplitPane = new JSplitPane();
		moveSplitPane.setResizeWeight(0.7);
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
				ExtendedMove selected= (ExtendedMove) ((DefaultMutableTreeNode)path.getLastPathComponent()).getUserObject();
				if(selected.grid==null) {
										
					Object[] p= ((DefaultMutableTreeNode)path.getLastPathComponent()).getUserObjectPath();
					Grid g=null;
					ExtendedMove m=null;
					for(Object o: p) {
						m=(ExtendedMove)o;
						if(g==null && m.grid!=null) { g= m.grid.copy(); continue; }
						if(g!=null) m.move.move(g);
					}
					selected.grid=g;
				}

				if(selected.grid.remainingPlayers>0) {
					synchronized(executor) {
						Grid g= selected.grid;
						g.resetTerritory();
						int nextPlayer= g.nextPlayer();
						int p= nextPlayer;
						do {
							g.calculateTerritory(p);
							p= g.nextPlayer(p);
						} while(p!=nextPlayer);
						
						g.resetArticulations();
						p=nextPlayer;
						do {
							g.hideHead(p);
							g.calculateArticulations(g.head[p]/*, g.territory*/);
							g.restoreHead(p);
							p= g.nextPlayer(p);
						} while(p!=nextPlayer);
					}
				}
				boardPanel.setGrid(selected.grid);
				
				form.setValue(selected);
				selectedMove= selected;
//				selectedMove.move.opponents=root.move.opponents;
//				selectedMove.move.strategy= root.move.strategy;
				Strategy s=selectedMove.move.strategy; 
				if(s!=null) {
					switch(s) {
					case NONE:
					case ADAPTATIVE:
						heuristicButton.setEnabled(true);
						heuristicButton.setActionCommand(Strategy.ADAPTATIVE.toString());
						heuristicButton.setText(Strategy.ADAPTATIVE.toString());
						heuristicField.setText("");
						break;
					case FIGHT:
					case TERRITORY:
					case SURVIVAL:
						heuristicButton.setEnabled(true);
						heuristicButton.setActionCommand(s.toString());
						heuristicButton.setText(s.toString());
						heuristicField.setText("");
						break;
					}
				} else {
					heuristicButton.setEnabled(true);
					heuristicButton.setActionCommand(Strategy.ADAPTATIVE.toString());
					heuristicButton.setText(Strategy.ADAPTATIVE.toString());
					heuristicField.setText("");
				}
				
				
			}

		});
		
	
		moveTreeScrollPane.setViewportView(moveTree);
		
		moveDetailScrollPane = new JScrollPane();
		form.setValue(new ExtendedMove(Move.get(), null));
		moveSplitPane.setRightComponent(moveDetailScrollPane);
		
		JPanel moveDetailPanel = new JPanel();
		moveDetailScrollPane.setViewportView(moveDetailPanel);
		moveDetailPanel.setLayout(new BorderLayout(0, 0));
		
		moveDetailPanel.add(form.asComponent(), BorderLayout.CENTER);

		heuristicPanel = new JPanel();
		heuristicPanel.setBorder(new EmptyBorder(3, 5, 3, 5));
		moveDetailPanel.add(heuristicPanel, BorderLayout.SOUTH);
		heuristicPanel.setLayout(new BoxLayout(heuristicPanel, BoxLayout.X_AXIS));
		
		heuristicButton = new JButton("NONE");
		heuristicButton.setEnabled(false);
		heuristicButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if(selectedMove==null) return;
				if("FIGHT".equals(e.getActionCommand())) {
					Grid g= selectedMove.grid;
					Move m= Move.get(selectedMove.move);
					try {
						long value= IA.evaluate_maxn(g, g.player, m.opponents, Watchdog.getInfinite());
						heuristicField.setText(format(value, Strategy.FIGHT));
					} catch (Timeout e1) {
					}
				} else if("TERRITORY".equals(e.getActionCommand())) {
					Grid g= selectedMove.grid;
					Move m= Move.get(selectedMove.move);
					try {
						int value= IA.evaluate_alphabeta(g, g.player, m.opponents.get(0), Watchdog.getInfinite());
						heuristicField.setText(format(value, Strategy.TERRITORY));
					} catch (Timeout e1) {
					}
				} else 	if("SURVIVAL".equals(e.getActionCommand())) {
					Grid g= selectedMove.grid;
					try {
						int value= IA.floodfill(g, g.head[g.player], Watchdog.getInfinite());
						heuristicField.setText(format(value, Strategy.SURVIVAL));
					} catch (Timeout e1) {
					}
				} else if("ADAPTATIVE".equals(e.getActionCommand())) {
					Grid g= selectedMove.grid;
					Move m= Move.get();
					IA.nextMove(m, g, g.player, 0, Watchdog.getInfinite());
					heuristicField.setText(format(m.eval, m.strategy));
				}

				
				
			}
		});
		heuristicPanel.add(heuristicButton);
		
		heuristicPanel.add(Box.createRigidArea(new Dimension(10, 0)));
		
		heuristicField = new JTextField();
		heuristicField.setHorizontalAlignment(SwingConstants.TRAILING);
		heuristicField.setAlignmentX(0.5f);
		heuristicField.setEditable(false);
		heuristicPanel.add(heuristicField);
		
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
		
		mntmNew = new JMenuItem("New");
		mntmNew.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				Game g= NewGameDialog.showDialog(frame, "New Game");
				if(g!=null) {
					initGame(g);
				}
			}
		});
		mntmNew.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_N, InputEvent.CTRL_MASK));
		mnGame.add(mntmNew);
		mntmLoad.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_L, InputEvent.CTRL_MASK));
		mnGame.add(mntmLoad);
		
		JMenuItem mntmSave = new JMenuItem("Save");
		mntmSave.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, InputEvent.CTRL_MASK));
		mntmSave.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				JFileChooser fc= new JFileChooser(".");
				fc.setDialogTitle("Save Game");
				
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
				int res= fc.showSaveDialog(frame);
				if(res== JFileChooser.APPROVE_OPTION) {
					saveGame(fc.getSelectedFile());
				}
			}
		});
		mnGame.add(mntmSave);
		
		loadGame(DEFAULT_GAME);
	}
	
	
	private void loadGame(File file) {
		if(file==null) return;
		try {
			Game game= GameUtils.loadGame(file);
			
			initGame(game);
			
		} catch (IOException e) {
		}
	}

	private void saveGame(File file) {
		if(file==null) return;
		try {
			if(file.getName().indexOf('.')==-1) {
				file= new File(file.getName()+".tron");
			}
			Grid g= selectedMove.grid;
			
			GameUtils.store(new Game(g, game.playersInfo),file);
			
		} catch (IOException e) {
		}
	}

	
	private void initGame(Game game) {
		this.game= game;
		playersPanel.removeAll();
		playersPanel.add(Box.createVerticalGlue());
		for(int p=0; p<game.playersInfo.size(); ++p) {
			PlayerInfo info= game.playersInfo.get(p);
			playersPanel.add(new PlayerInfoPanel(BoardPanel.playerColor[p], info));
		}
		playersPanel.add(Box.createVerticalGlue());
		
		gamePanel.revalidate();
		
		
		Grid g= game.grid.copy();
		lastPlayerMove= new Move[g.nbPlayers];
		gameStates.clear();
		for(int i=0, nb=g.nbMoves-g.nbPlayers; i<nb; ++i) {
			ExtendedMove m= new ExtendedMove(Move.get(),g.copy());
			gameStates.addFirst(m);
			g.unmove();
		}
		ExtendedMove m= new ExtendedMove(Move.get(),g.copy());
		gameStates.addFirst(m);
		gameStateIdx=-1;
		gameMoveFirst();
	}

	private DefaultMutableTreeNode toNode(Move m, Grid g) {
		DefaultMutableTreeNode node = new DefaultMutableTreeNode(new ExtendedMove(m,g));
		if (m.future != null) {
			Comparator<Move> c;
			switch(m.strategy) { 
			case TERRITORY:
				c=(m.depth&1)==1?IA.BEST_TERRITORY_FIRST:IA.BEST_TERRITORY_LAST;
				break;
			case FIGHT:
				c= IA.BEST_FIGHT_FIRST;
				break;
			case SURVIVAL:
				c= IA.BEST_SURVIVAL_FIRST;
			default:
				c=null;
			}
			if(c!=null) Collections.sort(m.future,  c);
			for (Move f : m.future) {
				node.add(toNode(f,null));
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
			Strategy strategy= (Strategy) strategyCombo.getSelectedItem();
			int timeout= (Integer) timeoutSpinner.getValue();
			int depth= (Integer) depthSpinner.getValue();
			executor.execute(new GameRunner(m, strategy, depth, timeout));
			
		} else {
			ExtendedMove m= gameStates.get(idx);

			if(!playing) {
				gameStateIdx=idx;
				gameStateSlider.setMaximum(gameStates.size()-1);
				gameStateSlider.setValue(idx);
				
				if(m.strategy!=null) strategyCombo.setSelectedItem(m.strategy);
				if(m.depth!=null) depthSpinner.setValue((Integer)m.depth);
				if(m.timeout!=null) timeoutSpinner.setValue((Integer)m.timeout);
				
				DefaultMutableTreeNode root= toNode(m.move, m.grid);
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
		Strategy strategy= (Strategy) strategyCombo.getSelectedItem();
		int timeout= (Integer) timeoutSpinner.getValue();
		int depth= (Integer) depthSpinner.getValue();
		executor.execute(new GameRunner(m, strategy, depth, timeout));
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
		initGame(game);
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
		private ExtendedMove emove;
		private Strategy strategy;
		private int depth;
		private int timeout;
		public GameRunner(ExtendedMove m, Strategy strategy, int depth, int timeout) {
			this.emove = m;
			this.strategy= strategy;
			this.depth=depth;
			this.timeout= timeout;
		}
		
		@Override
		public void run() {
			
			Timer t= new Timer();
			ExtendedMove current=emove;
			Move present= emove.move;
			Move tmp;
			try {
			do {
				int p= current.grid.nextPlayer();
				
				Move lastMove= lastPlayerMove[p];
				if(lastMove==null) lastMove= Move.get();
				if(!lastMove.future.isEmpty()) {
	      			// Try to find if we've already evaluated this position
					switch(lastMove.strategy) {
					case TERRITORY:
		      			for(Iterator<Move> it= lastMove.future.iterator(); it.hasNext(); ) {
		      				Move f= it.next();
		      				if(!f.match(present)) continue;
		      				it.remove();
		      				lastMove=f;
		      				break;
		      			}
		      			break;
					case FIGHT:
						// TODO: Try to find matching future
						lastMove.future.clear();
						break;
					}
				}
				synchronized(executor) {
					long start= System.currentTimeMillis();
					Watchdog dog= timeout==0?Watchdog.getInfinite():Watchdog.getDefault();
					t.schedule(dog, timeout);
					tmp= IA.nextMove(lastMove, current.grid.copy(), p, depth, dog);
					System.out.println("Duration: P"+p+" "+(System.currentTimeMillis()-start));
					dog.cancel();
				}
				present.future= lastMove.future;
				present.strategy= lastMove.strategy;
				present.depth= lastMove.depth;
				if(tmp==null) {
					// Suicide
					tmp= Move.get(p, Direction.NONE);
				} 
				Grid g= tmp.move(current.grid.copy());
				lastPlayerMove[p]= Move.get(tmp);
				
				// Update present
				present= Move.get(-1, present.dir);
				present.player=p;
				present.dir[p]=tmp.dir[p]==Direction.NONE?Direction.ANY:tmp.dir[p];
				
				current= new ExtendedMove(present, g, strategy, depth, timeout);
				gameStates.add(current);
				if(playing) {
					EventQueue.invokeLater(new Runnable() {
						public void run() {
							gameMoveLast();
						}
					});
				}
				
				System.gc();
				
			} while(playing && current.grid.remainingPlayers>1);
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
