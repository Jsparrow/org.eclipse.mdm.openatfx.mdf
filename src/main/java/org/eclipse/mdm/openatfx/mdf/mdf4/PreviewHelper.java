/*
 * Copyright (c) 2016 Audi AG
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package org.eclipse.mdm.openatfx.mdf.mdf4;

import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

import org.asam.ods.AoException;
import org.asam.ods.InstanceElement;
import org.asam.ods.InstanceElementIterator;
import org.asam.ods.Relationship;
import org.eclipse.mdm.openatfx.mdf.util.ODSHelper;
import org.eclipse.mdm.openatfx.mdf.util.ODSInsertStatement;
import org.eclipse.mdm.openatfx.mdf.util.ODSModelCache;

/**
 * Helper class to convert Sample Reduction blocks to a ASAM ODS 'AoMeasurement'.
 *
 * @author Tobias Leemann
 */
class PreviewHelper {

	// the cached lookup instance element
	private long previewMeaiid=Long.MIN_VALUE;
	private ODSModelCache cache;
	private AoSessionWriter writer;

	//The SRBLOCKs used.
	private SRBLOCK[] srBlocks;

	//The smIids in use.
	private Long[] smIids;

	private Map<String, Long> meqInstances = new HashMap<String, Long>();

	private int generationCount=0;


	public synchronized void setCache(ODSModelCache cache){
		this.cache = cache;
	}

	public synchronized void setWriter(AoSessionWriter writer){
		this.writer = writer;
	}

	/**
	 * Creates a preview Measurment if none exists.
	 * @param ieMea The InstanceElement of the Main Measurement
	 * @throws AoException If an ASAM ODS error occurs.
	 */
	private synchronized void createMeasurementIfNeeded(InstanceElement ieMea) throws AoException{
		// create 'AoMeasurement' instance (if not yet existing)
		if (previewMeaiid==Long.MIN_VALUE){
			// lookup parent 'AoTest' instance
			InstanceElementIterator iter = ieMea.getRelatedInstancesByRelationship(Relationship.FATHER, "*");
			InstanceElement ieTst = iter.nextOne();
			iter.destroy();
			String meaName = ieMea.getName() + "_previews";
			ODSInsertStatement ins = new ODSInsertStatement(cache, "mea");
			ins.setStringVal("iname", meaName);
			ins.setStringVal("mt", "application/x-asam.aomeasurement.mdf_preview");
			ins.setNameValueUnit(ieMea.getValue("date_created"));
			ins.setNameValueUnit(ieMea.getValue("mea_begin"));
			ins.setNameValueUnit(ieMea.getValue("mea_end"));
			ins.setLongLongVal("tst", ODSHelper.asJLong(ieTst.getId()));
			previewMeaiid =ins.execute();
		}
	}

	/**
	 * Creates Previews for a Channel in all SubMatrix elements created by createPreviewSubMatrices. This method
	 * must be invoked after createPreviewSubMatrices.
	 * @param channelName The name of the Channel the preview will be created for.
	 * @param idBlock The IDBLOCK.
	 * @param cgBlock The CGBLOCK of the Channel.
	 * @param dgBlock The DGBLOCK of the Channel.
	 * @param cnBlock The CNBLOCK of this Channel.
	 * @param ccBlock The CCBLOCK of the Channel.
	 * @param untInstances MapContaining all existing Units.
	 * @throws AoException
	 * @throws IOException
	 */
	public synchronized void createPreviewChannels(String channelName, IDBLOCK idBlock, CGBLOCK cgBlock,
			DGBLOCK dgBlock, CNBLOCK cnBlock, CCBLOCK ccBlock, Map<String, Long> untInstances) throws AoException, IOException{
		if(smIids == null){
			throw new IOException("Preview IIDS not set.");
		}
		if(srBlocks == null){
			throw new IOException("Preview srBlocks not set.");
		}
		if(writer== null){
			throw new IOException("Preview writer not set.");
		}
		String[] nameExtensions = {"average", "maximum", "minimum"};
		for(int reductionNo = 0; reductionNo < srBlocks.length; reductionNo++){
			for(int i = 0; i < 3; i++){
				String extendedName = channelName+"_"+nameExtensions[i];
				//create MeasurmentQuantity if needed
				Long iidMeq = meqInstances.get(extendedName);
				if(iidMeq == null){
					iidMeq = writer.createMeasurementQuantity(cache, cnBlock, ccBlock, extendedName,
							previewMeaiid, "application/x-asam.aomeasurementquantity.mdf_preview." + nameExtensions[i], untInstances);
					meqInstances.put(extendedName, iidMeq);
				}
				//create AoLocalColumns
				long iidLc = writer.createLocalColumn(cache, dgBlock, cgBlock, cnBlock, ccBlock, extendedName,
						smIids[reductionNo], iidMeq, null, "application/x-asam.aolocalcolumn.mdf_preview." + nameExtensions[i]);

				//create AoExternalComponents
				writer.writeEc(cache, iidLc, idBlock, dgBlock, cgBlock, cnBlock, srBlocks[reductionNo].getLnkRdData(), i+1);
			}
		}

	}

	/**
	 * Create SubMatices for all SRBLOCKs linked from <code>srBlock</code>
	 * This method must be called before <code>createPreviewChannels()</code>.
	 * @param ieMea The instance element of the main Measurement.
	 * @param srBlock The first SRBLOCK in the list.
	 * @return The IDs of the created SubMatrix elements.
	 * @throws AoException
	 * @throws IOException
	 */
	public synchronized long[] createPreviewSubMatrices(InstanceElement ieMea, SRBLOCK srBlock) throws AoException, IOException{
		if(srBlock == null){
			return null;
			//nothing to do
		}

		createMeasurementIfNeeded(ieMea);

		// create AoSubMatrix instance
		LinkedList<SRBLOCK> srBlocks = new LinkedList<SRBLOCK>();
		LinkedList<Long> iids = new LinkedList<Long>();
		while(srBlock !=null){
			ODSInsertStatement ins = new ODSInsertStatement(cache, "sm");
			ins.setStringVal("iname", getPrevName(srBlock));
			ins.setStringVal("mt", "application/x-asam.aosubmatrix.mdf_preview");
			ins.setLongVal("rows", (int) srBlock.getCycleCount());
			ins.setLongLongVal("mea", previewMeaiid);
			long smIid =ins.execute();
			srBlocks.add(srBlock);
			iids.add(smIid);
			srBlock = srBlock.getSrNextBlock();
		}
		this.srBlocks = srBlocks.toArray(new SRBLOCK[0]);
		smIids = iids.toArray(new Long[0]);
		generationCount++;
		long[] ret = new long[smIids.length];
		for(int i = 0; i < smIids.length; i++){
			ret[i] = smIids[i];
		}
		return ret;
	}


	/**
	 * Create a unique Channel name from an SRBLOCK.
	 * @param srBlock The SRBLOCK
	 * @return The name.
	 */
	private String getPrevName(SRBLOCK srBlock){
		String ret = "reduction_group"+generationCount+"_";
		ret += String.valueOf(srBlock.getInterval());
		switch(srBlock.getSyncType()){
		case 1:
			ret+="s";
			break;
		case 2:
			ret+="rad";
			break;
		case 3:
			ret+="m";
			break;
		case 4:
			ret+="records";
			break;
		default:
			break;
		}
		return ret;
	}

}
