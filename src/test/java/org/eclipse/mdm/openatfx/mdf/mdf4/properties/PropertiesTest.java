/*
 * Copyright (c) 2016 Audi AG
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package org.eclipse.mdm.openatfx.mdf.mdf4.properties;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

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
 * Test case for writing additional Properties to the MDF-File.
 *
 * @author Christian Rechner
 */
public class PropertiesTest {

	private static final String mdfFile = "org/eclipse/mdm/openatfx/mdf/mdf4/arrays/simple/Vector_MeasurementArrays.mf4";

	private static ORB orb;
	private static AoSession aoSession;

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		orb = ORB.init(new String[0], System.getProperties());
		Path path = Paths.get(ClassLoader.getSystemResource(mdfFile).toURI());
		MDFConverter reader = new MDFConverter();
		//set Properties
		Properties prop = new Properties();
		prop.put("String1", "str1");
		prop.put("Double", Double.valueOf(1.0));
		prop.put("Integer",Integer.valueOf(2));
		prop.put("Long", Long.valueOf(Integer.MAX_VALUE +1L));
		prop.put("String2", "str2");
		aoSession = reader.getAoSessionForMDF(orb, path, prop);
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
			assertEquals("MCD11.00", ODSHelper.getStringVal(ieTst.getValue("mdf_program")));
			assertEquals(0, ODSHelper.getLongVal(ieTst.getValue("mdf_unfin_flags")));
			assertEquals(0, ODSHelper.getLongVal(ieTst.getValue("mdf_custom_unfin_flags")));

			assertEquals(5, ieTst.listAttributes("*", AttrType.INSTATTR_ONLY).length);

			assertEquals("str1", ODSHelper.getStringVal(ieTst.getValue("String1")));
			assertEquals("str2", ODSHelper.getStringVal(ieTst.getValue("String2")));
			assertEquals(1.0, ODSHelper.getDoubleVal(ieTst.getValue("Double")),0);
			assertEquals(2, ODSHelper.getLongVal(ieTst.getValue("Integer")));
			assertEquals(Integer.MAX_VALUE +1L, ODSHelper.getLongLongVal(ieTst.getValue("Long")));
		} catch (AoException e) {
			fail(e.reason);
		}
	}

	public static junit.framework.Test suite() {
		return new JUnit4TestAdapter(PropertiesTest.class);
	}

}