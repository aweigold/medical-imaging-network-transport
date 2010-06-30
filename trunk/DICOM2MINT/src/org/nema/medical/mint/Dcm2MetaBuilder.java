package org.nema.medical.mint;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import org.dcm4che2.data.DicomElement;
import org.dcm4che2.data.DicomObject;
import org.dcm4che2.data.SpecificCharacterSet;
import org.dcm4che2.data.Tag;
import org.dcm4che2.data.TransferSyntax;
import org.dcm4che2.data.VR;
import org.dcm4che2.io.DicomInputStream;
import org.nema.medical.mint.common.mint.Attribute;
import org.nema.medical.mint.common.mint.AttributeStore;
import org.nema.medical.mint.common.mint.Instance;
import org.nema.medical.mint.common.mint.Item;
import org.nema.medical.mint.common.mint.Series;
import org.nema.medical.mint.common.mint.Study;
import org.nema.medical.mint.util.Iter;

/* Copyright (c) Vital Images, Inc. 2010. All Rights Reserved.
*
*    This is UNPUBLISHED PROPRIETARY SOURCE CODE of Vital Images, Inc.;
*    the contents of this file may not be disclosed to third parties,
*    copied or duplicated in any form, in whole or in part, without the
*    prior written permission of Vital Images, Inc.
*
*    RESTRICTED RIGHTS LEGEND:
*    Use, duplication or disclosure by the Government is subject to
*    restrictions as set forth in subdivision (c)(1)(ii) of the Rights
*    in Technical Data and Computer Software clause at DFARS 252.227-7013,
*    and/or in similar or successor clauses in the FAR, DOD or NASA FAR
*    Supplement. Unpublished rights reserved under the Copyright Laws of
*    the United States.
*/

/**
\brief Class used to build up a StudyMeta message from a set of DICOM instances.

To use this class, create a StudyMeta instance, a binary item vector, and a tag normalization map
(see the accumulateFile signature). Call accumulateFile() for each DICOM instance P10 file in the
study's dataset. This class will populate the passed-in parameters with the data from the P10
instances.


\par Example Usage
\code
Dcm2MetaBuilder::GroupElementTagSet final studyLevelTags = ... get study level attribute tags ...
Dcm2MetaBuilder::GroupElementTagSet final seriesLevelTags = ... get series-level attribute tags ...
Dcm2MetaBuilder builder(studyLevelTags, seriesLevelTags);

foreach (Path final& p10Path, ... list of DICOM P10 files ...)
{
   builder.accumulateFile(p10Path);
}

Dcm2MetaBuilder::MetaBinaryPair final data = builder.finish();

... Use data to produce on-disk representation ...
\endcode

\par Study/Series Summary-Level Tag Insertion Rules.
Tags can be organized, by request, to the study summary and series summary section for each
series. To do this, the caller provides the constructor with maps containing the tags for the
attributes that should be stored in the corresponding summary sections.
\par
When processing an instance, the attributes of the instance are checked against these two maps.
If the attribute's tag is found in one of the summary maps, this indicates the attribute is
a summary-level attribute. The attribute is inserted into the corresponding summary table and,
as a result, \e not into individual instance's attribute table. If the attribute already
existed in the specific summary table (i.e.: from a previous instance being processed), this
copy of the attribute is essentially discarded.
\par
This behavior may result in summary tags whose values differ between instances being discarded.
It may also result in attributes appearing to be present in all instances even if some instances
happened to not have the attributes. Both of these cases would be extremely rare and the current
behavior is deemed acceptable.
*/
public final class Dcm2MetaBuilder {

    /**
    Type used to contain the result of the build.

    This is returned by Dcm2MetaBuilder::finish().
    */
    public class MetaBinaryPair {
    /** The study's metadata */
    public final Study metadata;

    /** The study's binary data */
    public final List<byte[]> binaryItems;

    /**
    Constructor

    @param metadata The resulting normalized metadata for the study
    @param binaryItems All binary items for the study
    */
    private MetaBinaryPair(final Study metadata, final List<byte[]> binaryItems) {
            this.metadata = metadata;
            this.binaryItems = binaryItems;
        }
    }

   /**
   Create an instance of the class with the specified summary-level tag maps.

   The maps are used in subsequent calls to accumulateFile().

   @param studyLevelTags The caller must fill this in with tags that are considered to be part of
   the study-level & patient-level summary set. The builder will extract the associated tags from
   the instances passed into this function and place them in the resulting study's study-level
   summary tags section. See the note below for tag insertion rules.
   @param seriesLevelTags The caller must fill this in with tags that are considered to be part of
   the series-level set for each series. See the note below for tag insertion rules.

   \note If a given attribute tag is present in both maps, the studyLevelTags map takes
   precedence.
    */
   public Dcm2MetaBuilder(
           final Set<Integer> studyLevelTags,
           final Set<Integer> seriesLevelTags,
           final String studyInstanceUID) {
       this.studyLevelTags = studyLevelTags;
       this.seriesLevelTags = seriesLevelTags;
       this.study.setStudyInstanceUID(studyInstanceUID);
   }

   public Dcm2MetaBuilder(
           final Set<Integer> studyLevelTags,
           final Set<Integer> seriesLevelTags) {
       this(studyLevelTags, seriesLevelTags, null);
   }

   /**
   Accumulates the tags for the DICOM P10 instance specified by path into the overall study
   metadata.

   @param dcmPath The path to the DICOM P10 instance. All instances accumulated for a given
   StudyMeta must be part of the same study or an exception will be thrown.
   @throws vtal::InvalidArgument The instance referred to by the path either doesn't have a study
   instance UID or its study instance UID is not the same as previously accumulated instances.
    */
   public void accumulateFile(final File dcmPath) {
       try {
           final DicomInputStream dicomStream = new DicomInputStream(dcmPath);
           try {
               storeInstance(dcmPath, dicomStream);
           } finally {
               dicomStream.close();
           }
       } catch (IOException ex) {
           throw new RuntimeException(dcmPath + " -- failed to load file: " + ex, ex);
       }
   }

   /**
   This function completes the build of the meta data and returns the results.

   Normalizing the duplicate attributes in each series greatly reduces the size of the meta data
   and improves parsing times significantly.

   This should be called only after all P10 instances for the study have been processed with
   accumulateFile().

   @return A MetaBinaryPair instance containing the normalized metadata for the study and all the
   binary data blobs for the study.
    */
   public MetaBinaryPair finish() {
       for (final Entry<String, Map<Integer, NormalizationCounter>> seriesTagsEntry: tagNormalizerTable.entrySet()) {
           final Series series = study.getSeries(seriesTagsEntry.getKey());
           if (series == null) {
               throw new RuntimeException(
                       "Normalization: cannot find series " + seriesTagsEntry.getKey() + " in study data.");
           }

           final int nInstancesInSeries = series.instanceCount();
           if (nInstancesInSeries > 1) {
               for (final NormalizationCounter normCtr: seriesTagsEntry.getValue().values()) {
                   if (normCtr.count == nInstancesInSeries) {
                       // Move the attribute to the normalized section...
                       series.putNormalizedInstanceAttribute(normCtr.attr);

                       for (final Instance instanceMeta: Iter.iter(series.instanceIterator())) {
                           instanceMeta.removeAttribute(normCtr.attr.getTag());
                       }
                   }
               }
           }
       }
       return new MetaBinaryPair(study, binaryItems);
   }

     private static SpecificCharacterSet checkCharacterSet(final File dcmPath, final DicomObject dataset) {
         final SpecificCharacterSet specificCharacterSet = dataset.getSpecificCharacterSet();
         //Can't use DicomObject.getSpecificCharacterSet() due to maldesigned SpecificCharacterSet class
         final String str = dataset.getString(Tag.SpecificCharacterSet);
         // If no dataset is specified, it defaults to "ISO_IR 100" which is what we want.
         if (str != null && !str.equals("ISO_IR 100")) {
             throw new RuntimeException(dcmPath + " -- unsupported character set: " + str);
         }
         return specificCharacterSet;
     }

     private void storeInstance(final File dcmPath, final DicomInputStream dicomStream) throws IOException {
         //Read and cache
         final DicomObject dcmObj = dicomStream.readDicomObject();
         final SpecificCharacterSet charSet = checkCharacterSet(dcmPath, dcmObj);

         final String dataStudyInstanceUID = dcmObj.getString(Tag.StudyInstanceUID);
         if (dataStudyInstanceUID != null) {
             if (study.getStudyInstanceUID() == null) {
                 study.setStudyInstanceUID(dataStudyInstanceUID);
             }
             else if (!study.getStudyInstanceUID().equals(dataStudyInstanceUID)) {
                 throw new RuntimeException(dcmPath + " -- study instance uid (" + dataStudyInstanceUID +
                         ") does not match current study (" + study.getStudyInstanceUID() + ')');
             }
         }

         final String seriesInstanceUID = dcmObj.getString(Tag.SeriesInstanceUID);
         if (seriesInstanceUID == null) {
             throw new RuntimeException(dcmPath + " -- missing series instance uid");
         }

         Series series = study.getSeries(seriesInstanceUID);
         if (series == null) {
             series = new Series();
             series.setSeriesInstanceUID(seriesInstanceUID);
             study.putSeries(series);
             assert !tagNormalizerTable.containsKey(seriesInstanceUID);
             tagNormalizerTable.put(seriesInstanceUID, new HashMap<Integer, NormalizationCounter>());
         }

         final Instance instance = new Instance();
         final TransferSyntax xfer = dicomStream.getTransferSyntax();
         instance.setXfer(xfer.uid());
         series.putInstance(instance);

         // Now, iterate through all items in the object and store each appropriately.
         // This dispatches the Attribute storage to one of the study level, series level
         // or instance-level Attributes sets.
         for (final DicomElement dcmElement: Iter.iter(dcmObj.datasetIterator())) {
             final int tag = dcmElement.tag();
             if (studyLevelTags.contains(tag)) {
                 if (study.getAttribute(tag) == null) {
                     handleDICOMElement(dcmPath, charSet, dcmElement, study, null);
                 }
             }
             else if (seriesLevelTags.contains(tag)) {
                 if (series.getAttribute(tag) == null) {
                     handleDICOMElement(dcmPath, charSet, dcmElement, series, null);
                 }
             }
             else {
                 // tagNormalizerTable is only used for instance-level storage...
                 final Map<Integer, NormalizationCounter> seriesNormMap =
                     tagNormalizerTable.get(series.getSeriesInstanceUID());
                 assert seriesNormMap != null;
                 handleDICOMElement(dcmPath, charSet, dcmElement, instance, seriesNormMap);
             }
         }
     }

     private void handleDICOMElement(
             final File dcmPath,
             final SpecificCharacterSet charSet,
             final DicomElement dcmElem,
             final AttributeStore attrs,
             final Map<Integer, NormalizationCounter> seriesNormMap) {
         final IStore storePlain = new StorePlain(dcmPath, charSet, attrs, seriesNormMap);
         final IStore storeSequence = new StoreSequence(dcmPath, charSet, attrs);
         final IStore storeBinary = new StoreBinary(dcmPath, charSet, attrs, seriesNormMap);
         final VR vr = dcmElem.vr();
         if (vr == null) {
             //TODO can't handle (possibly private tag?)
             throw new RuntimeException("Null VR");
         } else if (vr == VR.OW || vr == VR.OB || vr == VR.UN || vr == VR.UN_SIEMENS) {
             //TODO define complete list of binary types
             //Binary
             storeBinary.store(dcmElem);
         } else if (vr == VR.SQ) {
             storeSequence.store(dcmElem);
         } else {
             //Non-binary, non-sequence
             //TODO need to restrict list to actual non-binary types
             storePlain.store(dcmElem);
         }
     }

     private interface IStore {
         void store(final DicomElement dcmItem);
     }

     private abstract class StoreBase implements IStore {
         public StoreBase(final File dcmPath, final SpecificCharacterSet charSet, final AttributeStore attrs,
                 final Map<Integer, NormalizationCounter> seriesNormMap) {
             this.dcmPath = dcmPath;
             this.charSet = charSet;
             this.attrs = attrs;
             this.seriesNormMap = seriesNormMap;
         }

         protected final File dcmPath;
         protected final SpecificCharacterSet charSet;
         protected final AttributeStore attrs;
         protected final Map<Integer, NormalizationCounter> seriesNormMap;
     }

     private final class StorePlain extends StoreBase {
         public StorePlain(final File dcmPath, final SpecificCharacterSet charSet, final AttributeStore attrs,
                 final Map<Integer, NormalizationCounter> seriesNormMap) {
             super(dcmPath, charSet, attrs, seriesNormMap);
         }

         public void store(final DicomElement elem) {
             assert elem != null;
             Attribute attr = null;
             NormalizationCounter normCounter = null;
             final String strVal = getStringValue(elem, charSet);
             if (seriesNormMap != null) {
                 normCounter = seriesNormMap.get(elem.tag());
                 if (normCounter != null) {
                     final Attribute ncAttr = normCounter.attr;
                     if (areEqual(ncAttr, elem, strVal)) {
                         // The data is the same. Instead of creating a new Attribute just to throw it
                         // away shortly, re-use the previously created attribute.
                         attr = ncAttr;
                         ++normCounter.count;
                     }
                 }
             }

             if (attr == null) {
                 attr = newAttr(elem);
                 if (strVal != null) {
                     attr.setVal(strVal);
                 }

                 if (seriesNormMap != null && normCounter == null) {
                     // This is the first occurrence of this particular attribute
                     normCounter = new NormalizationCounter();
                     normCounter.attr = attr;
                     seriesNormMap.put(elem.tag(), normCounter);
                 }
             }

             assert attr != null;
             attrs.putAttribute(attr);
         }
     }

     private final class StoreSequence extends StoreBase {
         public StoreSequence(final File dcmPath, final SpecificCharacterSet charSet, final AttributeStore attrs) {
             super(dcmPath, charSet, attrs, null);
         }

         public void store(final DicomElement itemSeq) {
             final Attribute attr = newAttr(itemSeq);
             attrs.putAttribute(attr);
             for (int i = 0; i < itemSeq.countItems(); ++i) {
                 final DicomObject dcmObj = itemSeq.getDicomObject(i);
                 final Item newItem = new Item();
                 attr.addItem(newItem);
                 for (final DicomElement dcmElement: Iter.iter(dcmObj.datasetIterator())) {
                     // Don't use tag normalization in sequence items...
                     handleDICOMElement(dcmPath, charSet, dcmElement, newItem, null);
                 }
             }
         }
     }

     private final class StoreBinary extends StoreBase {
         public StoreBinary(final File dcmPath, final SpecificCharacterSet charSet, final AttributeStore attrs,
                 final Map<Integer, NormalizationCounter> seriesNormMap) {
             super(dcmPath, charSet, attrs, seriesNormMap);
         }

         public void store(final DicomElement binData) {
             assert binData != null;

             final VR vr = binData.vr();
             if (vr == null || vr == VR.UN || vr == VR.UN_SIEMENS) {
                 return; // Discard all private tags.
             }

             Attribute attr = null;
             NormalizationCounter normCounter = null;
             final byte[] data = binData.getBytes();
             if (seriesNormMap != null) {
                 normCounter = seriesNormMap.get(binData.tag());
                 if (normCounter != null) {
                     final Attribute ncAttr = normCounter.attr;
                     if (areEqual(ncAttr, binData, data, binaryItems)) {
                         // The data is the same. Instead of creating a new Attribute just to throw it
                         // away shortly, re-use the previously created attribute.
                         attr = ncAttr;
                         ++normCounter.count;
                     }
                 }
             }

             if (attr == null) {
                 attr = newAttr(binData);
                 attr.setBid(binaryItems.size()); // Before we do the push back...
                 binaryItems.add(data);

                 if (seriesNormMap != null && normCounter == null) {
                     // This is the first occurrence of this particular attribute
                     normCounter = new NormalizationCounter();
                     normCounter.attr = attr;
                     seriesNormMap.put(binData.tag(), normCounter);
                 }
             }

             assert attr != null;
             attrs.putAttribute(attr);
         }
     }

     private static boolean areNonValueFieldsEqual(final Attribute a, final DicomElement obj) {
         return a.getTag() == obj.tag() && obj.vr().toString().equals(a.getVr().toString());
     }

     private static boolean areEqual(
         final Attribute a,
         final DicomElement binData,
         final byte[] binDataValue,
         final List<byte[]> binaryItems) {
         if (areNonValueFieldsEqual(a, binData)) {
             final byte[] binaryItem = binaryItems.get(a.getBid());
             if (binDataValue == null) {
                 return binaryItem == null;
             }
             return (binaryItem != null)
                 && (Arrays.equals(binaryItem, binDataValue));
         }

         return false;
     }

     private static String getStringValue(final DicomElement elem, final SpecificCharacterSet charSet) {
         return elem.getString(charSet, false);
     }

     private static boolean areEqual(final Attribute a, final DicomElement elem, final String value) {
         if (areNonValueFieldsEqual(a, elem)) {
             if (value == null) {
                 return a.getVal() == null;
             }
             return a.getVal() != null && a.getVal().equals(value);
         }
         return false;
     }

     private static Attribute newAttr(final DicomElement obj) {
         final Attribute attr = new Attribute();
         attr.setTag(obj.tag());
         attr.setVr(obj.vr().toString());
         return attr;
     }

     private static class NormalizationCounter {

         /** The DICOM attribute. */
         Attribute attr;
         /** The number of instances in the series that have
             an identical value for the attribute.
         */
         long count = 1;
     }

     private final Set<Integer> studyLevelTags;
     private final Set<Integer> seriesLevelTags;
     private final Map<String, Map<Integer, NormalizationCounter>> tagNormalizerTable =
         new HashMap<String, Map<Integer, NormalizationCounter>>();
     private final Study study = new Study();
     private final List<byte[]> binaryItems = new ArrayList<byte[]>();
}