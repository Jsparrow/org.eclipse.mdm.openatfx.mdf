/*
 * Copyright (c) 2016 Audi AG
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package org.eclipse.mdm.openatfx.mdf.mdf4.compressed_data.datalist;

import java.nio.file.Path;
import java.nio.file.Paths;

import org.eclipse.mdm.openatfx.mdf.ConvertException;
import org.eclipse.mdm.openatfx.mdf.MDFConverter;
import org.junit.Test;
import org.omg.CORBA.ORB;

import junit.framework.JUnit4TestAdapter;


/**
 * Test case for reading the example MDF4-file <code>Vector_DataList_Deflate.mf4</code>.
 *
 * @author Christian Rechner
 */
public class Test_Vector_DataList_Deflate {

	private static final String mdfFile = "org/eclipse/mdm/openatfx/mdf/mdf4/compressed_data/datalist/Vector_DataList_Deflate.mf4";

	@Test(expected=ConvertException.class)
	public  void setUpBeforeClass() throws Exception {
		ORB orb = ORB.init(new String[0], System.getProperties());
		Path path = Paths.get(ClassLoader.getSystemResource(mdfFile).toURI());
		MDFConverter reader = new MDFConverter();
		reader.getAoSessionForMDF(orb, path);
	}

	/*@AfterClass
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
            assertEquals("MDF4Lib", ODSHelper.getStringVal(ieTst.getValue("mdf_program")));
            assertEquals(0, ODSHelper.getLongVal(ieTst.getValue("mdf_unfin_flags")));
            assertEquals(0, ODSHelper.getLongVal(ieTst.getValue("mdf_custom_unfin_flags")));
        } catch (AoException e) {
            fail(e.reason);
        }
    }*/

	public static junit.framework.Test suite() {
		return new JUnit4TestAdapter(Test_Vector_DataList_Deflate.class);
	}

}
