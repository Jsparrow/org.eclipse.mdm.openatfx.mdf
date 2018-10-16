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
 * THE CHANNEL BLOCK <code>CNBLOCK</code>
 * </p>
 * The CNBLOCK describes a channel, i.e. it contains information about the
 * recorded signal and how its signal values are stored in the MDF file.
 *
 * @author Christian Rechner
 */
class CNBLOCK extends BLOCK {

	public static String BLOCK_ID = "##CN";

	/** Link section */

	// Pointer to next channel block (CNBLOCK) (can be NIL)
	// LINK
	private long lnkCnNext;

	// Composition of channels: Pointer to channel array block (CABLOCK) or
	// channel block (CNBLOCK) (can be NIL).
	// LINK
	private long lnkComposition;

	// Pointer to TXBLOCK with name (identification) of channel.
	// LINK
	private long lnkTxName;

	// Pointer to channel source (SIBLOCK) (can be NIL)
	// Must be NIL for component channels (members of a structure or array
	// elements) because they all must have the same
	// source and thus simply use the SIBLOCK of their parent CNBLOCK (direct
	// child of CGBLOCK).
	// LINK
	private long lnkSiSource;

	// Pointer to the conversion formula (CCBLOCK) (can be NIL, must be NIL for
	// complex channel data types, i.e. for
	// cn_data_type ≥ 10).
	// LINK
	private long lnkCcConversion;

	// Pointer to channel type specific signal data
	// LINK
	private long lnkData;

	// Pointer to TXBLOCK/MDBLOCK with designation for physical unit of signal
	// data (after conversion) or (only for
	// channel data types "MIME sample" and "MIME stream") to MIME context-type
	// text. (can be NIL).
	// LINK
	private long lnkMdUnit;

	// Pointer to TXBLOCK/MDBLOCK with comment and additional information about
	// the channel. (can be NIL)
	// LINK
	private long lnkMdComment;

	// List of attachments for this channel (references to ATBLOCKs in global
	// linked list of ATBLOCKs).
	// The length of the list is given by cn_attachment_count. It can be empty
	// (cn_attachment_count = 0), i.e. there are
	// no attachments for this channel.
	// LINK
	private long[] lnkAtReference;

	// Only present if "default X" flag (bit 12) is set.
	// Reference to channel to be preferably used as X axis.
	// The reference is a link triple with pointer to parent DGBLOCK, parent
	// CGBLOCK and CNBLOCK for the channel (none
	// of them must be NIL).
	// The referenced channel does not need to have the same raster nor
	// monotonously increasing values. It can be a
	// master channel, e.g. in case several master channels are present. In case
	// of different rasters, visualization may
	// depend on the interpolation method used by the tool.
	// In case no default X channel is specified, the tool is free to choose the
	// X axis; usually a master channels would
	// be used.
	// LINK
	private long[] lnkDefaultX;

	/** Data section */

	// Channel type
	// 0 = fixed length data channel channel value is contained in record.
	// 1 = variable length data channel also denoted as "variable length signal
	// data" (VLSD) channel
	// 2 = master channel for all signals of this group
	// 3 = virtual master channel
	// 4 = synchronization channel
	// 5 = maximum length data channel
	// 6 = virtual data channel
	// UINT8
	private byte channelType;

	// Sync type:
	// 0 = None (to be used for normal data channels)
	// 1 = Time (physical values must be seconds)
	// 2 = Angle (physical values must be radians)
	// 3 = Distance (physical values must be meters)
	// 4 = Index (physical values must be zero-based index values)
	// UINT8
	private byte syncType;

	// Channel data type of raw signal value
	// 0 = unsigned integer (LE Byte order)
	// 1 = unsigned integer (BE Byte order)
	// 2 = signed integer (two’s complement) (LE Byte order)
	// 3 = signed integer (two’s complement) (BE Byte order)
	// 4 = IEEE 754 floating-point format (LE Byte order)
	// 5 = IEEE 754 floating-point format (BE Byte order)
	// 6 = String (SBC, standard ASCII encoded (ISO-8859-1 Latin), NULL
	// terminated)
	// 7 = String (UTF-8 encoded, NULL terminated)
	// 8 = String (UTF-16 encoded LE Byte order, NULL terminated)
	// 9 = String (UTF-16 encoded BE Byte order, NULL terminated)
	// 10 = Byte Array with unknown content (e.g. structure)
	// 11 = MIME sample (sample is Byte Array with MIME content-type specified
	// in cn_md_unit)
	// 12 = MIME stream (all samples of channel represent a stream with MIME
	// content-type specified in cn_md_unit)
	// 13 = CANopen date (Based on 7 Byte CANopen Date data structure, see Table
	// 36)
	// 14 = CANopen time (Based on 6 Byte CANopen Time data structure, see Table
	// 37)
	// UINT8
	private byte dataType;

	// Bit offset (0-7): first bit (=LSB) of signal value after Byte offset has
	// been applied.
	// If zero, the signal value is 1-Byte aligned. A value different to zero is
	// only allowed for Integer data types
	// (cn_data_type ≤ 3) and if the Integer signal value fits into 8
	// contiguous Bytes (cn_bit_count + cn_bit_offset ≤
	// 64). For all other cases, cn_bit_offset must be zero.
	// UINT8
	private byte bitOffset;

	// Offset to first Byte in the data record that contains bits of the signal
	// value. The offset is applied to the
	// plain record data, i.e. skipping the record ID.
	// UINT32
	private long byteOffset;

	// Number of bits for signal value in record.
	// UINT32
	private long bitCount;

	// The value contains the following bit flags (Bit 0 = LSB):
	// Bit 0: All values invalid flag
	// Bit 1: Invalidation bit valid flag
	// Bit 2: Precision valid flag
	// Bit 3: Value range valid flag
	// Bit 4: Limit range valid flag
	// Bit 5: Extended limit range valid flag
	// Bit 6: Discrete value flag
	// Bit 7: Calibration flag
	// Bit 8: Calculated flag
	// Bit 10: Bus event flag
	// Bit 11: Monotonous flag
	// Bit 12: Default X axis flag
	// UINT32
	private long flags;

	// Position of invalidation bit.
	// The invalidation bit can be used to specify if the signal value in the
	// current record is valid or not.
	// Note: the invalidation bit is optional and can only be used if the
	// "invalidation bit valid" flag (bit 1) is set.
	// UINT32
	private long invalBitPos;

	// Precision for display of floating point values.
	// 0xFF means unrestricted precision (infinite).
	// Any other value specifies the number of decimal places to use for display
	// of floating point values.
	// Only valid if "precision valid" flag (bit 2) is set
	// UINT8
	private byte precision;

	// Length N of cn_at_reference list, i.e. number of attachments for this
	// channel. Can be zero.
	// UINT16
	private int attachmentCount;

	// Minimum signal value that occurred for this signal (raw value)
	// Only valid if "value range valid" flag (bit 3) is set.
	// REAL
	private double valRangeMin;

	// Maximum signal value that occurred for this signal (raw value)
	// Only valid if "value range valid" flag (bit 3) is set.
	// REAL
	private double valRangeMax;

	// Lower limit for this signal (physical value for numeric conversion rule,
	// otherwise raw value)
	// Only valid if "limit range valid" flag (bit 4) is set.
	// REAL
	private double limitMin;

	// Upper limit for this signal (physical value for numeric conversion rule,
	// otherwise raw value)
	// Only valid if "limit range valid" flag (bit 4) is set.
	// REAL
	private double limitMax;

	// Lower extended limit for this signal (physical value for numeric
	// conversion rule, otherwise raw value)
	// Only valid if "extended limit range valid" flag (bit 5) is set.
	// If cn_limit_min is valid, cn_limit_min must be larger or equal to
	// cn_limit_ext_min.
	// REAL
	private double limitExtMin;

	// Upper extended limit for this signal (physical value for numeric
	// conversion rule, otherwise raw value)
	// Only valid if "extended limit range valid" flag (bit 5) is set.
	// If cn_limit_max is valid, cn_limit_max must be less or equal to
	// cn_limit_ext_max.
	// REAL
	private double limitExtMax;

	/**
	 * Constructor.
	 *
	 * @param sbc
	 *            The byte channel pointing to the MDF file.
	 * @param pos
	 *            The position of the block within the MDF file.
	 */
	private CNBLOCK(SeekableByteChannel sbc, long pos) {
		super(sbc, pos);
	}

	public long getLnkCnNext() {
		return lnkCnNext;
	}

	public long getLnkComposition() {
		return lnkComposition;
	}

	public long getLnkTxName() {
		return lnkTxName;
	}

	public long getLnkSiSource() {
		return lnkSiSource;
	}

	public long getLnkCcConversion() {
		return lnkCcConversion;
	}

	public long getLnkData() {
		return lnkData;
	}

	public long getLnkMdUnit() {
		return lnkMdUnit;
	}

	public long getLnkMdComment() {
		return lnkMdComment;
	}

	public long[] getLnkAtReference() {
		return lnkAtReference;
	}

	public long[] getLnkDefaultX() {
		return lnkDefaultX;
	}

	public byte getChannelType() {
		return channelType;
	}

	public byte getSyncType() {
		return syncType;
	}

	public byte getDataType() {
		return dataType;
	}

	public byte getBitOffset() {
		return bitOffset;
	}

	public long getByteOffset() {
		return byteOffset;
	}

	public long getBitCount() {
		return bitCount;
	}

	public long getFlags() {
		return flags;
	}

	public boolean isValueRangeValid() { // 3rd bit of the flags
		return (flags & (byte) 0x8) != 0;
	}

	public long getInvalBitPos() {
		return invalBitPos;
	}

	public byte getPrecision() {
		return precision;
	}

	public int getAttachmentCount() {
		return attachmentCount;
	}

	public double getValRangeMin() {
		return valRangeMin;
	}

	public double getValRangeMax() {
		return valRangeMax;
	}

	public double getLimitMin() {
		return limitMin;
	}

	public double getLimitMax() {
		return limitMax;
	}

	public double getLimitExtMin() {
		return limitExtMin;
	}

	public double getLimitExtMax() {
		return limitExtMax;
	}

	private void setLnkCnNext(long lnkCnNext) {
		this.lnkCnNext = lnkCnNext;
	}

	private void setLnkComposition(long lnkComposition) {
		this.lnkComposition = lnkComposition;
	}

	private void setLnkTxName(long lnkTxName) {
		this.lnkTxName = lnkTxName;
	}

	private void setLnkSiSource(long lnkSiSource) {
		this.lnkSiSource = lnkSiSource;
	}

	private void setLnkCcConversion(long lnkCcConversion) {
		this.lnkCcConversion = lnkCcConversion;
	}

	private void setLnkData(long lnkData) {
		this.lnkData = lnkData;
	}

	private void setLnkMdUnit(long lnkMdUnit) {
		this.lnkMdUnit = lnkMdUnit;
	}

	private void setLnkMdComment(long lnkMdComment) {
		this.lnkMdComment = lnkMdComment;
	}

	private void setLnkAtReference(long[] lnkAtReference) {
		this.lnkAtReference = lnkAtReference;
	}

	private void setLnkDefaultX(long[] lnkDefaultX) {
		this.lnkDefaultX = lnkDefaultX;
	}

	private void setChannelType(byte channelType) {
		this.channelType = channelType;
	}

	private void setSyncType(byte syncType) {
		this.syncType = syncType;
	}

	private void setDataType(byte dataType) {
		this.dataType = dataType;
	}

	private void setBitOffset(byte bitOffset) {
		this.bitOffset = bitOffset;
	}

	private void setByteOffset(long byteOffset) {
		this.byteOffset = byteOffset;
	}

	private void setBitCount(long bitCount) {
		this.bitCount = bitCount;
	}

	private void setFlags(long flags) {
		this.flags = flags;
	}

	private void setInvalBitPos(long invalBitPos) {
		this.invalBitPos = invalBitPos;
	}

	private void setPrecision(byte precision) {
		this.precision = precision;
	}

	private void setAttachmentCount(int attachmentCount) {
		this.attachmentCount = attachmentCount;
	}

	private void setValRangeMin(double valRangeMin) {
		this.valRangeMin = valRangeMin;
	}

	private void setValRangeMax(double valRangeMax) {
		this.valRangeMax = valRangeMax;
	}

	private void setLimitMin(double limitMin) {
		this.limitMin = limitMin;
	}

	private void setLimitMax(double limitMax) {
		this.limitMax = limitMax;
	}

	private void setLimitExtMin(double limitExtMin) {
		this.limitExtMin = limitExtMin;
	}

	private void setLimitExtMax(double limitExtMax) {
		this.limitExtMax = limitExtMax;
	}

	public CNBLOCK getCnNextBlock() throws IOException {
		if (lnkCnNext > 0) {
			return CNBLOCK.read(sbc, lnkCnNext);
		}
		return null;
	}

	public TXBLOCK getCnTxNameBlock() throws IOException {
		if (lnkTxName > 0) {
			return TXBLOCK.read(sbc, lnkTxName);
		}
		return null;
	}

	public SIBLOCK getSiSourceBlock() throws IOException {
		if (lnkSiSource > 0) {
			return SIBLOCK.read(sbc, lnkSiSource);
		}
		return null;
	}

	public CCBLOCK getCcConversionBlock() throws IOException {
		if (lnkCcConversion > 0) {
			return CCBLOCK.read(sbc, lnkCcConversion);
		}
		return null;
	}

	public BLOCK getDataBlock() throws IOException {
		if (lnkData > 0) {
			String blockType = getBlockType(sbc, lnkData);
			// link points to a ATBLOCK
			if (blockType.equals(ATBLOCK.BLOCK_ID)) {
				return ATBLOCK.read(sbc, lnkData);
			}
			// links points to SDBLOCK
			else if (blockType.equals(SDBLOCK.BLOCK_ID)) {
				return SDBLOCK.read(sbc, lnkData);
			}
			// unknown
			else {
				throw new IOException("Unsupported block type for Channel Data: " + blockType);
			}
		}
		return null;
	}

	public BLOCK getMdUnitBlock() throws IOException {
		if (lnkMdUnit > 0) {
			String blockType = getBlockType(sbc, lnkMdUnit);
			// link points to a MDBLOCK
			if (blockType.equals(MDBLOCK.BLOCK_ID)) {
				return MDBLOCK.read(sbc, lnkMdUnit);
			}
			// links points to TXBLOCK
			else if (blockType.equals(TXBLOCK.BLOCK_ID)) {
				return TXBLOCK.read(sbc, lnkMdUnit);
			}
			// unknown
			else {
				throw new IOException("Unsupported block type for MdUnit: " + blockType);
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

	@Override
	public String toString() {
		return "CNBLOCK [lnkCnNext=" + lnkCnNext + ", lnkComposition=" + lnkComposition + ", lnkTxName=" + lnkTxName
				+ ", lnkSiSource=" + lnkSiSource + ", lnkCcConversion=" + lnkCcConversion + ", lnkData=" + lnkData
				+ ", lnkMdUnit=" + lnkMdUnit + ", lnkMdComment=" + lnkMdComment + ", lnkAtReference="
				+ Arrays.toString(lnkAtReference) + ", lnkDefaultX=" + Arrays.toString(lnkDefaultX) + ", channelType="
				+ channelType + ", syncType=" + syncType + ", dataType=" + dataType + ", bitOffset=" + bitOffset
				+ ", byteOffset=" + byteOffset + ", bitCount=" + bitCount + ", flags=" + flags + ", invalBitPos="
				+ invalBitPos + ", precision=" + precision + ", attachmentCount=" + attachmentCount + ", valRangeMin="
				+ valRangeMin + ", valRangeMax=" + valRangeMax + ", limitMin=" + limitMin + ", limitMax=" + limitMax
				+ ", limitExtMin=" + limitExtMin + ", limitExtMax=" + limitExtMax + "]";
	}

	/**
	 * Reads a CNBLOCK from the channel starting at current channel position.
	 *
	 * @param channel
	 *            The channel to read from.
	 * @param pos
	 *            The position within the channel.
	 * @return The block data.
	 * @throws IOException
	 *             The exception.
	 */
	public static CNBLOCK read(SeekableByteChannel channel, long pos) throws IOException {
		CNBLOCK block = new CNBLOCK(channel, pos);

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

		// read block data
		bb = ByteBuffer.allocate((int) block.getLength() - 24);
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

		// UINT8: Channel type
		block.setChannelType(MDF4Util.readUInt8(bb));

		// UINT8: Sync type
		block.setSyncType(MDF4Util.readUInt8(bb));

		// UINT8: Channel data type of raw signal value
		block.setDataType(MDF4Util.readUInt8(bb));

		// UINT8: Bit offset (0-7)
		block.setBitOffset(MDF4Util.readUInt8(bb));

		// UINT32: Offset to first Byte in the data record that contains bits of
		// the signal value.
		block.setByteOffset(MDF4Util.readUInt32(bb));

		// UINT32: Number of bits for signal value in record.
		block.setBitCount(MDF4Util.readUInt32(bb));

		// UINT32: Flags
		block.setFlags(MDF4Util.readUInt32(bb));

		// UINT32: Position of invalidation bit.
		block.setInvalBitPos(MDF4Util.readUInt32(bb));

		// UINT8: Precision for display of floating point values.
		block.setPrecision(MDF4Util.readUInt8(bb));

		// BYTE: Reserved
		bb.get();

		// UINT16: Length N of cn_at_reference list, i.e. number of attachments
		// for this channel. Can be zero.
		block.setAttachmentCount(MDF4Util.readUInt16(bb));

		// REAL: Minimum signal value that occurred for this signal (raw value)
		block.setValRangeMin(MDF4Util.readReal(bb));

		// REAL: Maximum signal value that occurred for this signal (raw value)
		block.setValRangeMax(MDF4Util.readReal(bb));

		// REAL: Lower limit for this signal (physical value for numeric
		// conversion rule, otherwise raw value)
		block.setLimitMin(MDF4Util.readReal(bb));

		// REAL: Upper limit for this signal (physical value for numeric
		// conversion rule, otherwise raw value)
		block.setLimitMax(MDF4Util.readReal(bb));

		// REAL: Lower extended limit for this signal (physical value for
		// numeric conversion rule, otherwise raw value)
		block.setLimitExtMin(MDF4Util.readReal(bb));

		// REAL: Upper extended limit for this signal (physical value for
		// numeric conversion rule, otherwise raw value)
		block.setLimitExtMax(MDF4Util.readReal(bb));

		// extract links after reading data (then we know how many attachments)
		block.setLnkCnNext(lnks[0]);
		block.setLnkComposition(lnks[1]);
		block.setLnkTxName(lnks[2]);
		block.setLnkSiSource(lnks[3]);
		block.setLnkCcConversion(lnks[4]);
		block.setLnkData(lnks[5]);
		block.setLnkMdUnit(lnks[6]);
		block.setLnkMdComment(lnks[7]);
		long[] lnkAtRef = new long[block.getAttachmentCount()];
		for (int i = 0; i < block.getAttachmentCount(); i++) {
			lnkAtRef[i] = lnks[i + 8];
		}
		block.setLnkAtReference(lnkAtRef);
		long[] lnkDefX = new long[3];
		if (lnks.length > block.getAttachmentCount() + 8) {
			lnkDefX[0] = lnks[block.getAttachmentCount() + 8];
			lnkDefX[1] = lnks[block.getAttachmentCount() + 9];
			lnkDefX[2] = lnks[block.getAttachmentCount() + 10];
		}
		block.setLnkDefaultX(lnkDefX);

		return block;
	}

}
