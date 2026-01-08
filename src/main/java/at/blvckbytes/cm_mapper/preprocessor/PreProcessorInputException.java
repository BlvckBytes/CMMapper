package at.blvckbytes.cm_mapper.preprocessor;

public class PreProcessorInputException extends RuntimeException {

  public final int lineNumber;
  public final String lineContents;
  public final InputConflict conflict;

  public PreProcessorInputException(int lineNumber, String lineContents, InputConflict conflict) {
    this.lineNumber = lineNumber;
    this.lineContents = lineContents;
    this.conflict = conflict;
  }
}
