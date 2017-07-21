/*
 * Copyright (c) 2016 Audi AG
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package org.eclipse.mdm.openatfx.mdf.mdf4.conversion.linear_conversion;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.nio.file.Path;
import java.nio.file.Paths;

import org.asam.ods.AoException;
import org.asam.ods.AoSession;
import org.asam.ods.ApplicationStructure;
import org.asam.ods.AttrType;
import org.asam.ods.InstanceElement;
import org.eclipse.mdm.openatfx.mdf.MDFConverter;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.omg.CORBA.ORB;

import de.rechner.openatfx.util.ODSHelper;
import junit.framework.JUnit4TestAdapter;

/**
 * Test case for reading the example MDF4-file
 * <code>dSPACE_LinearConversion.mf4</code>.
 *
 * @author Christian Rechner
 */
public class Test_dSPACE_LinearConversion {

	private static final String mdfFile = "org/eclipse/mdm/openatfx/mdf/mdf4/conversion/linear_conversion/dSPACE_LinearConversion.mf4";

	private static ORB orb;
	private static AoSession aoSession;

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		orb = ORB.init(new String[0], System.getProperties());
		Path path = Paths.get(ClassLoader.getSystemResource(mdfFile).toURI());
		MDFConverter reader = new MDFConverter();
		aoSession = reader.getAoSessionForMDF(orb, path);
	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
		if (aoSession != null) {
			aoSession.close();
		}
	}

	@Test
	public void testReadIDBlock() {
		try {
			ApplicationStructure as = aoSession.getApplicationStructure();
			InstanceElement ieTst = as.getElementByName("tst").getInstances("*").nextOne();
			assertEquals("MDF     ", ODSHelper.getStringVal(ieTst.getValue("mdf_file_id")));
			assertEquals("4.10    ", ODSHelper.getStringVal(ieTst.getValue("mdf_version_str")));
			assertEquals(410, ODSHelper.getLongVal(ieTst.getValue("mdf_version")));
			assertEquals("CtrlDesk", ODSHelper.getStringVal(ieTst.getValue("mdf_program")));
			assertEquals(0, ODSHelper.getLongVal(ieTst.getValue("mdf_unfin_flags")));
			assertEquals(0, ODSHelper.getLongVal(ieTst.getValue("mdf_custom_unfin_flags")));
		} catch (AoException e) {
			fail(e.reason);
		}
	}

	@Test
	public void testReadHDBlock() {
		try {
			ApplicationStructure as = aoSession.getApplicationStructure();
			InstanceElement ieTst = as.getElementByName("mea").getInstances("dSPACE_LinearConversion.mf4").nextOne();
			assertEquals("ASAM COMMON MDF 4.1 sample file created by dSPACE. Contents: signal with a linear conversion",
					ODSHelper.getStringVal(ieTst.getValue("desc")));
			assertEquals(9, ieTst.listAttributes("*", AttrType.INSTATTR_ONLY).length);
			assertEquals("Thilo Maeck", ODSHelper.getStringVal(ieTst.getValue("User")));
			assertEquals("20121107101610", ODSHelper.getDateVal(ieTst.getValue("DateTime")));
			assertEquals("Created by ControlDesk Next Generation's Measurement Data API",
					ODSHelper.getStringVal(ieTst.getValue("Origin")));
			assertEquals("", ODSHelper.getStringVal(ieTst.getValue("StartCondition")));
			assertEquals("", ODSHelper.getStringVal(ieTst.getValue("StopCondition")));
			assertEquals("0", ODSHelper.getStringVal(ieTst.getValue("XAxisOffset")));
			assertEquals("0.0040000000000000001", ODSHelper.getStringVal(ieTst.getValue("Length")));
			assertEquals("0", ODSHelper.getStringVal(ieTst.getValue("StartTimestamp")));
			assertEquals("0.0040000000000000001", ODSHelper.getStringVal(ieTst.getValue("StopTimestamp")));
		} catch (AoException e) {
			fail(e.reason);
		}
	}

	public static junit.framework.Test suite() {
		return new JUnit4TestAdapter(Test_dSPACE_LinearConversion.class);
	}

}
