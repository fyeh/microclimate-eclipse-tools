/*******************************************************************************
 * IBM Confidential
 * OCO Source Materials
 * (C) Copyright IBM Corp. 2018 All Rights Reserved.
 * The source code for this program is not published or otherwise
 * divested of its trade secrets, irrespective of what has
 * been deposited with the U.S. Copyright Office.
 *******************************************************************************/

package com.ibm.microclimate.core.internal.server.debug;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.jdt.launching.AbstractJavaLaunchConfigurationDelegate;
import org.eclipse.wst.server.core.IServer;
import org.eclipse.wst.server.core.ServerUtil;

import com.ibm.microclimate.core.internal.MCLogger;
import com.ibm.microclimate.core.internal.MCUtil;
import com.ibm.microclimate.core.internal.server.MicroclimateServer;
import com.ibm.microclimate.core.internal.server.MicroclimateServerBehaviour;

/**
 *
 * @author timetchells@ibm.com
 *
 */
public class MicroclimateServerLaunchConfigDelegate extends AbstractJavaLaunchConfigurationDelegate {

	@Override
	public void launch(ILaunchConfiguration config, String launchMode, ILaunch launch, IProgressMonitor monitor)
			throws CoreException {

        final IServer server = ServerUtil.getServer(config);
        if (server == null) {
            MCLogger.logError("Could not find server from configuration " + config.getName());
            return;
        }

        final MicroclimateServerBehaviour serverBehaviour = server.getAdapter(MicroclimateServerBehaviour.class);

        // Validate here that the server can actually be restarted.
        String errorTitle = "Error Starting Server";
        String errorMsg = "";
        if (serverBehaviour.getSuffix() != null) {
        	errorTitle = "Server can't be started";
        	errorMsg = serverBehaviour.getSuffix();
        }
        else if (serverBehaviour.getApp() == null) {
			errorTitle = "Server Error";
			errorMsg = "There was an error initializing " + server.getName() +
				". Please delete and re-create the server.";
        }

        if (!errorMsg.isEmpty()) {
        	MCUtil.openDialog(true, errorTitle, errorMsg);

        	monitor.setCanceled(true);
        	return;
        }

		MCLogger.log("Launching " + server.getName() + " in " + launchMode + " mode");

        final String projectName = server.getAttribute(MicroclimateServer.ATTR_ECLIPSE_PROJECT_NAME, "");
        if (projectName.isEmpty()) {
        	// Need the project name to set the source path - in this case the user can work around it by
        	// adding the project to the source path manually.
        	MCLogger.logError(MicroclimateServer.ATTR_ECLIPSE_PROJECT_NAME + " was not set on server "
        			+ server.getName());
        }

        ILaunchConfigurationWorkingCopy configWc = config.getWorkingCopy();
        configWc.setAttribute(MicroclimateServer.ATTR_ECLIPSE_PROJECT_NAME, projectName);
        config = configWc.doSave();

        setDefaultSourceLocator(launch, config);
        serverBehaviour.setLaunch(launch);

        serverBehaviour.doLaunch(config, launchMode, launch, monitor);
        monitor.done();
	}

}
