package at.blvckbytes.cm_mapper.section.command;

import at.blvckbytes.cm_mapper.cm.ComponentMarkup;
import at.blvckbytes.cm_mapper.mapper.section.ConfigSection;
import at.blvckbytes.cm_mapper.mapper.section.CSIgnore;
import at.blvckbytes.component_markup.expression.interpreter.InterpretationEnvironment;
import at.blvckbytes.component_markup.util.logging.InterpreterLogger;
import org.bukkit.command.Command;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

public abstract class CommandSection extends ConfigSection {

  private @Nullable ComponentMarkup name;
  private @Nullable List<ComponentMarkup> aliases;

  @CSIgnore
  public String evaluatedName;

  @CSIgnore
  private List<String> evaluatedAliases;

  @CSIgnore
  public final String initialName;

  public CommandSection(String initialName, InterpretationEnvironment baseEnvironment, InterpreterLogger interpreterLogger) {
    super(baseEnvironment, interpreterLogger);

    this.initialName = initialName;
  }

  @Override
  public void afterParsing(List<Field> fields) throws Exception {
    super.afterParsing(fields);

    evaluatedName = name != null ? name.asPlainString(null) : initialName;
    evaluatedAliases = new ArrayList<>();

    if (aliases != null) {
      for (var alias : aliases)
        evaluatedAliases.add(alias.asPlainString(null));
    }
  }

  public void apply(Command command, CommandUpdater commandUpdater) {
    if (!commandUpdater.tryUnregisterCommand(command))
      return;

    command.setAliases(evaluatedAliases);
    command.setName(evaluatedName);

    commandUpdater.tryRegisterCommand(command);
  }

  public boolean isLabel(String label) {
    if (evaluatedName.equalsIgnoreCase(label))
      return true;

    for (var evaluatedAlias : evaluatedAliases) {
      if (evaluatedAlias.equalsIgnoreCase(label))
        return true;
    }

    return false;
  }

  public String getShortestNameOrAlias() {
    if (evaluatedAliases.isEmpty())
      return evaluatedName;

    var shortestValue = evaluatedName;

    for (var alias : evaluatedAliases) {
      if (alias.length() < shortestValue.length())
        shortestValue = alias;
    }

    return shortestValue;
  }
}
