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

public class ConvertException extends Exception {

	private static final long serialVersionUID = 7917195873759906581L;

	public ConvertException(String message, Throwable cause) {
		super(message, cause);
	}

	public ConvertException(String message) {
		super(message);
	}

}
