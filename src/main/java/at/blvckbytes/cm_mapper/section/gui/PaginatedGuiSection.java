package at.blvckbytes.cm_mapper.section.gui;

import at.blvckbytes.cm_mapper.cm.ComponentExpression;
import at.blvckbytes.cm_mapper.mapper.MappingError;
import at.blvckbytes.cm_mapper.mapper.section.ConfigSection;
import at.blvckbytes.cm_mapper.mapper.section.CSIgnore;
import at.blvckbytes.component_markup.expression.interpreter.InterpretationEnvironment;
import at.blvckbytes.component_markup.util.logging.InterpreterLogger;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Set;

public abstract class PaginatedGuiSection<T extends ConfigSection> extends GuiSection<T> {

  protected @Nullable ComponentExpression paginationSlots;

  @CSIgnore
  private Set<Integer> _paginationSlots;

  public PaginatedGuiSection(Class<T> itemsSectionClass, InterpretationEnvironment baseEnvironment, InterpreterLogger interpreterLogger) {
    super(itemsSectionClass, baseEnvironment, interpreterLogger);
  }

  @Override
  public void afterParsing(List<Field> fields) throws Exception {
    super.afterParsing(fields);

    _paginationSlots = ComponentExpression.asIntSet(paginationSlots, inventoryEnvironment);

    for (var paginationSlot : _paginationSlots) {
      if (paginationSlot < 0 || paginationSlot > lastSlot)
        throw new MappingError("Pagination slot " + paginationSlot + " out of range [0;" + lastSlot + "]");
    }

    for (var field : itemsSectionClass.getDeclaredFields()) {
      if (!GuiItemStackSection.class.isAssignableFrom(field.getType()))
        continue;

      field.setAccessible(true);

      GuiItemStackSection itemSection = (GuiItemStackSection) field.get(items);

      if (itemSection == null)
        continue;

      for (var itemSlot : itemSection.getDisplaySlots()) {
        if (itemSlot < 0 || itemSlot > lastSlot)
          throw new MappingError("Slot " + itemSlot + " of item " + field.getName() + " out of range [0;" + lastSlot + "]");

        if (_paginationSlots.contains(itemSlot))
          throw new MappingError("Slot " + itemSlot + " of item " + field.getName() + " conflicts with pagination-slots " + paginationSlots);
      }
    }
  }

  public Set<Integer> getPaginationSlots() {
    return _paginationSlots;
  }
}
