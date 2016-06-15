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
 * THE ATTACHMENT BLOCK <code>ATBLOCK</code>
 * </p>
 * The data section of the ATBLOCK contains attachments (binary data)
 *
 * @author Christian Rechner, Tobias Leemann
 */
class ATBLOCK extends BLOCK {

	public static String BLOCK_ID = "##AT";

	long lnkAtNext;

	long lnkTxFilename;

	long lnkTxMIMEtype;

	long lnkMdComment;

	int flags;

	int creatorIndex;

	byte[] md5CheckSum;

	long origSize;

	long embeddedSize;


	/**
	 * Constructor.
	 *
	 * @param sbc The byte channel pointing to the MDF file.
	 * @param pos The position of the block within the MDF file.
	 */
	private ATBLOCK(SeekableByteChannel sbc, long pos) {
		super(sbc, pos);
	}



	public long getLnkAtNext() {
		return lnkAtNext;
	}

	public void setLnkAtNext(long lnkAtNext) {
		this.lnkAtNext = lnkAtNext;
	}

	public long getLnkTxFilename() {
		return lnkTxFilename;
	}

	public void setLnkTxFilename(long lnkTxFilename) {
		this.lnkTxFilename = lnkTxFilename;
	}

	public long getLnkTxMIMEtype() {
		return lnkTxMIMEtype;
	}

	public void setLnkTxMIMEtype(long lnkTxMIMEtype) {
		this.lnkTxMIMEtype = lnkTxMIMEtype;
	}

	public long getLnkMdComment() {
		return lnkMdComment;
	}

	public void setLnkMdComment(long lnkMdComment) {
		this.lnkMdComment = lnkMdComment;
	}

	public int getFlags() {
		return flags;
	}

	public void setFlags(int flags) {
		this.flags = flags;
	}

	public int getCreatorIndex() {
		return creatorIndex;
	}

	public void setCreatorIndex(int creatorIndex) {
		this.creatorIndex = creatorIndex;
	}

	public byte[] getMd5CheckSum() {
		return md5CheckSum;
	}

	public void setMd5CheckSum(byte[] md5CheckSum) {
		this.md5CheckSum = md5CheckSum;
	}

	public long getOrigSize() {
		return origSize;
	}

	public void setOrigSize(long origSize) {
		this.origSize = origSize;
	}

	public long getEmbeddedSize() {
		return embeddedSize;
	}

	public void setEmbeddedSize(long embeddedSize) {
		this.embeddedSize = embeddedSize;
	}

	public ATBLOCK getAtNextBlock() throws IOException {
		if (lnkAtNext > 0) {
			return ATBLOCK.read(sbc, lnkAtNext);
		}
		return null;
	}

	public TXBLOCK getTxFilennameBlock() throws IOException {
		if (lnkTxFilename > 0) {
			return TXBLOCK.read(sbc, lnkTxFilename);
		}
		return null;
	}

	public TXBLOCK getTxMIMETypeBlock() throws IOException {
		if (lnkTxMIMEtype > 0) {
			return TXBLOCK.read(sbc, lnkTxMIMEtype);
		}
		return null;
	}

	public MDBLOCK getMdCommentBlock() throws IOException {
		if (lnkMdComment > 0) {
			return MDBLOCK.read(sbc, lnkMdComment);
		}
		return null;
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
	 * @param channel The channel to read from.
	 * @param pos The position
	 * @return The block data.
	 * @throws IOException The exception.
	 */
	public static ATBLOCK read(SeekableByteChannel channel, long pos) throws IOException {
		ATBLOCK block = new ATBLOCK(channel, pos);

		// read block header
		ByteBuffer bb = ByteBuffer.allocate(24 +32 +40); //24 Head 32 Links 40 Data
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

		// LINK 1 Pointer to next ATBLOCK (NIL allowed)
		block.setLnkAtNext(MDF4Util.readLink(bb));

		// LINK 1 Pointer to Text Block with Filename (NIL allowed)
		block.setLnkTxFilename(MDF4Util.readLink(bb));

		// LINK 1 Pointer to Text Block with MIME Type (TXBLOCK) (NIL allowed)
		block.setLnkTxMIMEtype(MDF4Util.readLink(bb));

		// LINK 1 Pointer to comment Block MDBLOCK (NIL allowed)
		block.setLnkMdComment(MDF4Util.readLink(bb));

		// UINT16 flags
		block.setFlags(MDF4Util.readUInt16(bb));

		// UINT16 Creator Index
		block.setCreatorIndex(MDF4Util.readUInt16(bb));

		// BYTE 4: Reserved used for 8-Byte alignment
		bb.get(new byte[4]);

		// BYTE 16: MD5 Checksum
		byte[] md5sum = new byte[16];
		bb.get(md5sum);
		block.setMd5CheckSum(md5sum);

		// UINT64 OriginalSize
		block.setOrigSize(MDF4Util.readUInt64(bb));

		// UINT64 EmbeddedSize
		block.setEmbeddedSize(MDF4Util.readUInt64(bb));

		return block;
	}

}