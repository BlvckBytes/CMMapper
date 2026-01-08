package at.blvckbytes.cm_mapper.preprocessor;

public class PreProcessorException extends RuntimeException {

  public final int charIndex;
  public final PreProcessConflict conflict;

  public PreProcessorException(int charIndex, PreProcessConflict conflict) {
    this.charIndex = charIndex;
    this.conflict = conflict;
  }
}
