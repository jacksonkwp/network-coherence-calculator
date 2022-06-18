package org.cytoscape.networkCoherenceCalculator.internal;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.TreeSet;

import org.cytoscape.model.CyEdge;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNode;
import org.cytoscape.model.CyRow;
import org.cytoscape.work.AbstractTask;
import org.cytoscape.work.TaskMonitor;

public class NetCoCalcTask extends AbstractTask {
	
	private final ControlPanelInput input;
	private final ResultsPanel resultsPanel;
	private final DiffGeneCalculator diffGeneCalculator;
	
	public NetCoCalcTask (ControlPanelInput nInput, ResultsPanel nResultsPanel) {
		input = nInput;
		resultsPanel = nResultsPanel;
		diffGeneCalculator = new DiffGeneCalculator();
	}
	
	
	@Override
	public void run(final TaskMonitor taskMonitor) {
		
		//single
		if (!input.isBatch) {
			
			//calculate the network coherence and post the result
			NetCoResult res = singleNetCo(taskMonitor);
			if (cancelled || res == null) return;
			resultsPanel.updateResultSingle(res);
		}
		//batch
		else {
			
			//calculate the differential genes for each sample
			taskMonitor.setProgress(-1);
			taskMonitor.setStatusMessage("Calculating the differentially expressed genes...");
			final DiffGeneResult geneRes = diffGeneCalculator.getResult(input.keyRow, input.keyCol,
					input.firstRow, input.firstCol, input.expressionThreshold, input.inFile);
			
			//inform user if the differential gene calculation failed
			if (geneRes == null) {
				resultsPanel.displayMessage("<html>An error occurred while " +
						"calculating the differentially expressed genes. " +
						"Check that all values in the \"Enter Expressed Genes\" " +
						"section are accurate to the selected file.");
				return;
			}
			
			//check that none of the samples have more differentially than 80% of the total
			int maxGenes = (int) (0.8 * input.refNetwork.getNodeCount());
			boolean maxGenesExceeded = false;
			int geneCount = 0;
			int currentGene;
			for (currentGene = 0; currentGene < geneRes.geneMapper[0].length; currentGene++) { //for each sample
				geneCount = 0;
				for (int j = 0; j < geneRes.geneMapper.length; j++) { //for each gene
					if (geneRes.geneMapper[j][currentGene]) {
						geneCount++;
					}
				}
				if (geneCount > maxGenes) {
					maxGenesExceeded = true;
					break;
				}
			}
			
			//inform user if one of the samples has more differentially than 80% of the total
			if (maxGenesExceeded) {
				resultsPanel.displayMessage("<html>One or more samples " +
						"was calculated to have a list of differentially " +
						"expressed genes that makes up more than 80% " +
						"of the reference network's nodes. Please use a " +
						"larger reference network. If there are genes in " +
						"the file not present in the network, they should " +
						"be removed to not falsely contribute to the count." +
						"<br><br>" +
						"Sample " + geneRes.geneNames[currentGene] +
						" exceeds the max of " + maxGenes + " with a value of " + geneCount + ".");
				return;
			}
			
			//calculate the network coherence and post the result
			NetCoResult res = batchNetCo(taskMonitor, geneRes.geneNames, geneRes.geneMapper);
			if (!cancelled && res != null) {
				resultsPanel.updateResultBatch(geneRes.sampleLabels,res);
			}
		}
	}
	
	
	private NetCoResult singleNetCo (final TaskMonitor taskMonitor) {
		
		//create result object
		NetCoResult res = new NetCoResult();
		res.coherence = new double[1];
		res.notPresent = new HashSet<String>();
		
		//find the suids of the passed gene names
		taskMonitor.setStatusMessage("Locating genes in the reference network...");
		taskMonitor.setProgress(0);
		boolean found;
		List<CyRow> rows = input.refNetwork.getDefaultNodeTable().getAllRows();
		List<Long> suids = new ArrayList<Long>();
		Iterator<CyRow> it;
		for (int i = 0; i < input.geneNames.length; i++) {
			it = rows.iterator();
			found = false;
			while (it.hasNext() && !found) {
				if (cancelled) return null;
				
				CyRow row = it.next();
				if (String.valueOf(row.get(input.compareCol, Object.class)).equals(input.geneNames[i])) {
					found = true;
					suids.add(row.get("SUID", Long.class));
				}
			}
			if (!found) {
				res.notPresent.add(input.geneNames[i]);
			}

			//update progress to (genes found) / (total genes)
			taskMonitor.setProgress((double) (i+1) / input.geneNames.length);
		}
		if (cancelled) return null;
		
		//add the coherence to the result
		taskMonitor.setStatusMessage("Calculating the network coherence...");
		taskMonitor.setProgress(-1);
		res.coherence[0] = coherence(input.refNetwork, suids, input.replicateCount);
		
		return res;
	}
	
	
	private NetCoResult batchNetCo (final TaskMonitor taskMonitor, String[] geneNames, boolean[][] geneMapper) {
		
		//create result object
		NetCoResult res = new NetCoResult();
		res.notPresent = new HashSet<String>();
		
		//find the suid for each genes node
		taskMonitor.setStatusMessage("Locating genes in the reference network...");
		taskMonitor.setProgress(0);
		boolean found;
		long[] suids = new long[geneNames.length];
		List<CyRow> rows = input.refNetwork.getDefaultNodeTable().getAllRows();
		Iterator<CyRow> it;
		for (int i = 0; i < geneNames.length; i++) {
			
			//iterate over the network until the gene is found
			found = false;
			it = rows.iterator();
			while (it.hasNext() && !found) {
				if (cancelled) return null;
				
				CyRow row = it.next();
				if (String.valueOf(row.get(input.compareCol, Object.class)).equals(geneNames[i])) {
					found = true;
					suids[i] = row.get("SUID", Long.class);
				}
			}
			if (!found) {
				res.notPresent.add(geneNames[i]);
				
				//unconsider this gene for differential expression
				for (int j = 0; j < geneMapper[0].length; j++) { //for each sample
					geneMapper[i][j] = false;
				}
			}
			
			//update progress to (genes found) / (total genes)
			taskMonitor.setProgress((double) (i+1) / geneNames.length);
		}
		if (cancelled) return null;
		
		//run getResult for a single sample on each sample
		taskMonitor.setProgress(0);
		taskMonitor.setStatusMessage("Calculating network coherence... (0 of " +
				geneMapper[0].length + " done)");
		res.coherence = new double[geneMapper[0].length];
		for (int i = 0; i < geneMapper[0].length; i++) { //for each sample
			
			//make an list of expressed gene suids for the current sample
			List<Long> sampleSuids = new ArrayList<Long>();
			for (int j = 0; j < geneMapper.length; j++) { //for each gene
				if (geneMapper[j][i]) {
					sampleSuids.add(suids[j]);
				}
			}
			if (cancelled) return null;
			
			//get the sample's coherence and add it to res
			res.coherence[i] = coherence(input.refNetwork, sampleSuids, input.replicateCount);
			
			//update progress to (samples done) / (total samples)
			taskMonitor.setProgress((double) (i+1) / geneMapper[0].length);
			taskMonitor.setStatusMessage("Calculating network coherence... (" + (i+1) + " of " +
					geneMapper[0].length + " done)");
		}
		
		return res;
	}
	
	
	private int connectivity (CyNetwork refNetwork, List<Long> suids) {
		int c = 0;
		
		//get the nodes
		Set<CyNode> nodes = new TreeSet<CyNode>(new SortBySuid());
		for (Long suid : suids) {
			nodes.add(refNetwork.getNode(suid));
		}
			
		List<CyNode> preMarked = new ArrayList<CyNode>();
		for (CyNode node : nodes) {
			
			//check if already marked as having a valid edge
			int index = preMarked.indexOf(node);
			if (index != -1) {
				c++;
				preMarked.remove(index);
				continue;
			}
			
			//check if the node has a valid edge
			List<CyEdge> edges = refNetwork.getAdjacentEdgeList(node, CyEdge.Type.ANY);
			for (CyEdge edge : edges) {
				CyNode source = edge.getSource();
				CyNode target = edge.getTarget();
				
				if (nodes.contains(source) && source != node) {
					c++;
					preMarked.add(source);
					break;
				} else if (nodes.contains(target) && target != node) {
					c++;
					preMarked.add(target);
					break;
				}
			}
		}
	
		return c;
	}
	
	
	private double coherence (CyNetwork refNetwork, List<Long> suids, int replicateCount) {
		
		//save number of expressed genes
		int k = suids.size();
		
		//only calculate network coherence if k > 1
		double coherence;
		if (k > 1) {
			
			//calculate connectivity ratio of expressed genes
			double r = (double)connectivity(refNetwork, suids) / k;
			
			//calculate set of connectivity ratios from random samples
			Random rand = new Random();
			double[] R = new double[replicateCount];
			List<Long> allSuids = refNetwork.getDefaultNodeTable().getColumn("SUID").getValues(Long.class);
			int suidCount = allSuids.size();
			for (int i = 0; i < R.length; i++) {
				if (cancelled) return 0.0;
				
				//choose k random genes and count how many have connections
				List<Long> sampledSuids = new ArrayList<Long>();
				for (int j = 0; j < k; j++) {
					
					//randomly choose a gene, rechoose if it has already been chosen
					Long randSuid = allSuids.get(rand.nextInt(suidCount));
					while (sampledSuids.contains(randSuid.longValue())) {
						randSuid = allSuids.get(rand.nextInt(suidCount));
					}
					sampledSuids.add(randSuid);
				}
				
				//calculate connectivity of this sample
				R[i] = (double)connectivity(refNetwork, sampledSuids) / k;
			}
			
			//calculate coherence as the z-score
			double avrgR = Arrays.stream(R).average().orElse(Double.NaN);
			double sqDistSum = 0;
			for (double ri : R) {
				sqDistSum += Math.pow(ri-avrgR, 2);
			}
			double stdDevR = Math.sqrt(sqDistSum / (replicateCount-1));
			try {
				coherence = (r - avrgR) / stdDevR;
			} catch (Exception e) {
				coherence = Double.NaN;
			}
			
		} else {
			coherence = 0;
		}
		
		return coherence;
	}
	
}
