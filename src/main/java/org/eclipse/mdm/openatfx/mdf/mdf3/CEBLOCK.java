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


package org.eclipse.mdm.openatfx.mdf.mdf3;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.SeekableByteChannel;

/**
 * <p>
 * The Channel Extension Block CEBLOCK
 * </p>
 * The CEBLOCK serves to specify source specific properties of a channel, e.g.
 * name of the ECU or name and sender of the CAN message
 *
 * @author Christian Rechner
 */
class CEBLOCK extends BLOCK {

	public static String BLOCK_ID = "CE";

	// UINT16 1 Extension type identifier
	// 2 = DIM (DIM block supplement)
	// 19 = Vector CAN (Vector CAN block supplement)
	private int extensionTypeIdent;

	private CEBLOCK_DIM ceBlockDim;
	private CEBLOCK_VectorCAN ceBlockVectorCAN;

	/**
	 * Constructor.
	 *
	 * @param sbc
	 *            The byte channel pointing to the MDF file.
	 * @param pos
	 *            The position of the block within the MDF file.
	 */
	private CEBLOCK(SeekableByteChannel sbc, long pos) {
		super(sbc, pos);
	}

	public int getExtensionTypeIdent() {
		return extensionTypeIdent;
	}

	private void setExtensionTypeIdent(int extensionTypeIdent) {
		this.extensionTypeIdent = extensionTypeIdent;
	}

	public CEBLOCK_DIM getCeBlockDim() {
		return ceBlockDim;
	}

	private void setCeBlockDim(CEBLOCK_DIM ceBlockDim) {
		this.ceBlockDim = ceBlockDim;
	}

	public CEBLOCK_VectorCAN getCeBlockVectorCAN() {
		return ceBlockVectorCAN;
	}

	private void setCeBlockVectorCAN(CEBLOCK_VectorCAN ceBlockVectorCAN) {
		this.ceBlockVectorCAN = ceBlockVectorCAN;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String toString() {
		return new StringBuilder().append("CEBLOCK [extensionTypeIdent=").append(extensionTypeIdent).append(", ceBlockDim=").append(ceBlockDim).append(", ceBlockVectorCAN=").append(ceBlockVectorCAN).append("]")
				.toString();
	}

	/**
	 * Reads a CEBLOCK from the channel starting at pos
	 *
	 * @param sbc
	 *            The channel to read from.
	 * @param pos
	 *            The position to start reading.
	 * @return The block data.
	 * @throws IOException
	 *             The exception.
	 */
	public static CEBLOCK read(SeekableByteChannel sbc, long pos) throws IOException {
		CEBLOCK block = new CEBLOCK(sbc, pos);

		// read block
		ByteBuffer bb = ByteBuffer.allocate(6);
		bb.order(ByteOrder.LITTLE_ENDIAN);
		sbc.position(pos);
		sbc.read(bb);
		bb.rewind();

		// CHAR 2 Block type identifier
		block.setId(Mdf3Util.readChars(bb, 2));
		if (!block.getId().equals(BLOCK_ID)) {
			throw new IOException(new StringBuilder().append("Wrong block type - expected '").append(BLOCK_ID).append("', found '").append(block.getId()).append("'").toString());
		}

		// UINT16 1 Block size of this block in bytes
		block.setLength(Mdf3Util.readUInt16(bb));

		// UINT16 1 Extension type identifier
		block.setExtensionTypeIdent(Mdf3Util.readInt16(bb));

		// 2 = DIM (DIM block supplement)
		if (block.getExtensionTypeIdent() == 2) {
			block.setCeBlockDim(CEBLOCK_DIM.read(sbc, pos + 6));
		}
		// 19 = Vector CAN (Vector CAN block supplement)
		else if (block.getExtensionTypeIdent() == 19) {
			block.setCeBlockVectorCAN(CEBLOCK_VectorCAN.read(sbc, pos + 6));
		}

		return block;
	}

}
