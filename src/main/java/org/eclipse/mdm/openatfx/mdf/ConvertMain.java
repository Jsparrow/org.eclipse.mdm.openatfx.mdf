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

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

import org.apache.log4j.BasicConfigurator;
import org.omg.CORBA.ORB;

public class ConvertMain {

	public static void main(String[] args) {
		String filename = "C:\\Users\\EU2IYD9\\Documents\\MDF-Standard\\Testdaten_EA_MDF\\AU491out.mf4";
		if (args.length >= 1) {
			filename = args[0];
		}
		try {
			BasicConfigurator.configure();
			ORB orb = ORB.init(new String[0], System.getProperties());
			Path mdfFile = Paths.get(filename);

			MDFConverter converter = new MDFConverter();
			Properties props = new Properties();
			props.setProperty("replace_square_brackets", "true");
			// props.setProperty("readOnlyHeader", "true");
			converter.writeATFXHeader(orb, mdfFile, props);
		} catch (ConvertException e) {
			System.err.println(e.getMessage());
		}
	}

}
