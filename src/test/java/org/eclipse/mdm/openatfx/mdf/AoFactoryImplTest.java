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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.File;
import java.net.URL;

import org.asam.ods.AoException;
import org.asam.ods.AoFactory;
import org.asam.ods.AoSession;
import org.asam.ods.NameValue;
import org.asam.ods.TS_Union;
import org.asam.ods.TS_Value;
import org.junit.BeforeClass;
import org.junit.Test;
import org.omg.CORBA.ORB;

import junit.framework.JUnit4TestAdapter;

/**
 * Test case for <code>de.rechner.openatfx.AoFactoryImpl</code>.
 *
 * @author Christian Rechner
 */
public class AoFactoryImplTest {

	private static AoFactory aoFactory;

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		ORB orb = ORB.init(new String[0], System.getProperties());
		aoFactory = new MDFConverter().newAoFactory(orb);
	}

	/**
	 * Test method for
	 * {@link de.rechner.openatfx_mdf.atfx.AoFactoryImpl#getInterfaceVersion()}.
	 */
	@Test
	public void testGetInterfaceVersion() {
		try {
			assertEquals("V5.3.0", aoFactory.getInterfaceVersion());
		} catch (AoException e) {
			fail(e.reason);
		}
	}

	/**
	 * Test method for
	 * {@link de.rechner.openatfx_mdf.atfx.AoFactoryImpl#getType()}.
	 */
	@Test
	public void testGetType() {
		try {
			assertEquals("XATF-ASCII", aoFactory.getType());
		} catch (AoException e) {
			fail(e.reason);
		}
	}

	/**
	 * Test method for
	 * {@link de.rechner.openatfx_mdf.atfx.AoFactoryImpl#getName()}.
	 */
	@Test
	public void testGetName() {
		try {
			assertEquals("XATF-ASCII", aoFactory.getName());
		} catch (AoException e) {
			fail(e.reason);
		}
	}

	/**
	 * Test method for
	 * {@link de.rechner.openatfx_mdf.atfx.AoFactoryImpl#getDescription()}.
	 */
	@Test
	public void testGetDescription() {
		try {
			assertEquals("MDF file driver for ASAM OO-API", aoFactory.getDescription());
		} catch (AoException e) {
			fail(e.reason);
		}
	}

	/**
	 * Test method for
	 * {@link de.rechner.openatfx_mdf.atfx.AoFactoryImpl#newSession(java.lang.String)}.
	 */
	@Test
	public void testNewSession() {
		try {
			URL url = AoFactoryImplTest.class
					.getResource("/org/eclipse/mdm/openatfx/mdf/mdf4/simple/ETAS_SimpleSorted.mf4");
			AoSession aoSession = aoFactory.newSession("FILENAME=" + new File(url.getFile()));
			assertEquals("asam32", aoSession.getType());
			aoSession.close();
		} catch (AoException e) {
			fail(e.reason);
		}
	}

	/**
	 * Test method for
	 * {@link de.rechner.openatfx_mdf.atfx.AoFactoryImpl#newSessionNameValue(org.asam.ods.NameValue[])}
	 * .
	 */
	@Test
	public void testNewSessionNameValue() {
		try {
			URL url = AoFactoryImplTest.class
					.getResource("/org/eclipse/mdm/openatfx/mdf/mdf4/simple/ETAS_SimpleSorted.mf4");
			NameValue[] auth = new NameValue[1];
			auth[0] = new NameValue();
			auth[0].valName = "FILENAME";
			auth[0].value = new TS_Value();
			auth[0].value.flag = (short) 15;
			auth[0].value.u = new TS_Union();
			auth[0].value.u.stringVal(url.getPath());
			AoSession aoSession = aoFactory.newSessionNameValue(auth);
			assertEquals("asam32", aoSession.getType());
		} catch (AoException e) {
			fail(e.reason);
		}
	}

	public static junit.framework.Test suite() {
		return new JUnit4TestAdapter(AoFactoryImplTest.class);
	}

}
