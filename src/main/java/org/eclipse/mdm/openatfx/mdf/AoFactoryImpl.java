/*
 * Copyright (c) 2016 Audi AG
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package org.eclipse.mdm.openatfx.mdf;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.asam.ods.AoException;
import org.asam.ods.AoFactoryPOA;
import org.asam.ods.AoSession;
import org.asam.ods.ErrorCode;
import org.asam.ods.NameValue;
import org.asam.ods.SeverityFlag;
import org.asam.ods.TS_Union;
import org.asam.ods.TS_Value;
import org.omg.CORBA.ORB;


/**
 * Implementation of <code>org.asam.ods.AoFactory</code> enabling opening MDF4 files.
 *
 * @author Christian Rechner
 */
class AoFactoryImpl extends AoFactoryPOA {

	private static final Log LOG = LogFactory.getLog(AoFactoryImpl.class);

	private final ORB orb;

	/**
	 * Creates a new AoFactory object.
	 *
	 * @param orb The ORB.
	 */
	public AoFactoryImpl(ORB orb) {
		this.orb = orb;
	}

	/**
	 * {@inheritDoc}
	 *
	 * @see org.asam.ods.AoFactoryOperations#getInterfaceVersion()
	 */
	@Override
	public String getInterfaceVersion() throws AoException {
		return "V5.3.0";
	}

	/**
	 * {@inheritDoc}
	 *
	 * @see org.asam.ods.AoFactoryOperations#getType()
	 */
	@Override
	public String getType() throws AoException {
		return "XATF-ASCII";
	}

	/**
	 * {@inheritDoc}
	 *
	 * @see org.asam.ods.AoFactoryOperations#getName()
	 */
	@Override
	public String getName() throws AoException {
		return "XATF-ASCII";
	}

	/**
	 * {@inheritDoc}
	 *
	 * @see org.asam.ods.AoFactoryOperations#getDescription()
	 */
	@Override
	public String getDescription() throws AoException {
		return "MDF file driver for ASAM OO-API";
	}

	/**
	 * {@inheritDoc}
	 *
	 * @see org.asam.ods.AoFactoryOperations#newSession(java.lang.String)
	 */
	@Override
	public AoSession newSession(String auth) throws AoException {
		try {
			List<NameValue> list = new ArrayList<NameValue>();
			for (String str : auth.split(",")) {
				String[] parts = str.split("=");
				if (parts.length == 2) {
					NameValue nv = new NameValue();
					nv.valName = parts[0];
					nv.value = new TS_Value();
					nv.value.flag = (short) 15;
					nv.value.u = new TS_Union();
					nv.value.u.stringVal(parts[1]);
					list.add(nv);
				}
			}
			return newSessionNameValue(list.toArray(new NameValue[0]));
		} catch (AoException aoe) {
			LOG.error(aoe.reason, aoe);
			throw aoe;
		}
	}

	/**
	 * {@inheritDoc}
	 *
	 * @see org.asam.ods.AoFactoryOperations#newSessionNameValue(org.asam.ods.NameValue[])
	 */
	@Override
	public AoSession newSessionNameValue(NameValue[] auth) throws AoException {
		try {
			File mdfFile = null;
			for (NameValue nv : auth) {
				if (nv.valName.equalsIgnoreCase("FILENAME")) {
					mdfFile = new File(nv.value.u.stringVal());
				}
			}
			if (mdfFile == null) {
				throw new AoException(ErrorCode.AO_MISSING_VALUE, SeverityFlag.ERROR, 0,
						"Parameter 'FILENAME' not found");
			}
			MDFConverter converter = new MDFConverter();
			return converter.getAoSessionForMDF(orb, mdfFile.toPath());
		} catch (AoException aoe) {
			LOG.error(aoe.reason, aoe);
			throw aoe;
		} catch (ConvertException e) {
			LOG.error(e.getMessage(), e);
			throw new AoException(ErrorCode.AO_UNKNOWN_ERROR, SeverityFlag.ERROR, 0, e.getMessage());
		}
	}

}
