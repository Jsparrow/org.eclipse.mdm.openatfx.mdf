/********************************************************************************
 * Copyright (c) 2015-2018 Contributors to the Eclipse Foundation
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 ********************************************************************************/


package org.eclipse.mdm.openatfx.mdf;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import de.rechner.openatfx.IFileHandler;

/**
 * Implementation of the IFileHandler interface for the local file system.
 *
 * @author Christian Rechner
 */
class TmpFileHandler implements IFileHandler {

	private static final String ATFX_TEMPLATE = "model.atfx";

	/**
	 * {@inheritDoc}
	 */
	@Override
	public InputStream getFileStream(String path) throws IOException {
		InputStream in = getClass().getResourceAsStream(ATFX_TEMPLATE);
		if (in == null) {
			throw new IOException("Unable to get template ATFX: " + ATFX_TEMPLATE);
		}
		return in;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getFileRoot(String path) throws IOException {
		File file = new File(path);
		return file.getParentFile().getAbsolutePath().replaceAll("\\\\", "/");
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getFileName(String path) throws IOException {
		File file = new File(path);
		return file.getName();
	}

}
