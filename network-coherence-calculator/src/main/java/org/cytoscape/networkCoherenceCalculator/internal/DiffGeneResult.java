package org.cytoscape.networkCoherenceCalculator.internal;

public class DiffGeneResult {
	public String[] geneNames;
	public String[] sampleLabels;
	public boolean[][] geneMapper;
	
	public DiffGeneResult (int geneCount, int sampleCount) {
		geneNames = new String[geneCount];
		sampleLabels = new String[sampleCount];
		geneMapper = new boolean[geneCount][sampleCount];
	}
}
