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
  private String _SRAnonTool;

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
    String SRAnonTool
  ) {
    _decompress = decompress;
    _recompress = recompress;
    _setBIRElement = setBIRElement;
    _testmode = testmode;
    _SRAnonTool = SRAnonTool;

    if (pixelAnonScriptFile != null) {
      _pixelScript = new PixelScript(pixelAnonScriptFile);
    }

    _tagAnonScriptProps =
      DAScript.getInstance(tagAnonScriptFile).toProperties();

    _transcoder.setTransferSyntax(JPEGLossLess);
  }

  public void anonymize(File inFile, File outFile) {
    // NOTE(rkm 2023-12-01) This ensures CTP won't accidentally clobber the input file
    if (inFile.canWrite()) {
      System.err.println("Input file " + inFile + " was writeable");
      System.exit(1);
    }

    DicomObject dObj = null;
    File origFile = inFile; // inFile gets reassigned so keep a record of it.

    try {
      dObj = new DicomObject(inFile);
    } catch (Exception e) {
      System.err.println("Could not create dicom object from inFile: " + e);
      System.exit(1);
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
        System.err.println("Pixel anon failed: " + e);
        System.exit(1);
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
      System.err.println(
        "DICOMAnonymizer returned status " +
        status.getStatus() +
        ", with message " +
        status.getMessage()
      );
      System.exit(1);
    }

    // Structured Reports have an additional anonymisation step
    if (!isSR) return;

    String commandArray[] = {
      _SRAnonTool,
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
      System.err.println(
        "SRAnonTool exec failed with '" + e.getMessage() + "'"
      );
      System.exit(1);
    }

    if (rc != 0) {
      System.err.println(
        "SRAnonTool exited with " + rc + " and stderr '" + stderr + "'"
      );
      System.exit(1);
    }
  }

  private File DoPixelAnon(File inFile, File outFile, DicomObject dObj) {
    Signature signature = _pixelScript.getMatchingSignature(dObj);

    if (signature == null) {
      System.err.println("No signature found for pixel anonymization");
      System.exit(1);
    }

    Regions regions = signature.regions;

    if (regions == null || regions.size() == 0) {
      System.err.println(
        "No regions found for pixel anonymization in " + inFile
      );
      System.exit(1);
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
          System.err.println(
            "Could not create DicomObject from outFile '" + outFile + "'"
          );
          System.exit(1);
        }

        decompressed = true;
      } else {
        outFile.delete();
        System.err.println("Could not decompress outfile '" + outFile + "'");
        System.exit(1);
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
      System.err.println(
        "Pixel anonymisation failure: " +
        status.getStatus() +
        "\n" +
        status.getMessage()
      );
      System.exit(1);
    }

    if (decompressed && _recompress) _transcoder.transcode(outFile, outFile);

    return outFile;
  }
}
