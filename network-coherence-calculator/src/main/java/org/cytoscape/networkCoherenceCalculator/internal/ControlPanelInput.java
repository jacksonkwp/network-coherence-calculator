package org.cytoscape.networkCoherenceCalculator.internal;

import java.io.File;

import org.cytoscape.model.CyNetwork;

public class ControlPanelInput {
	
	//single and batch
	public CyNetwork refNetwork;
	public String compareCol;
	public int replicateCount;
	public boolean isBatch;

	//single only
	public String[] geneNames;
	
	//batch only
	public int keyRow;
	public int keyCol;
	public int firstRow;
	public int firstCol;
	public double expressionThreshold;
	public File inFile;
}
