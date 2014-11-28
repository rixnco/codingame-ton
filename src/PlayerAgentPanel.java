import javax.swing.BorderFactory;
import javax.swing.JPanel;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridBagLayout;
import java.awt.GridBagConstraints;

import javax.swing.JLabel;

import java.awt.Insets;

import java.awt.Font;


public class PlayerAgentPanel extends JPanel {

	private JPanel colorPanel;
	private JLabel lblName;
	private JLabel lblRank;
	private JLabel lblScore;
	private JLabel lblLanguage;

	/**
	 * Create the panel.
	 */
	public PlayerAgentPanel() {
		init(getBackground());
	}
	
	public PlayerAgentPanel(Color c,PlayerAgent agent) {
		init(c);
		
		lblName.setText(agent.name);
		lblRank.setText("["+agent.rank+"]");
		lblScore.setText(""+agent.score);
		lblLanguage.setText(agent.language);
		
	}
	
	private void init(Color c) {
		GridBagLayout gridBagLayout = new GridBagLayout();
		gridBagLayout.columnWidths = new int[]{0, 0, 0};
		gridBagLayout.rowHeights = new int[]{0, 0, 0, 0, 0};
		gridBagLayout.columnWeights = new double[]{0.0, 0.0, Double.MIN_VALUE};
		gridBagLayout.rowWeights = new double[]{0.0, 0.0, 0.0, 0.0, Double.MIN_VALUE};
		setLayout(gridBagLayout);
		
		setBorder(BorderFactory.createLineBorder(c,2));
		
		colorPanel= new JPanel();
		Dimension dim= new Dimension(10,10);
		colorPanel.setPreferredSize(dim);
		colorPanel.setMaximumSize(dim);
		colorPanel.setMinimumSize(dim);
		colorPanel.setBackground(c);

		GridBagConstraints gbc_panel = new GridBagConstraints();
		gbc_panel.weightx = 0.0;
		gbc_panel.weighty = 0.0;
		gbc_panel.gridheight = 4;
		gbc_panel.insets = new Insets(0, 0, 0, 5);
		gbc_panel.fill = GridBagConstraints.BOTH;
		gbc_panel.gridx = 0;
		gbc_panel.gridy = 0;
		add(colorPanel, gbc_panel);
		
		lblName= new JLabel("");
		lblName.setFont(new Font("Tahoma", Font.BOLD, 13));
		GridBagConstraints gbc_lblName = new GridBagConstraints();
		gbc_lblName.fill = GridBagConstraints.HORIZONTAL;
		gbc_lblName.weightx = 1.0;
		gbc_lblName.weighty = 0.0;
		gbc_lblName.insets = new Insets(0, 0, 5, 0);
		gbc_lblName.gridx = 1;
		gbc_lblName.gridy = 0;
		add(lblName, gbc_lblName);
		
		lblRank= new JLabel("");
		lblRank.setFont(new Font("Tahoma", Font.BOLD, 13));
		GridBagConstraints gbc_lblRank = new GridBagConstraints();
		gbc_lblRank.fill = GridBagConstraints.NONE;
		gbc_lblRank.anchor= GridBagConstraints.EAST;
		gbc_lblRank.weightx = 0.0;
		gbc_lblRank.weighty = 0.0;
		gbc_lblRank.insets = new Insets(0, 5, 5, 5);
		gbc_lblRank.gridx = 2;
		gbc_lblRank.gridy = 0;
		add(lblRank, gbc_lblRank);
		
		lblScore= new JLabel("");
		lblScore.setFont(new Font("Tahoma", Font.PLAIN, 10));
		GridBagConstraints gbc_lblScore = new GridBagConstraints();
		gbc_lblScore.fill = GridBagConstraints.HORIZONTAL;
		gbc_lblScore.weightx = 1.0;
		gbc_lblScore.weighty = 0.0;
		gbc_lblScore.insets = new Insets(0, 0, 5, 0);
		gbc_lblScore.gridx = 1;
		gbc_lblScore.gridy = 1;
		add(lblScore, gbc_lblScore);
		
		lblLanguage= new JLabel("");
		lblLanguage.setFont(new Font("Tahoma", Font.PLAIN, 10));
		GridBagConstraints gbc_lblLanguage = new GridBagConstraints();
		gbc_lblLanguage.fill = GridBagConstraints.HORIZONTAL;
		gbc_lblLanguage.weighty = 1.0;
		gbc_lblLanguage.weighty = 0.0;
		gbc_lblLanguage.gridx = 1;
		gbc_lblLanguage.gridy = 2;
		add(lblLanguage, gbc_lblLanguage);
		
		Dimension d= getPreferredSize();
		d.width=Integer.MAX_VALUE;
		setMaximumSize(d);
	}

}
