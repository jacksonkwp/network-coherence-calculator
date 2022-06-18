package org.cytoscape.networkCoherenceCalculator.internal;

import java.io.File;
import java.util.PriorityQueue;
import java.util.Scanner;

public class DiffGeneCalculator {
	
	public DiffGeneResult getResult (int keyRow, int keyCol, int firstRow,
			int firstCol, double expressionThreshold, File inFile) {
		
		DiffGeneResult res;
		Scanner scanner = null;
		
		try {
			
			//count the samples and extract their labels
			scanner = new Scanner(inFile);
			int curRow = 1;
			for (; curRow < keyRow; curRow++) {  //skip to key row
				scanner.nextLine();
			}
			String[] sampleLabelRow = scanner.nextLine().split("\t");
			int sampleCount = sampleLabelRow.length - (firstCol-1);
			curRow++;
			
			//count the genes
			int geneCount = 0;
			for (; curRow < firstRow; curRow++) {  //skip to first data row
				scanner.nextLine();
			}
			while (scanner.hasNextLine()) {
				scanner.nextLine();
				geneCount++;
			}
			scanner.close();
			
			//create result object and copy in sample labels
			res = new DiffGeneResult(geneCount, sampleCount);
			System.arraycopy(sampleLabelRow, firstCol-1, res.sampleLabels, 0, res.sampleLabels.length);

			//calculate how many genes are considered differentially expressed
			int selectCount = Math.max((int) (((expressionThreshold/100) * sampleCount) + 0.5), 1);
			
			//read in the gene labels and calculate differentially expressed genes
			scanner = new Scanner(inFile);
			for (int i = 1; i < firstRow; i++) {  //skip to first data row
				scanner.nextLine();
			}
			for (int i = 0; scanner.hasNextLine(); i++) {  //for every data line
				String[] row = scanner.nextLine().split("\t");
				res.geneNames[i] = row[keyCol-1];
				
				//initialize topVals with the first selectCount values
				PriorityQueue<KeyValuePair> topVals =
						new PriorityQueue<KeyValuePair>(selectCount, new SortByValue());
				for (int j = 0; j < selectCount; j++) {
					double val = Double.parseDouble(row[(firstCol-1) + j]);
					topVals.add(new KeyValuePair(j, val));
				}
				
				//update values in topVals with larger values if found
				for (int j = selectCount; j < sampleCount; j++) {
					double val = Double.parseDouble(row[(firstCol-1) + j]);
					if (val > topVals.peek().value) {
						topVals.poll();
						topVals.add(new KeyValuePair(j, val));
					}
				}
				
				//mark topVals as differentially expressed
				for (KeyValuePair pair : topVals) {
					if (pair.value != 0) {
						res.geneMapper[i][pair.key] = true;
					}
				}
			}
			scanner.close();
		
		} catch (Exception e) {
			scanner.close();
			res = null; //invalidate result
		}
		
		return res;
	}
	
}
