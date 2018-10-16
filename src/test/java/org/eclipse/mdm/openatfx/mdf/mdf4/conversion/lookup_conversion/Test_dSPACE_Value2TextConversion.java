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


package org.eclipse.mdm.openatfx.mdf.mdf4.conversion.lookup_conversion;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.nio.file.Path;
import java.nio.file.Paths;

import org.asam.ods.AoException;
import org.asam.ods.AoSession;
import org.asam.ods.ApplicationRelation;
import org.asam.ods.ApplicationStructure;
import org.asam.ods.InstanceElement;
import org.asam.ods.InstanceElementIterator;
import org.eclipse.mdm.openatfx.mdf.MDFConverter;
import org.eclipse.mdm.openatfx.mdf.util.ODSModelCache;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.omg.CORBA.ORB;

import de.rechner.openatfx.util.ODSHelper;
import junit.framework.JUnit4TestAdapter;

/**
 * Test case for reading the example MDF4-file
 * <code>dSPACE_Value2TextConversion.mf4</code>.
 *
 * @author Christian Rechner
 */
public class Test_dSPACE_Value2TextConversion {

	private static final String mdfFile = "org/eclipse/mdm/openatfx/mdf/mdf4/conversion/lookup_conversion/dSPACE_Value2TextConversion.mf4";

	private static ORB orb;
	private static AoSession aoSession;
	private static ODSModelCache modelCache;

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		orb = ORB.init(new String[0], System.getProperties());
		Path path = Paths.get(ClassLoader.getSystemResource(mdfFile).toURI());
		MDFConverter reader = new MDFConverter();
		aoSession = reader.getAoSessionForMDF(orb, path);
		modelCache = new ODSModelCache(aoSession);
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
	public void testReadLookupMea() {
		try {
			ApplicationStructure as = aoSession.getApplicationStructure();
			InstanceElement ieMea = as.getElementByName("mea").getInstances("*_lookup").nextOne();
			assertEquals("application/x-asam.aomeasurement.lookup", ODSHelper.getStringVal(ieMea.getValue("mt")));

			ApplicationRelation relMeaSm = modelCache.getApplicationRelation("mea", "sm", "sms");
			InstanceElement ieSm = ieMea.getRelatedInstances(relMeaSm, "*").nextOne();
			assertEquals("application/x-asam.aosubmatrix.lookup.value_range_to_text",
					ODSHelper.getStringVal(ieSm.getValue("mt")));
			assertEquals(2, ODSHelper.getLongVal(ieSm.getValue("rows")));

			ApplicationRelation relSmLc = modelCache.getApplicationRelation("sm", "lc", "lcs");

			InstanceElementIterator iter = ieSm.getRelatedInstances(relSmLc, "*");
			assertEquals(2, iter.getCount());

			ApplicationRelation relLcMeq = modelCache.getApplicationRelation("lc", "meq", "meq");

			for (int i = 0; i < 2; i++) {
				InstanceElement ieLc = iter.nextOne();
				String mimeType = ODSHelper.getStringVal(ieLc.getValue("mt"));
				assertTrue(mimeType.equals("application/x-asam.aolocalcolumn.lookup.key")
						|| mimeType.equals("application/x-asam.aolocalcolumn.lookup.value"));

				InstanceElement ieMeq = ieLc.getRelatedInstances(relLcMeq, "*").nextOne();
				mimeType = ODSHelper.getStringVal(ieMeq.getValue("mt"));
				assertTrue(mimeType.equals("application/x-asam.aomeasurementquantity.lookup.key")
						|| mimeType.equals("application/x-asam.aomeasurementquantity.lookup.value"));
				assertTrue(ODSHelper.getEnumVal(ieMeq.getValue("dt")) == 1
						|| ODSHelper.getEnumVal(ieMeq.getValue("dt")) == 7);

				if (ODSHelper.getEnumVal(ieMeq.getValue("dt")) == 1) {
					String[] values = ODSHelper.getStringSeq(ieLc.getValue("val"));
					assertEquals("off", values[0]);
					assertEquals("on", values[1]);
				}
			}

		} catch (AoException e) {
			fail(e.reason);
		}
	}

	@Test
	public void testReadPreviewMea() {
		try {
			ApplicationStructure as = aoSession.getApplicationStructure();
			InstanceElement ieMea = as.getElementByName("mea").getInstances("*_previews").nextOne();
			assertEquals("application/x-asam.aomeasurement.mdf_preview", ODSHelper.getStringVal(ieMea.getValue("mt")));

		} catch (AoException e) {
			fail(e.reason);
		}
	}

	public static junit.framework.Test suite() {
		return new JUnit4TestAdapter(Test_dSPACE_Value2TextConversion.class);
	}

}
