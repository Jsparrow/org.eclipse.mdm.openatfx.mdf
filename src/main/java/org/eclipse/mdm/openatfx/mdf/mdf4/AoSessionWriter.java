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
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.asam.ods.AoException;
import org.asam.ods.ApplicationElement;
import org.asam.ods.ApplicationRelation;
import org.asam.ods.ErrorCode;
import org.asam.ods.InstanceElement;
import org.asam.ods.SeverityFlag;
import org.asam.ods.T_ExternalReference;
import org.eclipse.mdm.openatfx.mdf.ConvertException;
import org.eclipse.mdm.openatfx.mdf.util.BitInputStream;
import org.eclipse.mdm.openatfx.mdf.util.FileUtil;
import org.eclipse.mdm.openatfx.mdf.util.LookupTableHelper;
import org.eclipse.mdm.openatfx.mdf.util.ODSHelper;
import org.eclipse.mdm.openatfx.mdf.util.ODSInsertStatement;
import org.eclipse.mdm.openatfx.mdf.util.ODSModelCache;

/**
 * Main class for writing the MDF4 file content into an ASAM ODS session backed
 * by an ATFX file.
 *
 * @author Christian Rechner
 */
public class AoSessionWriter {

	private static final Log LOG = LogFactory.getLog(AoSessionWriter.class);
	private static final int FLAGS_BUFFER_SIZE = 1_000_000;

	/** The number format having 5 digits used for count formatting */
	private final NumberFormat countFormat;

	/** The XML parser object used for parsing the embedded XML contents */
	private final MDF4XMLParser xmlParser;

	/** helper class for lookup tables */
	private final LookupTableHelper lookupTableHelper;

	/** helper class for previews */
	private PreviewHelper previewHelper;

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
	private boolean skipScaleConversionChannels = false;
	private boolean skipUINT64Channels = false;
	// skip channels with DT_BYTESTR
	// remove this once it is save to import channels with byte stream data
	@Deprecated
	private boolean skipByteStreamChannels = false;

	private boolean writeFlagsFile = true;
	private Path flagFile = null;

	private boolean readOnlyHeader = false;
	private Path customRatConfPath;

	// skip empty channels
    private boolean skipEmptyChannels = false;

	/**
	 * Constructor.
	 */
	public AoSessionWriter() {
		xmlParser = new MDF4XMLParser();
		countFormat = new DecimalFormat("00000");
		lookupTableHelper = new LookupTableHelper();
		previewHelper = new PreviewHelper();
	}

	/**
	 * Appends the content of the MDF4 file to the ASAM ODS session.
	 *
	 * @param modelCache
	 *            The application model cache.
	 * @param idBlock
	 *            The IDBLOCK.
	 * @param props
	 *            Map with additional attributes to be set.
	 * @throws AoException
	 *             Error writing to session.
	 * @throws IOException
	 *             Error reading from MDF file.
	 */
	public void writeTst(ODSModelCache modelCache, IDBLOCK idBlock, Properties props) throws AoException, IOException {
		Path fileName = idBlock.getMdfFilePath().getFileName();
		if (fileName == null) {
			throw new IOException("Unable to obtain file name!");
		}

		try {
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
				// suffix='_123' then 'test.mf4' will be converted to
				// 'test_123.mf4'
				if (props.containsKey("result_suffix")) {
					resultSuffix = props.getProperty("result_suffix"); // default
																		// =
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
				if (props.containsKey("skip_scale_conversion_channels")) {
					skipScaleConversionChannels = Boolean.valueOf(props.getProperty("skip_scale_conversion_channels"));
				}
				if (props.containsKey("skip_uint64_data_channels")) {
					skipUINT64Channels = Boolean.valueOf(props.getProperty("skip_uint64_data_channels"));
				}
				if (props.containsKey("skip_byte_stream_channels")) {
					skipByteStreamChannels = Boolean.valueOf(props.getProperty("skip_byte_stream_channels"));
				}
				if (props.containsKey("write_flags_file")) {
					writeFlagsFile = Boolean.valueOf(props.getProperty("write_flags_file"));
				}
				if (props.containsKey("skip_empty_channels")) {
	                skipEmptyChannels = Boolean.valueOf(props.getProperty("skip_empty_channels"));
	            }
			}

			ODSInsertStatement ins = new ODSInsertStatement(modelCache, "tst");
			ins.setStringVal("iname", FileUtil.stripExtension(fileName.toString()));

			ins.setStringVal("mdf_file_id", idBlock.getIdFile());
			ins.setStringVal("mdf_version_str", idBlock.getIdVers());
			ins.setLongVal("mdf_version", idBlock.getIdVer());
			ins.setStringVal("mdf_program", idBlock.getIdProg());
			ins.setLongVal("mdf_unfin_flags", idBlock.getIdUnfinFlags());
			ins.setLongVal("mdf_custom_unfin_flags", idBlock.getIdCustomUnfinFlags());

			// write properties
			if (props != null) {
				MDF4Util.writeProperites(ins, props);
			}

			// set Relations, to environment
			ins.setLongLongVal("env", 1L);

			long iidTst = ins.execute();

			previewHelper.setCache(modelCache);
			previewHelper.setWriter(this);

			// write 'AoMeasurement' instance
			writeMea(modelCache, iidTst, idBlock);
		} catch (AoException | IOException e) {
			// make sure file with manually calculated values is deleted in case
			// of errors
			if (customRatConfPath != null && Files.exists(customRatConfPath)) {
				try {
					Files.delete(customRatConfPath);
				} catch (IOException e2) {
					LOG.warn("failed to delete file with manually calculated values: '" + customRatConfPath + "'", e2);
				}
			}

			if (flagFile != null && Files.exists(flagFile)) {
				try {
					Files.delete(flagFile);
				} catch (IOException e2) {
					LOG.warn("failed to delete file with exported flags: '" + flagFile + "'", e2);
				}
			}

			// preserve origin error
			throw e;
		}
	}

	/**
	 * Appends the content of the MDF4 file to the ASAM ODS session.
	 *
	 * @param modelCache
	 *            The application model cache.
	 * @param ieTst
	 *            The parent 'AoTest' instance.
	 * @param idBlock
	 *            The IDBLOCK.
	 * @return The created AoMeasurement instance.
	 * @throws AoException
	 *             Error writing to session.
	 * @throws IOException
	 *             Error reading from MDF file.
	 */
	private void writeMea(ODSModelCache modelCache, long iidTst, IDBLOCK idBlock) throws AoException, IOException {
		Path fileName = idBlock.getMdfFilePath().getFileName();
		if (fileName == null) {
			throw new IOException("Unable to obtain file name!");
		}

		// create "AoMeasurement" instance

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

		// meta information
		HDBLOCK hdBlock = idBlock.getHDBlock();
		BLOCK block = hdBlock.getMdCommentBlock();
		if (block instanceof TXBLOCK) {
			ins.setStringVal("desc", ((TXBLOCK) block).getTxData());
		} else if (block instanceof MDBLOCK) {
			xmlParser.writeHDCommentToMea(ins, ((MDBLOCK) block).getMdData());
		}

		Calendar cal = Calendar.getInstance();
		cal.setTimeInMillis(hdBlock.getStartTimeNs() / 1000000);
		if (!hdBlock.isLocalTime() && hdBlock.isTimeFlagsValid()) { // UTC time
																	// given,
																	// calc
																	// local
			cal.add(Calendar.MINUTE, hdBlock.getTzOffsetMin());
			cal.add(Calendar.MINUTE, hdBlock.getDstOffsetMin());
		}
		ins.setDateVal("date_created", ODSHelper.asODSDate(cal.getTime()));
		ins.setDateVal("mea_begin", ODSHelper.asODSDate(cal.getTime()));
		ins.setLongLongVal("start_time_ns", hdBlock.getStartTimeNs());

		ins.setShortVal("local_time", hdBlock.isLocalTime() ? (short) 1 : (short) 0);
		ins.setShortVal("time_offsets_valid", hdBlock.isTimeFlagsValid() ? (short) 1 : (short) 0);
		ins.setShortVal("tz_offset_min", hdBlock.getTzOffsetMin());
		ins.setShortVal("dst_offset_min", hdBlock.getDstOffsetMin());

		ins.setEnumVal("time_quality_class", hdBlock.getTimeClass());
		ins.setShortVal("start_angle_valid", hdBlock.isStartAngleValid() ? (short) 1 : (short) 0);
		ins.setShortVal("start_distance_valid", hdBlock.isStartDistanceValid() ? (short) 1 : (short) 0);

		ins.setDoubleVal("start_angle_rad", hdBlock.getStartAngleRad());
		ins.setDoubleVal("start_distance_m", hdBlock.getStartDistanceM());

		// set Relations, to test
		ins.setLongLongVal("tst", iidTst);

		long iidMea = ins.execute();

		// write file history (FHBLOCK)
		Long[] iidFh = writeFh(modelCache, iidTst, hdBlock);

		// write channel hierarchy (CHBLOCK): not yet supported!
		if (hdBlock.getLnkChFirst() > 0) {
			LOG.warn("Found CHBLOCK, currently not yet supported!");
		}

		// write attachments (ATBLOCK): not yet supported!
		if (hdBlock.getLnkAtFirst() > 0) {
			LOG.warn("Found ATBLOCK, currently not yet supported!");
		}

		if (!readOnlyHeader) {
			writeEv(modelCache, iidTst, iidFh, hdBlock);

			// write submatrices
			Map<String, Integer> meqNames = new HashMap<String, Integer>();
			writeSm(modelCache, iidMea, idBlock, hdBlock, meqNames);
		}
	}

	/**
	 * Writes the content of all FHBLOCKS (file history) to the session.
	 *
	 * @param modelCache
	 *            The application model cache.
	 * @param ieTst
	 *            The parent 'AoTest' instance.
	 * @param hdBlock
	 *            The HDBLOCK.
	 * @throws AoException
	 *             Error writing to session.
	 * @throws IOException
	 *             Error reading from MDF file.
	 */
	private Long[] writeFh(ODSModelCache modelCache, long iidTst, HDBLOCK hdBlock) throws AoException, IOException {
		int no = 1;
		FHBLOCK fhBlock = hdBlock.getFhFirstBlock();
		List<Long> iids = new LinkedList<Long>();
		while (fhBlock != null) {

			ODSInsertStatement ins = new ODSInsertStatement(modelCache, "fh");
			ins.setStringVal("iname", "fh_" + countFormat.format(no));

			// meta information
			Calendar cal = Calendar.getInstance();
			cal.setTimeInMillis(fhBlock.getStartTimeNs() / 1000000);
			if (!fhBlock.isLocalTime() && fhBlock.isTimeFlagsValid()) { // UTC
																		// time
																		// given,
																		// calc
																		// local
				cal.add(Calendar.MINUTE, fhBlock.getTzOffsetMin());
				cal.add(Calendar.MINUTE, fhBlock.getDstOffsetMin());
			}

			ins.setNameValueUnit(ODSHelper.createDateNVU("date", ODSHelper.asODSDate(cal.getTime())));
			ins.setLongLongVal("start_time_ns", fhBlock.getStartTimeNs());
			ins.setShortVal("local_time", fhBlock.isLocalTime() ? (short) 1 : (short) 0);
			ins.setShortVal("time_offsets_valid", fhBlock.isTimeFlagsValid() ? (short) 1 : (short) 0);
			ins.setShortVal("tz_offset_min", fhBlock.getTzOffsetMin());
			ins.setShortVal("dst_offset_min", fhBlock.getDstOffsetMin());
			if (fhBlock.getLnkMdComment() != 0) {
				xmlParser.writeFHCommentToFh(ins, fhBlock.getMdCommentBlock().getMdData());
			}

			// set Relations.
			ins.setLongLongVal("tst", iidTst);
			iids.add(ins.execute());
			no++;
			fhBlock = fhBlock.getFhNextBlock();
		}
		return iids.toArray(new Long[0]);
	}

	/**
	 * Writes the content of all EVBLOCKS (event) to the session.
	 *
	 * @param modelCache
	 *            The application model cache.
	 * @param ieTst
	 *            The parent 'AoTest' instance.
	 * @param hdBlock
	 *            The HDBLOCK.
	 * @throws AoException
	 *             Error writing to session.
	 * @throws IOException
	 *             Error reading from MDF file.
	 */
	private void writeEv(ODSModelCache modelCache, long iidTst, Long[] iidFh, HDBLOCK hdBlock)
			throws AoException, IOException {
		int no = 1;
		EVBLOCK evBlock = hdBlock.getEvFirstBlock();
		while (evBlock != null) {
			if (evBlock.getLnkEvParent() != 0) {
				LOG.warn("Event Hirachy not supported.");
			}

			if (evBlock.getLnkEvRange() != 0) {
				LOG.warn("Event Range not supported.");
			}
			ODSInsertStatement ins = new ODSInsertStatement(modelCache, "ev");
			TXBLOCK name;
			if ((name = evBlock.getEvTxNameBlock()) != null) {
				ins.setStringVal("iname", name.getTxData());
			} else {
				ins.setStringVal("iname", "ev_" + countFormat.format(no));
			}

			// Get Descpription from XMLComment block
			BLOCK mdblock = evBlock.getMdCommentBlock();
			if (mdblock != null) {
				if (mdblock instanceof MDBLOCK) {
					xmlParser.writeEVCommentToEv(ins, ((MDBLOCK) mdblock).getMdData());
				} else if (mdblock instanceof TXBLOCK) {
					ins.setStringVal("desc", ((TXBLOCK) mdblock).getTxData());
				}
			}

			ins.setEnumVal("type", evBlock.getType());

			// System.out.println("Synctype: "+evBlock.getSyncType());

			ins.setEnumVal("sync_type", evBlock.getSyncType());
			ins.setEnumVal("range_type", evBlock.getRangeType());
			ins.setEnumVal("cause", evBlock.getCause());
			ins.setShortVal("flags", evBlock.getFlags());
			ins.setDoubleVal("sync_val", evBlock.getSyncValue());

			// set Relations.
			ins.setLongLongVal("tst", iidTst);
			if (evBlock.getCreatorIndex() < iidFh.length && evBlock.getCreatorIndex() >= 0) {
				ins.setLongLongVal("creator", iidFh[evBlock.getCreatorIndex()]);
			}

			ins.execute();

			no++;
			evBlock = evBlock.getEvNextBlock();
		}
	}

	/**
	 * Write the instances of 'AoSubMatrix'.
	 *
	 * @param modelCache
	 *            The application model cache.
	 * @param iidMea
	 *            The ID of the Parent Measurement.
	 * @param idBlock
	 *            The IDBLOCK.
	 * @param hdBlock
	 *            The HDBLOCK.
	 * @param meqNames
	 *            Map with existing MeasurmentQuantities and their IDs.
	 * @throws AoException
	 *             Error writing to session.
	 * @throws IOException
	 *             Error reading from MDF file.
	 */
	public void writeSm(ODSModelCache modelCache, long iidMea, IDBLOCK idBlock, HDBLOCK hdBlock,
			Map<String, Integer> meqNames) throws AoException, IOException {
		Map<String, Long> meqInstances = new HashMap<String, Long>();
		Map<String, Long> untInstances = new HashMap<String, Long>();
		// iterate over data group blocks
		int grpNo = 1;
		DGBLOCK dgBlock = hdBlock.getDgFirstBlock();
		while (dgBlock != null) {

			// if sorted, only one channel group block is available
			CGBLOCK cgBlock = dgBlock.getCgFirstBlock();

			if (cgBlock != null && cgBlock.getLnkCgNext() > 0) {
				throw new IOException(
						"Only 'sorted' MDF4 files are supported, found 'unsorted' data! [DGBLOCK=" + dgBlock + "]");
			}

			// skip channel groups having no channels (or optionally no values)
			boolean skipNoValues = skipEmptyChannels && cgBlock.getCycleCount() < 1;
			if (cgBlock != null && !skipNoValues) {

				// check flags (not yet supported)
				if (cgBlock.isBusEventChannel()) {
					throw new IOException("Bus event data currently not supported! [DGBLOCK=" + dgBlock + "]");
				}

				// create SubMatrix instance
				ODSInsertStatement ins = new ODSInsertStatement(modelCache, "sm");
				ins.setStringVal("iname", "sm_" + countFormat.format(grpNo));

				// write CGComment
				BLOCK mdblock = cgBlock.getMdCommentBlock();
				if (mdblock != null) {
					if (mdblock instanceof MDBLOCK) {
						xmlParser.writeCGCommentToCg(ins, ((MDBLOCK) mdblock).getMdData());
					} else if (mdblock instanceof TXBLOCK) {
						ins.setStringVal("desc", ((TXBLOCK) mdblock).getTxData());
					}
				}

				// write DGComment
				mdblock = dgBlock.getMdCommentBlock();
				if (mdblock != null) {
					if (mdblock instanceof MDBLOCK) {
						xmlParser.writeDGCommentToCg(ins, ((MDBLOCK) mdblock).getMdData());
					} else if (mdblock instanceof TXBLOCK) {
						ins.setStringVal("dg_desc", ((TXBLOCK) mdblock).getTxData());
					}
				}

				TXBLOCK txAcqName = cgBlock.getTxAcqNameBlock();
				if (txAcqName != null) {
					ins.setStringVal("acq_name", txAcqName.getTxData());
				}
				SIBLOCK siAcqSource = cgBlock.getSiAcqSourceBlock();
				if (siAcqSource != null) {
					writeSiBlock(ins, siAcqSource);
				}

				ins.setLongVal("rows", (int) cgBlock.getCycleCount());
				// Relation to measurement
				ins.setLongLongVal("mea", iidMea);
				long iidSm = ins.execute();

				ApplicationElement aeMea = modelCache.getApplicationElement("mea");
				InstanceElement ieMea = aeMea.getInstanceById(ODSHelper.asODSLongLong(iidMea));

				// write instances of
				// AoMeasurementQuantity,AoLocalColumn,AoExternalReference
				Map<String, Integer> mapMeq = new HashMap<String, Integer>();

				SRBLOCK srBlock = cgBlock.getSrFirstBlock();

				long[] iidPrevSm = previewHelper.createPreviewSubMatrices(ieMea, srBlock);
				writeLc(modelCache, iidMea, iidSm, iidPrevSm, idBlock, dgBlock, cgBlock, mapMeq, meqInstances,
						untInstances, srBlock);
			}

			dgBlock = dgBlock.getDgNextBlock();
			grpNo++;
		}
	}

	/**
	 * Write the instances of 'AoLocalColumn' and 'AoMeasurementQuantity'.
	 *
	 * @param modelCache
	 *            The application model cache.
	 * @param ieMea
	 *            The parent 'AoMeasurement' instance.
	 * @param ieSm
	 *            The parent 'AoSubMatrix' instance.
	 * @param dgBlock
	 *            The DGBLOCK.
	 * @param cgBlock
	 *            The CGBLOCK.
	 * @param srBlock
	 *            Possible SRBLOCK (can be null)
	 * @throws AoException
	 *             Error writing to session.
	 * @throws IOException
	 *             Error reading from MDF file.
	 */
	private void writeLc(ODSModelCache modelCache, long iidMea, long iidSm, long[] iidPrevSm, IDBLOCK idBlock,
			DGBLOCK dgBlock, CGBLOCK cgBlock, Map<String, Integer> meqNames, Map<String, Long> meqInstances,
			Map<String, Long> untInstances, SRBLOCK srBlock) throws AoException, IOException {

		// Performance?
		ApplicationElement aeMea = modelCache.getApplicationElement("mea");
		ApplicationElement aeLc = modelCache.getApplicationElement("lc");
		ApplicationElement aeSm = modelCache.getApplicationElement("sm");
		ApplicationRelation relLcSmLookup = modelCache.getApplicationRelation("lc", "sm", "LookupTable");
		ApplicationRelation relLcSmPrev = modelCache.getApplicationRelation("lc", "sm", "Previews");

		// iterate over channel blocks
		CNBLOCK cnBlock = cgBlock.getCnFirstBlock();
		while (cnBlock != null) {

			if ((cnBlock.getFlags() & 0x02) != 0 && cnBlock.getInvalBitPos() > 0) {
				if (writeFlagsFile) {
					// NOTE: flags are exported within writeEc()!
					LOG.debug("channel with invalid values found, "
							+ "export flags into separate file [CNBLOCK=" + cnBlock + "]");
				} else {
					LOG.debug("skipping channel with invalid values [CNBLOCK=" + cnBlock + "]");
					continue;
				}
			}
			// check invalidation bits (not yet supported)
			if (cnBlock.getLnkComposition() != 0) {
				LOG.warn("Composition of channels not supported! [CNBLOCK=" + cnBlock + "]");
				// throw new IOException("Composition of channels not supported!
				// [CNBLOCK=" + cnBlock + "]");
			}

			// cn_at_reference: attachments TODO
			if (cnBlock.getLnkAtReference().length > 0) {
				LOG.warn("Found channel 'cn_at_reference'>0, not yet supported ");
			}

			// build signal name
			String meqName = readMeqName(cnBlock).trim();

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

			CCBLOCK ccBlock = cnBlock.getCcConversionBlock();

			// check whether channel has to be or shall be skipped
			if (skipScaleConversionChannels && ccBlock != null && ccBlock.hasCCRefs()) {
				LOG.info("Channel '" + meqName + "' with scale conversion rules in CCBlocks skipped: " + ccBlock);
				cnBlock = cnBlock.getCnNextBlock();
				continue;
			} else if (skipByteStreamChannels && 10 == cnBlock.getDataType()) {
				// remove this block once it is save to import channels with
				// byte stream data
				LOG.info("Channel '" + meqName + "' with byte stream data skipped: " + ccBlock);
				cnBlock = cnBlock.getCnNextBlock();
				continue;
			} else if (10 == cnBlock.getDataType() && cnBlock.getLnkComposition() != 0) {
				LOG.info("Channel '" + meqName + "' with composed byte stream data skipped: " + ccBlock);
				cnBlock = cnBlock.getCnNextBlock();
				continue;
			} else if (64 == cnBlock.getBitCount() && (0 /* LEO */ == cnBlock.getDataType()
					|| 1 /* BEO */ == cnBlock.getDataType())) {
				// UNSIGNED 64 bit: if cnBlock represents a time channel (sync_type == 1)
				// => it is considered save to interpret data as signed 64 bit
				// => otherwise either fail or skip
				if (1 != cnBlock.getSyncType()) {
					// this is a data channel with 64 bit unsigned data; it is not possible to represent such data
					// => either throw an error or skip channel
					if (skipUINT64Channels) {
						LOG.info("Channel '" + meqName + "' with unsigned 64 bit data skipped: " + cnBlock);
						cnBlock = cnBlock.getCnNextBlock();
						continue;
					} else {
						throw new IOException("unable to write unsigned 64 bit data channel");
					}
				}
			}

			// create instance of 'AoMeasurementQuantity' (if not yet existing)
			Long iidMeq = meqInstances.get(meqName);
			if (iidMeq == null) {
				iidMeq = createMeasurementQuantity(modelCache, cnBlock, ccBlock, meqName, iidMea, null, untInstances);
				meqInstances.put(meqName, iidMeq);
			}

			// create 'AoLocalColumn' instance
			long iidLc = createLocalColumn(modelCache, dgBlock, cgBlock, cnBlock, ccBlock, meqName, iidSm, iidMeq,
					iidPrevSm, null);
			// relation to previews
			if (iidPrevSm != null) {
				InstanceElement ieLc = aeLc.getInstanceById(ODSHelper.asODSLongLong(iidLc));
				for (long element : iidPrevSm) {
					ieLc.createRelation(relLcSmPrev, aeSm.getInstanceById(ODSHelper.asODSLongLong(element)));
				}
			}

			// create 'AoExternalComponent' instance (if not a string data type
			// (6-9) or VLSD-Channel (Type ==1) or virtual master channel (Type == 3))
			if (!(cnBlock.getDataType() >= 6 && cnBlock.getDataType() <= 9
					|| cnBlock.getChannelType() == 1 || cnBlock.getChannelType() == 3)) {
				writeEc(modelCache, iidLc, idBlock, dgBlock, cgBlock, cnBlock, ccBlock, dgBlock.getLnkData(), 0);
			}

			// create Table for Lookup conversion, if conversion type is 4 to 10
			InstanceElement ieMea = aeMea.getInstanceById(ODSHelper.asODSLongLong(iidMea));
			InstanceElement ieLc = aeLc.getInstanceById(ODSHelper.asODSLongLong(iidLc));

			if (ccBlock != null && ccBlock.getType() >= 4 && ccBlock.getType() <= 10) {
				long iidLookup = createLookupTable(modelCache, ccBlock, ieMea, ieLc);
				InstanceElement ieSmLookup = aeSm.getInstanceById(ODSHelper.asODSLongLong(iidLookup));
				ieLc.createRelation(relLcSmLookup, ieSmLookup);
			}

			// create possible Sample Reduction Measurements
			if (srBlock != null) {
				previewHelper.createPreviewChannels(meqName, idBlock, cgBlock, dgBlock, cnBlock, ccBlock, untInstances);
			}

			// jump to next channel
			cnBlock = cnBlock.getCnNextBlock();
		}
	}

	/**
	 * Create and insert the actual MeasurementQuantity This function also
	 * creates the corresponding unit if it doesn't already exist.
	 * 
	 * @param modelCache
	 *            Cache with model Object
	 * @param cnBlock
	 *            The Channel Block.
	 * @param ccBlock
	 *            The Conversion Block.
	 * @param meqName
	 *            The Name the Measurement will have.
	 * @param iidMea
	 *            The ID of the Measurement this Quantity is related to.
	 * @param mimeType
	 *            The MIME-Type of this MeasurementQuantity. if mimeType==null,
	 *            no MIME-Type is set.
	 * @param untInstances
	 *            Map that contains iids of all created units so far. Key is the
	 *            name of the unit
	 * @return The ID of the created MeasurmentQuantity
	 * @throws IOException
	 *             If an I/O-error occurs.
	 * @throws AoException
	 */
	long createMeasurementQuantity(ODSModelCache modelCache, CNBLOCK cnBlock, CCBLOCK ccBlock, String meqName,
			long iidMea, String mimeType, Map<String, Long> untInstances) throws IOException, AoException {
		int seqRep = getSeqRep(cnBlock, ccBlock);
		double[] genParams = getGenerationParameters(ccBlock);

		ODSInsertStatement ins = new ODSInsertStatement(modelCache, "meq");
		ins.setStringVal("iname", meqName);

		// Write XML MetaData
		BLOCK mdblock = cnBlock.getMdCommentBlock();
		if (mdblock instanceof MDBLOCK) {
			xmlParser.writeCNCommentToMeq(ins, ((MDBLOCK) mdblock).getMdData());
		} else if (mdblock instanceof TXBLOCK) {
			ins.setStringVal("desc", ((TXBLOCK) mdblock).getTxData().trim());
		}

		// MIME-Type
		if (mimeType != null) {
			ins.setStringVal("mt", mimeType);
		}

		boolean expandDataType = genParams != null && genParams.length > 0 && seqRep != 0 && seqRep != 7;
		ins.setEnumVal("dt", getDataType(expandDataType, cnBlock, ccBlock));

		if (ccBlock != null && ccBlock.isPhysicalRangeValid()) {
			ins.setDoubleVal("min", ccBlock.getPhyRangeMin());
			ins.setDoubleVal("max", ccBlock.getPhyRangeMax());
		}
		/*
		 * // CEBLOCK (extension block) info CEBLOCK ceBlock =
		 * cnBlock.getCeblock(); if (ceBlock != null && ceBlock.getCeBlockDim()
		 * != null) { CEBLOCK_DIM ext = ceBlock.getCeBlockDim();
		 * ins.setLongVal("NumberOfModule", ext.getNumberOfModule());
		 * ins.setLongLongVal("Address", ext.getAddress());
		 * ins.setStringVal("DIMDescription", ext.getDescription());
		 * ins.setStringVal("ECUIdent", ext.getEcuIdent()); } else if (ceBlock
		 * != null && ceBlock.getCeBlockVectorCAN() != null) { CEBLOCK_VectorCAN
		 * ext = ceBlock.getCeBlockVectorCAN(); ins.setLongLongVal("CANIndex",
		 * ext.getCanIndex()); ins.setLongLongVal("MessageId",
		 * ext.getMessageId()); ins.setStringVal("MessageName",
		 * ext.getMessageName()); ins.setStringVal("SenderName",
		 * ext.getSenderName()); }
		 */
		ins.setLongLongVal("mea", iidMea);

		SIBLOCK siBlock = cnBlock.getSiSourceBlock();
		if (siBlock != null) {
			writeSiBlock(ins, siBlock);
		}

		// create 'AoUnit' instance if not yet existing
		long uiid = writeUnit(modelCache, cnBlock, ccBlock, untInstances);
		if (uiid != -1) {
			ins.setLongLongVal("unt", uiid);
		}
		long iidMeq = ins.execute();

		return iidMeq;
	}

	/**
	 * Create and insert the actual LocalColumn.
	 * 
	 * @param modelCache
	 *            Cache with model Object
	 * @param dgBlock
	 *            The Data Group Block.
	 * @param cgBlock
	 *            The Channel Group Block.
	 * @param cnBlock
	 *            The Channel Block.
	 * @param ccBlock
	 *            The Conversion Block.
	 * @param lcName
	 *            The Name the Local Column will have.
	 * @param iidMeq
	 *            The ID of the SubMatrix this LocalColumn is related to.
	 * @param iidMeq
	 *            The ID of the MeasurementQuantity this LocalColumn is related
	 *            to.
	 * @param mimeType
	 *            The MIME-Type of this MeasurementQuantity. if mimeType==null,
	 *            no MIME-Type is set.
	 * @return The ID of the created MeasurmentQuantity
	 * @throws IOException
	 *             If an I/O-error occurs.
	 * @throws AoException
	 */
	long createLocalColumn(ODSModelCache modelCache, DGBLOCK dgBlock, CGBLOCK cgBlock, CNBLOCK cnBlock, CCBLOCK ccBlock,
			String lcName, long iidSm, long iidMeq, long[] iidPrevSm, String mimeType) throws IOException, AoException {
		int seqRep = getSeqRep(cnBlock, ccBlock);
		double[] genParams = getGenerationParameters(ccBlock);
		ODSInsertStatement ins = new ODSInsertStatement(modelCache, "lc");
		ins.setStringVal("iname", lcName);
		ins.setLongLongVal("sm", iidSm);

		// MIME-Type
		if (mimeType != null) {
			ins.setStringVal("mt", mimeType);
		}
		// global flag
		ins.setShortVal("glb", (short) 15);

		// sequence_representation: string channels cannot be referenced in ASAM
		// ODS: read and write external
		if (cnBlock.getDataType() >= 6 && cnBlock.getDataType() <= 9 && cnBlock.getChannelType() != 1) { // 6
																											// To
																											// 9:
			// Datatype
			// String
			String[] stringDataValues = readStringDataValues(dgBlock, cgBlock, cnBlock);
			ins.setEnumVal("srp", 0);
			ins.setNameValueUnit(ODSHelper.createStringSeqNVU("val", stringDataValues));
			LOG.info("Unable to reference into MDF4, extracting string values. [Channel=" + lcName + "]");
		} else if (cnBlock.getChannelType() == 1) {
			// Read VLSDChannel values.
			ins.setEnumVal("srp", 0);
			LOG.info("Variable Length Channel! [Channel=" + lcName + "]");
			insertVLSDValues(ins, dgBlock, cgBlock, cnBlock);
		} else {
			ins.setEnumVal("srp", seqRep);
		}

		// independent flag
		short idp = cnBlock.getChannelType() > 0 ? (short) 1 : (short) 0;
		ins.setShortVal("idp", idp);

		// generation parameters
		ins.setDoubleSeq("par", genParams);

		// raw_datatype
		int valueType = getValueType(cnBlock);
		int rawDataType = getRawDataTypeForValueType(valueType, cnBlock);
		ins.setEnumVal("rdt", rawDataType);

		// axistype
		int axistype = cnBlock.getChannelType() == 0 ? 1 : 0;
		ins.setEnumVal("axistype", axistype);
		// minimum/maximum
		if (cnBlock.isValueRangeValid()) {
			ins.setDoubleVal("min", cnBlock.getValRangeMin());
			ins.setDoubleVal("max", cnBlock.getValRangeMax());
		}

		// relation to submatrix
		ins.setLongLongVal("sm", iidSm);
		ins.setLongLongVal("meq", iidMeq);

		long iidLc = ins.execute();
		return iidLc;
	}

	/**
	 * Read values from an VLSD-Channel and write them directly to file.
	 *
	 * @param ins
	 *            The Insertstatement to use.
	 * @param cnBlock
	 *            The VLSD-Channel
	 * @throws IOException
	 */
	private void insertVLSDValues(ODSInsertStatement ins, DGBLOCK dgBlock, CGBLOCK cgBlock, CNBLOCK cnBlock)
			throws IOException {
		BLOCK blk = cnBlock.getDataBlock();
		if (blk == null) {
			// assume empty SDBLOCK
			ins.setNameValueUnit(ODSHelper.createStringSeqNVU("val", new String[0]));
			return;
		} else if (!(blk instanceof SDBLOCK)) {
			throw new RuntimeException("Found VLSD Block with no valid signal data.");
		}
		SDBLOCK sdBlock = (SDBLOCK) blk;

		// start Reading the values.
		SeekableByteChannel sbc = cnBlock.sbc;
		long recordSize = dgBlock.getRecIdSize() + cgBlock.getDataBytes() + cgBlock.getInvalBytes();
		long recStart = dgBlock.getLnkData() + 24L;
		long[] offsets = new long[(int) cgBlock.getCycleCount()];

		long valueBitSize = cnBlock.getBitCount() + cnBlock.getBitOffset();
		int valueByteSize = (int) (valueBitSize % 8 == 0 ? valueBitSize / 8 : valueBitSize / 8 + 1);
		long offset = cnBlock.getByteOffset();
		ByteBuffer bb = ByteBuffer.allocate(valueByteSize);

		// iterate over records and read offsets
		long pos = recStart;
		for (int i = 0; i < cgBlock.getCycleCount(); i++) {
			// read offset
			bb.rewind();
			sbc.position(pos + offset);
			sbc.read(bb);
			bb.rewind();
			offsets[i] = MDF4Util.readValue(cnBlock.getBitOffset(), (int) cnBlock.getBitCount(), bb);
			pos += recordSize;
		}

		// read data
		// iterate over records
		ByteBuffer signals = ByteBuffer.allocate((int) (sdBlock.getLength() - 24));

		long signalpos = cnBlock.getLnkData() + 24L;
		sbc.position(signalpos);

		sbc.read(signals);
		signals.rewind();

		LinkedList<String> list = new LinkedList<String>();
		for (int i = 0; i < cgBlock.getCycleCount(); i++) {
			signals.position((int) offsets[i]);
			int size = (int) MDF4Util.readUInt32(signals.order(ByteOrder.LITTLE_ENDIAN));
			// read record
			byte[] record = new byte[size];
			signals.get(record);

			// build string value and append to list.
			// Switch for different encodings.
			String value;
			switch (cnBlock.getDataType()) {
			case 6: // ISO-8859
				value = MDF4Util.readCharsISO8859(ByteBuffer.wrap(record), record.length);
				break;
			case 7: // UTF-8
				value = MDF4Util.readCharsUTF8(ByteBuffer.wrap(record), record.length);
				break;
			case 8: // UFT-16LE
				value = MDF4Util.readCharsUTF16(ByteBuffer.wrap(record), record.length, true);
				break;
			case 9: // UTF-16BE
				value = MDF4Util.readCharsUTF16(ByteBuffer.wrap(record), record.length, false);
				break;
			default:
				throw new IllegalArgumentException("Illegal String encoding. Other encodings for VLSD not supported.");
			}
			list.add(value);
		}
		ins.setNameValueUnit(ODSHelper.createStringSeqNVU("val", list.toArray(new String[0])));
	}

	/**
	 * Creates a Lookup-Table from the given conversion Block. (must not be
	 * null!)
	 *
	 * @param modelCache
	 * @param cc
	 * @return The iid of the SubMatrix
	 * @throws IOException
	 * @throws AoException
	 */
	private long createLookupTable(ODSModelCache modelCache, CCBLOCK ccBlock, InstanceElement ieMea,
			InstanceElement ieLc) throws IOException, AoException {
		// Scale Lookups are not yet supported.
		if (ccBlock.getType() == 4 || ccBlock.getType() == 5) {
			double[] keys = ccBlock.getSecondValues(true);
			double[] values = ccBlock.getSecondValues(false);
			return lookupTableHelper.createValueToValueTable(modelCache, ieMea, ieLc, keys, values,
					ccBlock.getType() == 4);

		} else if (ccBlock.getType() == 6) {
			double[] minKeys = ccBlock.getThirdValues(0);
			double[] maxKeys = ccBlock.getThirdValues(1);
			double[] values = ccBlock.getThirdValues(2);
			return lookupTableHelper.createValueRangeToValueTable(modelCache, ieMea, ieLc, minKeys, maxKeys, values,
					ccBlock.getDefaultValueDouble());

		} else if (ccBlock.getType() == 7) { // Value to Text/Scale lookup
			double[] keys = ccBlock.getVal();
			String[] values = ccBlock.getValuesForTextTable();
			if (values.length != keys.length) {
				LOG.warn("Number of values and keys for Lookup-Table are not equal!");
			}
			return lookupTableHelper.createValueToTextTable(modelCache, ieMea, ieLc, keys, values,
					ccBlock.getDefaultValue());

		} else if (ccBlock.getType() == 8) { // Value Range to Text/Scale lookup
			double[] keysMin = ccBlock.getSecondValues(true);
			double[] keysMax = ccBlock.getSecondValues(false);
			String[] values = ccBlock.getValuesForTextTable();
			String defaultValue = ccBlock.getDefaultValue();
			return lookupTableHelper.createValueRangeToTextTable(modelCache, ieMea, ieLc, keysMin, keysMax, values,
					defaultValue);

		} else if (ccBlock.getType() == 9) {
			String[] keys = ccBlock.getRefValues();
			double[] values = ccBlock.getValuesForTextToValueTable();
			double defaultValue = ccBlock.getDefaultValueDouble();
			return lookupTableHelper.createTextToValueTable(modelCache, ieMea, ieLc, keys, values, defaultValue);

		} else if (ccBlock.getType() == 10) {
			String[] keys = ccBlock.getSecondTexts(true);
			String[] values = ccBlock.getSecondTexts(false);
			String defaultValue = ccBlock.getDefaultValue();
			return lookupTableHelper.createTextToTextTable(modelCache, ieMea, ieLc, keys, values, defaultValue);
		} else {
			LOG.warn("Unsupported Conversion.");
			return 0;
		}

	}

	/**
	 * Reads String values from a string channel.
	 * 
	 * @param dgBlock
	 *            The data group block.
	 * @param cgBlock
	 *            The channel group block.
	 * @param cnBlock
	 *            The channel block.
	 * @return The values as String-Array.
	 * @throws IOException
	 *             If an input error occurs.
	 */
	private static String[] readStringDataValues(DGBLOCK dgBlock, CGBLOCK cgBlock, CNBLOCK cnBlock) throws IOException {
		List<String> list = new ArrayList<String>();

		SeekableByteChannel sbc = dgBlock.sbc;
		int recordIdOffset = dgBlock.getRecIdSize();
		long mapStart = dgBlock.getLnkData() + 24L;
		long mapSize = (cgBlock.getDataBytes() + recordIdOffset) * cgBlock.getCycleCount();
		ByteBuffer bb = ByteBuffer.allocate((int) mapSize);
		sbc.position(mapStart);
		sbc.read(bb);
		bb.rewind();

		// iterate over records
		for (int i = 0; i < cgBlock.getCycleCount(); i++) {

			// read record
			byte[] record = new byte[(int) (cgBlock.getDataBytes() + recordIdOffset)];
			bb.get(record);

			// skip first bits and read value
			BitInputStream bis = new BitInputStream(record);
			int skipBits = (int) (recordIdOffset * 8L + cnBlock.getByteOffset() * 8L + cnBlock.getBitOffset());
			bis.skip(skipBits);
			byte[] b = bis.readByteArray((int) cnBlock.getBitCount());

			// build string value and append to list.
			// Switch for different encodings.
			String value;
			switch (cnBlock.getDataType()) {
			case 6: // ISO-8859
				value = MDF4Util.readCharsISO8859(ByteBuffer.wrap(b), b.length);
				break;
			case 7: // UTF-8
				value = MDF4Util.readCharsUTF8(ByteBuffer.wrap(b), b.length);
				break;
			case 8: // UFT-16LE
				value = MDF4Util.readCharsUTF16(ByteBuffer.wrap(b), b.length, true);
				break;
			case 9: // UTF-16BE
				value = MDF4Util.readCharsUTF16(ByteBuffer.wrap(b), b.length, false);
				break;
			default:
				throw new IllegalArgumentException("Illegal String encoding.");
			}
			list.add(value);
		}

		return list.toArray(new String[0]);
	}

	/**
	 * Write the instances of 'AoExternalComponent'. One instance is written for
	 * each data block.
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
	 * @param sectionstart
	 *            The link to data, can be a DL, RD or DT Block or HL, DZ, but
	 *            these are not supported.
	 * @param parity
	 *            Used for previews, where only each third record has to be
	 *            addressed. If every record will be addressed, parity has to be
	 *            set to 0. Otherwise valid values are 1, 2 and 3. (e.g. if
	 *            parity==1 the first of each three records will be addressed)
	 *
	 * @throws AoException
	 *             Error writing to session.
	 * @throws IOException
	 *             Error reading from MDF file.
	 */
	void writeEc(ODSModelCache modelCache, long iidLc, IDBLOCK idBlock, DGBLOCK dgBlock, CGBLOCK cgBlock,
			CNBLOCK cnBlock, CCBLOCK ccBlock, long sectionstart, int parity) throws AoException, IOException {

		if (isRatConv2ExtComp(ccBlock)) {
			// NOTE: once CCBLOCK is no longer required, it should be removed
			// from this method's signature!
			createCustomRatConvEC(modelCache, iidLc, idBlock, dgBlock, cgBlock, cnBlock, ccBlock, sectionstart, parity);
		} else {
			ODSInsertStatement ins = new ODSInsertStatement(modelCache, "ec");

			DLBLOCK currdl = null; // current list block
			int dlindex = 0; // index in data list block.
			int totalindex = 0; // nuber of blocks read;
			long currblock = -1;

			switch (BLOCK.getBlockType(idBlock.sbc, sectionstart)) {
			case "##DT":
			case "##RD":
				currblock = sectionstart;
				break;
			case "##DL":
				currdl = DLBLOCK.read(idBlock.sbc, sectionstart);
				currblock = currdl.getLnkDlData()[0];
				// perform check for records over blockborders.
				if (currdl.breaksRecords(cgBlock.getDataBytes() + dgBlock.getRecIdSize() + cgBlock.getInvalBytes())) {
					throw new IOException("This data list cannot be read because records are splitted.");
				}
				break;
			case "##DZ":
			case "##HL":
				throw new IOException("Zipped blocks cannot be parsed into the ODS-Format.");
			}

			while (currblock != -1) {
				ins.setStringVal("iname", "ec_" + countFormat.format(++totalindex));
				Path mdfFilePath = idBlock.getMdfFilePath().getFileName();
				if (mdfFilePath == null) {
					throw new IOException("mdfFilePath must not be null");
				}

				long startOffset = currblock + 24L;
				ins.setLongVal("on", totalindex);
				ins.setStringVal("fl", mdfFilePath.toString());

				int vt = getValueType(cnBlock);
				ins.setEnumVal("vt", vt);
				ins.setLongLongVal("so", startOffset);

				// TODO Strings, write number of Bytes? Spec4/61
				ins.setLongVal("vb", 1); // one value per block

				// Calculate ByteSize and Offset
				long bytesize;
				long valueOffset;
				long recSizeWoInval = cgBlock.getDataBytes() + dgBlock.getRecIdSize();
				if (parity == 0) {
					bytesize = recSizeWoInval + cgBlock.getInvalBytes();
					valueOffset = dgBlock.getRecIdSize() + cnBlock.getByteOffset() + cnBlock.getBitOffset() / 8;
				} else {
					// inval bytes once at the end
					bytesize = 3 * recSizeWoInval + cgBlock.getInvalBytes();
					valueOffset = (parity - 1) * recSizeWoInval + dgBlock.getRecIdSize() + cnBlock.getByteOffset()
							+ cnBlock.getBitOffset() / 8;
				}

				ins.setLongVal("bs", (int) bytesize);
				ins.setLongVal("vo", (int) valueOffset);

				// get Cycle count from Block size.
				int cycleCount = -1;
				if (BLOCK.getBlockType(idBlock.sbc, currblock).equals(DTBLOCK.BLOCK_ID)) {
					cycleCount = (int) ((DTBLOCK.read(idBlock.sbc, currblock).getLength() - 24) / bytesize);
					ins.setLongVal("cl", cycleCount);
				} else if (BLOCK.getBlockType(idBlock.sbc, currblock).equals(RDBLOCK.BLOCK_ID)) {
					cycleCount = (int) ((RDBLOCK.read(idBlock.sbc, currblock).getLength() - 24) / bytesize);
					ins.setLongVal("cl", cycleCount);
				} else {

				}

				// export flags
				exportFlags(ins, idBlock, dgBlock, cgBlock, cnBlock, startOffset, cycleCount);

				// type spec is of type: dt_bit_* => write bit offset (bo) and bit count (bc)
				boolean writeBitProps = vt > 26 && vt < 33;
				short bitOffset = cnBlock.getBitOffset();
				if ((bitOffset != 0 || cnBlock.getBitCount() % 8 != 0) && writeBitProps) {
					ins.setShortVal("bo", bitOffset);
					ins.setShortVal("bc", (short) cnBlock.getBitCount());
				}

				// bind to local column and write instance
				ins.setLongLongVal("lc", iidLc);
				ins.execute();

				// switch to next block or escape
				if (currdl == null) {
					currblock = -1; // just a single data block
				} else {
					// search next block in list.
					dlindex++;
					if (dlindex < currdl.getCount()) { // more blocks in list
						currblock = currdl.getLnkDlData()[dlindex];
					} else {
						// switch to next list, and start with first block?
						if (currdl.getLnkDlNext() > 0) {
							currdl = currdl.getDlNextBlock();
							dlindex = 0;
							currblock = currdl.getLnkDlData()[dlindex];
						} else {
							currblock = -1;
						}
					}
				}
			}
		}
	}

	/**
	 * Create a new Unit for a channel, if needed.
	 * 
	 * @param modelCache
	 *            The ODSModelCache in use.
	 * @param cnblock
	 *            The Channel Block.
	 * @param ccBlock
	 *            The Channel's conversion block.
	 * @param existingUnts
	 *            Map containing Name (key) and ID (Value) of all created units.
	 * @return The id of the corresponding unit.
	 */
	private long writeUnit(ODSModelCache modelCache, CNBLOCK cnblock, CCBLOCK ccBlock, Map<String, Long> existingUnts)
			throws AoException, IOException {

		// create 'AoUnit' instance if not yet existing
		// Get unit name and metadata
		String unitName = "";
		BLOCK unitblk = cnblock.getMdUnitBlock();
		MDBLOCK unitMetadata = null;

		// Search for correct unit in channel CNBLOCK first
		if (unitblk != null) {
			if (unitblk instanceof MDBLOCK) {
				unitName = xmlParser.extractCommentText(((MDBLOCK) unitblk).getMdData());
				unitMetadata = (MDBLOCK) unitblk;
			} else if (unitblk instanceof TXBLOCK) {
				unitName = ((TXBLOCK) unitblk).getTxData().trim();
			}
		} else { // Then look in CCBLOCK
			if (ccBlock != null) {
				unitblk = ccBlock.getMdUnitBlock();
				if (unitblk != null) {
					if (unitblk instanceof MDBLOCK) {
						unitName = xmlParser.extractCommentText(((MDBLOCK) unitblk).getMdData());
						unitMetadata = (MDBLOCK) unitblk;
					} else if (unitblk instanceof TXBLOCK) {
						unitName = ((TXBLOCK) unitblk).getTxData().trim();
					}
				}
			}
		}

		if (unitName.length() > 0) {
			Long iid = existingUnts.get(unitName);
			if (iid == null) {
				// create unit instance
				ODSInsertStatement ins = new ODSInsertStatement(modelCache, "unt");
				ins.setStringVal("iname", unitName);
				ins.setDoubleVal("factor", 1d);
				ins.setDoubleVal("offset", 0d);
				if (unitMetadata != null) {
					xmlParser.writeCNCommentToUnit(ins, unitMetadata.getMdData());
				}
				iid = ins.execute();
				existingUnts.put(unitName, iid);
			}
			return iid;

		}
		return -1;
	}

	/**************************************************************************************
	 * helper methods
	 **************************************************************************************/

	/**
	 * Get the conversion parameters of the conversion block for 1:1, linear or
	 * rational conversion.
	 * 
	 * @param ccBlock
	 *            The Conversion Block.
	 * @return The parameters as Double-Array.
	 * @throws AoException
	 *             in case of errors
	 */
	private static double[] getGenerationParameters(CCBLOCK ccBlock) throws AoException {
		// CCBLOCK may be null, assume explicit
		if (ccBlock == null) {
			return new double[0];
		}

		int formula = ccBlock.getType();

		// '1:1'
		if (formula == 0) {
			return new double[0];
		}
		// 'linear'
		else if (formula == 1) {
			return ccBlock.getVal();
		}
		// 'rational'
		else if (formula == 2) {
			double[] genParams = ccBlock.getVal();
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

		// No parameters otherwise
		return new double[0];
	}

	/**
	 * Writes the content of an SIBLOCK to the session.
	 *
	 * @param ins
	 *            The insert statement to use.
	 * @param siBlock
	 *            The SIBLOCK.
	 * @throws AoException
	 *             Error writing to session.
	 * @throws IOException
	 *             Error reading from MDF file.
	 */
	private void writeSiBlock(ODSInsertStatement ins, SIBLOCK siBlock) throws AoException, IOException {
		// si_tx_name
		TXBLOCK txName = siBlock.getTxNameBlock();
		if (txName != null) {
			ins.setStringVal("src_name", txName.getTxData());
		}
		// si_tx_path
		TXBLOCK txPath = siBlock.getTxPathBlock();
		if (txPath != null) {
			ins.setStringVal("src_path", txPath.getTxData());
		}
		// si_md_comment
		BLOCK block = siBlock.getMdCommentBlock();
		if (block instanceof TXBLOCK) {
			ins.setStringVal("src_cmt", ((TXBLOCK) block).getTxData());
		} else if (block instanceof MDBLOCK) {
			xmlParser.writeSICommentToCg(ins, ((MDBLOCK) block).getMdData());
		}

		ins.setEnumVal("src_type", siBlock.getSourceType());
		ins.setEnumVal("src_bus", siBlock.getBusType());
		ins.setShortVal("src_sim", (short) (siBlock.getFlags() & 1));
	}

	/**
	 * Read the name of a Channel.
	 * 
	 * @param cnBlock
	 *            The Channel Block
	 * @return The name.
	 * @throws IOException
	 */
	private static String readMeqName(CNBLOCK cnBlock) throws IOException {
		TXBLOCK nameblk = cnBlock.getCnTxNameBlock();
		String meqName = "default";
		if (nameblk != null) {
			meqName = nameblk.getTxData().trim();
		} else {
			LOG.warn("No channel name found!");
		}

		return meqName;
	}

	/**
	 * Returns the target ASAM ODS sequence representation for the external
	 * component description.<br/>
	 * List of MDF4 formula types:
	 * <ul>
	 * <li>0 = 1:1 Conversion</li>
	 * <li>1 = parametric, linear</li>
	 * <li>2 = rational conversion</li>
	 * <li>3 = ASAM-MCD2 Text formula</li>
	 * <li>4 = tabular with interpolation</li>
	 * <li>5 = tabular</li>
	 * <li>6 = Value Range to Value tabular lookup</li>
	 * <li>7 = Value to Text/Scale tabular lookup</li>
	 * <li>8 = Value Range to Text/Scale tabular lookup</li>
	 * <li>9 = Text to Value tabular lookup</li>
	 * <li>10 = Text to Text tabular lookup</li>
	 * </ul>
	 *
	 * @author Tobias Leemann
	 * @return The ASAM ODS sequence representation enum value.
	 * @throws ConvertException
	 */
	private static int getSeqRep(CNBLOCK cnBlock, CCBLOCK ccBlock) throws AoException {
		// CCBLOCK may be null, assume explicit with external Component
		if (ccBlock == null) {
			return 7;
		}

		if (cnBlock.getChannelType() == 3) {
			// virtual master channel -> implicit_linear
			return 2;
		}
		int formula = ccBlock.getType();
		// '1:1 conversion formula' => external_component
		if (formula == 0) {
			return 7;
		}
		// 'linear' => 'raw_linear_external'
		else if (formula == 1) {
			return 8;
		}
		// 'rational conversion' => 'external_component'
		else if (formula == 2) {
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
		// 'Text formula' => 'external_component'
		else if (formula == 3) {
			LOG.error("Text formulas are not supported at the moment.");
			return 7;
		}
		// 'ALL lookup conversions' => 'external_component'
		else if (formula <= 10) {
			return 7;
		} else {
			LOG.warn("Unsupported Formula type " + formula);
			return 7;
		}
	}

	/**
	 * Returns the target ASAM ODS external component type specification enum
	 * value for a MDF4 channel description.<br/>
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
		int dt = cnBlock.getDataType();
		int nb = (int) cnBlock.getBitCount();
		int bitOffset = cnBlock.getBitOffset();

		// 0 = unsigned integer LEO
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

		// 1 = unsigned integer BEO
		else if (dt == 1) {
			if (nb == 8 && bitOffset == 0) { // 8 bit: dt_byte
				return 1;
			} else if (nb == 16 && bitOffset == 0) { // 16 bit: dt_ushort_beo
				return 22;
			} else if (nb == 32 && bitOffset == 0) { // 32 bit: dt_ulong_beo
				return 24;
			} else if (nb == 64 && bitOffset == 0) { // 64 bit: dt_longlong_beo
				return 9;
			} else { // variable bit length: dt_bit_uint_beo
				return 30;
			}
		}

		// 2 = signed integer LEO
		else if (dt == 2) {
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

		// 3 = signed integer BEO
		else if (dt == 3) {
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

		// 4 IEEE 754 floating-point format LEO
		else if (dt == 4) {
			if (nb == 32 && bitOffset == 0) { // 32 bit: ieeefloat4
				return 5;
			} else if (nb == 64 && bitOffset == 0) { // 64 bit: ieeefloat8
				return 6;
			} else { // variable bit length: dt_bit_float
				return 31;
			}
		}

		// 5 = IEEE 754 floating-point format BEO
		else if (dt == 5) {
			if (nb == 32 && bitOffset == 0) { // 32 bit: ieeefloat4_beo
				return 10;
			} else if (nb == 64 && bitOffset == 0) { // 64 bit: ieeefloat8_beo
				return 11;
			} else { // variable bit length: dt_bit_float_beo
				return 32;
			}
		}

		// 6-9 = String (NULL terminated): dt_string
		else if (dt == 6 || dt == 7 || dt == 8 || dt == 9) {
			return 12;
		}

		// 10 = Byte Array: dt_bytestr
		else if (dt == 10) {
			return 13;
		} else {
			// TODO
			LOG.warn("Data type " + dt + " is not yet supported.");
			return 13;
		}
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
	private static int getRawDataTypeForValueType(int typeSpec, CNBLOCK cnBlock) throws AoException {
		if (cnBlock.getChannelType() == 3) {
			// virtual master channel (implicit linear), calculated with generation parameters
			return 7;
		}

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
			int dt = cnBlock.getDataType();
			int nb = (int) cnBlock.getBitCount();
			if ((dt == 0 || dt == 1) && nb >= 1 && nb <= 8) { // unsigned byte
				ret = 5; // DT_BYTE
			} else if ((dt == 2 || dt == 3) && nb >= 1 && nb <= 8) { // signed
																		// byte
				ret = 5; // DT_BYTE
			} else if ((dt == 0 || dt == 1) && nb >= 9 && nb <= 16) { // unsigned
																		// short
				ret = 6; // DT_LONG
			} else if ((dt == 2 || dt == 3) && nb >= 9 && nb <= 16) { // signed
																		// short
				ret = 2; // DT_SHORT
			} else if ((dt == 0 || dt == 1) && nb >= 17 && nb <= 32) { // unsigned
																		// int
				ret = 8; // DT_LONGLONG
			} else if ((dt == 2 || dt == 3) && nb >= 17 && nb <= 32) { // int
				ret = 6; // DT_LONG
			} else if ((dt == 0 || dt == 1) && nb >= 33) { // unsigned int >32
															// bit
				ret = 8; // DT_LONGLONG
			} else if ((dt == 2 || dt == 3) && nb >= 33) { // signed int >32 bit
				ret = 8; // DT_LONGLONG
			}
		}
		// dt_bit_float [31], dt_bit_float_beo [32]
		else if (typeSpec == 31 || typeSpec == 32) {
			int dt = cnBlock.getDataType();
			int nb = (int) cnBlock.getBitCount();
			if ((dt == 2 || dt == 3 || dt == 11 || dt == 12 || dt == 15 || dt == 16) && nb == 32) { // ieeefloat4,
				// ieeefloat4_beo
				ret = 3; // DT_FLOAT
			} else if ((dt == 2 || dt == 3 || dt == 11 || dt == 12 || dt == 15 || dt == 16) && nb == 64) { // ieeefloat8,
				// ieeefloat8_beo
				ret = 7; // DT_DOUBLE
			}
			// TODO Non-IEEE floats
		}
		// not found!
		else {
			throw new AoException(ErrorCode.AO_BAD_PARAMETER, SeverityFlag.ERROR, 0,
					"Unsupported typeSpec: " + typeSpec);
		}
		return ret;
	}

	/**
	 * Returns the target ASAM ODS measurement quantity data type for a MDF4
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
	 * @throws AoException in case of errors
	 */
	private static int getDataType(boolean expandDataType, CNBLOCK cnBlock, CCBLOCK ccBlock)
			throws IOException, AoException {
		// CCBLOCK may be null, assume 1:1
		int formula = 0;
		if (ccBlock != null) {
			formula = ccBlock.getType();
		}

		if (cnBlock.getChannelType() == 3) {
			// virtual master channel (implicit linear), calculated with generation parameters
			return 7;
		}

		int dt = cnBlock.getDataType();
		int nb = (int) cnBlock.getBitCount();

		// STRING
		if (dt >= 6 && dt <= 9) {
			return 1; // DT_STRING
		}

		// 1 = parametric, linear
		if (formula == 1) {
			if (nb == 1) {
				// 1 bit should be DT_BOOLEAN, but most of tools do not support
				// this
				return 5; // DT_BYTE
			} else if (nb >= 2 && nb <= 32) {
				return 3; // DT_FLOAT
			} else if (nb >= 33 && nb <= 64) {
				return 7; // DT_DOUBLE
			}
		}
		// 2 = rational conversion
		else if (formula == 2) {
			// values are calculated either way with real (64 bit ieee-754)
			// double values no matter how many bits are used for internal
			// values, therefore it is safe to return DT_DOUBLE here
			return 7; // DT_DOUBLE
		}

		// 0= 1:1 conversion formula (Int = Phys)
		else if (formula == 0) {
			if (dt == 10) { // ByteArray data
				return 11; // DT_BYTESTR
			} else if (dt <= 9 && dt >= 6) { // STRING (6-9)
				return 1; // DT_STRING
			} else if ((dt == 0 || dt == 1) && nb >= 1 && nb <= 8) { // dt_byte
				return expandDataType ? 3 : 5; // [DT_FLOAT] DT_BYTE
			} else if ((dt == 2 || dt == 3) && nb >= 1 && nb <= 8) { // dt_sbyte
				return expandDataType ? 3 : 2; // [DT_FLOAT] DT_SHORT
			} else if ((dt == 0 || dt == 1) && nb >= 9 && nb <= 16) { // dt_ushort,
																		// dt_ushort_beo
				return expandDataType ? 3 : 6; // [DT_FLOAT] DT_LONG
			} else if ((dt == 2 || dt == 3) && nb >= 9 && nb <= 16) { // dt_short,
																		// dt_short_beo
				return expandDataType ? 3 : 2; // [DT_FLOAT] DT_SHORT
			} else if ((dt == 0 || dt == 1) && nb >= 17 && nb <= 32) { // dt_ulong,
																		// dt_ulong_beo
				return expandDataType ? 7 : 8; // [DT_DOUBLE] DT_LONGLONG
			} else if ((dt == 2 || dt == 3) && nb >= 17 && nb <= 32) { // dt_long,
																		// dt_long_beo
				return expandDataType ? 3 : 6; // [DT_FLOAT] DT_LONG
			} else if ((dt == 0 || dt == 1) && nb >= 33) { // unsigned int >32
															// bit
				return expandDataType ? 7 : 8; // [DT_DOUBLE] DT_LONGLONG
			} else if ((dt == 2 || dt == 3) && nb >= 33 && nb <= 64) { // dt_longlong,
																		// dt_longlong_beo
				return expandDataType ? 7 : 8; // [DT_DOUBLE] DT_LONGLONG
			} else if ((dt == 4 || dt == 5) && nb == 32) { // ieeefloat4,
				// ieeefloat4_beo
				return 3; // DT_FLOAT
			} else if ((dt == 4 || dt == 5) && nb == 64) { // ieeefloat8,
				// ieeefloat8_beo
				return 7; // DT_DOUBLE
			}
		} else if (formula == 9 || formula == 10) { // X to text
			return 1; // DT_STRING
		} else if (formula == 4 || formula == 5 || formula == 6) {
			return 7; // DT_DOUBLE
		} else if (formula == 7 || formula == 8) {
			return getRawDataTypeForValueType(getValueType(cnBlock), cnBlock);
		}

		throw new IOException("Unsupported MDF4 datatype: " + cnBlock + "\n " + ccBlock);
	}

	/**
	 * Checks whether channel with rational conversion may be stored as a
	 * 'raw_linear_external'.
	 * <p>
	 * NOTE: This is a workaround to provide channels with formula == 2 with a
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
		} else if (ccBlock.getType() == 2) {
			// rational conversion
			double[] genParams = ccBlock.getVal();
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
	 * NOTE: This is a workaround to provide channels with formula == 2. Such
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
		} else if (ccBlock.getType() == 2) {
			// rational conversion
			double[] genParams = ccBlock.getVal();
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
	 * @param sectionstart
	 *            The link to data, can be a DL, RD or DT Block or HL, DZ, but
	 *            these are not supported.
	 * @param parity
	 *            Used for previews, where only each third record has to be
	 *            addressed. If every record will be addressed, parity has to be
	 *            set to 0. Otherwise valid values are 1, 2 and 3. (e.g. if
	 *            parity==1 the first of each three records will be addressed)
	 *
	 * @throws AoException
	 *             Error writing to session.
	 * @throws IOException
	 *             Error reading from MDF file.
	 */
	@Deprecated
	private void createCustomRatConvEC(ODSModelCache modelCache, long iidLc, IDBLOCK idBlock, DGBLOCK dgBlock,
			CGBLOCK cgBlock, CNBLOCK cnBlock, CCBLOCK ccBlock, long sectionstart, int parity)
			throws AoException, IOException {

		if (parity != 0) {
			throw new AoException(ErrorCode.AO_BAD_PARAMETER, SeverityFlag.ERROR, 0,
					"channel preview with formula == 2 (rational conversion) is currently not supported");
		}

		DLBLOCK currdl = null; // current list block
		int dlindex = 0; // index in data list block.
		long currblock = -1;

		switch (BLOCK.getBlockType(idBlock.sbc, sectionstart)) {
		case "##DT":
		case "##RD":
			currblock = sectionstart;
			break;
		case "##DL":
			currdl = DLBLOCK.read(idBlock.sbc, sectionstart);
			currblock = currdl.getLnkDlData()[0];
			// perform check for records over blockborders.
			if (currdl.breaksRecords(cgBlock.getDataBytes() + dgBlock.getRecIdSize() + cgBlock.getInvalBytes())) {
				throw new IOException("This data list cannot be read because records are splitted.");
			}
			break;
		case "##DZ":
		case "##HL":
			throw new IOException("Zipped blocks cannot be parsed into the ODS-Format.");
		}

		if (currblock < 0) {
			return;
		}

		if (customRatConfPath == null) {
			customRatConfPath = idBlock.getMdfFilePath().resolveSibling("rational_conversion.calc");
			if (!Files.exists(customRatConfPath)) {
				Files.createFile(customRatConfPath);
			}
		}

		try (SeekableByteChannel channel = Files.newByteChannel(customRatConfPath, StandardOpenOption.APPEND)) {
			ByteBuffer writeBuffer = ByteBuffer.wrap(new byte[8]);
			writeBuffer.order(ByteOrder.LITTLE_ENDIAN);
			long startOffset = channel.position();
			long count = 0;
			while (currblock != -1) {
				long vo = dgBlock.getRecIdSize() + cnBlock.getByteOffset() + cnBlock.getBitOffset() / 8;
				long recSizeWoInval = cgBlock.getDataBytes() + dgBlock.getRecIdSize();
				long bs = recSizeWoInval + cgBlock.getInvalBytes();
				long so = currblock + 24L;

				int cl;
				if (BLOCK.getBlockType(idBlock.sbc, currblock).equals(DTBLOCK.BLOCK_ID)) {
					cl = (int) ((DTBLOCK.read(idBlock.sbc, currblock).getLength() - 24) / bs);
				} else if (BLOCK.getBlockType(idBlock.sbc, currblock).equals(RDBLOCK.BLOCK_ID)) {
					cl = (int) ((RDBLOCK.read(idBlock.sbc, currblock).getLength() - 24) / bs);
				} else {
					throw new AoException(ErrorCode.AO_BAD_PARAMETER, SeverityFlag.ERROR, 0,
							"component length 'cl' is not allowed to be unknown");
				}

				short bo = cnBlock.getBitOffset();
				short bc = bo != 0 && cnBlock.getBitCount() % 8 != 0 ? (short) cnBlock.getBitCount() : 0;
				double[] p = ccBlock.getVal();

				if (bo != 0 || bc != 0 || cnBlock.getBitCount() % 8 != 0) {
					throw new AoException(ErrorCode.AO_BAD_PARAMETER, SeverityFlag.ERROR, 0, "bit count '" + bc
							+ "' and bit offset '" + bo + "' is not supported for custom ration conversion");
				}

				int bits = (int) cnBlock.getBitCount();
				int dt = cnBlock.getDataType();
				boolean isInteger = dt > -1 && dt < 4;
				boolean isReal = dt > 3 && dt < 6;
				boolean isUnsigned = dt == 0 || dt == 1;
				ByteOrder byteOrder = dt == 1 || dt == 3 || dt == 5 ? ByteOrder.BIG_ENDIAN : ByteOrder.LITTLE_ENDIAN;
				for (int i = 0; i < cl; i++) {
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

				// switch to next block or escape
				if (currdl == null) {
					currblock = -1; // just a single data block
				} else {
					// search next block in list.
					dlindex++;
					if (dlindex < currdl.getCount()) { // more blocks in list
						currblock = currdl.getLnkDlData()[dlindex];
					} else {
						// switch to next list, and start with first block?
						if (currdl.getLnkDlNext() > 0) {
							currdl = currdl.getDlNextBlock();
							dlindex = 0;
							currblock = currdl.getLnkDlData()[dlindex];
						} else {
							currblock = -1;
						}
					}
				}

				// switch to next block or escape
				if (currdl == null) {
					currblock = -1; // just a single data block
				} else {
					// search next block in list.
					dlindex++;
					if (dlindex < currdl.getCount()) { // more blocks in list
						currblock = currdl.getLnkDlData()[dlindex];
					} else {
						// switch to next list, and start with first block?
						if (currdl.getLnkDlNext() > 0) {
							currdl = currdl.getDlNextBlock();
							dlindex = 0;
							currblock = currdl.getLnkDlData()[dlindex];
						} else {
							currblock = -1;
						}
					}
				}
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

			// export flags
			exportFlags(ins, idBlock, dgBlock, cgBlock, cnBlock, startOffset, count);

			ins.execute();
		}
	}

	/**
	 * Exports ODS compliant flags into a separate file.
	 *
	 * @param ins  flag file an start offset will be added, not null
	 * @param idBlock  the {@link IDBLOCK}, not null
	 * @param dgBlock  the {@link DGBLOCK}, not null
	 * @param cgBlock  the {@link CGBLOCK}, not null
	 * @param cnBlock  the {@link CNBLOCK}, not null
	 * @param startOffset  start of the next record
	 * @param count  how many records to read
	 * @throws IOException  in case of errors
	 */
	private void exportFlags(ODSInsertStatement ins, IDBLOCK idBlock, DGBLOCK dgBlock, CGBLOCK cgBlock, CNBLOCK cnBlock,
			long startOffset, long count) throws IOException {
		if ((cnBlock.getFlags() & 0x02) == 0 || cnBlock.getInvalBitPos() < 1 || count < 1) {
			// either invalidation bit not set or deactivated or no values available -> nothing to do
			return;
		}

		try (SeekableByteChannel flagsChannel = loadFlagsFileChannel(idBlock)) {
			ins.setStringVal("ffl", flagFile.getFileName().toString());
			ins.setLongLongVal("fso", flagsChannel.position());

			int dt = cnBlock.getDataType();
			ByteOrder byteOrder = dt == 1 || dt == 3 || dt == 5 ? ByteOrder.BIG_ENDIAN : ByteOrder.LITTLE_ENDIAN;
			ByteBuffer flagsBuffer = ByteBuffer.allocate(FLAGS_BUFFER_SIZE);
			ByteBuffer bb = ByteBuffer.allocate(1);
			flagsBuffer.order(byteOrder);
			bb.order(byteOrder);

			long recordSize = dgBlock.getRecIdSize() + cgBlock.getDataBytes() + cgBlock.getInvalBytes();
			long flabBytesOffset = dgBlock.getRecIdSize() + cgBlock.getDataBytes() + (cnBlock.getInvalBitPos() >> 3);
			int bitOffset = ((int) cnBlock.getInvalBitPos()) & 0x07;
			for (int i = 0; i < count; i++) {
				// read current flag byte
				bb.rewind();
				idBlock.sbc.position(startOffset + recordSize * i + flabBytesOffset);
				idBlock.sbc.read(bb);
				bb.rewind();

				// convert flag bit to ODS flag and add to write buffer
				flagsBuffer.putShort((bb.get() & (1 << bitOffset)) != 0 ? 0 : (short) 15);

				// write flags if buffer is full
				if (flagsBuffer.position() % FLAGS_BUFFER_SIZE == 0) {
					flagsBuffer.rewind();
					flagsChannel.write(flagsBuffer);
					flagsBuffer.rewind();
				}
			}

			// write remaining flag bytes
			if (flagsBuffer.position() > 0) {
				int pos = flagsBuffer.position();
				flagsBuffer.rewind();
				flagsBuffer.limit(pos);
				flagsChannel.write(flagsBuffer);
			}
		}
	}

	/**
	 * Creates a {@link SeekableByteChannel} for the flags file.
	 *
	 * @param idBlock  used to resolve target flags file, not null
	 * @return  the {@link SeekableByteChannel}, not null
	 * @throws IOException  if unable to create a flags file
	 */
	private SeekableByteChannel loadFlagsFileChannel(IDBLOCK idBlock) throws IOException {
		if (flagFile == null) {
			flagFile = idBlock.getMdfFilePath().resolveSibling("flags.bin");
			while (Files.exists(flagFile)) {
				flagFile = flagFile.resolveSibling("flags_" + UUID.randomUUID().toString().split("-")[0] + ".bin");
			}

			Files.createFile(flagFile);
		}

		return Files.newByteChannel(flagFile, StandardOpenOption.APPEND);
	}

}
