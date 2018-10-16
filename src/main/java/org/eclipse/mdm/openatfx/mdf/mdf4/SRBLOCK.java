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
 * THE SAMPLE REDUCTION BLOCK <code>SRBLOCK<code>
 * </p>
 * The SRBLOCK describes a sample reduction for a channel group.
 *
 * @author Tobias Leemann
 */

class SRBLOCK extends BLOCK {

	public static String BLOCK_ID = "##SR";

	/** Link section */

	// Pointer to next SRBLOCK (can be NIL)
	// LINK
	private long lnkSrNext;

	// Pointer to RDBLOCK (or equivalent) with reduction data
	private long lnkRdData;

	/** Data section */

	// Number of Cycles
	// UINT64
	private long cycleCount;

	// Sample interval length
	// REAL
	private double interval;

	// Sync Type
	// 1 = Time
	// 2 = Angel
	// 3 = Distance
	// 4 = Index
	// UINT8
	private byte syncType;

	// Flags
	// The value contains the following bit flags (Bit 0 = LSB):
	// Bit 0: inval bits flag
	// UINT8
	private byte flags;

	/**
	 * Constructor.
	 *
	 * @param sbc
	 *            The byte channel pointing to the MDF file.
	 * @param pos
	 *            The position of the block within the MDF file.
	 */
	private SRBLOCK(SeekableByteChannel sbc, long pos) {
		super(sbc, pos);
	}

	public long getLnkSrNext() {
		return lnkSrNext;
	}

	public long getLnkRdData() {
		return lnkRdData;
	}

	public long getCycleCount() {
		return cycleCount;
	}

	public double getInterval() {
		return interval;
	}

	public byte getSyncType() {
		return syncType;
	}

	public byte getFlags() {
		return flags;
	}

	private void setLnkSrNext(long lnkSrNext) {
		this.lnkSrNext = lnkSrNext;
	}

	private void setLnkRdData(long lnkRdData) {
		this.lnkRdData = lnkRdData;
	}

	private void setCycleCount(long cycleCount) {
		this.cycleCount = cycleCount;
	}

	private void setInterval(double interval) {
		this.interval = interval;
	}

	private void setSyncType(byte syncType) {
		this.syncType = syncType;
	}

	private void setFlags(byte flags) {
		this.flags = flags;
	}

	public SRBLOCK getSrNextBlock() throws IOException {
		if (lnkSrNext > 0) {
			return SRBLOCK.read(sbc, lnkSrNext);
		}
		return null;
	}

	@Override
	public String toString() {
		return "SRBLOCK [lnkSrNext=" + lnkSrNext + ", lnkRdData=" + lnkRdData + ", cycleCount=" + cycleCount
				+ ", interval=" + interval + ", syncType=" + syncType + ", flags=" + flags + "]";
	}

	/**
	 * Reads a SRBLOCK from the channel starting at current channel position.
	 *
	 * @param channel
	 *            The channel to read from.
	 * @param pos
	 *            The position within the channel.
	 * @return The block data.
	 * @throws IOException
	 *             The exception.
	 */
	public static SRBLOCK read(SeekableByteChannel channel, long pos) throws IOException {
		SRBLOCK block = new SRBLOCK(channel, pos);

		// read block header
		ByteBuffer bb = ByteBuffer.allocate(24 + 16 + 24);
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

		// LINK: Pointer to next SRBLOCK
		block.setLnkSrNext(MDF4Util.readLink(bb));

		// LINK: Pointer to RDBLOCK with data
		block.setLnkRdData(MDF4Util.readLink(bb));

		// UINT64: CycleCount
		block.setCycleCount(MDF4Util.readUInt64(bb));

		// REAL: Interval
		block.setInterval(MDF4Util.readReal(bb));

		// UINT8: SyncType:
		block.setSyncType(MDF4Util.readUInt8(bb));

		// UINT8: Flags
		block.setFlags(MDF4Util.readUInt8(bb));

		return block;
	}
}
