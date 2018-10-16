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


package org.eclipse.mdm.openatfx.mdf.util;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.asam.ods.AoException;
import org.asam.ods.InstanceElement;
import org.asam.ods.InstanceElementIterator;
import org.asam.ods.Relationship;

/**
 * Helper class to convert the MDF lookup tables to a ASAM ODS 'AoMeasurement'.
 *
 * @author Christian Rechner
 */
public class LookupTableHelper {

	private final Map<String, Integer> nameCount = new HashMap<>();

	// the cached lookup instance element
	private long lookupMeaIid = -1L;

	private synchronized void createMeasurmentIfNeeded(ODSModelCache modelCache, InstanceElement ieMea)
			throws AoException {
		// create 'AoMeasurement' instance (if not yet existing)
		if (lookupMeaIid == -1L) {
			// lookup parent 'AoTest' instance
			InstanceElementIterator iter = ieMea.getRelatedInstancesByRelationship(Relationship.FATHER, "*");
			InstanceElement ieTst = iter.nextOne();
			iter.destroy();
			long iidTst = ODSHelper.asJLong(ieTst.getId());
			/*
			 * String meaName = ieMea.getName() + "_lookup"; this.lookupMeaIe =
			 * aeMea.createInstance(meaName);
			 * this.lookupMeaIe.setValue(ODSHelper.createStringNVU("mt",
			 * "application/x-asam.aomeasurement.lookup"));
			 * this.lookupMeaIe.setValue(ieMea.getValue("date_created"));
			 * this.lookupMeaIe.setValue(ieMea.getValue("mea_begin"));
			 * this.lookupMeaIe.setValue(ieMea.getValue("mea_end"));
			 * this.lookupMeaIe.createRelation(relMeaTst, ieTst);
			 */

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
			InstanceElement ieLc, double[] keys, double[] values, boolean interpolate) throws AoException, IOException {
		return createTable(modelCache, ieMea, ieLc, keys, null, values, null, 7, 7, interpolate);
	} // 4,5

	public synchronized long createValueRangeToValueTable(ODSModelCache modelCache, InstanceElement ieMea,
			InstanceElement ieLc, double[] minKeys, double[] maxKeys, double[] values, double defaultValue)
			throws AoException, IOException {
		return createTable(modelCache, ieMea, ieLc, minKeys, maxKeys, values, defaultValue, 7, 7, true);
	} // 6

	public synchronized long createValueToTextTable(ODSModelCache modelCache, InstanceElement ieMea,
			InstanceElement ieLc, double[] keys, String[] values, String defaultValue) throws AoException, IOException {
		return createTable(modelCache, ieMea, ieLc, keys, null, values, defaultValue, 7, 1, true);
	} // 7

	public synchronized long createValueRangeToTextTable(ODSModelCache modelCache, InstanceElement ieMea,
			InstanceElement ieLc, double[] minKeys, double[] maxKeys, String[] values, String defaultValue)
			throws AoException, IOException {
		return createTable(modelCache, ieMea, ieLc, minKeys, maxKeys, values, defaultValue, 7, 1, true);
	} // 8

	public synchronized long createTextToValueTable(ODSModelCache modelCache, InstanceElement ieMea,
			InstanceElement ieLc, String[] keys, double[] values, double defaultValue) throws AoException, IOException {
		return createTable(modelCache, ieMea, ieLc, keys, null, values, defaultValue, 1, 7, true);
	} // 9

	public synchronized long createTextToTextTable(ODSModelCache modelCache, InstanceElement ieMea,
			InstanceElement ieLc, String[] keys, String[] values, String defaultValue) throws AoException, IOException {
		return createTable(modelCache, ieMea, ieLc, keys, null, values, defaultValue, 1, 1, true);
	} // 10

	/**
	 * This is a general function for creating lookup Measurements in an
	 * ODS-File.
	 * 
	 * @param modelCache
	 *            The ODS MOdel Cache
	 * @param ieMea
	 *            Measurement instance
	 * @param ieLc
	 *            LocalColumn instance
	 * @param keys
	 *            The Keys or the lowerKeys of a Range as String[] or double[]
	 * @param keysMax
	 *            The upper limits of a Range as double[], if this is null, only
	 *            the normal keys are used as keys.
	 * @param values
	 *            The values for the lookup as String[] or double[]
	 * @param defaultValue
	 *            The default value of this conversion, String or double.
	 * @param keysType
	 *            7 if the keys are double (keys instanceof double[]) values. 1
	 *            if the keys are Strings (keys instanceof String[]).
	 * @param valueType
	 *            7 if the values are double values. 1 if the values are
	 *            Strings.
	 * @param interpolate
	 *            only used for conversion 4 and 5 in MDF4, specifies if
	 *            interpolation will be done. Value not used for other lookup
	 *            tables.
	 * @return The ID of the SubMatrix with the Previews.
	 * @throws AoException
	 */
	private long createTable(ODSModelCache modelCache, InstanceElement ieMea, InstanceElement ieLc, Object keys,
			Object keysMax, Object values, Object defaultValue, int keysType, int valueType, boolean interpolate)
			throws AoException {

		String lcName = ieLc.getName();
		Integer count = nameCount.get(lcName);
		if (count == null) {
			count = Integer.valueOf(0);
		} else {
			count = Integer.valueOf(count.intValue() + 1);
		}

		nameCount.put(lcName, count);
		lcName = count.intValue() > 0 ? lcName + "_" + count : lcName;

		createMeasurmentIfNeeded(modelCache, ieMea);

		// create 'AoSubMatrix' instance

		ODSInsertStatement ins = new ODSInsertStatement(modelCache, "sm");
		ins.setStringVal("iname", lcName);

		// Get MIME Type. See HighQSoft Guideline
		String mimeType = "application/x-asam.aosubmatrix.lookup.";
		if (keysType == 7 && valueType == 7 && keysMax == null) { // use
																	// interpolate
																	// value for
																	// conv. 4/5
			mimeType += "value_value_";
			mimeType += interpolate ? "with_interpolation" : "without_interpolation";
		} else {
			mimeType += "value_";
			mimeType += (keysMax == null && valueType != 1 ? "" : "range_") + "to_"; // range
																						// and
																						// text
																						// conversions
																						// with
																						// "_range"
			// mimeType += (valueType == 7 ? "value" : "text");
			mimeType += keysType == 1 || valueType == 1 ? "text" : "value"; // According
																			// to
																			// HighQSoft
		}
		ins.setStringVal("mt", mimeType);

		if (valueType == 7) {
			ins.setLongVal("rows", ((double[]) values).length);
		} else {
			ins.setLongVal("rows", ((String[]) values).length);
		}

		// set Relations, to measurement
		ins.setLongLongVal("mea", lookupMeaIid);

		long iidSm = ins.execute();

		String nameExtension = keysMax == null ? "" : "_min";

		// create 'AoMeasurementQuantity' instance for key (or key min)
		ins = new ODSInsertStatement(modelCache, "meq");
		ins.setStringVal("iname", lcName + "_key" + nameExtension);
		ins.setStringVal("mt", "application/x-asam.aomeasurementquantity.lookup.key" + nameExtension);
		ins.setEnumVal("dt", keysType);
		ins.setLongLongVal("mea", lookupMeaIid);
		long iidMeq = ins.execute();

		// create 'AoLocalColumn' instance for key (or key min)

		ins = new ODSInsertStatement(modelCache, "lc");
		ins.setStringVal("iname", lcName + "_key" + nameExtension);
		ins.setStringVal("mt", "application/x-asam.aolocalcolumn.lookup.key" + nameExtension);
		ins.setEnumVal("srp", ODSHelper.getEnumVal(ieLc.getValue("srp")));
		ins.setDoubleSeq("par", ODSHelper.getDoubleSeq(ieLc.getValue("par")));
		ins.setShortVal("idp", (short) 0);
		ins.setShortVal("glb", (short) 15);
		ins.setEnumVal("axistype", 0);
		if (keysType == 7) {
			ins.setDoubleSeq("val", (double[]) keys);
		} else {
			ins.setStringSeq("val", (String[]) keys);
		}

		ins.setLongLongVal("sm", iidSm);
		ins.setLongLongVal("meq", iidMeq);
		ins.execute();

		if (keysMax != null) {

			// create 'AoMeasurementQuantity' instance for key max
			ins = new ODSInsertStatement(modelCache, "meq");
			ins.setStringVal("iname", lcName + "_key_max");
			ins.setStringVal("mt", "application/x-asam.aomeasurementquantity.lookup.key_max" + nameExtension);
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
			if (keysType == 7) {
				ins.setDoubleSeq("val", (double[]) keysMax);
			} else {
				ins.setStringSeq("val", (String[]) keysMax);
			}

			ins.setLongLongVal("sm", iidSm);
			ins.setLongLongVal("meq", iidMeq);
			ins.execute();
		}

		// create 'AoMeasurementQuantity' instance for value
		ins = new ODSInsertStatement(modelCache, "meq");
		ins.setStringVal("iname", lcName + "_value");
		ins.setStringVal("mt", "application/x-asam.aomeasurementquantity.lookup.value" + nameExtension);
		ins.setEnumVal("dt", valueType);
		ins.setLongLongVal("mea", lookupMeaIid);
		iidMeq = ins.execute();

		// create 'AoLocalColumn' instance for values

		ins = new ODSInsertStatement(modelCache, "lc");
		ins.setStringVal("iname", lcName + "_value");
		ins.setStringVal("mt", "application/x-asam.aolocalcolumn.lookup.value");
		ins.setEnumVal("srp", 0);
		ins.setShortVal("idp", (short) 0);
		ins.setEnumVal("axistype", 1);
		if (valueType == 7) {
			ins.setDoubleSeq("val", (double[]) values);
			ins.setShortVal("glb", (short) 15);
		} else {
			String[] strVals = (String[]) values;
			String[] nonNullVals = new String[strVals.length];
			short[] flags = new short[strVals.length];
			boolean hasNotValid = false;

			for (int i = 0; i < strVals.length; i++) {
				nonNullVals[i] = strVals[i] == null ? "" : strVals[i];
				flags[i] = nonNullVals[i].isEmpty() ? 0 : (short) 15;
				hasNotValid |= flags[i] == 0;
			}
			ins.setStringSeq("val", nonNullVals);

			if (hasNotValid) {
				ins.setShortSeq("flg", flags);
			} else {
				ins.setShortVal("glb", (short) 15);
			}
		}

		ins.setLongLongVal("sm", iidSm);
		ins.setLongLongVal("meq", iidMeq);
		ins.execute();

		if (defaultValue != null) {
			// create 'AoLocalColumn' instance for default value
			// NameValueUnit[] nvuLcDefValue = new NameValueUnit[6];
			// nvuLcDefValue[0] = ODSHelper.createStringNVU("mt",
			// "application/x-asam.aolocalcolumn.lookup.default_value");
			// nvuLcDefValue[1] = ODSHelper.createEnumNVU("srp", 1);
			// nvuLcDefValue[2] = ODSHelper.createShortNVU("idp", (short) 0);
			// nvuLcDefValue[3] = ODSHelper.createShortNVU("glb", (short) 15);
			// nvuLcDefValue[4] = ODSHelper.createEnumNVU("axistype", 1);
			// nvuLcDefValue[5] = ODSHelper.createStringSeqNVU("val", new
			// String[] { defaultValue });
			// InstanceElement ieLcDefValue = aeLc.createInstance(lcName +
			// "_default_value");
			// ieLcDefValue.setValueSeq(nvuLcDefValue);
			// ieSm.createRelation(relSmLc, ieLcDefValue);

			// create 'AoMeasurementQuantity' instance for default value
			// InstanceElement ieMeqDefValue = aeMeq.createInstance(lcName +
			// "_default_value");
			// ieMeqDefValue.setValue(ODSHelper.createStringNVU("mt",
			// "application/x-asam.aomeasurementquantity.lookup.default_value"));
			// ieMeqDefValue.setValue(ODSHelper.createEnumNVU("dt", 1));
			// this.lookupMeaIe.createRelation(relMeaMeq, ieMeqDefValue);
			// ieLcDefValue.createRelation(relLcMeq, ieMeqDefValue);
		}

		return iidSm;
	}
}
