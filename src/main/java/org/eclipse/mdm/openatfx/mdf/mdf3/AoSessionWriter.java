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
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.asam.ods.AoException;
import org.asam.ods.ApplicationElement;
import org.asam.ods.ApplicationRelation;
import org.asam.ods.ApplicationStructure;
import org.asam.ods.DataType;
import org.asam.ods.ErrorCode;
import org.asam.ods.InstanceElement;
import org.asam.ods.InstanceElementIterator;
import org.asam.ods.SeverityFlag;
import org.asam.ods.T_ExternalReference;
import org.asam.ods.T_LONGLONG;
import org.eclipse.mdm.openatfx.mdf.ConvertException;
import org.eclipse.mdm.openatfx.mdf.mdf4.MDF4Util;
import org.eclipse.mdm.openatfx.mdf.util.BitInputStream;
import org.eclipse.mdm.openatfx.mdf.util.FileUtil;
import org.eclipse.mdm.openatfx.mdf.util.LookupTableHelper;
import org.eclipse.mdm.openatfx.mdf.util.ODSHelper;
import org.eclipse.mdm.openatfx.mdf.util.ODSInsertStatement;
import org.eclipse.mdm.openatfx.mdf.util.ODSModelCache;

/**
 * Main class for writing the MDF3 file content into an ATFX file
 *
 * @author Christian Rechner
 */
public class AoSessionWriter {

	private static final Log LOG = LogFactory.getLog(AoSessionWriter.class);
	private static final String MDF_DATEFORMAT = "dd:MM:yyyy HH:mm:ss";

	/** The number format having 5 digits used for count formatting */
	private final NumberFormat countFormat;
	/** The date format in the MDF file */
	private final DateFormat mdfDateFormat;
	/** helper class for lookup tables */
	private final LookupTableHelper lookupTableHelper;

	// Properties
	// replace channel name characters '[' with '{' and ']' with '}'
	private boolean replaceSquareBrackets; // default = false
	// whether to use the file name as AoMeasurement name, if false, property
	// value of 'result_name' is used
	private boolean useFileNameAsResultName = true; // default = true
	// the name of the created AoMeasurement (only used if
	// use_file_name_as_result_name=false)
	private String resultName = "Messdaten"; // default = "Messdaten"
	// the name suffix of the created AoMeasurement, e.g. if suffix='_123' then
	// 'test.mf4' will be converted to 'test_123.mf4'
	private String resultSuffix = ""; // default = null
	// the mimetype of the created AoMeasurement
	private String resultMimeType = "application/x-asam.aomeasurement.timeseries"; // default
																					// =
																					// "application/x-asam.aomeasurement.timeseries"
	// whether to add a reference to the original file to the AoMeasurement
	// instance
	private boolean addMDF3FileAsResultAttachment; // default = false
	// stop parsing after hd block.
	private boolean readOnlyHeader = false;
	// skip empty channels
	private boolean skipEmptyChannels = false;
	// skip channels with an unsupporter donversion formula
	private boolean skipUnsupportedFormula = false;
	// skip channels with unsigned 64 bit data
	private boolean skipUINT64Channels = false;
	// skip channels with DT_BYTESTR
	// remove this once it is save to import channels with byte stream data
	@Deprecated
	private boolean skipByteStreamChannels = false;

	private Path customRatConfPath;

	/**
	 * Constructor.
	 */
	public AoSessionWriter() {
		mdfDateFormat = new SimpleDateFormat(MDF_DATEFORMAT);
		countFormat = new DecimalFormat("00000");
		lookupTableHelper = new LookupTableHelper();
	}

	/**
	 * Appends the content of the MDF4 file to the ASAM ODS session.
	 *
	 * @param modelCache
	 *            The application model cache.
	 * @param idBlock
	 *            The IDBLOCK.
	 * @throws AoException
	 *             Error writing to session.
	 * @throws IOException
	 *             Error reading from MDF file.
	 */
	public void writeTst(ODSModelCache modelCache, IDBLOCK idBlock, Properties props) throws AoException, IOException {
		ApplicationElement aeEnv = modelCache.getApplicationElement("env");
		InstanceElement ieEnv = aeEnv.getInstanceById(new T_LONGLONG(0, 1));
		Path fileName = idBlock.getMdfFilePath().getFileName();
		if (fileName == null) {
			throw new IOException("Unable to obtain file name!");
		}

		if (props != null) {
			// replace channel name characters '[' with '{' and ']' with '}'
			if (props.containsKey("replace_square_brackets")) {
				replaceSquareBrackets = Boolean.valueOf(props.getProperty("replace_square_brackets")); // default
																										// =
																										// false
			}
			// whether to use the file name as AoMeasurement name, if false,
			// property value of 'result_name' is used
			if (props.containsKey("use_file_name_as_result_name")) {
				useFileNameAsResultName = Boolean.valueOf(props.getProperty("use_file_name_as_result_name")); // default
																												// =
																												// true
			}
			// the name of the created AoMeasurement (only used if
			// use_file_name_as_result_name=false)
			if (props.containsKey("result_name")) {
				resultName = props.getProperty("result_name"); // default =
																// "Messdaten"
			}
			// the name suffix of the created AoMeasurement, e.g. if
			// suffix='_123' then 'test.mf4' will be converted to 'test_123.mf4'
			if (props.containsKey("result_suffix")) {
				resultSuffix = props.getProperty("result_suffix"); // default =
																	// null
			}
			// the mimetype of the created AoMeasurement
			if (props.containsKey("result_mimetype")) {
				resultMimeType = props.getProperty("result_mimetype"); // default
																		// =
																		// "application/x-asam.aomeasurement.timeseries"
			}
			// whether to add a reference to the original file to the
			// AoMeasurement instance
			if (props.containsKey("attach_source_files")) {
				addMDF3FileAsResultAttachment = Boolean.valueOf(props.getProperty("attach_source_files")); // default
																											// =
																											// false
			}
			if (props.containsKey("read_only_header")) {
				readOnlyHeader = Boolean.valueOf(props.getProperty("read_only_header"));
			}
			if (props.containsKey("skip_empty_channels")) {
				skipEmptyChannels = Boolean.valueOf(props.getProperty("skip_empty_channels"));
			}
			if (props.containsKey("skip_unsupported_formula")) {
				skipUnsupportedFormula = Boolean.valueOf(props.getProperty("skip_unsupported_formula"));
			}
			if (props.containsKey("skip_uint64_data_channels")) {
				skipUINT64Channels = Boolean.valueOf(props.getProperty("skip_uint64_data_channels"));
			}
			if (props.containsKey("skip_byte_stream_channels")) {
				skipByteStreamChannels = Boolean.valueOf(props.getProperty("skip_byte_stream_channels"));
			}
		}

		// read and validate IDBLOCK
		ODSInsertStatement ins = new ODSInsertStatement(modelCache, "tst");
		ins.setStringVal("iname", FileUtil.stripExtension(fileName.toString()));
		ins.setStringVal("mdf_file_id", idBlock.getIdFile());
		ins.setStringVal("mdf_version_str", idBlock.getIdVers());
		ins.setLongVal("mdf_version", idBlock.getIdVer());
		ins.setStringVal("mdf_program", idBlock.getIdProg());
		ins.setLongVal("mdf_unfin_flags", idBlock.getIdUnfinFlags());
		ins.setLongVal("mdf_custom_unfin_flags", idBlock.getIdCustomUnfinFlags());
		ins.setLongLongVal("env", ieEnv.getId());
		long iidTst = ins.execute();

		// write properties
		if (props != null) {
			MDF4Util.writeProperites(ins, props);
		}

		// write 'AoMeasurement' instance
		writeMea(modelCache, iidTst, idBlock);
	}

	/**
	 * Write the instance of 'AoMeasurement'.
	 *
	 * @param modelCache
	 *            The application model cache.
	 * @param iidTst
	 *            The instance if of the parent 'AoTest' instance.
	 * @param idBlock
	 *            The IDBLOCK.
	 * @return the instance id of the created 'AoMeasurement' instance.
	 * @throws AoException
	 *             Error writing to session.
	 * @throws IOException
	 *             Error reading from MDF file.
	 */
	private long writeMea(ODSModelCache modelCache, long iidTst, IDBLOCK idBlock) throws AoException, IOException {
		Path fileName = idBlock.getMdfFilePath().getFileName();
		if (fileName == null) {
			throw new IOException("Unable to obtain file name!");
		}

		// create "AoMeasurement" instance and write descriptive data to
		// instance attributes
		ODSInsertStatement ins = new ODSInsertStatement(modelCache, "mea");

		if (useFileNameAsResultName) {
			ins.setStringVal("iname", FileUtil.getResultName(fileName.toString(), resultSuffix));
		} else {
			ins.setStringVal("iname", resultName + resultSuffix);
		}
		ins.setStringVal("mt", resultMimeType);

		if (addMDF3FileAsResultAttachment) {
			ins.setExtRefVal("attachment", new T_ExternalReference("original MDF-File", "", fileName.toString()));
		}

		ins.setLongLongVal("tst", iidTst);

		// write header attributes
		HDBLOCK hdBlock = idBlock.getHDBlock();
		TXBLOCK fileComment = hdBlock.getFileCommentTxt();
		if (fileComment != null) {
			ins.setStringVal("desc", fileComment.getText().trim());
		}

		// default date/time handling
		Date date = null;
		if (hdBlock.getDateStarted().length() > 0 && hdBlock.getTimeStarted().length() > 0) {
			try {
				date = mdfDateFormat.parse(hdBlock.getDateStarted() + " " + hdBlock.getTimeStarted());
			} catch (ParseException e) {
				LOG.warn(e.getMessage(), e);
			}
		} else {
			throw new IOException("No date information found in MDF file!");
		}
		ins.setDateVal("date_created", ODSHelper.asODSDate(date));
		ins.setDateVal("mea_begin", ODSHelper.asODSDate(date));

		// special date/time handling
		handleCLExportDate(ins, hdBlock, fileComment);

		ins.setStringVal("author", hdBlock.getAuthor().trim());
		ins.setStringVal("organization", hdBlock.getDepartment().trim());
		ins.setStringVal("project", hdBlock.getProjectName().trim());
		ins.setStringVal("meaObject", hdBlock.getMeaObject().trim());

		long meaIid = ins.execute();
		if (!readOnlyHeader) {
			// remember channel names to avoid duplicates
			// (key=channelName,value=number of)
			Map<String, Integer> meqNames = new HashMap<String, Integer>();

			// write 'AoSubMatrix' instances
			writeSm(modelCache, meaIid, idBlock, hdBlock, meqNames);
		}
		return meaIid;
	}

	/**
	 * Write the instances of 'AoSubMatrix'.
	 *
	 * @param modelCache
	 *            The application model cache.
	 * @param iidMea
	 *            The instance id of the parent 'AoMeasurement' instance.
	 * @param hdBlock
	 *            The HDBLOCK.
	 * @param meqNames
	 * @throws AoException
	 *             Error writing to session.
	 * @throws IOException
	 *             Error reading from MDF file.
	 */
	private void writeSm(ODSModelCache modelCache, long iidMea, IDBLOCK idBlock, HDBLOCK hdBlock,
			Map<String, Integer> meqNames) throws AoException, IOException {
		// iterate over data group blocks
		int grpNo = 1;
		DGBLOCK dgBlock = hdBlock.getFirstFileGroup();
		Map<String, Long> meqInstances = new HashMap<String, Long>();

		while (dgBlock != null) {
			// ONLY SORTED MDF files can be converted - check this!
			if (dgBlock.getNoChannelGroups() > 1) {
				throw new IOException(
						"Currently only 'sorted' MDF3 files are supported, found 'unsorted' data! [DGBLOCK=" + dgBlock
								+ "]");
			}

			// if sorted, only one channel group block is available
			CGBLOCK cgBlock = dgBlock.getNextCgBlock();

			// skip channel groups having no channels (or optionally no values)
			boolean skipNoValues = skipEmptyChannels && cgBlock.getNoOfRecords() < 1;
			if (cgBlock != null && cgBlock.getNoOfChannels() > 0 && !skipNoValues) {

				// create SubMatrix instance
				ODSInsertStatement ins = new ODSInsertStatement(modelCache, "sm");
				ins.setStringVal("iname", "sm_" + countFormat.format(grpNo));
				ins.setLongLongVal("mea", iidMea);
				ins.setLongVal("rows", (int) cgBlock.getNoOfRecords());
				// TODO: parse name:
				// DATA_SysOpmHvES.SysOpmHvES_wElMinDrv_C_VW\ETKC:1\SingleShotGroup
				TXBLOCK channelGroupComment = cgBlock.getChannelGroupComment();
				if (channelGroupComment != null) {
					ins.setStringVal("desc", channelGroupComment.getText());
				}
				long iidSm = ins.execute();

				// write LocalColumns
				writeLc(modelCache, iidMea, iidSm, idBlock, dgBlock, cgBlock, meqNames, meqInstances);
			}

			dgBlock = dgBlock.getNextDgBlock();
			grpNo++;
		}
	}

	/**
	 * Write the instances of 'AoLocalColumn'.
	 *
	 * @param modelCache
	 *            The application model cache.
	 * @param iidMea
	 *            The instance id of 'AoMeasurement'.
	 * @param iidSm
	 *            The instance idof 'AoSubMatrix'.
	 * @param dgBlock
	 *            The MDF data group block.
	 * @param cgBlock
	 *            The MDF channel group block.
	 * @param meqNames
	 * @param meqTime
	 * @throws AoException
	 *             Error writing to session.
	 * @throws IOException
	 *             Error reading from MDF file.
	 */
	private void writeLc(ODSModelCache modelCache, long iidMea, long iidSm, IDBLOCK idBlock, DGBLOCK dgBlock,
			CGBLOCK cgBlock, Map<String, Integer> meqNames, Map<String, Long> meqInstances)
			throws AoException, IOException {
		ApplicationElement aeMeq = modelCache.getApplicationElement("meq");
		ApplicationElement aeLc = modelCache.getApplicationElement("lc");
		ApplicationElement aeMea = modelCache.getApplicationElement("mea");
		ApplicationElement aeSm = modelCache.getApplicationElement("sm");
		ApplicationRelation relLcSm = modelCache.getApplicationRelation("lc", "sm", "LookupTable");

		// iterate over channel blocks
		CNBLOCK cnBlock = cgBlock.getFirstCnBlock();
		while (cnBlock != null) {

			// build signal name - parse the device info
			String meqName = readMeqName(cnBlock);
			String device = null;
			String[] str = meqName.split("\\\\");
			if (str.length > 0) {
				meqName = str[0].trim();
			}
			if (str.length > 1) {
				device = str[1].trim();
			}

			// replace channel names (concerto invalid characters)
			if (replaceSquareBrackets) {
				meqName = meqName.replaceAll("\\[", "{");
				meqName = meqName.replaceAll("\\]", "}");
			}

			// check for duplicate signal names and add suffix (except time
			// channel)
			if (cnBlock.getChannelType() == 0) {
				Integer noChannels = meqNames.get(meqName);
				if (noChannels == null) {
					noChannels = 0;
				}
				noChannels++;
				meqNames.put(meqName, noChannels);
				if (noChannels > 1) {
					meqName = meqName + "_" + noChannels;
				}
			}

			// sequence representation
			CCBLOCK ccBlock = cnBlock.getCcBlock();
			if (ccBlock != null && ccBlock.getFormulaIdent() == 10 && skipUnsupportedFormula) {
				LOG.info("Channel '" + meqName + "' with unsupported formula (10 MCD2 Text Formular) skipped: "
						+ ccBlock);
				// jump to next channel
				cnBlock = cnBlock.getNextCnBlock();
				continue;
			} else if (skipByteStreamChannels && 8 == cnBlock.getSignalDataType()) {
				// remove this block once it is save to import channels with
				// byte stream data
				LOG.info("Channel '" + meqName + "' with byte stream data skipped: " + ccBlock);
				cnBlock = cnBlock.getNextCnBlock();
				continue;
			} else if (8 == cnBlock.getSignalDataType() && cnBlock.getLnkCdBlock() != 0) {
				LOG.info("Channel '" + meqName + "' with composed byte stream data skipped: " + ccBlock);
				cnBlock = cnBlock.getNextCnBlock();
				continue;
			} else if (64 == cnBlock.getNumberOfBits() && (0 /* LEO */ == cnBlock.getSignalDataType()
					|| 9 /* BEO */ == cnBlock.getSignalDataType())) {
				// UNSIGNED 64 bit: if cnBlock represents a time channel (channel_type == 1)
				// => it is considered save to interpret data as signed 64 bit
				// => otherwise either fail or skip
				if (1 != cnBlock.getChannelType()) {
					// this is a data channel with 64 bit unsigned data; it is not possible to represent such data
					// => either throw an error or skip channel
					if (skipUINT64Channels) {
						LOG.info("Channel '" + meqName + "' with unsigned 64 bit data skipped: " + cnBlock);
						cnBlock = cnBlock.getNextCnBlock();
						continue;
					} else {
						throw new IOException("unable to write unsigned 64 bit data channel");
					}
				}
			}

			int seqRep = getSeqRep(ccBlock, meqName);
			if (cnBlock.getNumberOfBits() == 1) { // bit will be stored as bytes
				seqRep = 7;
			}

			// create 'AoMeasurementQuantity' instance if not yet existing
			Long iidMeq = meqInstances.get(meqName);
			double[] genParams = getGenerationParameters(ccBlock);

			if (iidMeq == null) {
				ODSInsertStatement ins = new ODSInsertStatement(modelCache, "meq");
				ins.setStringVal("iname", meqName);
				ins.setStringVal("desc", cnBlock.getSignalDescription().trim());
				boolean expandDataType = genParams != null && genParams.length > 0 && seqRep != 0 && seqRep != 7;
				DataType dt = DataType.from_int(getDataType(expandDataType, cnBlock, ccBlock));
				ins.setEnumVal("dt", seqRep == 1 || isRatConv2ExtComp(ccBlock) ? DataType.DT_DOUBLE.value() : dt.value());
				if (ccBlock != null && ccBlock.isKnownPhysValue()) {
					ins.setDoubleVal("min", ccBlock.getMinPhysValue());
					ins.setDoubleVal("max", ccBlock.getMaxPhysValue());
				}
				if (device != null && device.length() > 0) {
					ins.setStringVal("src_path", device);
				}
				// CEBLOCK (extension block) info
				CEBLOCK ceBlock = cnBlock.getCeblock();
				if (ceBlock != null && ceBlock.getCeBlockDim() != null) {
					CEBLOCK_DIM ext = ceBlock.getCeBlockDim();
					ins.setLongVal("NumberOfModule", ext.getNumberOfModule());
					ins.setLongLongVal("Address", ext.getAddress());
					ins.setStringVal("DIMDescription", ext.getDescription());
					ins.setStringVal("ECUIdent", ext.getEcuIdent());
				} else if (ceBlock != null && ceBlock.getCeBlockVectorCAN() != null) {
					CEBLOCK_VectorCAN ext = ceBlock.getCeBlockVectorCAN();
					ins.setLongLongVal("CANIndex", ext.getCanIndex());
					ins.setLongLongVal("MessageId", ext.getMessageId());
					ins.setStringVal("MessageName", ext.getMessageName());
					ins.setStringVal("SenderName", ext.getSenderName());
				}
				ins.setLongLongVal("mea", iidMea);
				iidMeq = ins.execute();
				meqInstances.put(meqName, iidMeq);
			}

			// create 'AoLocalColumn' instance
			ODSInsertStatement ins = new ODSInsertStatement(modelCache, "lc");
			ins.setStringVal("iname", meqName);
			ins.setLongLongVal("sm", iidSm);
			// sequence_representation: string channel cannot be referenced in
			// ASAM ODS: read and write external
			if (cnBlock.getSignalDataType() == 7) {
				String[] stringDataValues = readStringDataValues(dgBlock, cgBlock, cnBlock);
				ins.setEnumVal("srp", 0);
				ins.setNameValueUnit(ODSHelper.createStringSeqNVU("val", stringDataValues));
				LOG.info("Unable to reference into MDF3, extracting string values. [Channel=" + meqName + "]");
			} else {
				ins.setEnumVal("srp", seqRep);
			}
			// independent flag
			short idp = cnBlock.getChannelType() > 0 ? (short) 1 : (short) 0;
			ins.setShortVal("idp", idp);
			// global flag
			ins.setShortVal("glb", (short) 15);

			// generation parameters or values (for implicit_constant ONLY)
			if (seqRep == 1) {
				// implicit constant
				ins.setDoubleSeq("val", new double[] { genParams[0] });
				ins.setEnumVal("rdt", DataType.DT_DOUBLE.value());
			} else {
				// any other sequence representation
				ins.setDoubleSeq("par", genParams);
				// raw_datatype
				int valueType = getValueType(cnBlock);
				int rawDataType = getRawDataTypeForValueType(valueType, cnBlock);
				ins.setEnumVal("rdt", isRatConv2ExtComp(ccBlock) ? DataType.DT_DOUBLE.value() : rawDataType);
			}
			// axistype
			int axistype = cnBlock.getChannelType() == 0 ? 1 : 0;
			ins.setEnumVal("axistype", axistype);
			// minimum/maximum
			if (cnBlock.isKnownImplValue()) {
				ins.setDoubleVal("min", cnBlock.getMinImplValue());
				ins.setDoubleVal("max", cnBlock.getMaxImplValue());
			}

			// relation to submatrix
			ins.setLongLongVal("sm", iidSm);
			ins.setLongLongVal("meq", iidMeq);

			long iidLc = ins.execute();

			// create 'AoExternalComponent' instance
			if (cnBlock.getSignalDataType() != 7
					&& seqRep != 1 /* skip implicit_constant */) {
				writeEc(modelCache, iidLc, idBlock, dgBlock, cgBlock, cnBlock, ccBlock);
			}

			// create 'AoUnit' instance if not yet existing
			InstanceElement ieMeq = aeMeq.getInstanceById(ODSHelper.asODSLongLong(iidMeq));
			writeUnit(ieMeq, ccBlock);

			// special handling for formula 11 'ASAM-MCD2 Text Table,
			// (COMPU_VTAB)': create lookup table
			InstanceElement ieMea = aeMea.getInstanceById(ODSHelper.asODSLongLong(iidMea));
			InstanceElement ieLc = aeLc.getInstanceById(ODSHelper.asODSLongLong(iidLc));
			if (ccBlock != null && ccBlock.getFormulaIdent() == 11) {
				double[] keys = ccBlock.getKeysForTextTable();
				String[] values = ccBlock.getValuesForTextTable();
				// this.lookupTableHelper.createMCD2TextTableMeasurement(modelCache,
				// ieMea, ieLc, keys, values);
				long iidLookup = lookupTableHelper.createValueToTextTable(modelCache, ieMea, ieLc, keys, values, null);
				InstanceElement ieSmLookup = aeSm.getInstanceById(ODSHelper.asODSLongLong(iidLookup));
				ieLc.createRelation(relLcSm, ieSmLookup);
			}
			// special handling for formula 12 'ASAM-MCD2 Text Range Table
			// (COMPU_VTAB_RANGE)': create lookup table
			else if (ccBlock != null && ccBlock.getFormulaIdent() == 12) {
				double[] keysMin = ccBlock.getLowerRangeKeysForTextRangeTable();
				double[] keysMax = ccBlock.getUpperRangeKeysForTextRangeTable();
				String[] values = ccBlock.getValuesForTextRangeTable();
				String defaultValue = ccBlock.getDefaultTextForTextRangeTable();
				// this.lookupTableHelper.createMCD2TextRangeTableMeasurement(modelCache,
				// ieMea, ieLc, keysMin, keysMax,
				// values, defaultValue);
				long iidLookup = lookupTableHelper.createValueRangeToTextTable(modelCache, ieMea, ieLc, keysMin,
						keysMax, values, defaultValue);
				InstanceElement ieSmLookup = aeSm.getInstanceById(ODSHelper.asODSLongLong(iidLookup));
				ieLc.createRelation(relLcSm, ieSmLookup);
			}

			// jump to next channel
			cnBlock = cnBlock.getNextCnBlock();
		}
	}

	private static String readMeqName(CNBLOCK cnBlock) throws IOException {
		String meqName = cnBlock.getSignalName();
		TXBLOCK signalDisplayIdentifier = cnBlock.getSignalDisplayIdentifier();
		TXBLOCK mcdUniqueName = cnBlock.getMcdUniqueName();
		if (signalDisplayIdentifier != null) {
			String signalDisplayIdentifierTxt = signalDisplayIdentifier.getText().trim();
			if (signalDisplayIdentifierTxt.length() > 0) {
				meqName = signalDisplayIdentifierTxt;
			}
		} else if (mcdUniqueName != null) {
			String mcdUniqueNameTxt = mcdUniqueName.getText().trim();
			if (mcdUniqueNameTxt.length() > 0) {
				meqName = mcdUniqueNameTxt;
			}
		}
		return meqName;
	}

	private void writeUnit(InstanceElement ieMeq, CCBLOCK ccBlock) throws AoException {
		if (ieMeq == null) {
			return;
		}

		ApplicationElement aeMeq = ieMeq.getApplicationElement();
		ApplicationStructure as = aeMeq.getApplicationStructure();
		ApplicationElement aeUnt = as.getElementByName("unt");
		ApplicationRelation relMeqUnt = as.getRelations(aeMeq, aeUnt)[0];

		// create 'AoUnit' instance if not yet existing
		String unitName = "";
		if (ccBlock != null) {
			unitName = ccBlock.getPhysUnit().trim();
		}
		if (ieMeq != null && unitName.length() > 0) {
			InstanceElementIterator iter = aeUnt.getInstances(unitName);
			InstanceElement ieUnit = null;
			if (iter.getCount() > 0) {
				ieUnit = iter.nextOne();
			} else {
				ieUnit = aeUnt.createInstance(unitName);
				ieUnit.setValue(ODSHelper.createDoubleNVU("factor", 1d));
				ieUnit.setValue(ODSHelper.createDoubleNVU("offset", 0d));
			}
			iter.destroy();
			ieMeq.createRelation(relMeqUnt, ieUnit);
		}
	}

	/**
	 * Write the instances of 'AoExternalComponent'.
	 *
	 * @param modelCache
	 *            The application model cache.
	 * @param iidLc
	 *            The instance id of the 'AoLocalColumn' instance.
	 * @param dgBlock
	 *            The MDF data group block.
	 * @param cgBlock
	 *            The MDF channel group block.
	 * @param cnBlock
	 *            The MDF channel block.
	 * @throws AoException
	 *             Error writing to session.
	 * @throws IOException
	 *             Error reading from MDF file.
	 */
	private void writeEc(ODSModelCache modelCache, long iidLc, IDBLOCK idBlock, DGBLOCK dgBlock, CGBLOCK cgBlock,
			CNBLOCK cnBlock, CCBLOCK ccBlock) throws AoException, IOException {
		if (isRatConv2ExtComp(ccBlock)) {
			// NOTE: once CCBLOCK is no longer required, it should be removed
			// from this method's signature!
			createCustomRatConvEC(modelCache, iidLc, idBlock, dgBlock, cgBlock, cnBlock, ccBlock);
		} else {
			ODSInsertStatement ins = new ODSInsertStatement(modelCache, "ec");
			ins.setStringVal("iname", "ec_" + countFormat.format(1));
			Path mdfFilePath = idBlock.getMdfFilePath().getFileName();
			if (mdfFilePath == null) {
				throw new IOException("mdfFilePath must not be null");
			}
			ins.setStringVal("fl", mdfFilePath.toString());
			ins.setEnumVal("vt", getValueType(cnBlock));
			ins.setLongLongVal("so", dgBlock.getLnkDataRecords());
			ins.setLongVal("cl", (int) cgBlock.getNoOfRecords());
			ins.setLongVal("vb", 1);
			int recordIdOffset = dgBlock.getNoRecordIds() > 0 ? 1 : 0;
			ins.setLongVal("bs", cgBlock.getDataRecordSize() + recordIdOffset);
			int valOffset = recordIdOffset + cnBlock.getByteOffset() + cnBlock.getNumberOfFirstBits() / 8;
			ins.setLongVal("vo", valOffset);
			short bitOffset = (short) (cnBlock.getNumberOfFirstBits() % 8);
			if (bitOffset != 0 || cnBlock.getNumberOfBits() % 8 != 0 || cnBlock.getNumberOfBits() == 24) {
				ins.setShortVal("bo", bitOffset);
				ins.setShortVal("bc", (short) cnBlock.getNumberOfBits());
			}
			ins.setLongLongVal("lc", iidLc);
			ins.execute();
		}
	}

	/**
	 * Returns the target ASAM ODS external component type specification enum
	 * value for a MDF3 channel description.<br/>
	 * The data type is determined by the signal data type and the number of
	 * bits.<br/>
	 *
	 * @param cnBlock
	 *            The MDF3 CNBLOCK.
	 * @return The ASAM ODS type specification enumeration value.
	 * @throws IOException
	 *             Unsupported MDF3 data type.
	 */
	private static int getValueType(CNBLOCK cnBlock) throws IOException {
		int dt = cnBlock.getSignalDataType();
		int nb = cnBlock.getNumberOfBits();
		int bitOffset = cnBlock.getNumberOfFirstBits() % 8;

		// 0 = unsigned integer
		if (dt == 0) {
			if (nb == 8 && bitOffset == 0) { // 8 bit: dt_byte
				return 1;
			} else if (nb == 16 && bitOffset == 0) { // 16 bit: dt_ushort
				return 21;
			} else if (nb == 32 && bitOffset == 0) { // 32 bit: dt_ulong
				return 23;
			} else if (nb == 64 && bitOffset == 0) { // 64 bit: dt_longlong
				return 4;
			} else { // variable bit length: dt_bit_uint
				return 29;
			}
		}

		// 1 = signed integer
		else if (dt == 1) {
			if (nb == 8 && bitOffset == 0) { // 8 bit: dt_sbyte
				return 19;
			} else if (nb == 16 && bitOffset == 0) { // 16 bit: dt_short
				return 2;
			} else if (nb == 32 && bitOffset == 0) { // 32 bit: dt_long
				return 3;
			} else if (nb == 64 && bitOffset == 0) { // 64 bit: dt_longlong
				return 4;
			} else { // variable bit length: dt_bit_int
				return 27;
			}
		}

		// 2,3 = IEEE 754 floating-point format
		else if (dt == 2 || dt == 3) {
			if (nb == 32 && bitOffset == 0) { // 32 bit: ieeefloat4
				return 5;
			} else if (nb == 64 && bitOffset == 0) { // 64 bit: ieeefloat8
				return 6;
			} else { // variable bit length: dt_bit_float
				return 31;
			}
		}

		// 7 = String (NULL terminated): dt_string
		else if (dt == 7) {
			return 12;
		}

		// 8 = Byte Array: dt_bytestr
		else if (dt == 8) {
			return 13;
		}

		// 9 = unsigned integer BEO
		else if (dt == 9) {
			if (nb == 8 && bitOffset == 0) { // 8 bit: dt_byte
				return 1;
			} else if (nb == 16 && bitOffset == 0) { // 16 bit: dt_ushort_beo
				return 22;
			} else if (nb == 32 && bitOffset == 0) { // 32 bit: dt_ulong_beo
				return 24;
			} else if (nb == 64 && bitOffset == 0) { // 32 bit: dt_longlong_beo
				return 9;
			} else { // variable bit length: dt_bit_uint_beo
				return 30;
			}
		}

		// 10 = signed integer BEO
		else if (dt == 10) {
			if (nb == 8 && bitOffset == 0) { // 8 bit: dt_byte
				return 1;
			} else if (nb == 16 && bitOffset == 0) { // 16 bit: dt_short_beo
				return 7;
			} else if (nb == 32 && bitOffset == 0) { // 32 bit: dt_long_beo
				return 8;
			} else if (nb == 64 && bitOffset == 0) { // 64 bit: dt_longlong_beo
				return 9;
			} else { // variable bit length: dt_bit_int_beo
				return 28;
			}
		}

		// 11,12 = IEEE 754 floating-point format BEO
		else if (dt == 11 || dt == 12) {
			if (nb == 32 && bitOffset == 0) { // 32 bit: ieeefloat4_beo
				return 10;
			} else if (nb == 64 && bitOffset == 0) { // 64 bit: ieeefloat8_beo
				return 11;
			} else { // variable bit length: dt_bit_float_beo
				return 32;
			}
		}

		throw new IOException("Unsupported channel block: " + cnBlock);
	}

	/**
	 * Returns the raw dataType for given type specification.
	 *
	 * @param typeSpec
	 *            The TypeSpec.
	 * @param cnBlock
	 *            The CNBLOCK.
	 * @return The raw dataType.
	 * @throws AoException
	 *             unable to obtain raw datatype
	 */
	private int getRawDataTypeForValueType(int typeSpec, CNBLOCK cnBlock) throws AoException {
		int ret = 0;
		if (typeSpec == 0) { // dt_boolean
			ret = 4; // DT_BOOLEAN
		} else if (typeSpec == 1) { // dt_byte
			ret = 5; // DT_BYTE
		} else if (typeSpec == 2) { // dt_short
			ret = 2; // DT_SHORT
		} else if (typeSpec == 3) { // dt_long
			ret = 6; // DT_LONG
		} else if (typeSpec == 4) { // dt_longlong
			ret = 8; // DT_LONGLONG
		} else if (typeSpec == 5) { // ieeefloat4
			ret = 3; // DT_FLOAT
		} else if (typeSpec == 6) { // ieeefloat8
			ret = 7; // DT_DOUBLE
		} else if (typeSpec == 7) { // dt_short_beo
			ret = 2; // DT_SHORT
		} else if (typeSpec == 8) { // dt_long_beo
			ret = 6; // DT_LONG
		} else if (typeSpec == 9) { // dt_longlong_beo
			ret = 8; // DT_LONGLONG
		} else if (typeSpec == 10) { // ieeefloat4_beo
			ret = 3; // DT_FLOAT
		} else if (typeSpec == 11) { // ieeefloat8_beo
			ret = 7; // DT_DOUBLE
		} else if (typeSpec == 12) { // dt_string
			ret = 1; // DT_STRING
		} else if (typeSpec == 13) { // dt_bytestr
			ret = 11; // DT_BYTESTR
		} else if (typeSpec == 14) { // dt_blob
			ret = 12; // DT_BLOB
		} else if (typeSpec == 15) { // dt_boolean_flags_beo
			ret = 4; // DT_BOOLEAN
		} else if (typeSpec == 16) { // dt_byte_flags_beo
			ret = 5; // DT_BYTE
		} else if (typeSpec == 17) { // dt_string_flags_beo
			ret = 1; // DT_STRING
		} else if (typeSpec == 18) { // dt_bytestr_beo
			ret = 11; // DT_BYTESTR
		} else if (typeSpec == 19) { // dt_sbyte
			ret = 2; // DT_SHORT
		} else if (typeSpec == 20) { // dt_sbyte_flags_beo
			ret = 2; // DT_SHORT
		} else if (typeSpec == 21) { // dt_ushort
			ret = 6; // DT_LONG
		} else if (typeSpec == 22) { // dt_ushort_beo
			ret = 6; // DT_LONG
		} else if (typeSpec == 23) { // dt_ulong
			ret = 8; // DT_LONGLONG
		} else if (typeSpec == 24) { // dt_ulong_beo
			ret = 8; // DT_LONGLONG
		} else if (typeSpec == 25) { // dt_string_utf8
			ret = 1; // DT_STRING
		} else if (typeSpec == 26) { // dt_string_utf8_beo
			ret = 1; // DT_STRING
		}
		// dt_bit_int [27], dt_bit_int_beo [28], dt_bit_uint [29],
		// dt_bit_uint_beo [30]
		else if (typeSpec == 27 || typeSpec == 28 || typeSpec == 29 || typeSpec == 30) {
			int dt = cnBlock.getSignalDataType();
			int nb = cnBlock.getNumberOfBits();
			if ((dt == 0 || dt == 9 || dt == 13) && nb >= 1 && nb <= 8) { // unsigned
																			// byte
				ret = 5; // DT_BYTE
			} else if ((dt == 1 || dt == 10 || dt == 14) && nb >= 1 && nb <= 8) { // signed
																					// byte
				ret = 5; // DT_BYTE
			} else if ((dt == 0 || dt == 9 || dt == 13) && nb >= 9 && nb <= 16) { // unsigned
																					// short
				ret = 6; // DT_LONG
			} else if ((dt == 1 || dt == 10 || dt == 14) && nb >= 9 && nb <= 16) { // signed
																					// short
				ret = 2; // DT_SHORT
			} else if ((dt == 0 || dt == 9 || dt == 13) && nb >= 17 && nb <= 32) { // unsigned
																					// int
				ret = 8; // DT_LONGLONG
			} else if ((dt == 1 || dt == 10 || dt == 14) && nb >= 17 && nb <= 32) { // int
				ret = 6; // DT_LONG
			} else if ((dt == 0 || dt == 9 || dt == 13) && nb >= 33) { // unsigned
																		// int
																		// >32
																		// bit
				ret = 8; // DT_LONGLONG
			} else if ((dt == 1 || dt == 10 || dt == 14) && nb >= 33) { // signed
																		// int
																		// >32
																		// bit
				ret = 8; // DT_LONGLONG
			}
		}
		// dt_bit_float [31], dt_bit_float_beo [32]
		else if (typeSpec == 31 || typeSpec == 32) {
			int dt = cnBlock.getSignalDataType();
			int nb = cnBlock.getNumberOfBits();
			if ((dt == 2 || dt == 3 || dt == 11 || dt == 12 || dt == 15 || dt == 16) && nb == 32) { // ieeefloat4,
				// ieeefloat4_beo
				ret = 3; // DT_FLOAT
			} else if ((dt == 2 || dt == 3 || dt == 11 || dt == 12 || dt == 15 || dt == 16) && nb == 64) { // ieeefloat8,
				// ieeefloat8_beo
				ret = 7; // DT_DOUBLE
			}
		}
		// not found!
		else {
			throw new AoException(ErrorCode.AO_BAD_PARAMETER, SeverityFlag.ERROR, 0,
					"Unsupported typeSpec: " + typeSpec);
		}
		return ret;
	}

	/**
	 * Returns the target ASAM ODS measurement quantity data type for a MDF3
	 * channel description.<br/>
	 * The data type is determined by the formula, the signal data type and the
	 * number of bits.
	 *
	 * @param expandDataType
	 *            Indicates whether to expand data type or not.
	 * @param cnBlock
	 *            The MDF CNBLOCK.
	 * @param ccBlock
	 *            The MDF CCBLOCK.
	 * @return The ASAM ODS data type.
	 * @throws IOException
	 *             Unable to determine data type.
	 */
	private static int getDataType(boolean expandDataType, CNBLOCK cnBlock, CCBLOCK ccBlock) throws IOException {
		// CCBLOCK may be null
		int formula = 65535;
		if (ccBlock != null) {
			formula = ccBlock.getFormulaIdent();
		}
		int dt = cnBlock.getSignalDataType();
		int nb = cnBlock.getNumberOfBits();

		// STRING
		if (dt == 7) {
			return 1; // DT_STRING
		} else if (dt == 8) {
			return 11; // DT_BYTESTR
		}

		// 0 = parametric, linear
		// 6 = polynomial function
		// 7 = exponential function
		// 8 = logarithmic function
		else if (formula == 0 || formula == 1 || formula == 6 || formula == 7 || formula == 8) {
			if (nb == 0 && dt == 0) {
				return 8; // DT_LONGLONG
			} else if (nb == 1) {
				// 1 bit should be DT_BOOLEAN, but most of tools do not support
				// this
				return 5; // DT_BYTE
			} else if (nb >= 2 && nb <= 32) {
				return 3; // DT_FLOAT
			} else if (nb >= 33 && nb <= 64) {
				return 7; // DT_DOUBLE
			}
		}

		// 9 = ASAP2 Rational conversion formula
		// 11 = ASAM-MCD2 Text Table, (COMPU_VTAB)
		// 12 = ASAM-MCD2 Text Range Table (COMPU_VTAB_RANGE)
		// 65535 = 1:1 conversion formula (Int = Phys)
		else if (formula == 9 || formula == 11 || formula == 12 || formula == 65535) {
			if (dt == 8) {
				return 11; // DT_BYTESTR
			} else if ((dt == 0 || dt == 9 || dt == 13) && nb >= 1 && nb <= 8) { // dt_byte
				return expandDataType ? 3 : 5; // [DT_FLOAT] DT_BYTE
			} else if ((dt == 1 || dt == 10 || dt == 14) && nb >= 1 && nb <= 8) { // dt_sbyte
				return expandDataType ? 3 : 2; // [DT_FLOAT] DT_SHORT
			} else if ((dt == 0 || dt == 9 || dt == 13) && nb >= 9 && nb <= 16) { // dt_ushort,
																					// dt_ushort_beo
				return expandDataType ? 3 : 6; // [DT_FLOAT] DT_LONG
			} else if ((dt == 1 || dt == 10 || dt == 14) && nb >= 9 && nb <= 16) { // dt_short,
																					// dt_short_beo
				return expandDataType ? 3 : 2; // [DT_FLOAT] DT_SHORT
			} else if ((dt == 0 || dt == 9 || dt == 13) && nb >= 17 && nb <= 32) { // dt_ulong,
																					// dt_ulong_beo
				return expandDataType ? 7 : 8; // [DT_DOUBLE] DT_LONGLONG
			} else if ((dt == 1 || dt == 10 || dt == 14) && nb >= 17 && nb <= 32) { // dt_long,
																					// dt_long_beo
				return expandDataType ? 3 : 6; // [DT_FLOAT] DT_LONG
			} else if ((dt == 0 || dt == 9 || dt == 13) && nb >= 33) { // unsigned
																		// int
																		// >32
																		// bit
				return expandDataType ? 7 : 8; // [DT_DOUBLE] DT_LONGLONG
			} else if ((dt == 1 || dt == 10 || dt == 14) && nb >= 33 && nb <= 64) { // dt_longlong,
																					// dt_longlong_beo
				return expandDataType ? 7 : 8; // [DT_DOUBLE] DT_LONGLONG
			} else if ((dt == 2 || dt == 3 || dt == 11 || dt == 12 || dt == 15 || dt == 16) && nb == 32) { // ieeefloat4,
				// ieeefloat4_beo
				return 3; // DT_FLOAT
			} else if ((dt == 2 || dt == 3 || dt == 11 || dt == 12 || dt == 15 || dt == 16) && nb == 64) { // ieeefloat8,
				// ieeefloat8_beo
				return 7; // DT_DOUBLE
			}
		}

		throw new IOException("Unsupported MDF3 datatype: " + cnBlock + "\n " + ccBlock);
	}

	/**
	 * Returns the target ASAM ODS sequence representation for the external
	 * component description.<br/>
	 * List of MDF3 formula types:
	 * <ul>
	 * <li>0 = parametric, linear</li>
	 * <li>1 = tabular with interpolation</li>
	 * <li>2 = tabular</li>
	 * <li>6 = polynomial function</li>
	 * <li>7 = exponential function</li>
	 * <li>8 = logarithmic function</li>
	 * <li>9 = ASAP2 Rational conversion formula</li>
	 * <li>10 = ASAM-MCD2 Text formula</li>
	 * <li>11 = ASAM-MCD2 Text Table, (COMPU_VTAB)</li>
	 * <li>12 = ASAM-MCD2 Text Range Table (COMPU_VTAB_RANGE)</li>
	 * <li>132 = Date (Based on 7 Byte Date data structure)</li>
	 * <li>133 = time (Based on 6 Byte Time data structure)</li>
	 * <li>65535 = 1:1 conversion formula (Int = Phys)</li>
	 * </ul>
	 *
	 * @return The ASAM ODS sequence representation enum value.
	 * @throws AoException
	 * @throws ConvertException
	 */
	private static int getSeqRep(CCBLOCK ccBlock, String meqName) throws AoException, IOException {
		// CCBLOCK may be null, assume explicit
		if (ccBlock == null) {
			return 7;
		}

		int formula = ccBlock.getFormulaIdent();
		// 'parametric, linear' => 'raw_linear_external'
		if (formula == 0) {
			double[] genParams = ccBlock.getValuePairsForFormula();
			if (genParams != null && genParams.length == 2 && Double.compare(genParams[1], 0) == 0) {
				// this is a special case with a given offset and a scaling
				// factor of '0'
				// -> therefore sequence representation has to be implicit
				// constant
				return 1;
			} else {
				return 8;
			}
		}
		// 'tabular with interpolation' => 'external_component'
		else if (formula == 1) {
			return 7;
		}
		// 'polynomial function' => 'raw_polynomial_external'
		else if (formula == 6) {
			return 9;
		}
		// 'ASAP2 Rational conversion formula' => 'external_component'
		else if (formula == 9) {
			if (isRatConv2RawLinExt(ccBlock)) {
				// redirect => 'raw_linear_external'
				return 8;
			} else if (isRatConv2ExtComp(ccBlock)) {
				// manual calculation => 'external_component'
				return 7;
			} else {
				throw new AoException(ErrorCode.AO_IMPLEMENTATION_PROBLEM, SeverityFlag.ERROR, 0,
						"unable write channel with rational conversion for ccBlock: " + ccBlock);
			}
		}
		// 'ASAM-MCD2 Text Table, (COMPU_VTAB)' => 'external_component'
		else if (formula == 11) {
			return 7;
		}
		// 'ASAM-MCD2 Text Range Table (COMPU_VTAB_RANGE)'
		else if (formula == 12) {
			// 'ASAM-MCD2 Text Range Table (COMPU_VTAB_RANGE) with formula' =>
			// 'raw_linear_external'
			// CANape specific feature: default value contains macro for linear
			// formula
			if (ccBlock.getDefaultTextForTextRangeTable() != null
					&& ccBlock.getDefaultTextForTextRangeTable().startsWith("{LINEAR_CONV")) {
				return 8;
			}
			// 'external_component'
			else {
				return 7;
			}
		}
		// '1:1 conversion formula' => external_component
		else if (formula == 65535) {
			return 7;
		}
		throw new IOException(
				"Unsupported MDF Conversion formula identifier for channel '" + meqName + "': " + formula);
	}

	/**
	 * Returns the generation parameters
	 *
	 * @return The ASAM ODS sequence representation enum value.
	 * @throws AoException
	 * @throws IOException
	 */
	private static double[] getGenerationParameters(CCBLOCK ccBlock) throws AoException, IOException {
		// CCBLOCK may be null, assume explicit
		if (ccBlock == null) {
			return new double[0];
		}

		int formula = ccBlock.getFormulaIdent();

		// 'parametric, linear'
		if (formula == 0) {
			double[] genParams = ccBlock.getValuePairsForFormula();
			if (Double.compare(genParams[1], 0D) == 0) {
				// scaling factor is '0', reduce to offset (implicit_constant)
				return new double[] { genParams[0] };
			} else {
				return genParams;
			}
		}

		// 'polynomial function'
		else if (formula == 6) {
			double[] genParams = new double[7];
			genParams[0] = 5;
			genParams[1] = ccBlock.getValuePairsForFormula()[0];
			genParams[2] = ccBlock.getValuePairsForFormula()[1];
			genParams[3] = ccBlock.getValuePairsForFormula()[2];
			genParams[4] = ccBlock.getValuePairsForFormula()[3];
			genParams[5] = ccBlock.getValuePairsForFormula()[4];
			genParams[6] = ccBlock.getValuePairsForFormula()[5];
			return genParams;
		}

		else if (formula == 9) {
			double[] genParams = ccBlock.getValuePairsForFormula();
			if (isRatConv2RawLinExt(ccBlock)) {
				// sequence rep.: 'raw_linear_external'
				return new double[] { genParams[2], genParams[1] };
			} else if (isRatConv2ExtComp(ccBlock)) {
				// sequence rep.: 'external_component'
				return new double[0];
			} else {
				throw new AoException(ErrorCode.AO_IMPLEMENTATION_PROBLEM, SeverityFlag.ERROR, 0,
						"unable write channel with rational conversion for ccBlock: " + ccBlock);
			}
		}
		// 'ASAM-MCD2 Text Range Table (COMPU_VTAB_RANGE) with formula' =>
		// 'raw_linear_external'
		// CANape specific feature: default value contains macro for linear
		// formula
		// example: {LINEAR_CONV "0.3*{X}-30"}
		else if (formula == 12 && ccBlock.getDefaultTextForTextRangeTable() != null
				&& ccBlock.getDefaultTextForTextRangeTable().startsWith("{LINEAR_CONV")) {
			Pattern pattern = Pattern.compile("\\{LINEAR_CONV\\s\\\"(.*)\\*\\{X\\}(.*)\\\"\\}");
			Matcher matcher = pattern.matcher(ccBlock.getDefaultTextForTextRangeTable());
			if (matcher.matches()) {
				double[] genParams = new double[2];
				genParams[0] = Double.valueOf(matcher.group(2));
				genParams[1] = Double.valueOf(matcher.group(1));
				return genParams;
			} else {
				throw new IOException("Unparsable formula: " + ccBlock.getDefaultTextForTextRangeTable());
			}
		}

		return new double[0];
	}

	/**************************************************************************
	 * handling of special MDF3 contents
	 **************************************************************************/

	/**
	 * MDF3 files created by the G.i.N. CLExport tools may contain the correct
	 * measurement date/time only within the comment text. The predefined date
	 * fields in this case are filled with a dummy value.
	 *
	 * @param ieMea
	 *            The target AoMeasurement instance.
	 * @param hdBlock
	 *            The MDF3 header block.
	 * @param fileComment
	 *            The MDF3 file comment.
	 * @throws AoException
	 *             error setting date to instance.
	 */
	private static void handleCLExportDate(ODSInsertStatement ins, HDBLOCK hdBlock, TXBLOCK fileComment)
			throws AoException {
		if (hdBlock.getDateStarted() != null && !hdBlock.getDateStarted().equals("01:01:1980")) {
			return;
		}
		if (hdBlock.getTimeStarted() != null && !hdBlock.getTimeStarted().equals("00:00:00")) {
			return;
		}
		if (fileComment == null || fileComment.getText() == null || fileComment.getText().length() < 1) {
			return;
		}

		// 'Data date/time: 28.08.2015 17:00:40': REPRESENTS THE END-TIME OF A
		// MEASUREMENT!
		Pattern pattern = Pattern.compile("^.*Data date/time:\\s*(.*)$", Pattern.MULTILINE);
		Matcher matcher = pattern.matcher(fileComment.getText());
		if (matcher.find()) {
			try {
				DateFormat clDateFormat = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss");
				Date date = clDateFormat.parse(matcher.group(1));
				ins.setDateVal("date_created", ODSHelper.asODSDate(date));
				ins.setDateVal("mea_begin", null);
				ins.setDateVal("mea_end", ODSHelper.asODSDate(date));
				LOG.info("Found special CLExport date format in comment: " + matcher.group(1));
			} catch (ParseException e) {
				LOG.warn(e.getMessage(), e);
			}
		}
	}

	private static String[] readStringDataValues(DGBLOCK dgBlock, CGBLOCK cgBlock, CNBLOCK cnBlock) throws IOException {
		List<String> list = new ArrayList<String>();

		SeekableByteChannel sbc = dgBlock.sbc;
		int recordIdOffset = dgBlock.getNoRecordIds() > 0 ? 1 : 0;
		long mapStart = dgBlock.getLnkDataRecords();
		long mapSize = (cgBlock.getDataRecordSize() + recordIdOffset) * cgBlock.getNoOfRecords();
		ByteBuffer bb = ByteBuffer.allocate((int) mapSize);
		sbc.position(mapStart);
		sbc.read(bb);
		bb.rewind();

		// iterate over records
		for (int i = 0; i < cgBlock.getNoOfRecords(); i++) {

			// read record
			byte[] record = new byte[cgBlock.getDataRecordSize() + recordIdOffset];
			bb.get(record);

			// skip first bits and read value
			BitInputStream bis = new BitInputStream(record);
			int skipBits = recordIdOffset * 8 + cnBlock.getByteOffset() * 8 + cnBlock.getNumberOfFirstBits();
			bis.skip(skipBits);
			byte[] b = bis.readByteArray(cnBlock.getNumberOfBits());

			// build string value and append
			list.add(Mdf3Util.readChars(ByteBuffer.wrap(b), b.length));
		}

		return list.toArray(new String[0]);
	}

	/**
	 * Checks whether channel with rational conversion may be stored as a
	 * 'raw_linear_external'.
	 * <p>
	 * NOTE: This is a workaround to provide channels with formula == 9 with a
	 * reduced set of generation parameters. Such channel values are described
	 * with a rational conversion, which can not be described in ODS. To do so
	 * the generation parameters p1, p3 and p4 must be ZERO and p5 must be ONE.
	 * <p>
	 * <b>ATTENTION: THIS IS JUST A WORKAROUND UND HAS TO BE REMOVED AS SOON AS
	 * IT IS POSSIBLE TO DESCRIBE SUCH CHANNELS IN ODS!</b>
	 *
	 * @param ccBlock
	 *            the conversion block
	 * @return true if the values must be calculated and stored in an external
	 *         file
	 */
	@Deprecated
	private static boolean isRatConv2RawLinExt(CCBLOCK ccBlock) {
		if (ccBlock == null) {
			return false;
		} else if (ccBlock.getFormulaIdent() == 9) {
			// rational conversion
			double[] genParams = ccBlock.getValuePairsForFormula();
			if (genParams == null || genParams.length != 6) {
				return false;
			} else {
				boolean p1Zero = Double.compare(genParams[0], 0D) == 0;
				boolean p4Zero = Double.compare(genParams[3], 0D) == 0;
				boolean p5Zero = Double.compare(genParams[4], 0D) == 0;
				boolean p6One = Double.compare(genParams[5], 1D) == 0;
				return p1Zero && p4Zero && p5Zero && p6One;
			}
		}

		return false;
	}

	/**
	 * Checks whether channel with rational conversion may be stored as a
	 * 'external_component'.
	 * <p>
	 * NOTE: This is a workaround to provide channels with formula == 9. Such
	 * channel values are described with a rational conversion, which can not be
	 * described in ODS. Therefore the whole value sequence is calculated and
	 * referenced as an 'external_component'.
	 * <p>
	 * <b>ATTENTION: THIS IS JUST A WORKAROUND UND HAS TO BE REMOVED AS SOON AS
	 * IT IS POSSIBLE TO DESCRIBE SUCH CHANNELS IN ODS!</b>
	 *
	 * @param ccBlock
	 *            the conversion block
	 * @return true if the values must be calculated and stored in an external
	 *         file
	 */
	@Deprecated
	private static boolean isRatConv2ExtComp(CCBLOCK ccBlock) {
		if (ccBlock == null) {
			return false;
		} else if (ccBlock.getFormulaIdent() == 9) {
			// rational conversion
			double[] genParams = ccBlock.getValuePairsForFormula();
			if (genParams == null || genParams.length != 6) {
				return false;
			} else {
				boolean p1Zero = Double.compare(genParams[0], 0D) != 0;
				boolean p4Zero = Double.compare(genParams[3], 0D) != 0;
				boolean p5Zero = Double.compare(genParams[4], 0D) != 0;
				boolean p6One = Double.compare(genParams[5], 1D) != 0;
				return p1Zero || p4Zero || p5Zero || p6One;
			}
		}

		return false;
	}

	/**
	 * Write the instances of 'AoExternalComponent'.
	 * <p>
	 * One instance is written for all calculated vaues.
	 * <p>
	 * NOTE: This is a workaround to provide channels with formula == 2. Such
	 * channel values are described with a rational conversion, which can not be
	 * described in ODS. Therefore the whole value sequence is calculated and
	 * referenced as an 'external_component'.
	 * <p>
	 * <b>ATTENTION: THIS IS JUST A WORKAROUND UND HAS TO BE REMOVED AS SOON AS
	 * IT IS POSSIBLE TO DESCRIBE SUCH CHANNELS IN ODS!</b>
	 *
	 * @param modelCache
	 *            The application model cache.
	 * @param iidLc
	 *            The instance id of the 'AoLocalColumn' instance.
	 * @param idBlock
	 *            The IDBLOCK.
	 * @param dgBlock
	 *            The DGBLOCK.
	 * @param cgBlock
	 *            The CGBLOCK.
	 * @param cnBlock
	 *            The CNBLOCK.
	 * @param ccBlock
	 *            The CCBLOCK.
	 * @throws AoException
	 *             Error writing to session.
	 * @throws IOException
	 *             Error reading from MDF file.
	 */
	@Deprecated
	private void createCustomRatConvEC(ODSModelCache modelCache, long iidLc, IDBLOCK idBlock, DGBLOCK dgBlock,
			CGBLOCK cgBlock, CNBLOCK cnBlock, CCBLOCK ccBlock) throws AoException, IOException {

		if (customRatConfPath == null) {
			customRatConfPath = idBlock.getMdfFilePath().resolveSibling("rational_conversion.calc");
			if (!Files.exists(customRatConfPath)) {
				Files.createFile(customRatConfPath);
			}
		}

		short bo = (short) (cnBlock.getNumberOfFirstBits() % 8);
		short bc = bo != 0 && cnBlock.getNumberOfBits() % 8 != 0 ? (short) cnBlock.getNumberOfBits() : 0;
		if (bo != 0 || bc != 0 || cnBlock.getNumberOfBits() % 8 != 0) {
			throw new AoException(ErrorCode.AO_BAD_PARAMETER, SeverityFlag.ERROR, 0, "bit count '" + bc
				+ "' and bit offset '" + bo + "' is not supported for custom ration conversion");
		}

		try (SeekableByteChannel channel = Files.newByteChannel(customRatConfPath, StandardOpenOption.APPEND)) {
			ByteBuffer writeBuffer = ByteBuffer.wrap(new byte[8]);
			writeBuffer.order(ByteOrder.LITTLE_ENDIAN);
			long startOffset = channel.position();
			long count = 0;

			int recordIdOffset = dgBlock.getNoRecordIds() > 0 ? 1 : 0;
			long vo = recordIdOffset + cnBlock.getByteOffset() + cnBlock.getNumberOfFirstBits() / 8;
			long bs = cgBlock.getDataRecordSize() + recordIdOffset;
			long so = dgBlock.getLnkDataRecords();
			double[] p = ccBlock.getValuePairsForFormula();

			int bits = (int) cnBlock.getNumberOfBits();
			int dt = cnBlock.getSignalDataType();
			boolean isInteger = dt == 0 || dt == 1 || dt == 9 || dt == 10 || dt == 13 || dt == 14;
			boolean isReal = dt == 2 || dt == 3 || dt == 11 || dt == 12 || dt == 15 || dt == 16;
			boolean isUnsigned = dt == 0 || dt == 9 || dt == 13;
			ByteOrder byteOrder;
			if (dt < 4) {
				byteOrder = idBlock.getIdByteOrder() == 0 ? ByteOrder.LITTLE_ENDIAN : ByteOrder.BIG_ENDIAN;
			} else {
				if (dt > 8 && dt < 13) {
					byteOrder = ByteOrder.BIG_ENDIAN;
				} else if (dt > 12 && dt < 17) {
					byteOrder = ByteOrder.LITTLE_ENDIAN;
				} else {
					throw new AoException(ErrorCode.AO_BAD_PARAMETER, SeverityFlag.ERROR, 0,
							"unable to determine byte order for CN block with rational conversion :" + cnBlock);
				}
			}
			for (long i = 0; i < cgBlock.getNoOfRecords(); i++) {
				idBlock.sbc.position(so + i * bs + vo);
				ByteBuffer byteBuffer = ByteBuffer.allocate(bits / 8);
				byteBuffer.order(byteOrder);
				idBlock.sbc.read(byteBuffer);
				byteBuffer.rewind();

				double internal = 0;
				if (isInteger) {
					if (bits == 8) {
						// isUnsigned ? short : byte
						internal = isUnsigned ? byteBuffer.get() & 0xFF : byteBuffer.get();
					} else if (bits == 16) {
						// isUnsigned ? int : short
						internal = isUnsigned ? byteBuffer.getShort() & 0xFFFF : byteBuffer.getShort();
					} else if (bits == 32) {
						// isUnsigned ? long : int
						internal = isUnsigned ? byteBuffer.getInt() & 0xFFFFFFFF : byteBuffer.getInt();
					} else if (bits == 64) {
						if (isUnsigned) {
							throw new AoException(ErrorCode.AO_BAD_PARAMETER, SeverityFlag.ERROR, 0,
									"reading unsigned 64 bit integeres is not implemented");
						}
						// to support unsigned 64 bit, BigInteger has to be
						// used -> performance costs
						internal = byteBuffer.getLong();
					} else {
						String unsigned = isUnsigned ? "unsigned" : "signed";
						throw new AoException(ErrorCode.AO_BAD_PARAMETER, SeverityFlag.ERROR, 0,
								"customized reading of '" + bits + "' bit '" + unsigned
										+ "' intergers is not implemented");
					}
				} else if (isReal) {
					if (bits == 32) {
						// ieee754 floating point
						internal = byteBuffer.getFloat();
					} else if (bits == 64) {
						// ieee754 floating point
						internal = byteBuffer.getDouble();
					} else {
						String unsigned = isUnsigned ? "unsigned" : "signed";
						throw new AoException(ErrorCode.AO_BAD_PARAMETER, SeverityFlag.ERROR, 0,
								"customized reading of '" + bits + "' bit '" + unsigned
										+ "' real is not implemented");
					}
				} else {
					throw new AoException(ErrorCode.AO_BAD_PARAMETER, SeverityFlag.ERROR, 0,
							"given value is neither an integer nor a real number");
				}
				double phys = (p[0] * Math.pow(internal, 2) + p[1] * internal + p[2])
						/ (p[3] * Math.pow(internal, 2) + p[4] * internal + p[5]);

				writeBuffer.putDouble(phys);
				writeBuffer.rewind();
				channel.write(writeBuffer);
				writeBuffer.rewind();
				count++;
			}

			if (count < 0 || count != (int) count || count * 8 > Integer.MAX_VALUE) {
				throw new AoException(ErrorCode.AO_IMPLEMENTATION_PROBLEM, SeverityFlag.ERROR, 0,
						"value count exceeded max supported block size supported by ODS");
			}

			// values have been calculated and written to an external file
			ODSInsertStatement ins = new ODSInsertStatement(modelCache, "ec");
			ins.setStringVal("iname", "ec_custom_rat_conv");
			ins.setLongVal("cl", (int) count);
			ins.setEnumVal("vt", 6); // ieeefloat8 (little endian; 11 would be
										// big endian)
			ins.setLongLongVal("so", startOffset);
			ins.setLongVal("bs", (int) count * 8);
			ins.setLongVal("vb", (int) count);
			ins.setLongVal("vo", 0);
			ins.setStringVal("fl", customRatConfPath.getFileName().toString());
			ins.setLongLongVal("lc", iidLc);
			ins.execute();
		}
	}

}
