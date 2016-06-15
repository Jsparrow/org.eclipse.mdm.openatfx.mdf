/*
 * Copyright (c) 2016 Audi AG
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package org.eclipse.mdm.openatfx.mdf.mdf4.metadata.customextensions;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.nio.file.Path;
import java.nio.file.Paths;

import org.asam.ods.AoException;
import org.asam.ods.AoSession;
import org.asam.ods.ApplicationStructure;
import org.asam.ods.InstanceElement;
import org.asam.ods.InstanceElementIterator;
import org.eclipse.mdm.openatfx.mdf.MDFConverter;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.omg.CORBA.ORB;

import de.rechner.openatfx.util.ODSHelper;
import junit.framework.JUnit4TestAdapter;


/**
 * Test case for reading the example MDF4-file <code>Vector_CustomExtensions_CNcomment.mf4</code>.
 *
 * @author Christian Rechner
 */
public class Test_Vector_CustomExtensions_CNcomment {

	private static final String mdfFile = "org/eclipse/mdm/openatfx/mdf/mdf4/metadata/customextensions/Vector_CustomExtensions_CNcomment.mf4";

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
			assertEquals("MCD11.01", ODSHelper.getStringVal(ieTst.getValue("mdf_program")));
			assertEquals(0, ODSHelper.getLongVal(ieTst.getValue("mdf_unfin_flags")));
			assertEquals(0, ODSHelper.getLongVal(ieTst.getValue("mdf_custom_unfin_flags")));
		} catch (AoException e) {
			fail(e.reason);
		}
	}

	@Test
	public void testReadMeq() {
		try {
			ApplicationStructure as = aoSession.getApplicationStructure();
			InstanceElementIterator iter = as.getElementByName("meq").getInstances("t");
			assertEquals(1, iter.getCount());

			InstanceElement ieMeq = iter.nextOne();
			assertEquals("t", ODSHelper.getStringVal(ieMeq.getValue("iname")));
			assertEquals("100ms", ODSHelper.getStringVal(ieMeq.getValue("desc")));
			assertEquals(0.1, ODSHelper.getDoubleVal(ieMeq.getValue("raster")), 1e-9);
		} catch (AoException e) {
			fail(e.reason);
		}
	}

	public static junit.framework.Test suite() {
		return new JUnit4TestAdapter(Test_Vector_CustomExtensions_CNcomment.class);
	}

}
