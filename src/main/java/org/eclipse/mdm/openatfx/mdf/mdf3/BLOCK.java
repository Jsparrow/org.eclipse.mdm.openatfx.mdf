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

import java.nio.channels.SeekableByteChannel;

/**
 * Base class for all blocks.
 *
 * @author Christian Rechner
 */
abstract class BLOCK {

	// the position of the block within the MDF file
	private final long pos;

	// CHAR 2 Block type identifier, always "CC"
	private String id;

	// UINT16 1 Block size of this block in bytes
	private int length;

	protected final SeekableByteChannel sbc;

	/**
	 * Constructor.
	 *
	 * @param sbc
	 *            The byte channel pointing to the MDF file.
	 * @param pos
	 *            The position of the block within the MDF file.
	 */
	protected BLOCK(SeekableByteChannel sbc, long pos) {
		this.sbc = sbc;
		this.pos = pos;
	}

	public long getPos() {
		return pos;
	}

	public String getId() {
		return id;
	}

	protected void setId(String id) {
		this.id = id;
	}

	public int getLength() {
		return length;
	}

	protected void setLength(int length) {
		this.length = length;
	}

	/**
	 * {@inheritDoc}
	 *
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return new StringBuilder().append("BLOCK [pos=").append(pos).append(", id=").append(id).append(", length=").append(length).append("]")
				.toString();
	}

	/**
	 * {@inheritDoc}
	 *
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (int) (pos ^ pos >>> 32);
		return result;
	}

	/**
	 * {@inheritDoc}
	 *
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		BLOCK other = (BLOCK) obj;
		if (pos != other.pos) {
			return false;
		}
		return true;
	}

}