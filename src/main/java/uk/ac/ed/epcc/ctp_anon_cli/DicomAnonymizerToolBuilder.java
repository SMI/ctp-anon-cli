package uk.ac.ed.epcc.ctp_anon_cli;

import java.io.File;

/**
 * Helper class to wrap up all the options required by the DicomAnonymizerTool
 * and build new instances of the class
 */
public class DicomAnonymizerToolBuilder {

  /**
   * Anonymiser script file. No tag anonymisation will be performed if this is
   * null.
   */
  private File _tagAnonScriptFile;

  /**
   * Pixel anonymiser script file. No pixel anonymisation will be performed if
   * this is null.
   */
  private File _pixelAnonScriptFile = null;

  /**
   * Specifies that the image is to be decompressed if the pixel anonymiser
   * requires it.
   */
  private boolean _decompress = false;

  /**
   * Specifies that the image is to be recompressed after pixel anonymisation if
   * it was decompressed.
   */
  private boolean _recompress = false;

  // TODO This is set to true by default, need to check if we want this though.
  /**
   * Sets the BurnedInAnnotation element to NO during pixel anonymisation.
   */
  private boolean _setBIRElement = true;

  /**
   * Specifies that the pixel anonymiser is to blank regions in mid-gray. (True to
   * highlight blanked regions; false to render them in black).
   */
  private boolean _testmode = false;

  /**
   * Specifies that the anonymised image is to be tested to ensure that the images
   * load. <br>
   * null: No checking <br>
   * "" or "last": Checks last frame <br>
   * "first": Checks first frame <br>
   * "all": Checks all frames
   */
  // TODO For some reason the checking functionality throws an exception, skip for
  // now
  // private String _check = "first";
  private String _check = null;

  /**
   * Specifies the name of the executable which will perform SR text anonymisation.
   */
  private String _SRAnonTool = null;

  /**
   * Builds a new DicomAnonymizerTool object from the default and any set values
   *
   * @return New DicomAnonymizerTool object
   */
  public SmiCtpProcessor buildDat() {
    if (_tagAnonScriptFile == null) throw new IllegalArgumentException(
      "daScriptFile cannot be null"
    );

    return new SmiCtpProcessor(
      _tagAnonScriptFile,
      _pixelAnonScriptFile,
      _decompress,
      _recompress,
      _setBIRElement,
      _testmode,
      _check,
      _SRAnonTool
    );
  }

  // TODO Add overloads for some of these to take file paths and create the File
  // objects

  public DicomAnonymizerToolBuilder tagAnonScriptFile(File tagAnonScriptFile) {
    this._tagAnonScriptFile = tagAnonScriptFile;
    return this;
  }

  public DicomAnonymizerToolBuilder pixelAnonScriptFile(
    File pixelAnonScriptFile
  ) {
    this._pixelAnonScriptFile = pixelAnonScriptFile;
    return this;
  }

  public DicomAnonymizerToolBuilder decompress(boolean decompress) {
    this._decompress = decompress;
    return this;
  }

  public DicomAnonymizerToolBuilder recompress(boolean recompress) {
    this._recompress = recompress;
    return this;
  }

  public DicomAnonymizerToolBuilder setBIRElement(boolean setBIRElement) {
    this._setBIRElement = setBIRElement;
    return this;
  }

  public DicomAnonymizerToolBuilder testMode(boolean testMode) {
    this._testmode = testMode;
    return this;
  }

  public DicomAnonymizerToolBuilder check(String check) {
    this._check = check;
    return this;
  }

  public DicomAnonymizerToolBuilder SRAnonTool(String SRAnonTool) {
    this._SRAnonTool = SRAnonTool;
    return this;
  }
}
