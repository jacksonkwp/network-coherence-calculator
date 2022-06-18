package org.cytoscape.networkCoherenceCalculator.internal;

import java.util.Comparator;

public class SortByValue implements Comparator<KeyValuePair> {

	@Override
	public int compare(KeyValuePair o1, KeyValuePair o2) {
		return ((Double) o1.value).compareTo(o2.value);
	}

}
