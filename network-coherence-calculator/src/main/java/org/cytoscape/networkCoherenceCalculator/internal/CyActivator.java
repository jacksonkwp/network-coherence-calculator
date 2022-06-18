package org.cytoscape.networkCoherenceCalculator.internal;

import org.cytoscape.application.swing.CySwingApplication;

import org.cytoscape.application.swing.CytoPanelComponent;
import org.cytoscape.model.CyNetworkManager;
import org.cytoscape.model.events.NetworkAboutToBeDestroyedListener;
import org.cytoscape.model.events.NetworkAddedListener;
import org.cytoscape.application.swing.CyAction;

import org.osgi.framework.BundleContext;

import org.cytoscape.service.util.AbstractCyActivator;
import org.cytoscape.work.swing.DialogTaskManager;

import java.util.Properties;


public class CyActivator extends AbstractCyActivator {
	public CyActivator() {
		super();
	}


	public void start(BundleContext bc) {
		
		//get needed services
		CySwingApplication cytoscapeDesktopService = getService(bc,CySwingApplication.class);
		CyNetworkManager cyNetworkManager = getService(bc, CyNetworkManager.class);
		DialogTaskManager dialogTaskManager = getService(bc, DialogTaskManager.class);
		
		//instantiate and register results panel and its action
		ResultsPanel resultsPanel = new ResultsPanel();
		ResultsPanelAction resultsPanelAction = new ResultsPanelAction(cytoscapeDesktopService,resultsPanel);
		registerService(bc,resultsPanel,CytoPanelComponent.class, new Properties());
		registerService(bc,resultsPanelAction,CyAction.class, new Properties());
		
		//instantiate and register control panel and its action
		ControlPanel controlPanel = new ControlPanel(resultsPanel, resultsPanelAction, cyNetworkManager, dialogTaskManager);
		ControlPanelAction controlPanelAction = new ControlPanelAction(cytoscapeDesktopService,controlPanel);
		registerService(bc,controlPanel,CytoPanelComponent.class, new Properties());
		registerService(bc,controlPanelAction,CyAction.class, new Properties());
		
		//instantiate listeners to monitor when networks are added or removed
		NetworkListener networkListener = new NetworkListener(controlPanel);
		registerService(bc,networkListener,NetworkAddedListener.class, new Properties());
		registerService(bc,networkListener,NetworkAboutToBeDestroyedListener.class, new Properties());
	}
}

