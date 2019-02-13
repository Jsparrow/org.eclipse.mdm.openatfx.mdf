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
 * THE HEADER BLOCK <code>HDBLOCK<code>
 * </p>
 * The HDBLOCK always begins at file position 64. It contains general
 * information about the contents of the measured data file and is the root for
 * the block hierarchy.
 *
 * @author Christian Rechner
 */
class HDBLOCK extends BLOCK {

	public static String BLOCK_ID = "##HD";

	/** Link section */

	// Pointer to the first data group block (DGBLOCK) (can be NIL)
	// LINK
	private long lnkDgFirst;

	// Pointer to first file history block (FHBLOCK)
	// There must be at least one FHBLOCK with information about the application
	// which created the MDF file.
	// LINK
	private long lnkFhFirst;

	// Pointer to first channel hierarchy block (CHBLOCK) (can be NIL).
	// LINK
	private long lnkChFirst;

	// Pointer to first attachment block (ATBLOCK) (can be NIL)
	// LINK
	private long lnkAtFirst;

	// Pointer to first event block (EVBLOCK) (can be NIL)
	// LINK
	private long lnkEvFirst;

	// Pointer to the measurement file comment (TXBLOCK or MDBLOCK) (can be NIL)
	// LINK
	private long lnkMdComment;

	/** Data section */

	// Time stamp at start of measurement in nanoseconds elapsed since 00:00:00
	// 01.01.1970 (UTC time or local time,
	// depending on "local time" flag, see [UTC]). All time stamps for time
	// synchronized master channels or events are
	// always relative to this start time stamp.
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

	// Time quality class
	// 0 = local PC reference time (Default)
	// 10 = external time source
	// 16 = external absolute synchronized time
	// UINT8
	private byte timeClass;

	// Flags
	// The value contains the following bit flags (Bit 0 = LSB):
	// Bit 0: Start angle valid flag. If set, the start angle value below is
	// valid.
	// Bit 1: Start distance valid flag. If set, the start distance value below
	// is valid.
	// UINT8
	private byte flags;

	// Start angle in radians at start of measurement (only for angle
	// synchronous measurements)
	// Only valid if "start angle valid" flag is set.
	// REAL
	private double startAngleRad;

	// Start distance in meters at start of measurement
	// (only for distance synchronous measurements)
	// Only valid if "start distance valid" flag is set.
	// All distance values for distance synchronized master channels
	// REAL
	private double startDistanceM;

	/**
	 * Constructor.
	 *
	 * @param sbc
	 *            The byte channel pointing to the MDF file.
	 * @param mdfFilePath
	 *            THe path to the MDF file.
	 */
	public HDBLOCK(SeekableByteChannel sbc) {
		super(sbc, 64);
	}

	public long getLnkDgFirst() {
		return lnkDgFirst;
	}

	private void setLnkDgFirst(long lnkDgFirst) {
		this.lnkDgFirst = lnkDgFirst;
	}

	public long getLnkFhFirst() {
		return lnkFhFirst;
	}

	private void setLnkFhFirst(long lnkFhFirst) {
		this.lnkFhFirst = lnkFhFirst;
	}

	public long getLnkChFirst() {
		return lnkChFirst;
	}

	private void setLnkChFirst(long lnkChFirst) {
		this.lnkChFirst = lnkChFirst;
	}

	public long getLnkAtFirst() {
		return lnkAtFirst;
	}

	private void setLnkAtFirst(long lnkAtFirst) {
		this.lnkAtFirst = lnkAtFirst;
	}

	public long getLnkEvFirst() {
		return lnkEvFirst;
	}

	private void setLnkEvFirst(long lnkEvFirst) {
		this.lnkEvFirst = lnkEvFirst;
	}

	public long getLnkMdComment() {
		return lnkMdComment;
	}

	private void setLnkMdComment(long lnkMdComment) {
		this.lnkMdComment = lnkMdComment;
	}

	public long getStartTimeNs() {
		return startTimeNs;
	}

	private void setStartTimeNs(long startTimeNs) {
		this.startTimeNs = startTimeNs;
	}

	public short getTzOffsetMin() {
		return tzOffsetMin;
	}

	private void setTzOffsetMin(short tzOffsetMin) {
		this.tzOffsetMin = tzOffsetMin;
	}

	public short getDstOffsetMin() {
		return dstOffsetMin;
	}

	private void setDstOffsetMin(short dstOffsetMin) {
		this.dstOffsetMin = dstOffsetMin;
	}

	public byte getTimeFlags() {
		return timeFlags;
	}

	private void setTimeFlags(byte timeFlags) {
		this.timeFlags = timeFlags;
	}

	public byte getTimeClass() {
		return timeClass;
	}

	private void setTimeClass(byte timeClass) {
		this.timeClass = timeClass;
	}

	public byte getFlags() {
		return flags;
	}

	private void setFlags(byte flags) {
		this.flags = flags;
	}

	public double getStartAngleRad() {
		return startAngleRad;
	}

	private void setStartAngleRad(double startAngleRad) {
		this.startAngleRad = startAngleRad;
	}

	public double getStartDistanceM() {
		return startDistanceM;
	}

	private void setStartDistanceM(double startDistanceM) {
		this.startDistanceM = startDistanceM;
	}

	public boolean isLocalTime() {
		return BigInteger.valueOf(timeFlags).testBit(0);
	}

	public boolean isTimeFlagsValid() {
		return BigInteger.valueOf(timeFlags).testBit(1);
	}

	public boolean isStartAngleValid() {
		return BigInteger.valueOf(flags).testBit(0);
	}

	public boolean isStartDistanceValid() {
		return BigInteger.valueOf(flags).testBit(1);
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

	public FHBLOCK getFhFirstBlock() throws IOException {
		if (lnkMdComment > 0) {
			return FHBLOCK.read(sbc, lnkFhFirst);
		}
		return null;
	}

	public CHBLOCK getChFirstBlock() throws IOException {
		if (lnkChFirst > 0) {
			return CHBLOCK.read(sbc, lnkChFirst);
		}
		return null;
	}

	public DGBLOCK getDgFirstBlock() throws IOException {
		if (lnkDgFirst > 0) {
			return DGBLOCK.read(sbc, lnkDgFirst);
		}
		return null;
	}

	public EVBLOCK getEvFirstBlock() throws IOException {
		if (lnkEvFirst > 0) {
			return EVBLOCK.read(sbc, lnkEvFirst);
		}
		return null;
	}

	@Override
	public String toString() {
		return new StringBuilder().append("HDBLOCK [lnkDgFirst=").append(lnkDgFirst).append(", lnkFhFirst=").append(lnkFhFirst).append(", lnkChFirst=").append(lnkChFirst).append(", lnkAtFirst=")
				.append(lnkAtFirst).append(", lnkEvFirst=").append(lnkEvFirst).append(", lnkMdComment=").append(lnkMdComment).append(", startTimeNs=").append(startTimeNs)
				.append(", tzOffsetMin=").append(tzOffsetMin).append(", dstOffsetMin=").append(dstOffsetMin).append(", timeFlags=").append(timeFlags).append(", timeClass=")
				.append(timeClass).append(", flags=").append(flags).append(", startAngleRad=").append(startAngleRad).append(", startDistanceM=").append(startDistanceM)
				.append("]").toString();
	}

	/**
	 * Reads a HDBLOCK from the channel starting at current channel position.
	 *
	 * @param sbc
	 *            The channel to read from.
	 * @return The block data.
	 * @throws IOException
	 *             The exception.
	 */
	public static HDBLOCK read(SeekableByteChannel sbc) throws IOException {
		HDBLOCK block = new HDBLOCK(sbc);

		// read block header
		ByteBuffer bb = ByteBuffer.allocate(112);
		bb.order(ByteOrder.LITTLE_ENDIAN);
		sbc.position(64);
		sbc.read(bb);
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

		// LINK: Pointer to the first data group block (DGBLOCK) (can be NIL)
		block.setLnkDgFirst(MDF4Util.readLink(bb));

		// LINK: Pointer to first file history block (FHBLOCK)
		block.setLnkFhFirst(MDF4Util.readLink(bb));

		// LINK: Pointer to first channel hierarchy block (CHBLOCK) (can be
		// NIL).
		block.setLnkChFirst(MDF4Util.readLink(bb));

		// LINK: Pointer to first attachment block (ATBLOCK) (can be NIL)
		block.setLnkAtFirst(MDF4Util.readLink(bb));

		// LINK: Pointer to first event block (EVBLOCK) (can be NIL)
		block.setLnkEvFirst(MDF4Util.readLink(bb));

		// LINK: Pointer to the measurement file comment (TXBLOCK or MDBLOCK)
		// (can be NIL)
		block.setLnkMdComment(MDF4Util.readLink(bb));

		// UINT64: Time stamp at start of measurement in nanoseconds elapsed
		// since 00:00:00 01.01.1970
		block.setStartTimeNs(MDF4Util.readUInt64(bb));

		// INT16: Time zone offset in minutes.
		block.setTzOffsetMin(MDF4Util.readInt16(bb));

		// INT16: Daylight saving time (DST) offset in minutes for start time
		// stamp.
		block.setDstOffsetMin(MDF4Util.readInt16(bb));

		// UINT8: Time flags
		block.setTimeFlags(MDF4Util.readUInt8(bb));

		// UINT8: Time quality class
		block.setTimeClass(MDF4Util.readUInt8(bb));

		// UINT8: Flags
		block.setFlags(MDF4Util.readUInt8(bb));
		if (block.getFlags() != 0) {
			throw new IOException("HDBLOCK hd_flags!=0 not yet supported");
		}

		// BYTE: Reserved
		bb.get();

		// REAL: Start angle in radians at start of measurement (only for angle
		// synchronous measurements)
		block.setStartAngleRad(MDF4Util.readReal(bb));

		// REAL: Start distance in meters at start of measurement
		block.setStartDistanceM(MDF4Util.readReal(bb));

		return block;
	}

}
