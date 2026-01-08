package at.blvckbytes.cm_mapper.sections;

import at.blvckbytes.cm_mapper.mapper.section.ConfigSection;
import at.blvckbytes.component_markup.expression.interpreter.InterpretationEnvironment;

public class CustomObjectSection extends ConfigSection {

  public CustomObject customObject;

  public CustomObjectSection(InterpretationEnvironment baseEnvironment) {
    super(baseEnvironment);
  }

  public CustomObject getCustomObject() {
    return customObject;
  }
}
