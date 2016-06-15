/*
 * Copyright (c) 2016 Audi AG
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package org.eclipse.mdm.openatfx.mdf.mdf4;

import java.io.IOException;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.SeekableByteChannel;
import java.util.Arrays;


/**
 * <p>
 * THE DATA LIST BLOCK <code>DLBLOCK</code>
 * </p>
 * The DLBLOCK references a list of data blocks (DTBLOCK) or a list of signal data blocks (SDBLOCK) or a list of
 * reduction data blocks (RDBLOCK). This list of blocks is equivalent to using a single (signal/reduction) data block
 * and can be used to avoid a huge data block by splitting it into smaller parts.
 *
 * @author Christian Rechner
 */
class DLBLOCK extends BLOCK {

	public static String BLOCK_ID = "##DL";

	/** Link section */

	// Pointer to next data list block (DLBLOCK) (can be NIL).
	// LINK
	private long lnkDlNext;

	// Pointers to the data blocks (DTBLOCK, SDBLOCK or RDBLOCK or a DZBLOCK of the respective block type).
	// None of the links in the list can be NIL.
	// LINK N
	private long[] lnkDlData;

	/** Data section */

	// Flags
	// Bit 0: Equal length flag
	// UINT8
	private byte flags;

	// Number of referenced blocks N
	// UINT32
	private long count;

	// Only present if "equal length" flag (bit 0 in dl_flags) is set.
	// Equal data section length.
	// UINT64
	private long equalLength;

	// Only present if "equal length" flag (bit 0 in dl_flags) is not set.
	// Start offset (in Bytes) for the data section of each referenced block.
	// UINT64
	private long[] offset;

	/**
	 * Constructor.
	 *
	 * @param sbc The byte channel pointing to the MDF file.
	 * @param pos The position of the block within the MDF file.
	 */
	private DLBLOCK(SeekableByteChannel sbc, long pos) {
		super(sbc, pos);
	}

	public long getLnkDlNext() {
		return lnkDlNext;
	}

	public long[] getLnkDlData() {
		return lnkDlData;
	}

	public byte getFlags() {
		return flags;
	}

	public long getCount() {
		return count;
	}

	public long getEqualLength() {
		return equalLength;
	}

	public long[] getOffset() {
		return offset;
	}

	private void setLnkDlNext(long lnkDlNext) {
		this.lnkDlNext = lnkDlNext;
	}

	private void setLnkDlData(long[] lnkDlData) {
		this.lnkDlData = lnkDlData;
	}

	private void setFlags(byte flags) {
		this.flags = flags;
	}

	private void setCount(long count) {
		this.count = count;
	}

	private void setEqualLength(long equalLength) {
		this.equalLength = equalLength;
	}

	private void setOffset(long[] offset) {
		this.offset = offset;
	}

	public boolean isEqualLengthFlag() {
		return BigInteger.valueOf(flags).testBit(0);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String toString() {
		return "DLBLOCK [lnkDlNext=" + lnkDlNext + ", lnkDlData=" + Arrays.toString(lnkDlData) + ", flags=" + flags
				+ ", count=" + count + ", equalLength=" + equalLength + ", offset=" + Arrays.toString(offset) + "]";
	}

	public DLBLOCK getDlNextBlock() throws IOException {
		if (lnkDlNext > 0) {
			return DLBLOCK.read(sbc, lnkDlNext);
		}
		return null;
	}

	public long getDataBlockSize(int index) throws IOException {
		if(getLnkDlData()[index]==0){
			return 0;
		}
		String blockType = getBlockType(sbc, lnkDlData[index]);
		// link points to a SDBLOCK
		if (blockType.equals(SDBLOCK.BLOCK_ID)) {
			return SDBLOCK.read(sbc, lnkDlData[index]).getLength();
		}
		// links points to DTBLOCK
		else if (blockType.equals(DTBLOCK.BLOCK_ID)) {
			return DTBLOCK.read(sbc, lnkDlData[index]).getLength();
		}
		// links points to RDBLOCK
		else if (blockType.equals(RDBLOCK.BLOCK_ID)) {
			return RDBLOCK.read(sbc, lnkDlData[index]).getLength();
		}
		// unknown
		else {
			throw new IOException("Unsupported block type for MdComment: " + blockType);
		}
	}

	/**
	 * Reads a DLBLOCK from the channel starting at current channel position.
	 *
	 * @param channel The channel to read from.
	 * @param pos The position
	 * @return The block data.
	 * @throws IOException The exception.
	 */
	public static DLBLOCK read(SeekableByteChannel channel, long pos) throws IOException {
		DLBLOCK block = new DLBLOCK(channel, pos);

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

		// read block content
		bb = ByteBuffer.allocate((int) block.getLength() + 24);
		bb.order(ByteOrder.LITTLE_ENDIAN);
		channel.position(pos + 24);
		channel.read(bb);
		bb.rewind();

		// read links
		long[] lnks = new long[(int) block.getLinkCount()];
		for (int i = 0; i < lnks.length; i++) {
			lnks[i] = MDF4Util.readLink(bb);
		}

		// read data

		// UINT8: Flags
		block.setFlags(MDF4Util.readUInt8(bb));

		// BYTE 3: Reserved
		bb.get(new byte[3]);

		// UINT32: Number of referenced blocks
		block.setCount(MDF4Util.readUInt32(bb));

		// UINT64: Equal data section length.
		// !!! Only present if "equal length" flag (bit 0 in dl_flags) is set. !!!
		if (block.isEqualLengthFlag()) {
			block.setEqualLength(MDF4Util.readUInt64(bb));
		}

		if(!block.isEqualLengthFlag()){
			// UINT64 N: Start offset (in Bytes) for the data section of each referenced block.
			long[] offset = new long[(int) block.getCount()];
			for (int i = 0; i < offset.length; i++) {
				offset[i] = MDF4Util.readUInt64(bb);
			}
			block.setOffset(offset);
		}

		// extract links after reading data (then we know how many attachments)
		block.setLnkDlNext(lnks[0]);

		long[] lnkDlData = new long[(int) block.getCount()];
		for (int i = 0; i < lnkDlData.length; i++) {
			lnkDlData[i] = lnks[i + 1];
		}
		block.setLnkDlData(lnkDlData);

		return block;
	}

	public boolean breaksRecords(long recordsize) throws IOException{
		if(isEqualLengthFlag()){
			if (equalLength % recordsize != 0){
				// if we are the only block in the list, this size doesn't matter.
				// Check size of DTBlock instead.
				if(getCount() != 1){
					return true;
				}
			}
		} else {
			for(long offset : this.offset) {
				if (offset % recordsize != 0){
					return true;
				}
			}
		}

		//this list fulfills requirements, but next one?
		if(getLnkDlNext() > 0){
			return getDlNextBlock().breaksRecords(recordsize);
		} else {
			return false;
		}
	}

}
