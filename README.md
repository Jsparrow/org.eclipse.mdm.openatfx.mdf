# Documentation open ATFX MDF4 driver

## Copyright and License ##
Copyright (c) 2015-2018 Contributors to the Eclipse Foundation

 See the NOTICE file(s) distributed with this work for additional
 information regarding copyright ownership.

 This program and the accompanying materials are made available under the
 terms of the Eclipse Public License v. 2.0 which is available at
 http://www.eclipse.org/legal/epl-2.0.

 SPDX-License-Identifier: EPL-2.0

Author: Christian Rechner, AUDI AG

## Introduction
This application allows accessing the content of an MDF3/MDF4 file via an ASAM ODS Session, backed by an ATFX-File, and the export of this file.
The file contents including Meta-Data are transformed to the ASAM ODS standard and can be accessed using ASAMs OO-API.

## Dependencies
This project uses the following librarys:

* openatfx (ATFX backed ODS backend, can be obtained from SourceForge)
* JUnit (Testing Framework)
* ASAM ODS (Interfaces for the ASAM OO-API)
* Apache Commons Logging (Logging Framework)
* Log4j (Logging Framework)
* STAX API (Java XML Framework)
* STAX2 API (Java XML Framework)

## Java code example for opening a ASAM ODS session on an ATFX file

    import java.nio.file.Path;
    import java.nio.file.Paths;
    
    import org.asam.ods.AoSession;
    import org.omg.CORBA.ORB;
    
    import de.rechner.openatfx_mdf.ConvertException;
    import de.rechner.openatfx_mdf.MDFConverter;
    
    public class Example{
        public static void main(String[] args) throws ConvertException{
            ORB orb = ORB.init(new String[0], System.getProperties());
            Path path = Paths.get("C:\\myExample.mf4");
            MDFConverter reader = new MDFConverter();
            AoSession aoSession = reader.getAoSessionForMDF(orb, path);
        }
    }


## Known bugs/missing features:
### BLOCKs
* IDBLOCK
  - id_unfin_flags must be 0
  - id_custom_unfin_flags must be 0
* HDBLOCK
  - hd_ch_first must be 0 (channel hierarchy not yet supported)
  - hd_flags must be 0 [bits 00] (start angle value below is invalid, start distance value below is invalid)
* CHBLOCK (MDF3): not yet supported, will be ignored with warning
* CABLOCK (MDF4): not yet supported, will be ignored with warning
* ATBLOCK: not yet supported, will be ignored with warning
* EVBLOCK:
  - ev_ev_parent will be ignored with warning
  - ev_ev_range will be ignored with warning
  - formula tag in ev_md_comment not entirely supported
* DGBLOCK
  - may contain only ONE channel group (sorted MDF), otherwise an exception is thrown
  - Records must not be zipped or split up in multiple datablocks if parts of one record are in multiple blocks.
* CGBLOCK
  - cg_flags must be null (VLSD and bus events not supported), otherwise an exception is thrown
  - cg_inval_bytes must be null (invalidation bits not supported), otherwise an exception is thrown
* CNBLOCK
  - cn_composition must be null (composition not supported), otherwise an exception is thrown
  - cn_at_reference: not yet supported, will be ignored with warning
  - cn_limit_min: not yet supported, will be ignored with warning
  - cn_limit_max: not yet supported, will be ignored with warning
  - cn_limit_ext_min: not yet supported, will be ignored with warning
  - cn_limit_ext_max: not yet supported, will be ignored with warning
  - VLSD only supports String datatypes.


### XML content
* mdf_base.xsd
  - type 'common_properties'
   - 'tree' will be ignored with warning
   - 'list' will be ignored with warning
   - 'elist' will be ignored with warning
* hd_comment.xsd
  - 'UNIT-SPEC' will be ignored with warning