package org.cytoscape.networkCoherenceCalculator.internal;

import org.cytoscape.model.events.NetworkAboutToBeDestroyedEvent;
import org.cytoscape.model.events.NetworkAboutToBeDestroyedListener;
import org.cytoscape.model.events.NetworkAddedEvent;
import org.cytoscape.model.events.NetworkAddedListener;


public class NetworkListener implements NetworkAddedListener, NetworkAboutToBeDestroyedListener {
	
	private final ControlPanel controlPanel;
	
	public NetworkListener (ControlPanel cp) {
		controlPanel = cp;
	}
	
	public void handleEvent(NetworkAddedEvent e){
		controlPanel.addNetwork(e.getNetwork());
	}

	public void handleEvent(NetworkAboutToBeDestroyedEvent e){
		controlPanel.removeNetwork(e.getNetwork());
	}
}
