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


package org.eclipse.mdm.openatfx.mdf.mdf4;

import static org.junit.Assert.assertArrayEquals;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.asam.ods.AoException;
import org.asam.ods.AoSession;
import org.asam.ods.ApplicationStructure;
import org.asam.ods.InstanceElement;
import org.eclipse.mdm.openatfx.mdf.ConvertException;
import org.eclipse.mdm.openatfx.mdf.MDFConverter;
import org.eclipse.mdm.openatfx.mdf.util.ODSInsertStatement;
import org.eclipse.mdm.openatfx.mdf.util.ODSModelCache;
import org.omg.CORBA.ORB;

import de.rechner.openatfx.util.ODSHelper;

public class XMLParserTest {
	public void testXMLParseElist() throws ConvertException, URISyntaxException, AoException, IOException {
		String test = new StringBuilder().append("<common_properties>").append("<elist ci=\"1\" name=\"Outlaws\">").append("<eli ci=\"1\">Robin Hood</eli> ").append("<eli ci=\"1\">John Little</eli>").append("<eli ci=\"2\">Friar Tuck</eli>").append("</elist>").append("<elist name=\"score\" unit=\"points\" type=\"integer\">").append("<eli>1</eli>")
				.append("<eli ci=\"1\">1</eli>").append("<eli>2</eli>").append("<eli>4</eli>").append("</elist>").append("</common_properties>").toString();

		String mdfFile = "org/eclipse/mdm/openatfx/mdf/mdf4/arrays/simple/Vector_ArrayWithFixedAxes.MF4";
		MDF4XMLParser pars = new MDF4XMLParser();
		ORB orb = ORB.init(new String[0], System.getProperties());
		Path path = Paths.get(ClassLoader.getSystemResource(mdfFile).toURI());
		MDFConverter reader = new MDFConverter();
		AoSession aoSession = reader.getAoSessionForMDF(orb, path);

		ODSInsertStatement ins = new ODSInsertStatement(new ODSModelCache(aoSession), "mea");
		ins.setStringVal("iname", "TestMea");
		pars.writeHDCommentToMea(ins, test);
		ins.execute();
		ApplicationStructure as = aoSession.getApplicationStructure();

		InstanceElement ieMea = as.getElementByName("mea").getInstances("TestMea").nextOne();
		assertArrayEquals(new String[] { "Robin Hood", "John Little", "Friar Tuck" },
				ODSHelper.getStringSeq(ieMea.getValue("Outlaws")));
		assertArrayEquals(new int[] { 1, 1, 2, 4 }, ODSHelper.getLongSeq(ieMea.getValue("score")));

	}
}
