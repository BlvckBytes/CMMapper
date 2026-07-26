package at.blvckbytes.cm_mapper.section.gui;

import org.bukkit.inventory.ItemStack;

@FunctionalInterface
public interface ItemConsumer {

  void handle(int slot, ItemStack item);

}
