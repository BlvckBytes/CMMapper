package at.blvckbytes.cm_mapper.sections;

import at.blvckbytes.cm_mapper.mapper.section.ConfigSection;
import at.blvckbytes.component_markup.expression.interpreter.InterpretationEnvironment;
import at.blvckbytes.component_markup.util.logging.InterpreterLogger;

public class CustomObjectSection extends ConfigSection {

  public CustomObject customObject;

  public CustomObjectSection(InterpretationEnvironment baseEnvironment, InterpreterLogger interpreterLogger) {
    super(baseEnvironment, interpreterLogger);
  }

  public CustomObject getCustomObject() {
    return customObject;
  }
}
