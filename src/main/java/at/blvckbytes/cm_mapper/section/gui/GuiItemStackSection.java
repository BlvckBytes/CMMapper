package at.blvckbytes.cm_mapper.section.gui;

import at.blvckbytes.cm_mapper.cm.ComponentExpression;
import at.blvckbytes.cm_mapper.mapper.section.CSIgnore;
import at.blvckbytes.cm_mapper.section.item.ItemStackSection;
import at.blvckbytes.component_markup.expression.interpreter.InterpretationEnvironment;
import org.bukkit.inventory.Inventory;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Set;

public class GuiItemStackSection extends ItemStackSection {

  private @Nullable ComponentExpression slots;

  @CSIgnore
  private @Nullable Set<Integer> displaySlots;

  public GuiItemStackSection(InterpretationEnvironment baseEnvironment) {
    super(baseEnvironment);
  }

  @Override
  public void afterParsing(List<Field> fields) throws Exception {
    super.afterParsing(fields);
  }

  public void initializeDisplaySlots(InterpretationEnvironment inventoryEnvironment) {
    displaySlots = ComponentExpression.asIntSet(slots, inventoryEnvironment);
  }

  public Set<Integer> getDisplaySlots() {
    return displaySlots == null ? Set.of() : displaySlots;
  }

  public void renderInto(Inventory inventory, InterpretationEnvironment environment) {
    if (displaySlots == null)
      return;

    var item = build(environment);
    var inventorySize = inventory.getSize();

    for (var slot : displaySlots) {
      if (slot < inventorySize)
        inventory.setItem(slot, item);
    }
  }
}
