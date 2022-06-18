package org.cytoscape.networkCoherenceCalculator.internal;

import java.awt.event.ActionEvent;

import org.cytoscape.application.swing.AbstractCyAction;
import org.cytoscape.application.swing.CySwingApplication;
import org.cytoscape.application.swing.CytoPanel;
import org.cytoscape.application.swing.CytoPanelName;
import org.cytoscape.application.swing.CytoPanelState;


public class ResultsPanelAction extends AbstractCyAction {

	private static final long serialVersionUID = 1L;
	private CySwingApplication desktopApp;
	private final CytoPanel cytoPanelEast;
	private ResultsPanel resultsPanel;
	
	public ResultsPanelAction(CySwingApplication desktopApp,
			ResultsPanel myCytoPanel){
		// Add a menu item -- Apps->sample02
		super("Results Panel");
		setPreferredMenu("Apps.Network Coherence Calculator");

		this.desktopApp = desktopApp;
		
		//Note: resultsPanel is bean we defined and registered as a service
		this.cytoPanelEast = this.desktopApp.getCytoPanel(CytoPanelName.EAST);
		this.resultsPanel = myCytoPanel;
	}
	
	public void actionPerformed(ActionEvent e) {
		// If the state of the cytoPanelEast is HIDE, show it
		if (cytoPanelEast.getState() == CytoPanelState.HIDE) {
			cytoPanelEast.setState(CytoPanelState.DOCK);
		}	

		// Select my panel
		int index = cytoPanelEast.indexOfComponent(resultsPanel);
		if (index == -1) {
			return;
		}
		cytoPanelEast.setSelectedIndex(index);
	}

}
