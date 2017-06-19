/*
 * Copyright (c) 2016 Audi AG
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

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
		String test = "<common_properties>" + "<elist ci=\"1\" name=\"Outlaws\">" + "<eli ci=\"1\">Robin Hood</eli> "
				+ "<eli ci=\"1\">John Little</eli>" + "<eli ci=\"2\">Friar Tuck</eli>" + "</elist>"
				+ "<elist name=\"score\" unit=\"points\" type=\"integer\">" + "<eli>1</eli>" + "<eli ci=\"1\">1</eli>"
				+ "<eli>2</eli>" + "<eli>4</eli>" + "</elist>" + "</common_properties>";

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
