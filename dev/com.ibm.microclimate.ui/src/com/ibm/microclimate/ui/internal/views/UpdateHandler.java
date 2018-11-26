/*******************************************************************************
 * IBM Confidential
 * OCO Source Materials
 * (C) Copyright IBM Corp. 2018 All Rights Reserved.
 * The source code for this program is not published or otherwise
 * divested of its trade secrets, irrespective of what has
 * been deposited with the U.S. Copyright Office.
 *******************************************************************************/

package com.ibm.microclimate.ui.internal.views;

import com.ibm.microclimate.core.internal.IUpdateHandler;
import com.ibm.microclimate.core.internal.MicroclimateApplication;
import com.ibm.microclimate.core.internal.connection.MicroclimateConnection;

/**
 * Update handler registered on the Microclimate core plug-in in order to keep
 * the Microclimate view up to date.
 */
public class UpdateHandler implements IUpdateHandler {
	
	@Override
	public void updateAll() {
		ViewHelper.refreshMicroclimateExplorerView(null);
	}

	@Override
	public void updateConnection(MicroclimateConnection connection) {
		ViewHelper.refreshMicroclimateExplorerView(connection);
	}

	@Override
	public void updateApplication(MicroclimateApplication application) {
		ViewHelper.refreshMicroclimateExplorerView(application);
	}

}
