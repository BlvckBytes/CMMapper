package at.blvckbytes.cm_mapper.section.gui;

import at.blvckbytes.cm_mapper.cm.ComponentExpression;
import at.blvckbytes.cm_mapper.cm.ComponentMarkup;
import at.blvckbytes.cm_mapper.mapper.MappingError;
import at.blvckbytes.cm_mapper.mapper.section.ConfigSection;
import at.blvckbytes.cm_mapper.mapper.section.CSAlways;
import at.blvckbytes.cm_mapper.mapper.section.CSDecide;
import at.blvckbytes.cm_mapper.mapper.section.CSIgnore;
import at.blvckbytes.component_markup.constructor.SlotType;
import at.blvckbytes.component_markup.expression.interpreter.InterpretationEnvironment;
import org.bukkit.Bukkit;
import org.bukkit.inventory.Inventory;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;
import java.util.List;

public class GuiSection<T extends ConfigSection> extends ConfigSection {

  private static final int DEFAULT_ROWS = 3;

  protected @Nullable ComponentMarkup title;
  protected @Nullable ComponentExpression rows;

  @CSAlways
  @CSDecide
  public T items;

  @CSIgnore
  protected final Class<T> itemsSectionClass;

  @CSIgnore
  protected int _rows, lastSlot;

  @CSIgnore
  public InterpretationEnvironment inventoryEnvironment;

  public GuiSection(Class<T> itemsSectionClass) {
    this.itemsSectionClass = itemsSectionClass;
  }

  @Override
  public @Nullable Class<?> runtimeDecide(String field) {
    if (field.equals("items"))
      return itemsSectionClass;

    return super.runtimeDecide(field);
  }

  @Override
  public void afterParsing(List<Field> fields) throws Exception {
    super.afterParsing(fields);

    _rows = DEFAULT_ROWS;

    if (rows != null) {
      var rowsValue = rows.interpret(null);

      if (rowsValue != null)
        _rows = (int) InterpretationEnvironment.DEFAULT_INTERPRETER.asLong(rowsValue);
    }

    if (_rows < 1 || _rows > 6)
      throw new MappingError("Rows out of range [1;6]");

    lastSlot = _rows * 9 - 1;

    inventoryEnvironment = new InterpretationEnvironment()
      .withVariable("number_of_rows", _rows)
      .withVariable("last_slot", lastSlot);

    for (var field : itemsSectionClass.getDeclaredFields()) {
      if (!GuiItemStackSection.class.isAssignableFrom(field.getType()))
        continue;

      field.setAccessible(true);
      ((GuiItemStackSection) field.get(items)).initializeDisplaySlots(inventoryEnvironment);
    }
  }

  public Inventory createInventory(InterpretationEnvironment environment) {
    if (title == null)
      return Bukkit.createInventory(null, _rows * 9);

    var titleComponent = title.interpret(SlotType.SINGLE_LINE_CHAT, environment).get(0);

    return Bukkit.createInventory(null, _rows * 9, titleComponent);
  }

  public int getRows() {
    return _rows;
  }
}
