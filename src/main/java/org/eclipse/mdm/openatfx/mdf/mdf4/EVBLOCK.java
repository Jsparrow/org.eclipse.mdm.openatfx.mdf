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
import java.util.Arrays;

/**
 * <p>
 * THE EVENT BLOCK <code>EVBLOCK<code>
 * </p>
 * The EVBLOCK stored Data about an event, that influenced the measurement, e.g.
 * start, end, errors...
 *
 * @author Tobias Leemann
 */
class EVBLOCK extends BLOCK {

	public static String BLOCK_ID = "##EV";

	/** Link section */

	// Pointer to next event block (EVBLOCK) (can be NIL)
	// LINK
	private long lnkEvNext;

	// Pointer to parent EVBLOCK (can be NIL)
	// LINK
	private long lnkEvParent;

	// Pointer to event block which defines the beginning of a range (EVBLOCK)
	// (can be NIL)
	// LINK
	private long lnkEvRange;

	// Pointer to TextBlock with EventName (can be NIL)
	// LINK
	private long lnkTxName;

	// Pointer to comment and additional information (TXBLOCK or MDBLOCK) (can
	// be NIL)
	// LINK
	private long lnkMdComment;

	// Pointers to Blocks affected by this event. (CNBLOCK or CGBLOCK) (can be
	// NIL)
	// LINK
	private long[] lnkScope;

	// Pointers to Referenced Attachment Blocks (ATBLOCK) (can be NIL)
	// LINK
	private long[] lnkAtReference;

	/** Data section */

	// Event Type
	// 0 = Recording
	// 1 = Recording Interrupt
	// 2 = Acquisition Interrupt
	// 3 = Start Recording Trigger
	// 4 = Stop Recording Trigger
	// 5 = Trigger
	// 6 = Marker
	// UINT8
	private byte type;

	private byte syncType;

	// Range Type
	// 0 = Point
	// 1 = Beginning of Range
	// 2 = End of Ranger
	// UINT8
	private byte rangeType;

	// Event Cause
	// 0 = OTHER
	// 1 = ERROR
	// 2 = TOOL
	// 3 = SCRIPT
	// 4 = USER
	// UINT8
	private byte cause;

	// Flags
	// Bit 0 = PostProcessingFlag
	// UINT8
	private byte flags;

	/**
	 * Number of Scope Links
	 */
	private long scopeCount;

	/**
	 * Number of Attachments
	 */
	private int attachmentCount;

	/**
	 * Number of FH-Block in list, specifying the tool responsible for this
	 * event.
	 */
	private int creatorIndex;

	/**
	 * Base for sync value.
	 */
	private long syncBaseValue;

	/**
	 * Factor for sync value.
	 */
	private double syncFactor;

	/**
	 * Constructor.
	 *
	 * @param sbc
	 *            The byte channel pointing to the MDF file.
	 * @param pos
	 *            The position of the block within the MDF file.
	 */
	private EVBLOCK(SeekableByteChannel sbc, long pos) {
		super(sbc, pos);
	}

	public static String getBLOCK_ID() {
		return BLOCK_ID;
	}

	public long getLnkEvNext() {
		return lnkEvNext;
	}

	public long getLnkEvParent() {
		return lnkEvParent;
	}

	public long getLnkEvRange() {
		return lnkEvRange;
	}

	public long getLnkTxName() {
		return lnkTxName;
	}

	public long getLnkMdComment() {
		return lnkMdComment;
	}

	public long[] getLnkScope() {
		return lnkScope;
	}

	public long[] getLnkAtReference() {
		return lnkAtReference;
	}

	public byte getType() {
		return type;
	}

	public byte getSyncType() {
		return syncType;
	}

	public byte getRangeType() {
		return rangeType;
	}

	public byte getCause() {
		return cause;
	}

	public byte getFlags() {
		return flags;
	}

	public long getScopeCount() {
		return scopeCount;
	}

	public int getAttachmentCount() {
		return attachmentCount;
	}

	public int getCreatorIndex() {
		return creatorIndex;
	}

	public long getSyncBaseValue() {
		return syncBaseValue;
	}

	public double getSyncFactor() {
		return syncFactor;
	}

	private void setLnkEvNext(long lnkEvNext) {
		this.lnkEvNext = lnkEvNext;
	}

	private void setLnkEvParent(long lnkEvParent) {
		this.lnkEvParent = lnkEvParent;
	}

	private void setLnkEvRange(long lnkEvRange) {
		this.lnkEvRange = lnkEvRange;
	}

	private void setLnkTxName(long lnkTxName) {
		this.lnkTxName = lnkTxName;
	}

	private void setLnkMdComment(long lnkMdComment) {
		this.lnkMdComment = lnkMdComment;
	}

	private void setLnkScope(long[] lnkScope) {
		this.lnkScope = lnkScope;
	}

	private void setLnkAtReference(long[] lnkAtReference) {
		this.lnkAtReference = lnkAtReference;
	}

	private void setType(byte type) {
		this.type = type;
	}

	private void setSyncType(byte syncType) {
		this.syncType = syncType;
	}

	private void setRangeType(byte rangeType) {
		this.rangeType = rangeType;
	}

	private void setCause(byte cause) {
		this.cause = cause;
	}

	private void setFlags(byte flags) {
		this.flags = flags;
	}

	private void setScopeCount(long scopeCount) {
		this.scopeCount = scopeCount;
	}

	private void setAttachmentCount(int attachmentCount) {
		this.attachmentCount = attachmentCount;
	}

	private void setCreatorIndex(int creatorIndex) {
		this.creatorIndex = creatorIndex;
	}

	private void setSyncBaseValue(long syncBaseValue) {
		this.syncBaseValue = syncBaseValue;
	}

	private void setSyncFactor(double syncFactor) {
		this.syncFactor = syncFactor;
	}

	public double getSyncValue() {
		return syncBaseValue * syncFactor;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String toString() {
		return new StringBuilder().append("EVBLOCK [lnkEvNext=").append(lnkEvNext).append(", lnkEvParent=").append(lnkEvParent).append(", lnkEvRange=").append(lnkEvRange).append(", lnkTxName=")
				.append(lnkTxName).append(", lnkMdComment=").append(lnkMdComment).append(", lnkScope=").append(Arrays.toString(lnkScope)).append(", lnkAtReference=").append(Arrays.toString(lnkAtReference))
				.append(", type=").append(type).append(", syncType=").append(syncType).append(", rangeType=").append(rangeType).append(", cause=")
				.append(cause).append(", flags=").append(flags).append(", scopeCount=").append(scopeCount).append(", attachmentCount=").append(attachmentCount)
				.append(", creatorIndex=").append(creatorIndex).append(", syncBaseBValue=").append(syncBaseValue).append(", syncFactor=").append(syncFactor).append("]").toString();
	}

	public EVBLOCK getEvNextBlock() throws IOException {
		if (lnkEvNext > 0) {
			return EVBLOCK.read(sbc, lnkEvNext);
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

	public TXBLOCK getEvTxNameBlock() throws IOException {
		if (lnkTxName > 0) {
			return TXBLOCK.read(sbc, lnkTxName);
		}
		return null;
	}

	/**
	 * Reads an EVBLOCK from the channel.
	 *
	 * @param channel
	 *            The channel to read from.
	 * @param pos
	 *            The position within the channel.
	 * @return The block data.
	 * @throws IOException
	 *             The exception.
	 */
	public static EVBLOCK read(SeekableByteChannel channel, long pos) throws IOException {
		EVBLOCK block = new EVBLOCK(channel, pos);

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

		// Read link section
		bb = ByteBuffer.allocate((int) (8 * block.getLinkCount()));
		bb.order(ByteOrder.LITTLE_ENDIAN);
		channel.read(bb);
		bb.rewind();
		// LINK: Pointer to next EVBLOCK (can be NIL)
		block.setLnkEvNext(MDF4Util.readLink(bb));

		// LINK: Pointer to Parent EVBLOCK (can be NIL)
		block.setLnkEvParent(MDF4Util.readLink(bb));

		// LINK: Pointer to Range limit EVBLOCK (can be NIL)
		block.setLnkEvRange(MDF4Util.readLink(bb));

		// LINK: Pointer to name block TXBLOCK
		block.setLnkTxName(MDF4Util.readLink(bb));

		// LINK: Pointer to comment and additional information (TXBLOCK or
		// MDBLOCK) (can be NIL)
		block.setLnkMdComment(MDF4Util.readLink(bb));

		// Read remaining links
		long[] remaining = new long[(int) (block.getLinkCount() - 5)];
		for (int i = 0; i < block.getLinkCount() - 5; i++) {
			remaining[i] = MDF4Util.readLink(bb);
		}

		// Read data section
		bb = ByteBuffer.allocate(32);
		bb.order(ByteOrder.LITTLE_ENDIAN);
		channel.read(bb);
		bb.rewind();

		// UINT8: Type
		block.setType(MDF4Util.readUInt8(bb));

		// UINT8: SyncType
		block.setSyncType(MDF4Util.readUInt8(bb));

		// UINT8: RangeType
		block.setRangeType(MDF4Util.readUInt8(bb));

		// UINT8: Cause
		block.setCause(MDF4Util.readUInt8(bb));

		// UINT8: Flags
		block.setFlags(MDF4Util.readUInt8(bb));

		// Skip 3 Byte
		bb.get(new byte[3]);

		// UINT32: Scope List
		block.setScopeCount(MDF4Util.readUInt32(bb));

		long[] scopel = new long[(int) block.getScopeCount()];
		System.arraycopy(remaining, 0, scopel, 0, (int) block.getScopeCount());
		block.setLnkScope(scopel);

		// UINT16: Attachment count
		block.setAttachmentCount(MDF4Util.readUInt16(bb));

		long[] attmt = new long[block.getAttachmentCount()];
		System.arraycopy(remaining, (int) block.getScopeCount(), attmt, 0, block.getAttachmentCount());
		block.setLnkAtReference(attmt);

		// UINT16: Creator index
		block.setCreatorIndex(MDF4Util.readUInt16(bb));

		// UINT64: Sync Base
		block.setSyncBaseValue(MDF4Util.readUInt64(bb));

		// REAL: Sync Factor
		block.setSyncFactor(MDF4Util.readReal(bb));

		return block;
	}

}
