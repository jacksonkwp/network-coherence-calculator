package org.cytoscape.networkCoherenceCalculator.internal;

import java.util.Comparator;

import org.cytoscape.model.CyNode;

public class SortBySuid implements Comparator<CyNode>{
	
	@Override
	public int compare(CyNode o1, CyNode o2) {
		return o1.getSUID().compareTo(o2.getSUID());
	}
}
