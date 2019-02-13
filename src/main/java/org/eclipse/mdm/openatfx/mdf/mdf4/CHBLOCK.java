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
 * THE CHANNEL HIERARCHY BLOCK <code>CHBLOCK</code>
 * </p>
 * The CHBLOCKs describe a logical ordering of the channels in a tree-like
 * structure. This only serves to structure the channels and is totally
 * independent to the data group and channel group structuring. A channel even
 * may not be referenced at all or more than one time.<br/>
 * Each CHBLOCK can be seen as a node in a tree which has a number of channels
 * as leafs and which has a reference to its next sibling and its first child
 * node (both CHBLOCKs). The reference to a channel is always a triple link to
 * the CNBLOCK of the channel and its parent CGBLOCK and DGBLOCK. Each CHBLOCK
 * can have a name.
 *
 * @author Christian Rechner
 */
class CHBLOCK extends BLOCK {

	public static String BLOCK_ID = "##CH";

	/**
	 * Constructor.
	 *
	 * @param sbc
	 *            The byte channel pointing to the MDF file.
	 * @param pos
	 *            The position of the block within the MDF file.
	 */
	private CHBLOCK(SeekableByteChannel sbc, long pos) {
		super(sbc, pos);
	}

	/**
	 * Reads a CHBLOCK from the channel starting at current channel position.
	 *
	 * @param channel
	 *            The channel to read from.
	 * @param pos
	 *            The position
	 * @return The block data.
	 * @throws IOException
	 *             The exception.
	 */
	public static CHBLOCK read(SeekableByteChannel channel, long pos) throws IOException {
		CHBLOCK block = new CHBLOCK(channel, pos);

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

		// read block content
		bb = ByteBuffer.allocate((int) block.getLength() + 24);
		bb.order(ByteOrder.LITTLE_ENDIAN);
		channel.position(pos + 24);
		channel.read(bb);
		bb.rewind();

		// TODO: implement reading

		return block;
	}

}
