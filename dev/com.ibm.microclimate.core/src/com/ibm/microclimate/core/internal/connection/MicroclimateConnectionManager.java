/*******************************************************************************
 * IBM Confidential
 * OCO Source Materials
 * (C) Copyright IBM Corp. 2018 All Rights Reserved.
 * The source code for this program is not published or otherwise
 * divested of its trade secrets, irrespective of what has
 * been deposited with the U.S. Copyright Office.
 *******************************************************************************/

package com.ibm.microclimate.core.internal.connection;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;

import com.ibm.microclimate.core.MicroclimateCorePlugin;
import com.ibm.microclimate.core.internal.MCLogger;
import com.ibm.microclimate.core.internal.MCUtil;
import com.ibm.microclimate.core.internal.MicroclimateObjectFactory;;

/**
 * Singleton class to keep track of the list of current Microclimate Connections,
 * and manage persisting them to and from the Preferences.
 */
public class MicroclimateConnectionManager {

	// Singleton instance. Never access this directly. Use the instance() method.
	private static MicroclimateConnectionManager instance;

	public static final String CONNECTION_LIST_PREFSKEY = "mcc-connections"; //$NON-NLS-1$

	private List<MicroclimateConnection> connections = new ArrayList<>();
	// this list tracks the URLs of connections that have never successfully connected
	private List<String> brokenConnections = new ArrayList<>();

	private MicroclimateConnectionManager() {
		instance = this;
		// MCLogger.log("Init MicroclimateConnectionManager");
		loadFromPreferences();

		// Add a preference listener to reload the cached list of connections each time it's modified.
		MicroclimateCorePlugin.getDefault().getPreferenceStore()
			.addPropertyChangeListener(new IPropertyChangeListener() {
				@Override
				public void propertyChange(PropertyChangeEvent event) {
				    if (event.getProperty() == MicroclimateConnectionManager.CONNECTION_LIST_PREFSKEY) {
				    	// MCLogger.log("Loading prefs in MCCM");
				        loadFromPreferences();
				    }
				}
			});
	}

	private static MicroclimateConnectionManager instance() {
		if (instance == null) {
			instance = new MicroclimateConnectionManager();
		}
		return instance;
	}

	/**
	 * Adds the given connection to the list of connections.
	 */
	public synchronized static void add(MicroclimateConnection connection) {
		if (connection == null) {
			MCLogger.logError("Null connection passed to be added"); //$NON-NLS-1$
			return;
		}

		instance().connections.add(connection);
		MCLogger.log("Added a new MCConnection: " + connection.baseUrl); //$NON-NLS-1$
		instance().writeToPreferences();
	}

	/**
	 * @return An <b>unmodifiable</b> copy of the list of existing MC connections.
	 */
	public synchronized static List<MicroclimateConnection> activeConnections() {
		return Collections.unmodifiableList(instance().connections);
	}

	public synchronized static MicroclimateConnection getActiveConnection(String baseUrl) {
		for(MicroclimateConnection mcc : activeConnections()) {
			if(mcc.baseUrl.toString().equals(baseUrl)) {
				return mcc;
			}
		}
		return null;
	}

	public synchronized static int activeConnectionsCount() {
		return instance().connections.size();
	}

	/**
	 * Try to remove the given connection. Removal will fail if the connection is still in use (ie. has a linked app).
	 * @return
	 * 	true if the connection was removed,
	 * 	false if not because it didn't exist.
	 */
	synchronized static boolean remove(String baseUrl) {
		boolean removeResult = false;

		MicroclimateConnection connection = MicroclimateConnectionManager.getActiveConnection(baseUrl.toString());
		if (connection != null) {
			connection.close();
			removeResult = instance().connections.remove(connection);
		}
		else {
			removeResult = instance().brokenConnections.remove(baseUrl);
		}

		if (!removeResult) {
			MCLogger.logError("Tried to remove MCConnection " + baseUrl + ", but it didn't exist"); //$NON-NLS-1$ //$NON-NLS-2$
		}
		instance().writeToPreferences();
		MCUtil.updateAll();
		return removeResult;
	}

	/**
	 * Deletes all of the instance's connections. Does NOT write to preferences after doing so.
	 */
	public synchronized static void clear() {
		MCLogger.log("Clearing " + instance().connections.size() + " connections"); //$NON-NLS-1$ //$NON-NLS-2$

		Iterator<MicroclimateConnection> it = instance().connections.iterator();

		while(it.hasNext()) {
			MicroclimateConnection connection = it.next();
			connection.close();
			it.remove();
		}
	}

	/**
	 * @return An <b>unmodifiable</b> copy of the list of broken MC Connection URLs.
	 */
	public synchronized static List<String> brokenConnections() {
		return Collections.unmodifiableList(instance().brokenConnections);
	}

	public synchronized static String getBrokenConnection(String url) {
		for (String brokenConnectionUrl : brokenConnections()) {
			if (brokenConnectionUrl.toString().equals(url)) {
				return brokenConnectionUrl;
			}
		}
		return null;
	}

	// Preferences serialization
	private void writeToPreferences() {
		StringBuilder prefsBuilder = new StringBuilder();

		for (MicroclimateConnection mcc : activeConnections()) {
			prefsBuilder.append(mcc.toPrefsString()).append('\n');
		}
		for (String mcc : brokenConnections()) {
			prefsBuilder.append(mcc).append('\n');
		}

		MCLogger.log("Writing connections to preferences: " + prefsBuilder.toString()); //$NON-NLS-1$

		MicroclimateCorePlugin.getDefault().getPreferenceStore()
				.setValue(CONNECTION_LIST_PREFSKEY, prefsBuilder.toString());
	}

	private void loadFromPreferences() {
		clear();

		String storedConnections = MicroclimateCorePlugin.getDefault()
				.getPreferenceStore()
				.getString(CONNECTION_LIST_PREFSKEY).trim();

		MCLogger.log("Reading connections from preferences: \"" + storedConnections + "\""); //$NON-NLS-1$ //$NON-NLS-2$

		for(String line : storedConnections.split("\n")) { //$NON-NLS-1$
			line = line.trim();
			if(line.isEmpty()) {
				continue;
			}

			try {
				// Assume all connections are active. If they are broken they will be handled in the catch below.
				URI uri = new URI(line);
				MicroclimateConnection connection = MicroclimateObjectFactory.createMicroclimateConnection(uri);
				add(connection);
			}
			catch (MicroclimateConnectionException mce) {
				// The MC instance we wanted to connect to is down.
				brokenConnections.add(mce.connectionUrl.toString());
				MicroclimateReconnectJob.createAndStart(mce.connectionUrl);
			}
			catch (Exception e) {
				MCLogger.logError("Error loading MCConnection from preferences", e); //$NON-NLS-1$
			}
		}

	}

	public static boolean removeConnection(String mcConnectionUrl) {
		MicroclimateConnectionManager.remove(mcConnectionUrl);
		return true;
	}
}
