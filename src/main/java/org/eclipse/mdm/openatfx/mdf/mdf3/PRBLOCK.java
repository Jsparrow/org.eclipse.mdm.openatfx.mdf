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


package org.eclipse.mdm.openatfx.mdf.mdf3;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.SeekableByteChannel;

/**
 * <p>
 * Program block: Contains proprietary data of the application generating the
 * MDF file
 * </p>
 * The PRBLOCK contains non standardized data to exchange between the
 * acquisition program and the evaluation program.
 *
 * @author Christian Rechner
 */
class PRBLOCK extends BLOCK {

	public static String BLOCK_ID = "PR";

	// CHAR variable Program-specific data
	private String text;

	/**
	 * Constructor.
	 *
	 * @param sbc
	 *            The byte channel pointing to the MDF file.
	 * @param pos
	 *            The position of the block within the MDF file.
	 */
	private PRBLOCK(SeekableByteChannel sbc, long pos) {
		super(sbc, pos);
	}

	public String getText() {
		return text;
	}

	private void setText(String text) {
		this.text = text;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String toString() {
		return new StringBuilder().append("PRBLOCK [text=").append(text).append("]").toString();
	}

	/**
	 * Reads a PRBLOCK from the channel starting at pos
	 *
	 * @param sbc
	 *            The channel to read from.
	 * @param pos
	 *            The position to start reading.
	 * @return The block data.
	 * @throws IOException
	 *             The exception.
	 */
	public static PRBLOCK read(SeekableByteChannel sbc, long pos) throws IOException {
		PRBLOCK block = new PRBLOCK(sbc, pos);

		// read block
		ByteBuffer bb = ByteBuffer.allocate(4);
		bb.order(ByteOrder.LITTLE_ENDIAN);
		sbc.position(pos);
		sbc.read(bb);
		bb.rewind();

		// CHAR 2 Block type identifier
		block.setId(Mdf3Util.readChars(bb, 2));

		// UINT16 1 Block size of this block in bytes
		block.setLength(Mdf3Util.readUInt16(bb));
		if (!block.getId().equals(BLOCK_ID)) {
			throw new IOException(new StringBuilder().append("Wrong block type - expected '").append(BLOCK_ID).append("', found '").append(block.getId()).append("'").toString());
		}

		// CHAR variable Program-specific data
		block.setText(Mdf3Util.readChars(sbc, block.getLength() - 5));

		return block;
	}

}
