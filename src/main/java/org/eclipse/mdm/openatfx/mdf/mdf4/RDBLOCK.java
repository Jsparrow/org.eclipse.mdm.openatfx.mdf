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
 * THE REDUCTION DATA BLOCK <code>RDBLOCK</code>
 * </p>
 *
 * @author Tobias Leemann
 */
class RDBLOCK extends BLOCK {

	public static String BLOCK_ID = "##RD";

	/**
	 * Constructor.
	 *
	 * @param sbc
	 *            The byte channel pointing to the MDF file.
	 * @param pos
	 *            The position of the block within the MDF file.
	 */
	private RDBLOCK(SeekableByteChannel sbc, long pos) {
		super(sbc, pos);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String toString() {
		return "RDBLOCK [pos=" + getPos() + "]";
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
	public static RDBLOCK read(SeekableByteChannel channel, long pos) throws IOException {
		RDBLOCK block = new RDBLOCK(channel, pos);

		// read block header
		ByteBuffer bb = ByteBuffer.allocate(24);
		bb.order(ByteOrder.LITTLE_ENDIAN);
		channel.position(pos);
		channel.read(bb);
		bb.rewind();

		// CHAR 4: Block type identifier
		block.setId(MDF4Util.readCharsISO8859(bb, 4));
		if (!block.getId().equals(BLOCK_ID)) {
			throw new IOException("Wrong block type - expected '" + BLOCK_ID + "', found '" + block.getId() + "'");
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
