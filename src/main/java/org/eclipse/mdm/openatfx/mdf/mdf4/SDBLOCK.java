/*
 * Copyright (c) 2016 Audi AG
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package org.eclipse.mdm.openatfx.mdf.mdf4;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.SeekableByteChannel;

/**
 * <p>
 * THE SIGNAL DATA BLOCK <code>SDBLOCK</code>
 * </p>
 * The data section of the SDBLOCK contains signal data of variable length from
 * its parent CNBLOCK.
 *
 * @author Christian Rechner, Tobias Leemann
 */
class SDBLOCK extends BLOCK {

	public static String BLOCK_ID = "##SD";

	/**
	 * Constructor.
	 *
	 * @param sbc
	 *            The byte channel pointing to the MDF file.
	 * @param pos
	 *            The position of the block within the MDF file.
	 */
	private SDBLOCK(SeekableByteChannel sbc, long pos) {
		super(sbc, pos);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String toString() {
		return "DTBLOCK [pos=" + getPos() + "]";
	}

	/**
	 * Reads a SDBLOCK from the channel starting at current channel position.
	 *
	 * @param channel
	 *            The channel to read from.
	 * @param pos
	 *            The position
	 * @return The block data.
	 * @throws IOException
	 *             The exception.
	 */
	public static SDBLOCK read(SeekableByteChannel channel, long pos) throws IOException {
		SDBLOCK block = new SDBLOCK(channel, pos);

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