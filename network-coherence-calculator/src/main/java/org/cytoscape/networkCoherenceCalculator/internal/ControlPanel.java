package org.cytoscape.networkCoherenceCalculator.internal;

import java.awt.CardLayout;
import java.awt.Component;
import java.text.NumberFormat;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.GroupLayout;
import javax.swing.GroupLayout.Alignment;
import javax.swing.Icon;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.text.NumberFormatter;
import javax.swing.JList;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JFormattedTextField;
import javax.swing.JLabel;

import org.cytoscape.application.swing.CytoPanelComponent;
import org.cytoscape.application.swing.CytoPanelName;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNetworkManager;
import org.cytoscape.work.TaskIterator;
import org.cytoscape.work.swing.DialogTaskManager;

public class ControlPanel extends JPanel implements CytoPanelComponent {
	
	private static final long serialVersionUID = 8292806967891823933L;
	private final ResultsPanel resultsPanel;
	private final ResultsPanelAction resultsPanelAction;
	private final CyNetworkManager cyNetworkManager;
	private final DialogTaskManager dialogTaskManager;
	private final DiffGeneCalculator diffgeneCalculator;

	private final JComboBox<CyNetwork> refNetwork;
	private final JComboBox<String> compareCol;
	private boolean isBatch;
	private final DefaultListModel<String> geneListModel;
	private final JFormattedTextField replicateCount;

	private final JFormattedTextField keyRow;
	private final JFormattedTextField keyCol;
	private final JFormattedTextField firstRow;
	private final JFormattedTextField firstCol;
	private final JFormattedTextField highExpressThreshold;
	private final JFileChooser fileChooser;

	public ControlPanel(ResultsPanel rp, ResultsPanelAction rpa, CyNetworkManager nm, DialogTaskManager dtm) {

		resultsPanel = rp;
		resultsPanelAction = rpa;
		cyNetworkManager = nm;
		dialogTaskManager = dtm;
		diffgeneCalculator = new DiffGeneCalculator();
		
		//create a text formatter for integers to use in various JFormattedTextFields
		NumberFormatter intFormatter = new NumberFormatter(NumberFormat.getInstance());
		intFormatter.setValueClass(Integer.class);
		intFormatter.setMinimum(2);
		intFormatter.setMaximum(Integer.MAX_VALUE);
		intFormatter.setAllowsInvalid(true);
		intFormatter.setCommitsOnValidEdit(true);
	    
		//create a text formatter for integers to use in various JFormattedTextFields
		NumberFormatter percentFormatter = new NumberFormatter(NumberFormat.getInstance());
		percentFormatter.setValueClass(Double.class);
		percentFormatter.setMinimum(0.001);
		percentFormatter.setMaximum(100.0);
		percentFormatter.setAllowsInvalid(true);
		percentFormatter.setCommitsOnValidEdit(true);
		
		//-----Select Reference Network Panel----------------------------------
		
		//create labels for the combo boxes
		final JLabel netLabel = new JLabel("Network:");
		final JLabel colLabel = new JLabel("Gene name column:");
		
		//create a combo box populated with the open networks
		Set<CyNetwork> netSet = cyNetworkManager.getNetworkSet();
		refNetwork = new JComboBox<CyNetwork>(netSet.toArray(new CyNetwork[netSet.size()]));
		
		//create a combo box populated with the fullyQualifiedNames of the selected network
		compareCol = new JComboBox<String>();
		refNetwork.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				compareCol.removeAllItems();
				
				//if there is a selected network, populate compareCol with the list of
				//fullyQualifiedNames from that network grouped by namespace then alphabetized
				if (refNetwork.getItemCount() > 0) {
					try {
						Set<String> colSet = ((CyNetwork)refNetwork.getSelectedItem()).getDefaultNodeTable()
														.getAllRows().get(0).getAllValues().keySet();
						
						//group labels with names spaces
						Set<String> nameSpace = new HashSet<String>();
						Set<String> noNameSpace = new HashSet<String>();
						for (String item : colSet) {
							if (item.contains("::")) {
								nameSpace.add(item);
							} else {
								noNameSpace.add(item);
							}
						}
						
						//convert the sets to arrays and sort alphabetically
						String[] nameSpaceArr = nameSpace.toArray(new String[nameSpace.size()]);
						String[] noNameSpaceArr = noNameSpace.toArray(new String[noNameSpace.size()]);
						Arrays.sort(nameSpaceArr);
						Arrays.sort(noNameSpaceArr);
						
						//add the array contents to the combo box
						for (String item : noNameSpaceArr) {
							compareCol.addItem(item);
						}
						for (String item : nameSpaceArr) {
							compareCol.addItem(item);
						}
						
					} catch (Exception e) {}
				}
			}
		});
		try {
			refNetwork.setSelectedIndex(0);
		} catch (Exception e) {}
		
		//create and format "Select Reference Network" panel layout
		final JPanel selectNetPanel = new JPanel();
		selectNetPanel.setBorder(BorderFactory.createTitledBorder("Select Reference Network"));
		GroupLayout selectNetLayout = new GroupLayout(selectNetPanel);
		selectNetPanel.setLayout(selectNetLayout);
		selectNetLayout.setAutoCreateGaps(true);
		selectNetLayout.setAutoCreateContainerGaps(true);
		selectNetLayout.setHorizontalGroup(
			selectNetLayout.createParallelGroup(Alignment.CENTER)
				.addGroup(selectNetLayout.createSequentialGroup()
					.addComponent(netLabel)
					.addComponent(refNetwork)
				)
				.addGroup(selectNetLayout.createSequentialGroup()
					.addComponent(colLabel)
					.addComponent(compareCol)
				)
		);
		selectNetLayout.setVerticalGroup(
			selectNetLayout.createSequentialGroup()
				.addGroup(selectNetLayout.createParallelGroup(Alignment.CENTER)
					.addComponent(netLabel)
					.addComponent(refNetwork, GroupLayout.DEFAULT_SIZE, 20, 20)
				)
				.addGroup(selectNetLayout.createParallelGroup(Alignment.CENTER)
					.addComponent(colLabel)
					.addComponent(compareCol, GroupLayout.DEFAULT_SIZE, 20, 20)
				)
		);
		
		//-----Enter Expressed Genes Card Single-------------------------------
		
		//create list in a scrollPane to display expressed genes
		geneListModel = new DefaultListModel<String>();
		final JList<String> geneList = new JList<String>(geneListModel);
		final JScrollPane genePane = new JScrollPane(geneList);
		
		//create text field to enter genes
		final JTextField newGene = new JTextField();
		newGene.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				
				//get value from text field then clear it
				String geneName = newGene.getText();
				newGene.setText("");
				
				//check if the new value is a duplicate, if not add it to the list
				for (int i = 0; i < geneListModel.getSize(); i++) {
					if (geneName.equals(geneListModel.get(i))) {
						return;
					}
				}
				geneListModel.addElement(geneName);
			}
		});
		
		//create button to add genes to the list
		final JButton addBtn = new JButton("Add");
		addBtn.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				newGene.postActionEvent();
			}
		});
		
		//create button to remove selected genes
		final JButton rmBtn = new JButton("Remove");
		rmBtn.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				
				//delete selected elements by reverse order of their indices
				int[] indices = geneList.getSelectedIndices();
				for (int i = indices.length-1; i >= 0; i--) {
					geneListModel.remove(indices[i]);
				}
			}
		});
		
		//create button to remove selected genes
		final JButton rmAllBtn = new JButton("Remove All");
		rmAllBtn.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				geneListModel.clear();
			}
		});
		
		//create and format "Enter Expressed Genes" card single layout
		final JPanel enterGenesCardSingle = new JPanel();
		GroupLayout enterGenesSingleLayout = new GroupLayout(enterGenesCardSingle);
		enterGenesCardSingle.setLayout(enterGenesSingleLayout);
		enterGenesSingleLayout.setAutoCreateGaps(true);
		enterGenesSingleLayout.setAutoCreateContainerGaps(true);
		enterGenesSingleLayout.setHorizontalGroup(
				enterGenesSingleLayout.createParallelGroup(Alignment.TRAILING)
					.addGroup(enterGenesSingleLayout.createSequentialGroup()
						.addComponent(newGene)
						.addComponent(addBtn)
					)
					.addComponent(genePane)
					.addGroup(enterGenesSingleLayout.createSequentialGroup()
						.addComponent(rmBtn)
						.addComponent(rmAllBtn)
					)
		);
		enterGenesSingleLayout.setVerticalGroup(
				enterGenesSingleLayout.createSequentialGroup()
				.addGroup(enterGenesSingleLayout.createParallelGroup(Alignment.CENTER)
					.addComponent(newGene, GroupLayout.DEFAULT_SIZE, 20, 20)
					.addComponent(addBtn)
				)
				.addComponent(genePane, GroupLayout.DEFAULT_SIZE, 170, 170)
				.addGroup(enterGenesSingleLayout.createParallelGroup(Alignment.CENTER)
					.addComponent(rmBtn)
					.addComponent(rmAllBtn)
				)
		);
		
		//-----Enter Expressed Genes Card Batch--------------------------------
		
		//create labels for the data fields
		final JLabel keyRowLabel = new JLabel("Sample labels are in row:");
		final JLabel keyColLabel = new JLabel("Gene labels are in column:");
		final JLabel firstRowLabel = new JLabel("Data starts on row:");
		final JLabel firstColLabel = new JLabel("Data starts on column:");
		final JLabel highExpressThresholdLabel = new JLabel("High expression threshold (%):");
		final JLabel fileTypeLabel = new JLabel("(Tab seperated value file)");
		
		
		//create data fields
		keyRow = new JFormattedTextField(intFormatter);
		keyCol = new JFormattedTextField(intFormatter);
		firstRow = new JFormattedTextField(intFormatter);
		firstCol = new JFormattedTextField(intFormatter);
		highExpressThreshold = new JFormattedTextField(percentFormatter);
		
		//init data fields
		keyRow.setValue(1);
		keyCol.setValue(1);
		firstRow.setValue(2);
		firstCol.setValue(2);
		highExpressThreshold.setValue(5.0);
		
		//create file chooser, its button and label
		fileChooser = new JFileChooser();
		fileChooser.setFileFilter(new FileNameExtensionFilter("Text File (.txt)", "txt"));
		fileChooser.setAcceptAllFileFilterUsed(false);
		final JButton selectFile = new JButton("Select a File");
		final JLabel selectedFile = new JLabel("...");
		
		//setup file chooser button action
		selectFile.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				int returnVal = fileChooser.showOpenDialog(ControlPanel.this);
				if (returnVal == JFileChooser.APPROVE_OPTION) {
	                selectedFile.setText(fileChooser.getSelectedFile().getName());
	            }
			}
		});
		
		//create and format "Enter Expressed Genes" card batch layout
		final JPanel enterGenesCardBatch = new JPanel();
		GroupLayout enterGenesBatchLayout = new GroupLayout(enterGenesCardBatch);
		enterGenesCardBatch.setLayout(enterGenesBatchLayout);
		enterGenesBatchLayout.setAutoCreateGaps(true);
		enterGenesBatchLayout.setAutoCreateContainerGaps(true);
		enterGenesBatchLayout.setHorizontalGroup(
				enterGenesBatchLayout.createParallelGroup()
					.addGroup(enterGenesBatchLayout.createSequentialGroup()
						.addComponent(keyRowLabel)
						.addComponent(keyRow)
					)
					.addGroup(enterGenesBatchLayout.createSequentialGroup()
						.addComponent(keyColLabel)
						.addComponent(keyCol)
					)
					.addGroup(enterGenesBatchLayout.createSequentialGroup()
						.addComponent(firstRowLabel)
						.addComponent(firstRow)
					)
					.addGroup(enterGenesBatchLayout.createSequentialGroup()
						.addComponent(firstColLabel)
						.addComponent(firstCol)
					)
					.addGroup(enterGenesBatchLayout.createSequentialGroup()
						.addComponent(highExpressThresholdLabel)
						.addComponent(highExpressThreshold)
					)
					.addGroup(enterGenesBatchLayout.createSequentialGroup()
						.addComponent(selectFile)
						.addComponent(selectedFile)
					)
					.addComponent(fileTypeLabel)
		);
		enterGenesBatchLayout.setVerticalGroup(
				enterGenesBatchLayout.createSequentialGroup()
					.addGroup(enterGenesBatchLayout.createParallelGroup(Alignment.CENTER)
						.addComponent(keyRowLabel)
						.addComponent(keyRow, GroupLayout.DEFAULT_SIZE, 20, 20)
					)
					.addGroup(enterGenesBatchLayout.createParallelGroup(Alignment.CENTER)
						.addComponent(keyColLabel)
						.addComponent(keyCol, GroupLayout.DEFAULT_SIZE, 20, 20)
					)
					.addGroup(enterGenesBatchLayout.createParallelGroup(Alignment.CENTER)
						.addComponent(firstRowLabel)
						.addComponent(firstRow, GroupLayout.DEFAULT_SIZE, 20, 20)
					)
					.addGroup(enterGenesBatchLayout.createParallelGroup(Alignment.CENTER)
						.addComponent(firstColLabel)
						.addComponent(firstCol, GroupLayout.DEFAULT_SIZE, 20, 20)
					)
					.addGroup(enterGenesBatchLayout.createParallelGroup(Alignment.CENTER)
						.addComponent(highExpressThresholdLabel)
						.addComponent(highExpressThreshold, GroupLayout.DEFAULT_SIZE, 20, 20)
					)
					.addGroup(enterGenesBatchLayout.createParallelGroup(Alignment.CENTER)
						.addComponent(selectFile)
						.addComponent(selectedFile)
					)
					.addComponent(fileTypeLabel)
		);
		
		//-----Enter Expressed Genes Main Panel--------------------------------
		
		//Create cards panel
		final JPanel enterGenesCards = new JPanel(new CardLayout());
		final String keySingle = "single";
		final String keyBatch = "batch";
		enterGenesCards.add(enterGenesCardSingle, keySingle);
		enterGenesCards.add(enterGenesCardBatch, keyBatch);
		
		//create radio buttons to choose input method
		final JRadioButton singleButton = new JRadioButton("Set of differentially expressed genes");
		final JRadioButton batchButton = new JRadioButton("File with expression data for multiple samples");
		singleButton.setSelected(true);
		isBatch = false;
		
		//set actions for radio buttons
		singleButton.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				if (singleButton.isSelected()) {
					batchButton.setSelected(false);
					isBatch = false;
					CardLayout cl = (CardLayout)(enterGenesCards.getLayout());
		        	cl.show(enterGenesCards, keySingle);
				} else {
					singleButton.setSelected(true);
					batchButton.setSelected(false);
				}
			}
		});
		batchButton.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				if (batchButton.isSelected()) {
					singleButton.setSelected(false);
					isBatch = true;
					CardLayout cl = (CardLayout)(enterGenesCards.getLayout());
					cl.show(enterGenesCards, keyBatch);
				} else {
					batchButton.setSelected(true);
					singleButton.setSelected(false);
				}
			}
		});
		
		//create and format "Enter Expressed Genes" panel layout
		final JPanel enterGenesPanel = new JPanel();
		enterGenesPanel.setBorder(BorderFactory.createTitledBorder("Enter Expressed Genes"));
		GroupLayout enterGenesLayout = new GroupLayout(enterGenesPanel);
		enterGenesPanel.setLayout(enterGenesLayout);
		enterGenesLayout.setAutoCreateGaps(true);
		enterGenesLayout.setAutoCreateContainerGaps(true);
		enterGenesLayout.setHorizontalGroup(
				enterGenesLayout.createParallelGroup()
					.addComponent(singleButton)
					.addComponent(batchButton)
					.addComponent(enterGenesCards)
		);
		enterGenesLayout.setVerticalGroup(
				enterGenesLayout.createSequentialGroup()
					.addComponent(singleButton)
					.addComponent(batchButton)
					.addComponent(enterGenesCards, GroupLayout.DEFAULT_SIZE, 250, 250)
		);
		
		//-----Enter Number of Null Model Replicates Panel---------------------
		
		//create a text field for number of replicates
	    replicateCount = new JFormattedTextField(intFormatter);
	    replicateCount.setValue(2000);
		
		
		//create and format "Enter Number of Null Model Replicates" panel layout
		final JPanel enterReplicateCountPanel = new JPanel();
		enterReplicateCountPanel.setBorder(BorderFactory.createTitledBorder("Enter Number of Null Model Replicates"));
		GroupLayout enterReplicateCountLayout = new GroupLayout(enterReplicateCountPanel);
		enterReplicateCountPanel.setLayout(enterReplicateCountLayout);
		enterReplicateCountLayout.setAutoCreateGaps(true);
		enterReplicateCountLayout.setAutoCreateContainerGaps(true);
		enterReplicateCountLayout.setHorizontalGroup(
				enterReplicateCountLayout.createParallelGroup(Alignment.CENTER)
				.addComponent(replicateCount)
		);
		enterReplicateCountLayout.setVerticalGroup(
				enterReplicateCountLayout.createSequentialGroup()
				.addComponent(replicateCount, GroupLayout.DEFAULT_SIZE, 20, 20)
		);
		
		//---------------------------------------------------------------------
		
		//create a button that triggers the results calculation and display
		final JButton calcBtn = new JButton("Calculate");
		calcBtn.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				calculate();
			}
		});
		
		//format base panel layout
		GroupLayout layout = new GroupLayout(this);
		this.setLayout(layout);
		layout.setAutoCreateGaps(true);
		layout.setAutoCreateContainerGaps(true);
		layout.setHorizontalGroup(
		layout.createParallelGroup(Alignment.CENTER)
			.addComponent(selectNetPanel)
			.addComponent(enterGenesPanel)
			.addComponent(enterReplicateCountPanel)
			.addComponent(calcBtn)
		);
		layout.setVerticalGroup(
		layout.createSequentialGroup()
			.addComponent(selectNetPanel)
			.addComponent(enterGenesPanel)
			.addComponent(enterReplicateCountPanel)
			.addComponent(calcBtn)
		);
		this.setVisible(true);
	}


	private void calculate() {
		
		//open the results panel
		resultsPanelAction.actionPerformed(null);
		
		//get validated user input and calculate
		ControlPanelInput input = getInput();
		if (input != null) {
			
			//execute the calculation on another thread
			NetCoCalcTask task = new NetCoCalcTask(input, resultsPanel);
			dialogTaskManager.execute(new TaskIterator(task));
		}
	}
	
	
	private ControlPanelInput getInput () {
		ControlPanelInput input = new ControlPanelInput();
		input.isBatch = isBatch;
		String err = "<html>";
		
		//get input values for single and batch
		try {
			input.refNetwork = (CyNetwork) refNetwork.getSelectedItem();
		} catch (Exception e) {
			input.refNetwork = null;
		}
		try {
			input.compareCol = (String) compareCol.getSelectedItem();
		} catch (Exception e) {
			input.compareCol = null;
		}
		try {
			input.replicateCount = (Integer) replicateCount.getValue();
		} catch (Exception e) {
			input.replicateCount = 0;
		}
		
		//check if input is valid for single and batch
		if (input.refNetwork == null) err += "No reference network was provided.<br>";
		if (input.compareCol == null) err += "No gene name column was provided.<br>";
		if (input.replicateCount < 1) err += "An invalid number of samples was passed.<br>";
		
		//single only input
		if (!isBatch) {
			
			//get input for single
			try {
				input.geneNames = Arrays.copyOf(geneListModel.toArray(), geneListModel.getSize(), String[].class);
			} catch (Exception e) {
				input.geneNames = null;
			}
			
			//check if input is valid for single
			if (input.geneNames == null || input.geneNames.length < 1) {
				err += "No expressed genes were provided.<br>";
			} else if (input.geneNames.length == 1) {
				err += "At least two expressed genes must be provided.<br>";
			}
			if (input.geneNames.length > (int) (0.8 * input.refNetwork.getNodeCount())) {
				err += "The number of genes entered makes up more than 80% of the " +
						"reference network's nodes. Please use a larger reference " +
						"network. If there are genes in the list not present in " +
						"the network, they should be removed to not falsely " +
						"contribute to the count.<br>";
			}
		}
		//batch only input
		else {
			
			//get input for batch
			input.inFile = fileChooser.getSelectedFile();
			try {
				input.keyRow = (Integer) keyRow.getValue();
			} catch (Exception e) {
				input.keyRow = 0;
			}
			try {
				input.keyCol = (Integer) keyCol.getValue();
			} catch (Exception e) {
				input.keyCol = 0;
			}
			try {
				input.firstRow = (Integer) firstRow.getValue();
			} catch (Exception e) {
				input.firstRow = 0;
			}
			try {
				input.firstCol = (Integer) firstCol.getValue();
			} catch (Exception e) {
				input.firstCol = 0;
			}
			try {
				input.expressionThreshold = (Double) highExpressThreshold.getValue();
			} catch (Exception e) {
				input.expressionThreshold = 0;
			}
			
			//check if input is valid for batch
			if (input.keyRow < 1) err += "An invalid sample label row was passed.<br>";
			if (input.keyCol < 1) err += "An invalid sample label column was passed.<br>";
			if (input.firstRow < 1) err += "An invalid first data row was passed.<br>";
			else if (input.firstRow <= input.keyRow) err += "Data must start after the label row.<br>";
			if (input.firstCol < 1) err += "An invalid first data column was passed.<br>";
			else if (input.firstCol <= input.keyCol) err += "Data must start after the label column.<br>";
			if (input.expressionThreshold <= 0)
				err += "An invalid differential expression threshold was passed.<br>";
			if (input.inFile == null) {
				err += "No file was selected.<br>";
			} else {
				int extentionI = input.inFile.getName().lastIndexOf(".");
				if (extentionI < 0 || !input.inFile.getName().substring(extentionI+1).equals("txt")) {
					err += "A file with an invalid name or extension was selected.<br>";
				}
			}
		}
		
		//return the input if there are no errors, else display the error message
		if (err.equals("<html>")) {
			return input;
		} else {
			resultsPanel.displayMessage(err);
			return null;
		}
	}
	
	
	//allows NetworkListener.java to add a network to the JCombobox options
	//if one is added to the Cytoscape session
	public void addNetwork (CyNetwork net) {
		refNetwork.addItem(net);
	}


	//allows NetworkListener.java to remove a network to the JCombobox options
	//if one is removed from the Cytoscape session
	public void removeNetwork (CyNetwork net) {
		refNetwork.removeItem(net);
	}


	public Component getComponent() {
		return this;
	}


	public CytoPanelName getCytoPanelName() {
		return CytoPanelName.WEST;
	}


	public String getTitle() {
		return "Network Coherence Calculator";
	}


	public Icon getIcon() {
		return null;
	}
}
