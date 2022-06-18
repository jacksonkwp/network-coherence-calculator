package org.cytoscape.networkCoherenceCalculator.internal;

import java.awt.Component;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import javax.swing.DefaultListModel;
import javax.swing.GroupLayout;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.GroupLayout.Alignment;

import org.cytoscape.application.swing.CytoPanelComponent;
import org.cytoscape.application.swing.CytoPanelName;
import javax.swing.JLabel;
import javax.swing.JList;

public class ResultsPanel extends JPanel implements CytoPanelComponent {
	
	private static final long serialVersionUID = 8292806967891823933L;
	private static final int message = 0;
	private static final int single = 1;
	private static final int batch = 2;

	private int currentDisplay;
	private String[] sampleLabels;
	private double[] coherences;
	
	private final JFileChooser fileChooser;
	private final JLabel coherence;
	private final DefaultListModel<String> coherenceModel;
	private final DefaultListModel<String> notPresentModel;

	public ResultsPanel() {
		currentDisplay = message;
		
		//initialize results components
		fileChooser = new JFileChooser();
		coherence = new JLabel();
		coherenceModel = new DefaultListModel<String>();
		notPresentModel = new DefaultListModel<String>();
		
		//make place holder
		displayMessage("No computation has been performed.");
	}
	
	
	public void displayMessage(String msg) {
		currentDisplay = message;
		
		JLabel label = new JLabel(msg);
		
		//display the message
		this.removeAll(); //clear panel
		GroupLayout layout = new GroupLayout(this);
		this.setLayout(layout);
		layout.setAutoCreateGaps(true);
		layout.setAutoCreateContainerGaps(true);
		layout.setHorizontalGroup(
		layout.createParallelGroup(Alignment.LEADING)
			.addComponent(label)
		);
		layout.setVerticalGroup(
			layout.createSequentialGroup()
				.addComponent(label)
		);
		this.setVisible(true);
	}
	
	
	private void buildResultLayoutSingle() {
		currentDisplay = single;
		
		//components for network coherence
		final JLabel coherenceLabel = new JLabel("Network Coherence: ");
		
		//components for the genes not present in the reference network
		final JLabel notPresentLabel = new JLabel("Genes not present in reference network:");
		final JScrollPane notPresentPane = new JScrollPane(new JList<String>(notPresentModel));
		
		//format panel layout
		this.removeAll(); //clear panel
		GroupLayout layout = new GroupLayout(this);
		this.setLayout(layout);
		layout.setAutoCreateGaps(true);
		layout.setAutoCreateContainerGaps(true);
		layout.setHorizontalGroup(
		layout.createParallelGroup(Alignment.LEADING)
			.addGroup(layout.createSequentialGroup()
				.addComponent(coherenceLabel)
				.addComponent(coherence)
			)
			.addComponent(notPresentLabel)
			.addComponent(notPresentPane)
		);
		layout.setVerticalGroup(
		layout.createSequentialGroup()
			.addGroup(layout.createParallelGroup()
				.addComponent(coherenceLabel)
				.addComponent(coherence)
			)
			.addComponent(notPresentLabel)
			.addComponent(notPresentPane, GroupLayout.DEFAULT_SIZE, 170, 170)
		);
		this.setVisible(true);
	}


	public void updateResultSingle(NetCoResult res) {
		if (currentDisplay != single) {
			this.buildResultLayoutSingle();
		}
		
		//set coherence
		coherence.setText(String.format("%.3e", res.coherence[0]));
		
		//set the notPresentList to contain the proper elements
		notPresentModel.clear();
		for (String gene : res.notPresent) {
			notPresentModel.addElement(gene);
		}
		notPresentModel.trimToSize();
	}


	public void buildResultLayoutBatch() {
		currentDisplay = batch;
		
		//components for network coherence
		final JLabel coherenceLabel = new JLabel("Network Coherences: ");
		final JScrollPane coherencePane = new JScrollPane(new JList<String>(coherenceModel));
		
		//button for saving network coherence
		final JButton saveButton = new JButton("Save to File");
		saveButton.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				int returnVal = fileChooser.showSaveDialog(ResultsPanel.this);
				if (returnVal == JFileChooser.APPROVE_OPTION) {
	                
					//ensure files is a csv
					String outPath = fileChooser.getSelectedFile().getAbsolutePath();
	                if (!outPath.endsWith(".csv")) {
	                	outPath += ".csv";
	                }
	                
	                try {
	                	//prepare outFile for writing
	                	File outFile = new File(outPath);
	        			outFile.createNewFile();
	        			FileWriter writer = new FileWriter(outFile);
	        			
	        			//write sample labels and network coherence in csv format
	        			for (int i = 0; i < sampleLabels.length; i++) {
	        				writer.write(sampleLabels[i] + "," + coherences[i] + "\n");
	        			}
	        			writer.close();
	        		} catch (IOException e) {}
	            }
			}
		});
		
		
		//components for the genes not present in the reference network
		final JLabel notPresentLabel = new JLabel("Genes not present in reference network:");
		final JScrollPane notPresentPane = new JScrollPane(new JList<String>(notPresentModel));
		
		//format panel layout
		this.removeAll(); //clear panel
		GroupLayout layout = new GroupLayout(this);
		this.setLayout(layout);
		layout.setAutoCreateGaps(true);
		layout.setAutoCreateContainerGaps(true);
		layout.setHorizontalGroup(
		layout.createParallelGroup(Alignment.LEADING)
			.addComponent(coherenceLabel)
			.addComponent(coherencePane)
			.addComponent(saveButton)
			.addComponent(notPresentLabel)
			.addComponent(notPresentPane)
		);
		layout.setVerticalGroup(
		layout.createSequentialGroup()
			.addComponent(coherenceLabel)
			.addComponent(coherencePane, GroupLayout.DEFAULT_SIZE, 170, 170)
			.addComponent(saveButton)
			.addComponent(notPresentLabel)
			.addComponent(notPresentPane, GroupLayout.DEFAULT_SIZE, 170, 170)
		);
		this.setVisible(true);
	}
	
	
	public void updateResultBatch(String[] nSampleLabels, NetCoResult res) {
		
		if (currentDisplay != batch) {
			this.buildResultLayoutBatch();
		}
		
		//store data for potential file save
		sampleLabels = nSampleLabels;
		coherences = res.coherence;
		
		//set coherenceModel to contain the proper elements
		coherenceModel.clear();
		for (int i = 0; i < nSampleLabels.length; i++) {
			coherenceModel.addElement(nSampleLabels[i] + ": " + String.format("%.3e", res.coherence[i]));
		}
		coherenceModel.trimToSize();
		
		//set the notPresentModel to contain the proper elements
		notPresentModel.clear();
		for (String gene : res.notPresent) {
			notPresentModel.addElement(gene);
		}
		notPresentModel.trimToSize();
	}
	
	
	public Component getComponent() {
		return this;
	}


	public CytoPanelName getCytoPanelName() {
		return CytoPanelName.EAST;
	}


	public String getTitle() {
		return "Network Coherence Result";
	}


	public Icon getIcon() {
		return null;
	}
}
