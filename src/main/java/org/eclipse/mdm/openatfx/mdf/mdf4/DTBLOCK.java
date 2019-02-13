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

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.SeekableByteChannel;

/**
 * <p>
 * THE DATA BLOCK <code>DTBLOCK</code>
 * </p>
 * The data section of the DTBLOCK contains a sequence of records. It contains
 * records of all channel groups assigned to its parent DGBLOCK.
 *
 * @author Christian Rechner
 */
class DTBLOCK extends BLOCK {

	public static String BLOCK_ID = "##DT";

	/**
	 * Constructor.
	 *
	 * @param sbc
	 *            The byte channel pointing to the MDF file.
	 * @param pos
	 *            The position of the block within the MDF file.
	 */
	private DTBLOCK(SeekableByteChannel sbc, long pos) {
		super(sbc, pos);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String toString() {
		return new StringBuilder().append("DTBLOCK [pos=").append(getPos()).append("]").toString();
	}

	/**
	 * Reads a DTBLOCK from the channel starting at current channel position.
	 *
	 * @param channel
	 *            The channel to read from.
	 * @param pos
	 *            The position
	 * @return The block data.
	 * @throws IOException
	 *             The exception.
	 */
	public static DTBLOCK read(SeekableByteChannel channel, long pos) throws IOException {
		DTBLOCK block = new DTBLOCK(channel, pos);

		// read block header
		ByteBuffer bb = ByteBuffer.allocate(24);
		bb.order(ByteOrder.LITTLE_ENDIAN);
		channel.position(pos);
		channel.read(bb);
		bb.rewind();

		// CHAR 4: Block type identifier
		block.setId(MDF4Util.readCharsISO8859(bb, 4));
		if (!block.getId().equals(BLOCK_ID)) {
			throw new IOException(new StringBuilder().append("Wrong block type - expected '").append(BLOCK_ID).append("', found '").append(block.getId()).append("'").toString());
		}

		// BYTE 4: Reserved used for 8-Byte alignment
		bb.get(new byte[4]);

		// UINT64: Length of block
		block.setLength(MDF4Util.readUInt64(bb));

		// UINT64: Number of links
		block.setLinkCount(MDF4Util.readUInt64(bb));

		return block;
	}

}
