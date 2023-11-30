package uk.ac.ed.epcc.ctp_anon_cli;

public enum CtpAnonymisationStatus {
  /**
   * The file has been anonymised successfully
   */
  Anonymised(0),

  /**
   * Error with the input file
   */
  InputFileException(1),

  /**
   * The pixel anonymisation failed
   */
  PixelAnonFailed(2),

  /**
   * The tag anonymisation failed
   */
  TagAnonFailed(3),

  /**
   * The checks of the anonymised file failed
   */
  OutputFileChecksFailed(3);

  public final int status;

  private CtpAnonymisationStatus(int status) {
    this.status = status;
  }
}
