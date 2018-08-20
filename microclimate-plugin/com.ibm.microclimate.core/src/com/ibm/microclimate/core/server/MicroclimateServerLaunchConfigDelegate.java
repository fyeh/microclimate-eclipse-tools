package com.ibm.microclimate.core.server;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.jdt.launching.AbstractJavaLaunchConfigurationDelegate;
import org.eclipse.wst.server.core.IServer;
import org.eclipse.wst.server.core.ServerUtil;

import com.ibm.microclimate.core.MCLogger;

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

		MCLogger.log("Launching " + server.getName() + " in " + launchMode + " mode");

        final MicroclimateServerBehaviour serverBehaviour = server.getAdapter(MicroclimateServerBehaviour.class);

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

        serverBehaviour.doRestart(config, launchMode, launch, monitor);
	}

}