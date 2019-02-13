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
 * THE DATA GROUP BLOCK <code>DGBLOCK<code>
 * </p>
 * The DGBLOCK gathers information and links related to its data block. Thus the
 * branch in the tree of MDF blocks that is opened by the DGBLOCK contains all
 * information necessary to understand and decode the data block referenced by
 * the DGBLOCK. The DGBLOCK can contain several channel groups. In this case the
 * data group (and thus the MDF file) is "unsorted". If there is only one
 * channel group in the DGBLOCK, the data group is "sorted".
 *
 * @author Christian Rechner
 */
class DGBLOCK extends BLOCK {

	public static String BLOCK_ID = "##DG";

	/** Link section */

	// Pointer to next data group block (DGBLOCK) (can be NIL)
	// LINK
	private long lnkDgNext;

	// Pointer to first channel group block (CGBLOCK) (can be NIL)
	// LINK
	private long lnkCgFirst;

	// Pointer to data block (DTBLOCK or DZBLOCK for this block type) or data
	// list block (DLBLOCK of data blocks or its
	// HLBLOCK) (can be NIL)
	// LINK
	private long lnkData;

	// Pointer to comment and additional information (TXBLOCK or MDBLOCK) (can
	// be NIL)
	// LINK
	private long lnkMdComment;

	/** Data section */

	// Number of Bytes used for record IDs in the data block.
	// 0 = data records without record ID (only possible for sorted data group)
	// 1 = record ID (UINT8) before each data record
	// 2 = record ID (UINT16, LE Byte order) before each data record
	// 4 = record ID (UINT32, LE Byte order) before each data record
	// 8 = record ID (UINT64, LE Byte order) before each data record
	// UINT8
	private byte recIdSize;

	/**
	 * Constructor.
	 *
	 * @param sbc
	 *            The byte channel pointing to the MDF file.
	 * @param pos
	 *            The position of the block within the MDF file.
	 */
	private DGBLOCK(SeekableByteChannel sbc, long pos) {
		super(sbc, pos);
	}

	public long getLnkDgNext() {
		return lnkDgNext;
	}

	public long getLnkCgFirst() {
		return lnkCgFirst;
	}

	public long getLnkData() {
		return lnkData;
	}

	public long getLnkMdComment() {
		return lnkMdComment;
	}

	public byte getRecIdSize() {
		return recIdSize;
	}

	private void setLnkDgNext(long lnkDgNext) {
		this.lnkDgNext = lnkDgNext;
	}

	private void setLnkCgFirst(long lnkCgFirst) {
		this.lnkCgFirst = lnkCgFirst;
	}

	private void setLnkData(long lnkData) {
		this.lnkData = lnkData;
	}

	private void setLnkMdComment(long lnkMdComment) {
		this.lnkMdComment = lnkMdComment;
	}

	private void setRecIdSize(byte recIdSize) {
		this.recIdSize = recIdSize;
	}

	public DGBLOCK getDgNextBlock() throws IOException {
		if (lnkDgNext > 0) {
			return DGBLOCK.read(sbc, lnkDgNext);
		}
		return null;
	}

	public CGBLOCK getCgFirstBlock() throws IOException {
		if (lnkCgFirst > 0) {
			return CGBLOCK.read(sbc, lnkCgFirst);
		}
		return null;
	}

	public BLOCK getDataBlock() throws IOException {
		if (lnkData > 0) {
			String blockType = getBlockType(sbc, lnkData);
			// link points to a DTBLOCK
			if ("##DT".equals(blockType)) {
				return DTBLOCK.read(sbc, lnkData);
			}
			// link points to a DZBLOCK
			else if ("##DZ".equals(blockType)) {
				// TODO: implement
				return null;
			}
			// link points to a DLBLOCK
			else if (blockType.equals(DLBLOCK.BLOCK_ID)) {
				return DLBLOCK.read(sbc, lnkData);
			}
			// link points to a HLBLOCK
			else if ("##HL".equals(blockType)) {
				// TODO: implement
				return null;
			}
			// unknown
			else {
				throw new IOException("Unsupported block type for data: " + blockType);
			}
		}
		return null;
	}

	public BLOCK getMdCommentBlock() throws IOException {
		if (lnkMdComment > 0) {
			String blockType = getBlockType(sbc, lnkMdComment);
			// link points to a MDBLOCK
			if (blockType.equals(MDBLOCK.BLOCK_ID)) {
				return MDBLOCK.read(sbc, lnkMdComment);
			}
			// links points to TXBLOCK
			else if (blockType.equals(TXBLOCK.BLOCK_ID)) {
				return TXBLOCK.read(sbc, lnkMdComment);
			}
			// unknown
			else {
				throw new IOException("Unsupported block type for MdComment: " + blockType);
			}
		}
		return null;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String toString() {
		return new StringBuilder().append("DGBLOCK [lnkDgNext=").append(lnkDgNext).append(", lnkCgFirst=").append(lnkCgFirst).append(", lnkData=").append(lnkData).append(", lnkMdComment=")
				.append(lnkMdComment).append(", recIdSize=").append(recIdSize).append("]").toString();
	}

	/**
	 * Reads a DGBLOCK from the channel starting at current channel position.
	 *
	 * @param channel
	 *            The channel to read from.
	 * @param pos
	 *            The position within the channel.
	 * @return The block data.
	 * @throws IOException
	 *             The exception.
	 */
	public static DGBLOCK read(SeekableByteChannel channel, long pos) throws IOException {
		DGBLOCK block = new DGBLOCK(channel, pos);

		// read block header
		ByteBuffer bb = ByteBuffer.allocate(64);
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

		// LINK: Pointer to next data group block (DGBLOCK) (can be NIL)
		block.setLnkDgNext(MDF4Util.readLink(bb));

		// LINK: Pointer to first channel group block (CGBLOCK) (can be NIL)
		block.setLnkCgFirst(MDF4Util.readLink(bb));

		// LINK: Pointer to data block (DTBLOCK or DZBLOCK for this block type)
		// or data list block (DLBLOCK of data
		// blocks or its HLBLOCK) (can be NIL)
		block.setLnkData(MDF4Util.readLink(bb));

		// LINK: Pointer to comment and additional information (TXBLOCK or
		// MDBLOCK) (can be NIL)
		block.setLnkMdComment(MDF4Util.readLink(bb));

		// UINT8: Number of Bytes used for record IDs in the data block.
		block.setRecIdSize(MDF4Util.readUInt8(bb));

		return block;
	}

}
