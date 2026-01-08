package at.blvckbytes.cm_mapper.preprocessor;

import java.io.IOException;
import java.io.Writer;

public interface PreProcessorLine {

  void append(Writer writer) throws IOException;

}
