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


package org.eclipse.mdm.openatfx.mdf.mdf4.conversion.partial_conversion;

import java.nio.file.Path;
import java.nio.file.Paths;

import org.eclipse.mdm.openatfx.mdf.ConvertException;
import org.eclipse.mdm.openatfx.mdf.MDFConverter;
import org.junit.Test;
import org.omg.CORBA.ORB;

import junit.framework.JUnit4TestAdapter;

/**
 * Test case for reading the example MDF4-file
 * <code>Vector_PartialConversionLinearIdentityAlgebraic.mf4</code>.
 *
 * @author Christian Rechner
 */
public class Test_Vector_PartialConversionLinearIdentityAlgebraic {

	private static final String mdfFile = "org/eclipse/mdm/openatfx/mdf/mdf4/conversion/partial_conversion/Vector_PartialConversionLinearIdentityAlgebraic.mf4";

	@Test(expected = ConvertException.class)
	public void setUpBeforeClass() throws Exception {
		ORB orb = ORB.init(new String[0], System.getProperties());
		Path path = Paths.get(ClassLoader.getSystemResource(mdfFile).toURI());
		MDFConverter reader = new MDFConverter();
		reader.getAoSessionForMDF(orb, path);
	}

	/*
	 * @AfterClass public static void tearDownAfterClass() throws Exception { if
	 * (aoSession != null) { aoSession.close(); } }
	 * 
	 * @Test public void testReadIDBlock() { try { ApplicationStructure as =
	 * aoSession.getApplicationStructure(); InstanceElement ieTst =
	 * as.getElementByName("tst").getInstances("*").nextOne();
	 * assertEquals("MDF     ",
	 * ODSHelper.getStringVal(ieTst.getValue("mdf_file_id")));
	 * assertEquals("4.10    ",
	 * ODSHelper.getStringVal(ieTst.getValue("mdf_version_str")));
	 * assertEquals(410, ODSHelper.getLongVal(ieTst.getValue("mdf_version")));
	 * assertEquals("MDF4Lib",
	 * ODSHelper.getStringVal(ieTst.getValue("mdf_program"))); assertEquals(0,
	 * ODSHelper.getLongVal(ieTst.getValue("mdf_unfin_flags"))); assertEquals(0,
	 * ODSHelper.getLongVal(ieTst.getValue("mdf_custom_unfin_flags"))); } catch
	 * (AoException e) { fail(e.reason); } }
	 */

	public static junit.framework.Test suite() {
		return new JUnit4TestAdapter(Test_Vector_PartialConversionLinearIdentityAlgebraic.class);
	}

}
