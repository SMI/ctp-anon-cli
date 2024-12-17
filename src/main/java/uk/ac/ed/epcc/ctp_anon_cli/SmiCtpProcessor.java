package uk.ac.ed.epcc.ctp_anon_cli;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.Properties;
import java.util.stream.Collectors;
import org.rsna.ctp.objects.DicomObject;
import org.rsna.ctp.stdstages.anonymizer.AnonymizerStatus;
import org.rsna.ctp.stdstages.anonymizer.dicom.DAScript;
import org.rsna.ctp.stdstages.anonymizer.dicom.DICOMAnonymizer;
import org.rsna.ctp.stdstages.anonymizer.dicom.DICOMDecompressor;
import org.rsna.ctp.stdstages.anonymizer.dicom.DICOMPixelAnonymizer;
import org.rsna.ctp.stdstages.anonymizer.dicom.PixelScript;
import org.rsna.ctp.stdstages.anonymizer.dicom.Regions;
import org.rsna.ctp.stdstages.anonymizer.dicom.Signature;
import org.rsna.ctp.stdstages.anonymizer.dicom.Transcoder;

public class SmiCtpProcessor {

  private static final String JPEGBaseline = "1.2.840.10008.1.2.4.50";
  private static final String JPEGLossLess = "1.2.840.10008.1.2.4.70";

  private boolean _decompress;
  private boolean _recompress;
  private boolean _setBIRElement;
  private boolean _testmode;
  private File _srAnonTool;

  private PixelScript _pixelScript;

  private Properties _tagAnonScriptProps;

  private Transcoder _transcoder = new Transcoder();

  public SmiCtpProcessor(
    File tagAnonScriptFile,
    File pixelAnonScriptFile,
    boolean decompress,
    boolean recompress,
    boolean setBIRElement,
    boolean testmode,
    String check,
    File srAnonTool
  ) {
    _decompress = decompress;
    _recompress = recompress;
    _setBIRElement = setBIRElement;
    _testmode = testmode;
    _srAnonTool = srAnonTool;

    if (pixelAnonScriptFile != null) {
      _pixelScript = new PixelScript(pixelAnonScriptFile);
    }

    _tagAnonScriptProps =
      DAScript.getInstance(tagAnonScriptFile).toProperties();

    _transcoder.setTransferSyntax(JPEGLossLess);
  }

  public void anonymize(File inFile, File outFile) throws Exception {
    // NOTE(rkm 2023-12-01) This ensures CTP won't accidentally clobber the input file
    if (inFile.canWrite()) {
      throw new Exception("Input file was writeable");
    }

    DicomObject dObj = null;
    File origFile = inFile; // inFile gets reassigned so keep a record of it.

    try {
      dObj = new DicomObject(inFile);
    } catch (Exception e) {
      throw new Exception(
        "Could not create dicom object from inFile: " + e.getMessage()
      );
    }

    // See API https://github.com/johnperry/CTP/blob/master/source/java/org/rsna/ctp/objects/DicomObject.java
    // See list of possible values for Modality https://www.dicomlibrary.com/dicom/modality/
    boolean isSR = dObj.getElementValue("Modality").trim().equals("SR");

    // Run the DICOMPixelAnonymizer first before the elements used in signature
    // matching are modified by the DicomAnonymizer.
    if (_pixelScript != null && dObj.isImage()) {
      try {
        inFile = DoPixelAnon(inFile, outFile, dObj);
      } catch (Exception e) {
        throw new Exception("Pixel anon failed: " + e);
      }
    }

    // Ref:
    // https://github.com/johnperry/CTP/blob/master/source/java/org/rsna/ctp/stdstages/anonymizer/dicom/DICOMAnonymizer.java
    AnonymizerStatus status = DICOMAnonymizer.anonymize(
      inFile,
      outFile,
      _tagAnonScriptProps,
      null,
      null,
      false,
      false
    );

    if (!status.isOK()) {
      throw new Exception(
        "DICOMAnonymizer returned status " +
        status.getStatus() +
        ", with message " +
        status.getMessage()
      );
    }

    if (!outFile.isFile()) {
      throw new Exception(
        "DICOMAnonymizer returned OK but outFile does not exist"
      );
    }

    // Structured Reports have an additional anonymisation step
    if (!isSR || _srAnonTool == null) {
        System.out.println("OK");
        return;
    }

    String commandArray[] = {
      _srAnonTool.getAbsolutePath(),
      "-i",
      origFile.getAbsolutePath(),
      "-o",
      outFile.getAbsolutePath(),
    };

    String stderr = null;
    int rc = -1;

    try {
      Process process = Runtime.getRuntime().exec(commandArray);
      BufferedReader errorReader = new BufferedReader(
        new InputStreamReader(process.getErrorStream())
      );
      stderr =
        errorReader.lines().collect(Collectors.joining(System.lineSeparator()));
      errorReader.close();
      process.waitFor();
      rc = process.exitValue();
      process.destroy();
    } catch (Exception e) {
      // TODO(rkm 2022-02-17) Add a more specific CtpAnonymisationStatus for "SRAnonTool failed"
      throw new Exception(
        "SRAnonTool exec failed with '" + e.getMessage() + "'"
      );
    }

    if (rc != 0) {
      throw new Exception(
        "SRAnonTool exited with " + rc + " and stderr '" + stderr + "'"
      );
    }

    System.out.println("OK");
  }

  private File DoPixelAnon(File inFile, File outFile, DicomObject dObj)
    throws Exception {
    Signature signature = _pixelScript.getMatchingSignature(dObj);

    if (signature == null) {
      throw new Exception("No signature found for pixel anonymization");
    }

    Regions regions = signature.regions;

    if (regions == null || regions.size() == 0) {
      throw new Exception("No regions found for pixel anonymization in inFile");
    }

    boolean decompressed = false;

    if (
      _decompress &&
      dObj.isEncapsulated() &&
      !dObj.getTransferSyntaxUID().equals(JPEGBaseline)
    ) {
      if (DICOMDecompressor.decompress(inFile, outFile).isOK()) {
        try {
          dObj = new DicomObject(outFile);
        } catch (Exception e) {
          throw new Exception("Could not create DicomObject from outFile");
        }

        decompressed = true;
      } else {
        outFile.delete();
        throw new Exception("Could not decompress outfile");
      }
    }

    // Ref:
    // https://github.com/johnperry/CTP/blob/master/source/java/org/rsna/ctp/stdstages/anonymizer/dicom/DICOMPixelAnonymizer.java
    AnonymizerStatus status = DICOMPixelAnonymizer.anonymize(
      dObj.getFile(),
      outFile,
      regions,
      _setBIRElement,
      _testmode
    );

    if (!status.isOK()) {
      outFile.delete();
      throw new Exception(
        "Pixel anonymisation failure: " +
        status.getStatus() +
        "\n" +
        status.getMessage()
      );
    }

    if (decompressed && _recompress) _transcoder.transcode(outFile, outFile);

    return outFile;
  }
}
