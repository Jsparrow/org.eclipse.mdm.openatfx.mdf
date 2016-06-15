/*
 * Copyright (c) 2016 Audi AG
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package org.eclipse.mdm.openatfx.mdf.util;

import java.io.IOException;

import org.asam.ods.AoException;
import org.asam.ods.ApplicationElement;
import org.asam.ods.ApplicationRelation;
import org.asam.ods.InstanceElement;
import org.asam.ods.InstanceElementIterator;
import org.asam.ods.NameValueUnit;
import org.asam.ods.Relationship;

/**
 * Helper class to convert the MDF lookup tables to a ASAM ODS 'AoMeasurement'.
 *
 * @author Christian Rechner
 */
public class LookupTableHelper {

	// the cached lookup instance element
	private long lookupMeaIid=-1L;

	private InstanceElement lookupMeaIe ;

	@Deprecated
	public synchronized void createMCD2TextTableMeasurement(ODSModelCache modelCache, InstanceElement ieMea,
			InstanceElement ieLc, double[] keys, String[] values) throws AoException, IOException {
		ApplicationElement aeMea = modelCache.getApplicationElement("mea");
		ApplicationElement aeMeq = modelCache.getApplicationElement("meq");
		ApplicationElement aeSm = modelCache.getApplicationElement("sm");
		ApplicationElement aeLc = modelCache.getApplicationElement("lc");
		ApplicationRelation relMeaTst = modelCache.getApplicationRelation("mea", "tst", "tst");
		ApplicationRelation relSmMea = modelCache.getApplicationRelation("sm", "mea", "mea");
		ApplicationRelation relSmLc = modelCache.getApplicationRelation("sm", "lc", "lcs");
		ApplicationRelation relMeaMeq = modelCache.getApplicationRelation("mea", "meq", "meqs");
		ApplicationRelation relLcMeq = modelCache.getApplicationRelation("lc", "meq", "meq");
		String lcName = ieLc.getName();

		// create 'AoMeasurement' instance (if not yet existing)
		if (lookupMeaIe == null) {
			// lookup parent 'AoTest' instance
			InstanceElementIterator iter = ieMea.getRelatedInstancesByRelationship(Relationship.FATHER, "*");
			InstanceElement ieTst = iter.nextOne();
			iter.destroy();

			String meaName = ieMea.getName() + "_lookup";
			lookupMeaIe = aeMea.createInstance(meaName);
			lookupMeaIe.setValue(ODSHelper.createStringNVU("mt", "application/x-asam.aomeasurement.lookup"));
			lookupMeaIe.setValue(ieMea.getValue("date_created"));
			lookupMeaIe.setValue(ieMea.getValue("mea_begin"));
			lookupMeaIe.setValue(ieMea.getValue("mea_end"));
			lookupMeaIe.createRelation(relMeaTst, ieTst);
		}

		// create 'AoSubMatrix' instance
		InstanceElement ieSm = aeSm.createInstance(lcName);
		ieSm.setValue(ODSHelper.createStringNVU("mt", "application/x-asam.aosubmatrix.lookup.value_to_text"));
		ieSm.setValue(ODSHelper.createLongNVU("rows", keys.length));
		ieSm.createRelation(relSmMea, lookupMeaIe);

		// create 'AoLocalColumn' instance for key
		NameValueUnit[] nvuLcKey = new NameValueUnit[7];
		nvuLcKey[0] = ODSHelper.createStringNVU("mt", "application/x-asam.aolocalcolumn.lookup.key");
		nvuLcKey[1] = ieLc.getValue("srp");
		nvuLcKey[2] = ieLc.getValue("par");
		nvuLcKey[3] = ODSHelper.createShortNVU("idp", (short) 0);
		nvuLcKey[4] = ODSHelper.createShortNVU("glb", (short) 15);
		nvuLcKey[5] = ODSHelper.createEnumNVU("axistype", 0);
		nvuLcKey[6] = ODSHelper.createDoubleSeqNVU("val", keys);
		InstanceElement ieLcKey = aeLc.createInstance(lcName + "_key");
		ieLcKey.setValueSeq(nvuLcKey);
		ieSm.createRelation(relSmLc, ieLcKey);

		// create 'AoMeasurementQuantity' instance for key
		InstanceElement ieMeqKey = aeMeq.createInstance(lcName + "_key");
		ieMeqKey.setValue(ODSHelper.createStringNVU("mt", "application/x-asam.aomeasurementquantity.lookup.key"));
		ieMeqKey.setValue(ODSHelper.createEnumNVU("dt", 7));
		lookupMeaIe.createRelation(relMeaMeq, ieMeqKey);
		ieLcKey.createRelation(relLcMeq, ieMeqKey);

		// create 'AoLocalColumn' instance for values
		NameValueUnit[] nvuLcValues = new NameValueUnit[6];
		nvuLcValues[0] = ODSHelper.createStringNVU("mt", "application/x-asam.aolocalcolumn.lookup.value");
		nvuLcValues[1] = ODSHelper.createEnumNVU("srp", 0);
		nvuLcValues[2] = ODSHelper.createShortNVU("idp", (short) 0);
		nvuLcValues[3] = ODSHelper.createShortNVU("glb", (short) 15);
		nvuLcValues[4] = ODSHelper.createEnumNVU("axistype", 1);
		nvuLcValues[5] = ODSHelper.createStringSeqNVU("val", values);
		InstanceElement ieLcValues = aeLc.createInstance(lcName + "_value");
		ieLcValues.setValueSeq(nvuLcValues);
		ieSm.createRelation(relSmLc, ieLcValues);

		// create 'AoMeasurementQuantity' instance for text
		InstanceElement ieMeqValues = aeMeq.createInstance(lcName + "_value");
		ieMeqValues.setValue(ODSHelper.createStringNVU("mt", "application/x-asam.aomeasurementquantity.lookup.value"));
		ieMeqValues.setValue(ODSHelper.createEnumNVU("dt", 1));
		lookupMeaIe.createRelation(relMeaMeq, ieMeqValues);
		ieLcValues.createRelation(relLcMeq, ieMeqValues);
	}

	@Deprecated
	public synchronized void createMCD2TextRangeTableMeasurement(ODSModelCache modelCache, InstanceElement ieMea,
			InstanceElement ieLc, double[] keysMin, double[] keysMax, String[] values, String defaultValue)
					throws AoException, IOException {
		ApplicationElement aeMea = modelCache.getApplicationElement("mea");
		ApplicationElement aeMeq = modelCache.getApplicationElement("meq");
		ApplicationElement aeSm = modelCache.getApplicationElement("sm");
		ApplicationElement aeLc = modelCache.getApplicationElement("lc");
		ApplicationRelation relMeaTst = modelCache.getApplicationRelation("mea", "tst", "tst");
		ApplicationRelation relSmMea = modelCache.getApplicationRelation("sm", "mea", "mea");
		ApplicationRelation relSmLc = modelCache.getApplicationRelation("sm", "lc", "lcs");
		ApplicationRelation relMeaMeq = modelCache.getApplicationRelation("mea", "meq", "meqs");
		ApplicationRelation relLcMeq = modelCache.getApplicationRelation("lc", "meq", "meq");
		String lcName = ieLc.getName();

		// create 'AoMeasurement' instance (if not yet existing)
		if (lookupMeaIe == null) {
			// lookup parent 'AoTest' instance
			InstanceElementIterator iter = ieMea.getRelatedInstancesByRelationship(Relationship.FATHER, "*");
			InstanceElement ieTst = iter.nextOne();
			iter.destroy();

			String meaName = ieMea.getName() + "_lookup";
			lookupMeaIe = aeMea.createInstance(meaName);
			lookupMeaIe.setValue(ODSHelper.createStringNVU("mt", "application/x-asam.aomeasurement.lookup"));
			lookupMeaIe.setValue(ieMea.getValue("date_created"));
			lookupMeaIe.setValue(ieMea.getValue("mea_begin"));
			lookupMeaIe.setValue(ieMea.getValue("mea_end"));
			lookupMeaIe.createRelation(relMeaTst, ieTst);
		}

		// create 'AoSubMatrix' instance
		InstanceElement ieSm = aeSm.createInstance(lcName);
		ieSm.setValue(ODSHelper.createStringNVU("mt", "application/x-asam.aosubmatrix.lookup.value_range_to_value"));
		ieSm.setValue(ODSHelper.createLongNVU("rows", values.length));
		ieSm.createRelation(relSmMea, lookupMeaIe);

		// create 'AoLocalColumn' instance for key min
		NameValueUnit[] nvuLcKeyMin = new NameValueUnit[7];
		nvuLcKeyMin[0] = ODSHelper.createStringNVU("mt", "application/x-asam.aolocalcolumn.lookup.key_min");
		nvuLcKeyMin[1] = ieLc.getValue("srp");
		nvuLcKeyMin[2] = ieLc.getValue("par");
		nvuLcKeyMin[3] = ODSHelper.createShortNVU("idp", (short) 0);
		nvuLcKeyMin[4] = ODSHelper.createShortNVU("glb", (short) 15);
		nvuLcKeyMin[5] = ODSHelper.createEnumNVU("axistype", 0);
		nvuLcKeyMin[6] = ODSHelper.createDoubleSeqNVU("val", keysMin);
		InstanceElement ieLcKeyMin = aeLc.createInstance(lcName + "_key_min");
		ieLcKeyMin.setValueSeq(nvuLcKeyMin);
		ieSm.createRelation(relSmLc, ieLcKeyMin);

		// create 'AoMeasurementQuantity' instance for key min
		InstanceElement ieMeqKeyMin = aeMeq.createInstance(lcName + "_key_min");
		ieMeqKeyMin.setValue(ODSHelper.createStringNVU("mt", "application/x-asam.aomeasurementquantity.lookup.key_min"));
		ieMeqKeyMin.setValue(ODSHelper.createEnumNVU("dt", 7));
		lookupMeaIe.createRelation(relMeaMeq, ieMeqKeyMin);
		ieLcKeyMin.createRelation(relLcMeq, ieMeqKeyMin);

		// create 'AoLocalColumn' instance for key max
		NameValueUnit[] nvuLcKeyMax = new NameValueUnit[7];
		nvuLcKeyMax[0] = ODSHelper.createStringNVU("mt", "application/x-asam.aolocalcolumn.lookup.key_max");
		nvuLcKeyMax[1] = ieLc.getValue("srp");
		nvuLcKeyMax[2] = ieLc.getValue("par");
		nvuLcKeyMax[3] = ODSHelper.createShortNVU("idp", (short) 0);
		nvuLcKeyMax[4] = ODSHelper.createShortNVU("glb", (short) 15);
		nvuLcKeyMax[5] = ODSHelper.createEnumNVU("axistype", 0);
		nvuLcKeyMax[6] = ODSHelper.createDoubleSeqNVU("val", keysMax);
		InstanceElement ieLcKeyMax = aeLc.createInstance(lcName + "_key_max");
		ieLcKeyMax.setValueSeq(nvuLcKeyMax);
		ieSm.createRelation(relSmLc, ieLcKeyMax);

		// create 'AoMeasurementQuantity' instance for key max
		InstanceElement ieMeqKeyMax = aeMeq.createInstance(lcName + "_key_max");
		ieMeqKeyMax.setValue(ODSHelper.createStringNVU("mt", "application/x-asam.aomeasurementquantity.lookup.key_max"));
		ieMeqKeyMax.setValue(ODSHelper.createEnumNVU("dt", 7));
		lookupMeaIe.createRelation(relMeaMeq, ieMeqKeyMax);
		ieLcKeyMax.createRelation(relLcMeq, ieMeqKeyMax);

		// create 'AoLocalColumn' instance for values
		NameValueUnit[] nvuLcValues = new NameValueUnit[6];
		nvuLcValues[0] = ODSHelper.createStringNVU("mt", "application/x-asam.aolocalcolumn.lookup.value");
		nvuLcValues[1] = ODSHelper.createEnumNVU("srp", 0);
		nvuLcValues[2] = ODSHelper.createShortNVU("idp", (short) 0);
		nvuLcValues[3] = ODSHelper.createShortNVU("glb", (short) 15);
		nvuLcValues[4] = ODSHelper.createEnumNVU("axistype", 1);
		nvuLcValues[5] = ODSHelper.createStringSeqNVU("val", values);
		InstanceElement ieLcValues = aeLc.createInstance(lcName + "_value");
		ieLcValues.setValueSeq(nvuLcValues);
		ieSm.createRelation(relSmLc, ieLcValues);

		// create 'AoMeasurementQuantity' instance for value
		InstanceElement ieMeqValues = aeMeq.createInstance(lcName + "_value");
		ieMeqValues.setValue(ODSHelper.createStringNVU("mt", "application/x-asam.aomeasurementquantity.lookup.value"));
		ieMeqValues.setValue(ODSHelper.createEnumNVU("dt", 1)); //Data type string
		lookupMeaIe.createRelation(relMeaMeq, ieMeqValues);
		ieLcValues.createRelation(relLcMeq, ieMeqValues);

		// create 'AoLocalColumn' instance for default value
		//        NameValueUnit[] nvuLcDefValue = new NameValueUnit[6];
		//        nvuLcDefValue[0] = ODSHelper.createStringNVU("mt", "application/x-asam.aolocalcolumn.lookup.default_value");
		//        nvuLcDefValue[1] = ODSHelper.createEnumNVU("srp", 1);
		//        nvuLcDefValue[2] = ODSHelper.createShortNVU("idp", (short) 0);
		//        nvuLcDefValue[3] = ODSHelper.createShortNVU("glb", (short) 15);
		//        nvuLcDefValue[4] = ODSHelper.createEnumNVU("axistype", 1);
		//        nvuLcDefValue[5] = ODSHelper.createStringSeqNVU("val", new String[] { defaultValue });
		//        InstanceElement ieLcDefValue = aeLc.createInstance(lcName + "_default_value");
		//        ieLcDefValue.setValueSeq(nvuLcDefValue);
		//        ieSm.createRelation(relSmLc, ieLcDefValue);

		// create 'AoMeasurementQuantity' instance for default value
		InstanceElement ieMeqDefValue = aeMeq.createInstance(lcName + "_default_value");
		ieMeqDefValue.setValue(ODSHelper.createStringNVU("mt",
				"application/x-asam.aomeasurementquantity.lookup.default_value"));
		ieMeqDefValue.setValue(ODSHelper.createEnumNVU("dt", 1));
		lookupMeaIe.createRelation(relMeaMeq, ieMeqDefValue);
		//        ieLcDefValue.createRelation(relLcMeq, ieMeqDefValue);
	}

	private synchronized void createMeasurmentIfNeeded(ODSModelCache modelCache, InstanceElement ieMea) throws AoException{
		// create 'AoMeasurement' instance (if not yet existing)
		if (lookupMeaIid == -1L) {
			// lookup parent 'AoTest' instance
			InstanceElementIterator iter = ieMea.getRelatedInstancesByRelationship(Relationship.FATHER, "*");
			InstanceElement ieTst = iter.nextOne();
			iter.destroy();
			long iidTst = ODSHelper.asJLong(ieTst.getId());
			/*String meaName = ieMea.getName() + "_lookup";
            this.lookupMeaIe = aeMea.createInstance(meaName);
            this.lookupMeaIe.setValue(ODSHelper.createStringNVU("mt", "application/x-asam.aomeasurement.lookup"));
            this.lookupMeaIe.setValue(ieMea.getValue("date_created"));
            this.lookupMeaIe.setValue(ieMea.getValue("mea_begin"));
            this.lookupMeaIe.setValue(ieMea.getValue("mea_end"));
            this.lookupMeaIe.createRelation(relMeaTst, ieTst);*/

			ODSInsertStatement ins = new ODSInsertStatement(modelCache, "mea");
			ins.setStringVal("iname", ieMea.getName() + "_lookup");
			ins.setStringVal("mt", "application/x-asam.aomeasurement.lookup");
			ins.setNameValueUnit(ieMea.getValue("date_created"));
			ins.setNameValueUnit(ieMea.getValue("mea_begin"));
			ins.setNameValueUnit(ieMea.getValue("mea_end"));
			ins.setLongLongVal("tst", iidTst);
			lookupMeaIid = ins.execute();
		}
	}

	public synchronized long createValueToValueTable(ODSModelCache modelCache, InstanceElement ieMea,
			InstanceElement ieLc, double[] keys, double[] values, boolean interpolate)
					throws AoException, IOException {
		return createTable(modelCache, ieMea, ieLc, keys, null, values, null, 7, 7, interpolate);
	} //4,5

	public synchronized long createValueRangeToValueTable(ODSModelCache modelCache, InstanceElement ieMea,
			InstanceElement ieLc, double[] minKeys, double[] maxKeys, double[] values, double defaultValue)
					throws AoException, IOException {
		return createTable(modelCache, ieMea, ieLc, minKeys, maxKeys, values, defaultValue, 7, 7, true);
	} //6

	public synchronized long createValueToTextTable(ODSModelCache modelCache, InstanceElement ieMea,
			InstanceElement ieLc, double[] keys, String[] values, String defaultValue)
					throws AoException, IOException {
		return createTable(modelCache, ieMea, ieLc, keys, null, values, defaultValue, 7, 1, true);
	} //7

	public synchronized long createValueRangeToTextTable(ODSModelCache modelCache, InstanceElement ieMea,
			InstanceElement ieLc, double[] minKeys, double[] maxKeys, String[] values, String defaultValue)
					throws AoException, IOException {
		return createTable(modelCache, ieMea, ieLc, minKeys, maxKeys, values, defaultValue, 7, 1, true);
	} //8

	public synchronized long createTextToValueTable(ODSModelCache modelCache, InstanceElement ieMea,
			InstanceElement ieLc, String[] keys, double[] values, double defaultValue)
					throws AoException, IOException {
		return createTable(modelCache, ieMea, ieLc, keys, null, values, defaultValue, 1, 7, true);
	} //9

	public synchronized long createTextToTextTable(ODSModelCache modelCache, InstanceElement ieMea,
			InstanceElement ieLc, String[] keys, String[] values, String defaultValue)
					throws AoException, IOException {
		return createTable(modelCache, ieMea, ieLc, keys, null, values, defaultValue, 1, 1, true);
	} //10



	/**
	 * This is a general function for creating lookup Measurements in an ODS-File.
	 * @param modelCache The ODS MOdel Cache
	 * @param ieMea Measurement instance
	 * @param ieLc LocalColumn instance
	 * @param keys The Keys or the lowerKeys of a Range as String[] or double[]
	 * @param keysMax The upper limits of a Range as double[], if this is null, only the normal keys are used as keys.
	 * @param values The values for the lookup as String[] or double[]
	 * @param defaultValue The default value of this conversion, String or double.
	 * @param keysType 7 if the keys are double (keys instanceof double[]) values. 1 if the keys are Strings (keys instanceof String[]).
	 * @param valueType 7 if the values are double values. 1 if the values are Strings.
	 * @param interpolate only used for conversion 4 and 5 in MDF4, specifies if interpolation will be done. Value not used for other lookup tables.
	 * @return The ID of the SubMatrix with the Previews.
	 * @throws AoException
	 */
	private long createTable(ODSModelCache modelCache, InstanceElement ieMea, InstanceElement ieLc,
			Object keys, Object keysMax,  Object values, Object defaultValue, int keysType, int valueType, boolean interpolate) throws AoException{

		/*ApplicationElement aeMea = modelCache.getApplicationElement("mea");
        ApplicationElement aeMeq = modelCache.getApplicationElement("meq");
        ApplicationElement aeSm = modelCache.getApplicationElement("sm");
        ApplicationElement aeLc = modelCache.getApplicationElement("lc");
        ApplicationRelation relMeaTst = modelCache.getApplicationRelation("mea", "tst", "tst");
        ApplicationRelation relSmMea = modelCache.getApplicationRelation("sm", "mea", "mea");
        ApplicationRelation relSmLc = modelCache.getApplicationRelation("sm", "lc", "lcs");
        ApplicationRelation relMeaMeq = modelCache.getApplicationRelation("mea", "meq", "meqs");
        ApplicationRelation relLcMeq = modelCache.getApplicationRelation("lc", "meq", "meq"); */
		String lcName = ieLc.getName();

		createMeasurmentIfNeeded(modelCache, ieMea);


		// create 'AoSubMatrix' instance

		ODSInsertStatement ins = new ODSInsertStatement(modelCache, "sm");
		ins.setStringVal("iname", lcName);

		//Get MIME Type. See HighQSoft Guideline
		String mimeType = "application/x-asam.aosubmatrix.lookup.";
		if(keysType ==7 && valueType == 7 && keysMax == null){ //use interpolate value for conv. 4/5
			mimeType += "value_value_";
			mimeType += interpolate ? "with_interpolation" : "without_interpolation";
		}else{
			mimeType += "value_";
			mimeType += (keysMax == null && valueType !=1 ? "" : "range_") + "to_"; //range and text conversions with "_range"
			//mimeType += (valueType == 7 ? "value" : "text");
			mimeType += keysType == 1 || valueType ==1 ? "text" : "value"; //According to HighQSoft
		}
		ins.setStringVal("mt", mimeType);

		if(valueType == 7){
			ins.setLongVal("rows", ((double[])values).length);
		}else{
			ins.setLongVal("rows", ((String[])values).length);
		}

		//set Relations, to measurement
		ins.setLongLongVal("mea", lookupMeaIid);

		long iidSm = ins.execute();

		String nameExtension = keysMax == null ?"": "_min";

		// create 'AoMeasurementQuantity' instance for key (or key min)
		ins = new ODSInsertStatement(modelCache, "meq");
		ins.setStringVal("iname", lcName + "_key"+nameExtension);
		ins.setStringVal("mt", "application/x-asam.aomeasurementquantity.lookup.key"+ nameExtension);
		ins.setEnumVal("dt", keysType);
		ins.setLongLongVal("mea", lookupMeaIid);
		long iidMeq = ins.execute();

		// create 'AoLocalColumn' instance for key (or key min)

		ins = new ODSInsertStatement(modelCache, "lc");
		ins.setStringVal("iname", lcName + "_key"+nameExtension);
		ins.setStringVal("mt", "application/x-asam.aolocalcolumn.lookup.key" +nameExtension);
		ins.setEnumVal("srp", ODSHelper.getEnumVal(ieLc.getValue("srp")));
		ins.setDoubleSeq("par", ODSHelper.getDoubleSeq(ieLc.getValue("par")));
		ins.setShortVal("idp", (short) 0);
		ins.setShortVal("glb", (short) 15);
		ins.setEnumVal("axistype", 0);
		if(keysType == 7){
			ins.setDoubleSeq("val", (double[])keys);
		}else{
			ins.setStringSeq("val", (String[])keys);
		}

		ins.setLongLongVal("sm", iidSm);
		ins.setLongLongVal("meq", iidMeq);
		ins.execute();

		if(keysMax!=null){

			// create 'AoMeasurementQuantity' instance for key max
			ins = new ODSInsertStatement(modelCache, "meq");
			ins.setStringVal("iname", lcName + "_key_max");
			ins.setStringVal("mt", "application/x-asam.aomeasurementquantity.lookup.key_max"+ nameExtension);
			ins.setEnumVal("dt", keysType);
			ins.setLongLongVal("mea", lookupMeaIid);
			iidMeq = ins.execute();

			// create 'AoLocalColumn' instance for key max
			ins = new ODSInsertStatement(modelCache, "lc");
			ins.setStringVal("iname", lcName + "_key_max");
			ins.setStringVal("mt", "application/x-asam.aolocalcolumn.lookup.key_max");
			ins.setEnumVal("srp", ODSHelper.getEnumVal(ieLc.getValue("srp")));
			ins.setDoubleSeq("par", ODSHelper.getDoubleSeq(ieLc.getValue("par")));
			ins.setShortVal("idp", (short) 0);
			ins.setShortVal("glb", (short) 15);
			ins.setEnumVal("axistype", 0);
			if(keysType == 7){
				ins.setDoubleSeq("val", (double[])keysMax);
			}else{
				ins.setStringSeq("val", (String[])keysMax);
			}

			ins.setLongLongVal("sm", iidSm);
			ins.setLongLongVal("meq", iidMeq);
			ins.execute();
		}

		// create 'AoMeasurementQuantity' instance for value
		ins = new ODSInsertStatement(modelCache, "meq");
		ins.setStringVal("iname", lcName + "_value");
		ins.setStringVal("mt", "application/x-asam.aomeasurementquantity.lookup.value"+ nameExtension);
		ins.setEnumVal("dt", valueType);
		ins.setLongLongVal("mea", lookupMeaIid);
		iidMeq = ins.execute();

		// create 'AoLocalColumn' instance for values

		ins = new ODSInsertStatement(modelCache, "lc");
		ins.setStringVal("iname", lcName + "_value");
		ins.setStringVal("mt", "application/x-asam.aolocalcolumn.lookup.value");
		ins.setEnumVal("srp", 0);
		ins.setShortVal("idp", (short) 0);
		ins.setShortVal("glb", (short) 15);
		ins.setEnumVal("axistype", 1);
		if(valueType == 7){
			ins.setDoubleSeq("val", (double[])values);
		}else{
			ins.setStringSeq("val", (String[])values);
		}

		ins.setLongLongVal("sm", iidSm);
		ins.setLongLongVal("meq", iidMeq);
		ins.execute();



		if(defaultValue!= null){
			// create 'AoLocalColumn' instance for default value
			//          NameValueUnit[] nvuLcDefValue = new NameValueUnit[6];
			//          nvuLcDefValue[0] = ODSHelper.createStringNVU("mt", "application/x-asam.aolocalcolumn.lookup.default_value");
			//          nvuLcDefValue[1] = ODSHelper.createEnumNVU("srp", 1);
			//          nvuLcDefValue[2] = ODSHelper.createShortNVU("idp", (short) 0);
			//          nvuLcDefValue[3] = ODSHelper.createShortNVU("glb", (short) 15);
			//          nvuLcDefValue[4] = ODSHelper.createEnumNVU("axistype", 1);
			//          nvuLcDefValue[5] = ODSHelper.createStringSeqNVU("val", new String[] { defaultValue });
			//          InstanceElement ieLcDefValue = aeLc.createInstance(lcName + "_default_value");
			//          ieLcDefValue.setValueSeq(nvuLcDefValue);
			//          ieSm.createRelation(relSmLc, ieLcDefValue);

			// create 'AoMeasurementQuantity' instance for default value
			//InstanceElement ieMeqDefValue = aeMeq.createInstance(lcName + "_default_value");
			//ieMeqDefValue.setValue(ODSHelper.createStringNVU("mt",
			//                                                 "application/x-asam.aomeasurementquantity.lookup.default_value"));
			//ieMeqDefValue.setValue(ODSHelper.createEnumNVU("dt", 1));
			//this.lookupMeaIe.createRelation(relMeaMeq, ieMeqDefValue);
			//          ieLcDefValue.createRelation(relLcMeq, ieMeqDefValue);
		}

		return iidSm;
	}
}
