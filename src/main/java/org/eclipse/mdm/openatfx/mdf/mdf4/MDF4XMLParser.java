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
import java.io.StringReader;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedList;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.asam.ods.AoException;
import org.eclipse.mdm.openatfx.mdf.util.ODSHelper;
import org.eclipse.mdm.openatfx.mdf.util.ODSInsertStatement;

/**
 * Helper class for performant parsing of the XML content of an MDF4 file.
 *
 * @author Christian Rechner
 */
public class MDF4XMLParser {

	private static final Log LOG = LogFactory.getLog(MDF4XMLParser.class);

	private final XMLInputFactory xmlInputFactory;
	private final DateFormat xmlDateTimeFormat;

	/**
	 * Constructor.
	 */
	public MDF4XMLParser() {
		xmlInputFactory = XMLInputFactory.newInstance();
		xmlDateTimeFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss"); // e.g.
																			// 2012-11-07T10:16:03
	}

	/**
	 * Writes the content of the meta data block of a header block to the
	 * instance element attributes
	 *
	 * @param ins
	 *            The ODSInsertStatement to use.
	 * @param mdCommentXML
	 *            The XML string to parse.
	 * @throws IOException
	 * @throws AoException
	 */
	public void writeHDCommentToMea(ODSInsertStatement ins, String mdCommentXML) throws IOException, AoException {
		XMLStreamReader reader = null;
		try {
			reader = xmlInputFactory.createXMLStreamReader(new StringReader(mdCommentXML));
			while (reader.hasNext()) {
				reader.next();
				// TX
				if (reader.isStartElement() && "TX".equals(reader.getLocalName())) {
					ins.setStringVal("desc", reader.getElementText());
				}
				// time_source
				else if (reader.isStartElement() && "time_source".equals(reader.getLocalName())) {
					ins.setStringVal("time_source", reader.getElementText());
				}
				// constants
				else if (reader.isStartElement() && "constants".equals(reader.getLocalName())) {
					LOG.warn("Constants tag may not be fully supported");
					LinkedList<String> vars = new LinkedList<>();
					while (!(reader.isEndElement() && "constants".equals(reader.getLocalName()))) {
						if (reader.isStartElement() && "const".equals(reader.getLocalName())) {
							String name = reader.getAttributeValue(null, "name");
							String value = reader.getElementText();
							vars.add(new StringBuilder().append(name).append("=").append(value).toString());
						}
						reader.next();
					}
					vars.toArray(new String[0]);
				}
				// UNITSPEC
				else if (reader.isStartElement() && "UNITSPEC".equals(reader.getLocalName())) {

					LOG.warn("UNITSPEC in XML content 'HDcomment' is not yet supported!");
					throw new RuntimeException();
				}
				// common_properties
				else if (reader.isStartElement() && "common_properties".equals(reader.getLocalName())) {
					writeCommonProperties(ins, reader);
				}
			}
		} catch (XMLStreamException e) {
			LOG.error(e.getMessage(), e);
			throw new IOException(e.getMessage(), e);
		} finally {
			if (reader != null) {
				try {
					reader.close();
				} catch (XMLStreamException e) {
					LOG.error(e.getMessage(), e);
					throw new IOException(e.getMessage(), e);
				}
			}
		}
	}

	/**
	 * Extracts the TX-Section of an XML-Text
	 *
	 * @param mdCommentXML
	 *            The XML string to parse.
	 * @throws IOException
	 * @throws AoException
	 */
	public String extractCommentText(String mdCommentXML) throws IOException, AoException {
		XMLStreamReader reader = null;
		try {
			reader = xmlInputFactory.createXMLStreamReader(new StringReader(mdCommentXML));
			while (reader.hasNext()) {
				reader.next();
				// TX
				if (reader.isStartElement() && "TX".equals(reader.getLocalName())) {
					return reader.getElementText();
				}
			}
		} catch (XMLStreamException e) {
			LOG.error(e.getMessage(), e);
			throw new IOException(e.getMessage(), e);
		} finally {
			if (reader != null) {
				try {
					reader.close();
				} catch (XMLStreamException e) {
					LOG.error(e.getMessage(), e);
					throw new IOException(e.getMessage(), e);
				}
			}
		}
		return "";
	}

	/**
	 * Writes the content of the meta data block of a data group block to the
	 * instance element attributes
	 *
	 * @param ins
	 *            The ODSInsertStatement to use.
	 * @param mdCommentXML
	 *            The XML string to parse.
	 * @throws IOException
	 * @throws AoException
	 */
	public void writeDGCommentToCg(ODSInsertStatement ins, String mdCommentXML) throws IOException, AoException {
		XMLStreamReader reader = null;
		try {
			reader = xmlInputFactory.createXMLStreamReader(new StringReader(mdCommentXML));
			while (reader.hasNext()) {
				reader.next();
				// TX
				if (reader.isStartElement() && "TX".equals(reader.getLocalName())) {
					ins.setStringVal("desc_dg", reader.getElementText());
				}
				// common_properties
				else if (reader.isStartElement() && "common_properties".equals(reader.getLocalName())) {
					writeCommonProperties(ins, reader, "_dg");
				}
			}
		} catch (XMLStreamException e) {
			LOG.error(e.getMessage(), e);
			throw new IOException(e.getMessage(), e);
		} finally {
			if (reader != null) {
				try {
					reader.close();
				} catch (XMLStreamException e) {
					LOG.error(e.getMessage(), e);
					throw new IOException(e.getMessage(), e);
				}
			}
		}
	}

	/**
	 * Writes the content of the meta data block of a file history block to the
	 * instance element attributes
	 *
	 * @param ins
	 *            The InsertStatement in use
	 * @param mdCommentXML
	 *            The XML string to parse.
	 * @throws IOException
	 * @throws AoException
	 */
	public void writeFHCommentToFh(ODSInsertStatement ins, String mdCommentXML) throws IOException, AoException {
		XMLStreamReader reader = null;
		try {
			reader = xmlInputFactory.createXMLStreamReader(new StringReader(mdCommentXML));
			while (reader.hasNext()) {
				reader.next();
				// TX
				if (reader.isStartElement() && "TX".equals(reader.getLocalName())) {
					ins.setNameValueUnit(ODSHelper.createStringNVU("desc", reader.getElementText()));
				}
				// tool_id
				else if (reader.isStartElement() && "tool_id".equals(reader.getLocalName())) {
					ins.setNameValueUnit(ODSHelper.createStringNVU("tool_id", reader.getElementText()));
				}
				// tool_vendor
				else if (reader.isStartElement() && "tool_vendor".equals(reader.getLocalName())) {
					ins.setNameValueUnit(ODSHelper.createStringNVU("tool_vendor", reader.getElementText()));
				}
				// tool_version
				else if (reader.isStartElement() && "tool_version".equals(reader.getLocalName())) {
					ins.setNameValueUnit(ODSHelper.createStringNVU("tool_version", reader.getElementText()));
				}
				// user_name
				else if (reader.isStartElement() && "user_name".equals(reader.getLocalName())) {
					ins.setNameValueUnit(ODSHelper.createStringNVU("user_name", reader.getElementText()));
				}
				// common_properties
				else if (reader.isStartElement() && "common_properties".equals(reader.getLocalName())) {
					writeCommonProperties(ins, reader);
				}
			}
		} catch (XMLStreamException e) {
			LOG.error(e.getMessage(), e);
			throw new IOException(e.getMessage(), e);
		} finally {
			if (reader != null) {
				try {
					reader.close();
				} catch (XMLStreamException e) {
					LOG.error(e.getMessage(), e);
					throw new IOException(e.getMessage(), e);
				}
			}
		}
	}

	/**
	 * Writes the content of the meta data block of a channel block to the
	 * instance element attributes
	 *
	 * @param ins
	 *            The InsertStatement in use
	 * @param mdCommentXML
	 *            The XML string to parse.
	 * @throws IOException
	 * @throws AoException
	 */
	public void writeCNCommentToMeq(ODSInsertStatement ins, String mdCommentXML) throws IOException, AoException {
		XMLStreamReader reader = null;
		try {
			reader = xmlInputFactory.createXMLStreamReader(new StringReader(mdCommentXML));
			while (reader.hasNext()) {
				reader.next();
				// TX
				if (reader.isStartElement() && "TX".equals(reader.getLocalName())) {
					ins.setStringVal("desc", reader.getElementText());
				}
				// linker_name
				else if (reader.isStartElement() && "linker_name".equals(reader.getLocalName())) {
					String bc = reader.getAttributeValue(null, "offset");
					String value = reader.getElementText();
					ins.setStringVal("linker_name", value);
					if (bc != null) {
						ins.setLongVal("linker_name_offset", Integer.parseInt(bc));
					}
				}
				// linker_address
				else if (reader.isStartElement() && "linker_address".equals(reader.getLocalName())) {
					String bc = reader.getAttributeValue(null, "byte_count");
					String bitmask = reader.getAttributeValue(null, "bit_mask");
					String bo = reader.getAttributeValue(null, "byte_order");
					String value = reader.getElementText();
					ins.setLongLongVal("linker_address", parseHex(value));
					if (bc != null) {
						ins.setLongVal("linker_address_byte_count", Integer.parseInt(bc));
					}
					if (bitmask != null) {
						ins.setStringVal("linker_address_bit_mask", bitmask);
					}
					if (bo != null) {
						ins.setStringVal("linker_address_byte_order", bo);
					}
				}
				// address
				else if (reader.isStartElement() && "address".equals(reader.getLocalName())) {
					String bc = reader.getAttributeValue(null, "byte_count");
					String bitmask = reader.getAttributeValue(null, "bit_mask");
					String bo = reader.getAttributeValue(null, "byte_order");
					String value = reader.getElementText();
					ins.setLongLongVal("address", parseHex(value));
					if (bc != null) {
						ins.setLongVal("address_byte_count", Integer.valueOf(bc));
					}
					if (bitmask != null) {
						ins.setStringVal("address_bit_mask", bitmask);
					}
					if (bo != null) {
						ins.setStringVal("address_byte_order", bo);
					}
				}
				// axis_monotony
				else if (reader.isStartElement() && "axis_monotony".equals(reader.getLocalName())) {
					String value = reader.getElementText();
					ins.setStringVal("axis_monotony", value);
					// use enum?
				}
				// raster
				else if (reader.isStartElement() && "raster".equals(reader.getLocalName())) {

					String min = reader.getAttributeValue(null, "min");
					String max = reader.getAttributeValue(null, "max");
					String avg = reader.getAttributeValue(null, "average");
					String value = reader.getElementText();
					if (value != null) {
						ins.setDoubleVal("raster", Double.valueOf(value));
					}
					if (min != null) {
						ins.setDoubleVal("raster_min", Double.valueOf(min));
					}
					if (max != null) {
						ins.setDoubleVal("raster_max", Double.valueOf(max));
					}
					if (avg != null) {
						ins.setDoubleVal("raster_average", Double.valueOf(avg));
					}
				}
				// names
				else if (reader.isStartElement() && "names".equals(reader.getLocalName())) {
					writeNames(ins, reader, "");
				}
				// formula
				else if (reader.isStartElement() && "formula".equals(reader.getLocalName())) {
					writeFormula(ins, reader);
				}
				// common_properties
				else if (reader.isStartElement() && "common_properties".equals(reader.getLocalName())) {
					writeCommonProperties(ins, reader);
				}

			}
		} catch (XMLStreamException e) {
			LOG.error(e.getMessage(), e);
			throw new IOException(e.getMessage(), e);
		} finally {
			if (reader != null) {
				try {
					reader.close();
				} catch (XMLStreamException e) {
					LOG.error(e.getMessage(), e);
					throw new IOException(e.getMessage(), e);
				}
			}
		}
	}

	/**
	 * Writes the content of the unit comment block of a channel block to the
	 * instance element attributes
	 *
	 * @param ins
	 *            The InsertStatement in use
	 * @param mdCommentXML
	 *            The XML string to parse.
	 * @throws IOException
	 * @throws AoException
	 */
	public void writeCNCommentToUnit(ODSInsertStatement ins, String mdCommentXML) throws IOException, AoException {
		XMLStreamReader reader = null;
		try {
			reader = xmlInputFactory.createXMLStreamReader(new StringReader(mdCommentXML));
			while (reader.hasNext()) {
				reader.next();
				if (reader.isStartElement() && "ho_unit".equals(reader.getLocalName())) {
					String ur = reader.getAttributeValue(null, "unit_ref");
					if (ur != null) {
						ins.setStringVal("ho_unit_ref", ur);
					}
				}
				// common_properties
				else if (reader.isStartElement() && "common_properties".equals(reader.getLocalName())) {
					writeCommonProperties(ins, reader);
				}
			}
			throw new RuntimeException("");
		} catch (XMLStreamException e) {
			LOG.error(e.getMessage(), e);
			throw new IOException(e.getMessage(), e);
		} finally {
			if (reader != null) {
				try {
					reader.close();
				} catch (XMLStreamException e) {
					LOG.error(e.getMessage(), e);
					throw new IOException(e.getMessage(), e);
				}
			}
		}

	}

	/**
	 * Writes the content of the meta data block of a channel group block to the
	 * instance element attributes
	 *
	 * @param ins
	 *            The ODSInsertStatement to use.
	 * @param mdCommentXML
	 *            The XML string to parse.
	 * @throws IOException
	 * @throws AoException
	 */
	public void writeCGCommentToCg(ODSInsertStatement ins, String mdCommentXML) throws IOException, AoException {
		XMLStreamReader reader = null;
		try {
			reader = xmlInputFactory.createXMLStreamReader(new StringReader(mdCommentXML));
			while (reader.hasNext()) {
				reader.next();
				// TX
				if (reader.isStartElement() && "TX".equals(reader.getLocalName())) {
					ins.setStringVal("desc", reader.getElementText());
				}
				// names
				else if (reader.isStartElement() && "names".equals(reader.getLocalName())) {
					LOG.warn("'names' in XML content 'CGcomment' is not yet supported!");
				}
				// common_properties
				else if (reader.isStartElement() && "common_properties".equals(reader.getLocalName())) {
					writeCommonProperties(ins, reader);
				}
			}
		} catch (XMLStreamException e) {
			LOG.error(e.getMessage(), e);
			throw new IOException(e.getMessage(), e);
		} finally {
			if (reader != null) {
				try {
					reader.close();
				} catch (XMLStreamException e) {
					LOG.error(e.getMessage(), e);
					throw new IOException(e.getMessage(), e);
				}
			}
		}
	}

	/**
	 * Writes the content of the meta data block of a channel group block to the
	 * instance element attributes
	 *
	 * @param ins
	 *            The ODSInsertStatement to use
	 * @param mdCommentXML
	 *            The XML string to parse.
	 * @throws IOException
	 * @throws AoException
	 */
	public void writeEVCommentToEv(ODSInsertStatement ins, String mdCommentXML) throws IOException, AoException {
		XMLStreamReader reader = null;
		try {
			reader = xmlInputFactory.createXMLStreamReader(new StringReader(mdCommentXML));
			while (reader.hasNext()) {
				reader.next();
				// TX
				if (reader.isStartElement() && "TX".equals(reader.getLocalName())) {
					ins.setStringVal("desc", reader.getElementText());
				}
				// pre trigger
				else if (reader.isStartElement() && "pre_trigger_interval".equals(reader.getLocalName())) {
					ins.setDoubleVal("pre_trigger_interval", Double.valueOf(reader.getElementText()));
				}
				// post_trigger
				else if (reader.isStartElement() && "post_trigger_interval".equals(reader.getLocalName())) {
					ins.setDoubleVal("post_trigger_interval", Double.valueOf(reader.getElementText()));
				}
				// formula
				else if (reader.isStartElement() && "formula".equals(reader.getLocalName())) {
					// ins.setStringVal("formula", reader.getElementText());
					writeFormula(ins, reader);
				}
				// timeout
				else if (reader.isStartElement() && "timeout".equals(reader.getLocalName())) {

					String triggered = reader.getAttributeValue(null, "triggered");
					if (triggered != null) {
						ins.setShortVal("timeout_triggered", "true".equals(triggered) ? (short) 1 : (short) 0);
					}
					ins.setDoubleVal("timeout", Double.valueOf(reader.getElementText()));
				}
				// common_properties
				else if (reader.isStartElement() && "common_properties".equals(reader.getLocalName())) {
					writeCommonProperties(ins, reader);
				}
			}
		} catch (XMLStreamException e) {
			LOG.error(e.getMessage(), e);
			throw new IOException(e.getMessage(), e);
		} finally {
			if (reader != null) {
				try {
					reader.close();
				} catch (XMLStreamException e) {
					LOG.error(e.getMessage(), e);
					throw new IOException(e.getMessage(), e);
				}
			}
		}
	}

	/**
	 * Writes the content of the meta data block of a channel group block to the
	 * instance element attributes
	 *
	 * @param ins
	 *            The ODSInsertStatement to use.
	 * @param mdCommentXML
	 *            The XML string to parse.
	 * @throws IOException
	 * @throws AoException
	 */
	public void writeSICommentToCg(ODSInsertStatement ins, String mdCommentXML) throws IOException, AoException {
		XMLStreamReader reader = null;
		try {
			reader = xmlInputFactory.createXMLStreamReader(new StringReader(mdCommentXML));
			while (reader.hasNext()) {
				reader.next();
				// TX
				if (reader.isStartElement() && "TX".equals(reader.getLocalName())) {
					ins.setStringVal("src_cmt", reader.getElementText());
				}
				// names
				else if (reader.isStartElement() && "names".equals(reader.getLocalName())) {
					writeNames(ins, reader, "src_");
				}
				// path
				else if (reader.isStartElement() && "path".equals(reader.getLocalName())) {
					ins.setStringVal("src_path", reader.getElementText());
				}
				// bus
				else if (reader.isStartElement() && "bus".equals(reader.getLocalName())) {
					ins.setStringVal("src_bus", reader.getElementText());
				}
				// protocol
				else if (reader.isStartElement() && "protocol".equals(reader.getLocalName())) {
					ins.setStringVal("src_protocol", reader.getElementText());
				}
				// common_properties
				else if (reader.isStartElement() && "common_properties".equals(reader.getLocalName())) {
					writeCommonProperties(ins, reader, "src_");
				}
			}
		} catch (XMLStreamException e) {
			LOG.error(e.getMessage(), e);
			throw new IOException(e.getMessage(), e);
		} finally {
			if (reader != null) {
				try {
					reader.close();
				} catch (XMLStreamException e) {
					LOG.error(e.getMessage(), e);
					throw new IOException(e.getMessage(), e);
				}
			}
		}
	}

	/**
	 * Writes the content of 'formulas' from the XML stream reader as ASAM ODS
	 * instance attributes.
	 *
	 * @param ins
	 *            The insert statement in use.
	 * @param reader
	 *            The XML stream reader.
	 * @throws XMLStreamException
	 *             Error reading XML content.
	 * @throws AoException
	 * @throws NumberFormatException
	 */
	private void writeFormula(ODSInsertStatement ins, XMLStreamReader reader)
			throws XMLStreamException, AoException {
		reader.nextTag();
		while (!(reader.isEndElement() && "formula".equals(reader.getLocalName()))) {
			if (reader.isStartElement() && "syntax".equals(reader.getLocalName())) {
				String version = reader.getAttributeValue(null, "version");
				String value = reader.getElementText();
				ins.setStringVal("syntax", value);
				if (version != null) {
					ins.setStringVal("syntax_version", version);
				}
			} else if (reader.isStartElement() && "custom_syntax".equals(reader.getLocalName())) {
				String source = reader.getAttributeValue(null, "source");
				String version = reader.getAttributeValue(null, "version");
				String value = reader.getElementText();
				ins.setStringVal("custom_syntax", value);
				if (source != null) {
					ins.setStringVal("custom_syntax_source", source);
				}
				if (version != null) {
					ins.setStringVal("custom_syntax_version", version);
				}
			} else if (reader.isStartElement() && "variables".equals(reader.getLocalName())) {
				// TODO full support
				LOG.warn("Variables Tag not yet fully supported");
				LinkedList<String> vars = new LinkedList<>();
				while (!(reader.isEndElement() && "variables".equals(reader.getLocalName()))) {
					if (reader.isStartElement() && "var".equals(reader.getLocalName())) {
						String value = reader.getElementText();
						vars.add(value);
					}
					reader.next();
				}
				vars.toArray(new String[0]);
			}
			reader.next();
		}
	}

	/**
	 * Write names.
	 *
	 * @param ins
	 *            The insert statement in use.
	 * @param reader
	 *            The XML stream reader.
	 * @param nameExtension
	 *            Prefix for unique instanceAttribute names
	 * @throws XMLStreamException
	 *             Error reading XML content.
	 * @throws AoException
	 * @throws NumberFormatException
	 */
	private void writeNames(ODSInsertStatement ins, XMLStreamReader reader, String nameExtension)
			throws XMLStreamException, AoException {

		reader.nextTag();
		LinkedList<String> names = new LinkedList<>();
		LinkedList<String> displays = new LinkedList<>();
		LinkedList<String> vendors = new LinkedList<>();
		LinkedList<String> descriptions = new LinkedList<>();
		while (!(reader.isEndElement() && "names".equals(reader.getLocalName()))) {
			if (reader.isStartElement() && "name".equals(reader.getLocalName())) {
				names.add(reader.getElementText());
			}
			// display
			else if (reader.isStartElement() && "display".equals(reader.getLocalName())) {
				displays.add(reader.getElementText());
			}
			// vendors
			else if (reader.isStartElement() && "vendor".equals(reader.getLocalName())) {
				vendors.add(reader.getElementText());
			}
			// description
			else if (reader.isStartElement() && "description".equals(reader.getLocalName())) {
				descriptions.add(reader.getElementText());
			}
			reader.next();
		}
		if (names.size() == 1) {
			ins.setStringVal(nameExtension + "names", names.get(0));
		}
		if (vendors.size() == 1) {
			ins.setStringVal(nameExtension + "vendors", vendors.get(0));
		}
		if (descriptions.size() == 1) {
			ins.setStringVal(new StringBuilder().append(nameExtension).append("descriptions").append(nameExtension).toString(), descriptions.get(0));
		}
		if (displays.size() == 1) {
			ins.setStringVal(new StringBuilder().append(nameExtension).append("displays").append(nameExtension).toString(), displays.get(0));
		}
	}

	/**
	 * Writes the content of 'common_properties' from the XML stream reader as
	 * ASAM ODS instance attributes.
	 *
	 * @param ins
	 *            The insert statement in use.
	 * @param reader
	 *            The XML stream reader.
	 * @throws XMLStreamException
	 *             Error reading XML content.
	 * @throws AoException
	 * @throws NumberFormatException
	 */
	private void writeCommonProperties(ODSInsertStatement ins, XMLStreamReader reader)
			throws XMLStreamException, AoException {
		writeCommonProperties(ins, reader, "");
	}

	/**
	 * Writes the content of 'common_properties' from the XML stream reader as
	 * ASAM ODS instance attributes.
	 *
	 * @param ins
	 *            The insert statement in use.
	 * @param reader
	 *            The XML stream reader.
	 * @param nameExtension
	 *            Prefix to append to attribute names. For unique instance
	 *            attribute names.
	 * @throws XMLStreamException
	 *             Error reading XML content.
	 * @throws AoException
	 * @throws NumberFormatException
	 */
	private void writeCommonProperties(ODSInsertStatement ins, XMLStreamReader reader, String nameExtension)
			throws XMLStreamException, AoException {
		reader.nextTag();
		while (!(reader.isEndElement() && "common_properties".equals(reader.getLocalName()))) {
			// e
			if (reader.isStartElement() && "e".equals(reader.getLocalName())) {

				String name = nameExtension + reader.getAttributeValue(null, "name");
				String type = reader.getAttributeValue(null, "type");
				String value = reader.getElementText();
				if (type == null || type.length() < 1 || "string".equalsIgnoreCase(type)) {
					if (value == null) {
						value = "";
					}
					ins.setStringVal(name, value);
				} else if ("decimal".equalsIgnoreCase(type)) {
					ins.setNameValueUnit(ODSHelper.createDoubleNVU(name, Double.valueOf(value)));
				} else if ("integer".equalsIgnoreCase(type)) {
					ins.setNameValueUnit(ODSHelper.createLongNVU(name, Integer.parseInt(value)));
				} else if ("float".equalsIgnoreCase(type)) {
					ins.setNameValueUnit(ODSHelper.createFloatNVU(name, Float.valueOf(value)));
				} else if ("boolean".equalsIgnoreCase(type)) {
					short s = Boolean.valueOf(value) ? (short) 1 : (short) 0;
					ins.setNameValueUnit(ODSHelper.createShortNVU(name, s));
				} else if ("datetime".equalsIgnoreCase(type)) {
					try {
						Date date = xmlDateTimeFormat.parse(value);
						ins.setNameValueUnit(ODSHelper.createDateNVU(name, ODSHelper.asODSDate(date)));
					} catch (ParseException e) {
						LOG.warn(e.getMessage(), e);
						ins.setNameValueUnit(ODSHelper.createStringNVU(name, value));
					}
				} else {
					ins.setNameValueUnit(ODSHelper.createStringNVU(name, value));
				}

			}
			// tree
			else if (reader.isStartElement() && "tree".equals(reader.getLocalName())) {
				LOG.warn("'tree' in XML content 'common_properties' is not yet supported!");
			}
			// list
			else if (reader.isStartElement() && "list".equals(reader.getLocalName())) {
				LOG.warn("'list' in XML content 'common_properties' is not yet supported!");
			}
			// elist
			else if (reader.isStartElement() && "elist".equals(reader.getLocalName())) {
				writeEList(ins, reader, nameExtension);
			}
			reader.next();
		}
	}

	/**
	 * Parses a Value in 0x___-Notation to long.
	 *
	 * @param val
	 *            The value as hex string.
	 * @return The value as long.
	 */
	public long parseHex(String val) {
		if (val.length() > 2 && "0x".equals(val.substring(0, 2))) {
			return Long.parseLong(val.substring(2), 16);
		}
		return 0;
	}

	private void writeEList(ODSInsertStatement ins, XMLStreamReader reader, String nameExtension)
			throws XMLStreamException {
		String attrname = nameExtension + reader.getAttributeValue(null, "name");
		String attrtype = reader.getAttributeValue(null, "type");
		LinkedList<Object> valuelist = new LinkedList<>();
		while (!(reader.isEndElement() && "elist".equals(reader.getLocalName()))) {
			// eli
			if (reader.isStartElement() && "eli".equals(reader.getLocalName())) {
				String value = reader.getElementText();
				if (attrtype == null || attrtype.length() < 1 || "string".equalsIgnoreCase(attrtype)) {
					if (value == null) {
						value = "";
					}
					valuelist.add(value);
				} else if ("decimal".equalsIgnoreCase(attrtype)) {
					valuelist.add(Double.valueOf(value));
				} else if ("integer".equalsIgnoreCase(attrtype)) {
					valuelist.add(Integer.parseInt(value));
				} else if ("float".equalsIgnoreCase(attrtype)) {
					valuelist.add(Float.valueOf(value));
				} else if ("boolean".equalsIgnoreCase(attrtype)) {
					short s = Boolean.valueOf(value) ? (short) 1 : (short) 0;
					valuelist.add(s);
				} else if ("datetime".equalsIgnoreCase(attrtype)) {
					try {
						Date date = xmlDateTimeFormat.parse(value);
						valuelist.add(date);
					} catch (ParseException e) {
						LOG.warn(e.getMessage(), e);
					}
				} else {
					valuelist.add(value);
				}
			}
			reader.next();
		}
		if (attrtype == null || attrtype.length() < 1 || "string".equalsIgnoreCase(attrtype)) {
			// ins.setStringSeq(attrname, valuelist.toArray(new String[0]));
		} else if ("decimal".equalsIgnoreCase(attrtype)) {
			ins.setDoubleSeq(attrname, valuelist.toArray(new Double[0]));
		} else if ("integer".equalsIgnoreCase(attrtype)) {
			ins.setLongSeq(attrname, valuelist.toArray(new Integer[0]));
		} else if ("float".equalsIgnoreCase(attrtype)) {
			ins.setFloatSeq(attrname, valuelist.toArray(new Float[0]));
		} else if ("boolean".equalsIgnoreCase(attrtype)) {
			ins.setShortSeq(attrname, valuelist.toArray(new Short[0]));
		} else if ("datetime".equalsIgnoreCase(attrtype)) {
			ins.setDateSeq(attrname, valuelist.toArray(new Date[0]));
		} else {
			// ins.setStringSeq(attrname, valuelist.toArray(new String[0]));
		}
	}
}
