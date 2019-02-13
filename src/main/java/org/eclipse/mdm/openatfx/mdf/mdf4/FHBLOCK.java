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
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.SeekableByteChannel;

/**
 * <p>
 * THE FILE HISTORY BLOCK <code>FHBLOCK<code>
 * </p>
 * The FHBLOCK describes who/which tool generated or changed the MDF file. Each
 * FHBLOCK contains a change log entry for the MDF file. The first FHBLOCK
 * referenced by the HDBLOCK must contain information about the tool which
 * created the MDF file. Starting from this FHBLOCK then a linked list of
 * FHBLOCKs can be maintained with a chronological change history, i.e. any
 * other application that changes the file must append a new FHBLOCK to the
 * list. A zero-based index value is used to reference blocks within this linked
 * list, thus the ordering of the blocks must not be changed.
 *
 * @author Christian Rechner
 */
class FHBLOCK extends BLOCK {

	public static String BLOCK_ID = "##FH";

	/** Link section */

	// Link to next FHBLOCK (can be NIL if list finished)
	// LINK
	private long lnkFhNext;

	// Link to MDBLOCK containing comment about the creation or modification of
	// the MDF file.
	// LINK
	private long lnkMdComment;

	/** Data section */

	// Time stamp at which the file has been changed/created (first entry) in
	// nanoseconds elapsed since 00:00:00
	// 01.01.1970 (UTC time or local time, depending on "local time" flag).
	// UINT64
	private long startTimeNs;

	// Time zone offset in minutes.
	// The value is not necessarily a multiple of 60 and can be negative! For
	// the current time zone definitions, it is
	// expected to be in the range [-840,840] min.
	// For example a value of 60 (min) means UTC+1 time zone = Central European
	// Time
	// Only valid if "time offsets valid" flag is set in time flags.
	// INT16
	private short tzOffsetMin;

	// Daylight saving time (DST) offset in minutes for start time stamp. During
	// the summer months, most regions observe
	// a DST offset of 60 min (1 hour).
	// Only valid if "time offsets valid" flag is set in time flags.
	// INT16
	private short dstOffsetMin;

	// Time flags
	// The value contains the following bit flags (Bit 0 = LSB):
	// Bit 0: Local time flag
	// If set, the start time stamp in nanoseconds represents the local time
	// instead of the UTC time, In this case, time
	// zone and DST offset must not be considered (time offsets flag must not be
	// set). Should only be used if UTC time
	// is unknown.
	// If the bit is not set (default), the start time stamp represents the UTC
	// time.
	// Bit 1: Time offsets valid flag
	// If set, the time zone and DST offsets are valid. Must not be set together
	// with "local time" flag (mutually
	// exclusive).
	// If the offsets are valid, the locally displayed time at start of
	// recording can be determined
	// (after conversion of offsets to ns) by
	// Local time = UTC time + time zone offset + DST offset.
	// UINT8
	private byte timeFlags;

	/**
	 * Constructor.
	 *
	 * @param sbc
	 *            The byte channel pointing to the MDF file.
	 * @param pos
	 *            The position of the block within the MDF file.
	 */
	private FHBLOCK(SeekableByteChannel sbc, long pos) {
		super(sbc, pos);
	}

	private void setLnkFhNext(long lnkFhNext) {
		this.lnkFhNext = lnkFhNext;
	}

	private void setLnkMdComment(long lnkMdComment) {
		this.lnkMdComment = lnkMdComment;
	}

	private void setStartTimeNs(long startTimeNs) {
		this.startTimeNs = startTimeNs;
	}

	private void setTzOffsetMin(short tzOffsetMin) {
		this.tzOffsetMin = tzOffsetMin;
	}

	private void setDstOffsetMin(short dstOffsetMin) {
		this.dstOffsetMin = dstOffsetMin;
	}

	private void setTimeFlags(byte timeFlags) {
		this.timeFlags = timeFlags;
	}

	public long getLnkFhNext() {
		return lnkFhNext;
	}

	public long getLnkMdComment() {
		return lnkMdComment;
	}

	public long getStartTimeNs() {
		return startTimeNs;
	}

	public short getTzOffsetMin() {
		return tzOffsetMin;
	}

	public short getDstOffsetMin() {
		return dstOffsetMin;
	}

	public byte getTimeFlags() {
		return timeFlags;
	}

	public boolean isLocalTime() {
		return BigInteger.valueOf(timeFlags).testBit(0);
	}

	public boolean isTimeFlagsValid() {
		return BigInteger.valueOf(timeFlags).testBit(1);
	}

	public FHBLOCK getFhNextBlock() throws IOException {
		if (lnkFhNext > 0) {
			return FHBLOCK.read(sbc, lnkFhNext);
		}
		return null;
	}

	public MDBLOCK getMdCommentBlock() throws IOException {
		if (lnkMdComment > 0) {
			return MDBLOCK.read(sbc, lnkMdComment);
		}
		return null;
	}

	@Override
	public String toString() {
		return new StringBuilder().append("FHBLOCK [lnkFhNext=").append(lnkFhNext).append(", lnkMdComment=").append(lnkMdComment).append(", startTimeNs=").append(startTimeNs).append(", tzOffsetMin=")
				.append(tzOffsetMin).append(", dstOffsetMin=").append(dstOffsetMin).append(", timeFlags=").append(timeFlags).append("]").toString();
	}

	/**
	 * Reads a FHBLOCK from the channel starting at current channel position.
	 *
	 * @param channel
	 *            The channel to read from.
	 * @param pos
	 *            The position within the channel.
	 * @return The block data.
	 * @throws IOException
	 *             The exception.
	 */
	public static FHBLOCK read(SeekableByteChannel channel, long pos) throws IOException {
		FHBLOCK block = new FHBLOCK(channel, pos);

		// read block header
		ByteBuffer bb = ByteBuffer.allocate(56);
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

		// LINK: Link to next FHBLOCK (can be NIL if list finished)
		block.setLnkFhNext(MDF4Util.readLink(bb));

		// LINK: Link to MDBLOCK containing comment about the creation or
		// modification of the MDF file.
		block.setLnkMdComment(MDF4Util.readLink(bb));

		// UINT64: Time stamp at which the file has been changed/created (first
		// entry)
		block.setStartTimeNs(MDF4Util.readUInt64(bb));

		// INT16: Time zone offset in minutes.
		block.setTzOffsetMin(MDF4Util.readInt16(bb));

		// INT16: Daylight saving time (DST) offset in minutes for start time
		// stamp.
		block.setDstOffsetMin(MDF4Util.readInt16(bb));

		// UINT8: Time flags
		block.setTimeFlags(MDF4Util.readUInt8(bb));

		return block;
	}

}
