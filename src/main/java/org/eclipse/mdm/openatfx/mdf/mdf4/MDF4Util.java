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
import java.util.Date;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Properties;

import org.eclipse.mdm.openatfx.mdf.util.ODSHelper;
import org.eclipse.mdm.openatfx.mdf.util.ODSInsertStatement;

/**
 * Utility class having methods to read MDF file contents.
 *
 * @author Christian Rechner
 */
public abstract class MDF4Util {

	// For the strings in the IDBLOCK and for the block identifiers, always
	// single byte character (SBC) encoding is used
	// (standard ASCII extension ISO-8859-1 Latin character set).
	private static final String CHARSET_ISO8859 = "ISO-8859-1";

	// The string encoding used in an MDF file is UTF-8 (1-4 Bytes for each
	// character).
	// This applies to TXBLOCK and MDBLOCK data.
	private static final String CHARSET_UTF8 = "UTF-8";

	/**
	 * Read an 8-bit unsigned integer from the byte buffer.
	 *
	 * @param bb
	 *            The byte buffer.
	 * @return The value.
	 */
	public static byte readUInt8(ByteBuffer bb) {
		return bb.get();
	}

	/**
	 * Read an 16-bit unsigned integer from the byte buffer.
	 *
	 * @param bb
	 *            The byte buffer.
	 * @return The value.
	 */
	public static int readUInt16(ByteBuffer bb) {
		return bb.getShort() & 0xffff;
	}

	/**
	 * Read an 16-bit signed integer from the byte buffer.
	 *
	 * @param bb
	 *            The byte buffer.
	 * @return The value.
	 */
	public static short readInt16(ByteBuffer bb) {
		return bb.getShort();
	}

	/**
	 * Read an 32-bit unsigned integer from the byte buffer.
	 *
	 * @param bb
	 *            The byte buffer.
	 * @return The value.
	 */
	public static long readUInt32(ByteBuffer bb) {
		return bb.getInt() & 0xffffffffL;
	}

	/**
	 * Read an 32-bit signed integer from the byte buffer.
	 *
	 * @param bb
	 *            The byte buffer.
	 * @return The value.
	 */
	public static int readInt32(ByteBuffer bb) {
		return bb.getInt();
	}

	/**
	 * Read an 64-bit unsigned integer from the byte buffer.
	 *
	 * @param bb
	 *            The byte buffer.
	 * @return The value.
	 */
	public static long readUInt64(ByteBuffer bb) {
		byte[] data = new byte[8];
		bb.get(data);
		long l1 = ((long) data[0] & 0xff) << 0 | ((long) data[1] & 0xff) << 8 | ((long) data[2] & 0xff) << 16
				| ((long) data[3] & 0xff) << 24;
		long l2 = ((long) data[4] & 0xff) << 0 | ((long) data[5] & 0xff) << 8 | ((long) data[6] & 0xff) << 16
				| ((long) data[7] & 0xff) << 24;
		return l1 << 0 | l2 << 32;
	}

	/**
	 * Read an 64-bit signed integer from the byte buffer.
	 *
	 * @param bb
	 *            The byte buffer.
	 * @return The value.
	 */
	public static long readInt64(ByteBuffer bb) {
		return bb.getLong();
	}

	/**
	 * Read a floating-point value compliant with IEEE 754, double precision (64
	 * bits) (see [IEEE-FP]) from the byte buffer. An infinite value (e.g. for
	 * tabular ranges in conversion rule) can be expressed using the NaNs
	 * INFINITY resp. –INFINITY
	 *
	 * @param bb
	 *            The byte buffer.
	 * @return The value.
	 */
	public static double readReal(ByteBuffer bb) {
		return bb.getDouble();
	}

	/**
	 * Read a 64-bit signed integer from the byte buffer, used as byte position
	 * within the file. If a LINK is NIL (corresponds to 0), this means the LINK
	 * cannot be de-referenced. A link must be a multiple of 8.
	 *
	 * @param bb
	 *            The byte buffer.
	 * @return The value.
	 */
	public static long readLink(ByteBuffer bb) {
		byte[] data = new byte[8];
		bb.get(data);
		long l1 = ((long) data[0] & 0xff) << 0 | ((long) data[1] & 0xff) << 8 | ((long) data[2] & 0xff) << 16
				| ((long) data[3] & 0xff) << 24;
		long l2 = ((long) data[4] & 0xff) << 0 | ((long) data[5] & 0xff) << 8 | ((long) data[6] & 0xff) << 16
				| ((long) data[7] & 0xff) << 24;
		return l1 << 0 | l2 << 32;
	}

	public static String readCharsISO8859(ByteBuffer bb, int length) throws IOException {
		byte[] b = new byte[length];
		bb.get(b);

		// lookup null character for string termination
		int strLength = 0;
		for (byte element : b) {
			if (element == 0) {
				break;
			}
			strLength++;
		}

		return new String(b, 0, strLength, CHARSET_ISO8859);
	}

	public static String readCharsUTF8(ByteBuffer bb, int length) throws IOException {
		byte[] b = new byte[length];
		bb.get(b);

		// lookup null character for string termination
		int strLength = 0;
		for (byte element : b) {
			if (element == 0) {
				break;
			}
			strLength++;
		}

		return new String(b, 0, strLength, CHARSET_UTF8);
	}

	public static String readCharsUTF16(ByteBuffer bb, int length, boolean littleEndian) throws IOException {
		byte[] b = new byte[length];
		bb.get(b);

		// lookup null character for string termination
		int strLength = 0;
		while (b[strLength] != 0 && strLength != length) {
			strLength++;
		}
		if (littleEndian) {
			return new String(b, 0, strLength, "UTF-16LE");
		} else {
			return new String(b, 0, strLength, "UTF-16BE");
		}
	}

	/**
	 * Read a LE unsigned int value from the data.
	 * 
	 * @param bitOffset
	 *            The start bit in the first byte
	 * @param bitSize
	 *            The number of bits to read
	 * @param bb
	 *            The ByteBuffer. Reading starts a its current position.
	 * @return The Value (as Long)
	 */
	public static long readValue(int bitOffset, int bitSize, ByteBuffer bb) {
		if (bitSize % 8 != 0) {
			throw new IllegalArgumentException("Cannot read value with " + bitSize + "bits");
		}
		// first bit.
		byte curr = bb.get();
		// throw away first bits.
		curr = (byte) (curr << bitOffset);
		byte[] newdata = new byte[8];
		newdata[0] = curr; // read higher bits

		for (int i = 1; i < bitSize / 8; i++) { // read next bytes
			curr = bb.get();
			byte lower = (byte) ((curr & 0xFF) >> 8 - bitOffset);
			byte upper = (byte) (curr << bitOffset);
			newdata[i - 1] = (byte) (newdata[i - 1] | lower);
			newdata[i] = upper;
		}
		if (bitOffset != 0) {
			curr = bb.get();
			byte lower = (byte) (curr >> 8 - bitOffset);
			newdata[bitSize / 8 - 1] = (byte) (newdata[bitSize / 8 - 1] | lower);
		}
		return ByteBuffer.wrap(newdata).order(ByteOrder.LITTLE_ENDIAN).getLong();
	}

	public static void writeProperites(ODSInsertStatement ins, Properties properties) {
		Iterator<Entry<Object, Object>> iter = properties.entrySet().iterator();

		while (iter.hasNext()) {
			Entry<Object, Object> ent = iter.next();
			Object key = ent.getKey();
			Object value = ent.getValue();
			if (value instanceof Integer) {
				ins.setNameValueUnit(ODSHelper.createLongNVU(key.toString(), (Integer) value));
			} else if (value instanceof Double) {
				ins.setNameValueUnit(ODSHelper.createDoubleNVU(key.toString(), (Double) value));
			} else if (value instanceof Float) {
				ins.setNameValueUnit(ODSHelper.createFloatNVU(key.toString(), (Float) value));
			} else if (value instanceof Long) {
				ins.setNameValueUnit(ODSHelper.createLongLongNVU(key.toString(), (Long) value));
			} else if (value instanceof Boolean) {
				short s = (Boolean) value ? (short) 1 : (short) 0;
				ins.setNameValueUnit(ODSHelper.createShortNVU(key.toString(), s));
			} else if (value instanceof Short) {
				ins.setNameValueUnit(ODSHelper.createShortNVU(key.toString(), (Short) value));
			} else if (value instanceof Date) {
				Date date = (Date) value;
				ins.setNameValueUnit(ODSHelper.createDateNVU(key.toString(), ODSHelper.asODSDate(date)));
			} else {
				ins.setNameValueUnit(ODSHelper.createStringNVU(key.toString(), value.toString()));
			}
		}
	}
}
