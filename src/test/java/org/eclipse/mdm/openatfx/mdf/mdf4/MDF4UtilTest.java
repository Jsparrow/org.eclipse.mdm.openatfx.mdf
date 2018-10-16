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

import static org.junit.Assert.assertEquals;

import java.nio.ByteBuffer;

import org.junit.Test;

public class MDF4UtilTest {

	@Test
	public void testReadWithoutShift() {
		byte[] test = new byte[] { -1, -1, -1, -1, 0, 0, 0, 0 };
		assertEquals(2L * Integer.MAX_VALUE + 1L, MDF4Util.readValue(0, 64, ByteBuffer.wrap(test)));
	}

	@Test
	public void testReadWithShift() {
		byte[] test2 = new byte[] { -1, -1, -1, -65, -128, 0, 0, 0, 0 };
		assertEquals(Integer.MAX_VALUE, MDF4Util.readValue(1, 64, ByteBuffer.wrap(test2)));
	}

	@Test
	public void testShortSize() {
		byte[] test2 = new byte[] { -1, -1 };
		assertEquals(2 * Short.MAX_VALUE + 1, MDF4Util.readValue(0, 16, ByteBuffer.wrap(test2)));
	}

}
