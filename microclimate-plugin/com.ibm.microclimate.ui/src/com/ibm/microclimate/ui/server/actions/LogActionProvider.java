/*******************************************************************************
 * Copyright (c) 2016 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.microclimate.ui.server.actions;

import java.io.File;
import java.util.Iterator;

import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.ide.IDE;
import org.eclipse.ui.navigator.CommonActionProvider;
import org.eclipse.ui.navigator.ICommonActionExtensionSite;
import org.eclipse.ui.navigator.ICommonMenuConstants;
import org.eclipse.wst.server.core.IServer;

import com.ibm.microclimate.core.MCLogger;
import com.ibm.microclimate.core.internal.MicroclimateApplication;
import com.ibm.microclimate.core.server.MicroclimateServerBehaviour;

/**
 * From com.ibm.ws.st.ui.internal.actions.LogActionProvider
 *
 */
public class LogActionProvider extends CommonActionProvider implements ISelectionChangedListener {

    protected MicroclimateServerBehaviour mcServerBehaviour;
    protected Shell shell;

    @Override
    public void init(ICommonActionExtensionSite aSite) {
        super.init(aSite);
        shell = aSite.getViewSite().getShell();

        ISelectionProvider selectionProvider = aSite.getStructuredViewer();
        selectionProvider.addSelectionChangedListener(this);
        onSelectionChange(selectionProvider.getSelection());
    }

	@Override
	public void selectionChanged(SelectionChangedEvent selectionChangedEvent) {
		onSelectionChange(selectionChangedEvent.getSelection());
	}

	private void onSelectionChange(ISelection selection) {
		if (selection instanceof StructuredSelection) {
			IStructuredSelection sel = (IStructuredSelection) selection;

	        Iterator<?> iterator = sel.iterator();
	        while (iterator.hasNext()) {
	            Object obj = iterator.next();
	            if (obj instanceof IServer) {
	                IServer server = (IServer) obj;
	                mcServerBehaviour = (MicroclimateServerBehaviour)
	                		server.loadAdapter(MicroclimateServerBehaviour.class, null);
	            }
	        }
		}
	}

    @Override
    public void fillContextMenu(IMenuManager menu) {

    	if (mcServerBehaviour == null) {
    		return;
    	}

        MenuManager openLogsMenu = new MenuManager("Open Log File", "OpenLogFiles");

    	for (IPath logFilePath : mcServerBehaviour.getApp().getLogFilePaths()) {
    		String name = logFilePath.lastSegment();
			if (name.endsWith(MicroclimateApplication.BUILD_LOG_SHORTNAME)) {
				name = MicroclimateApplication.BUILD_LOG_SHORTNAME;
			}

			OpenLogAction openLogAction = new OpenLogAction("Open " + name, shell, logFilePath);
			openLogsMenu.add(openLogAction);
    	}

        menu.appendToGroup(ICommonMenuConstants.GROUP_ADDITIONS, openLogsMenu);

    }

    private class LocalFileAction extends Action {

        private final File file;

        /** {@inheritDoc} */
        @Override
        public String getText() {
            Path path = new Path(file.getPath());
            return path.lastSegment();
        }

        public LocalFileAction(File file) {
            this.file = file;
        }

        /** {@inheritDoc} */
        @Override
        public void run() {
            final IPath path = new Path(file.getAbsolutePath());
            final IFileStore fileStore = EFS.getLocalFileSystem().getStore(path);
            Display.getDefault().asyncExec(new Runnable() {
                @Override
                public void run() {
                    IWorkbenchPage page = null;
                    if (!fileStore.fetchInfo().isDirectory() && fileStore.fetchInfo().exists()) {
                        IWorkbenchWindow window;
                        window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
                        page = window.getActivePage();
                    }
                    try {
                        if (page != null) {
                            IDE.openEditorOnFileStore(page, fileStore);
                        }

                    } catch (PartInitException e) {
                        MCLogger.logError("Error Opening " + path.toOSString(), e);
                    }
                }
            });
        }
    }
}