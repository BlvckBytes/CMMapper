package at.blvckbytes.cm_mapper.section.command;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandMap;
import org.bukkit.command.SimpleCommandMap;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Locale;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

public class CommandUpdater {

  private final Field commandMapField;
  private final Method syncCommandsMethod;

  private final SimpleCommandMap commandMap;
  private final Map<String, Command> commandMapCommands;

  private final Logger logger;
  private final String pluginPrefix;

  public CommandUpdater(Plugin plugin) {
    this.logger = plugin.getLogger();
    this.pluginPrefix = plugin.getName().toLowerCase(Locale.ROOT);

    var craftServerClass = locateCraftServerClass(Bukkit.getServer().getClass().getPackageName());

    this.commandMapField = locateCommandMapField();
    this.commandMap = locateCommandMap(craftServerClass);
    this.commandMapCommands = locateCommandMapCommands(this.commandMap);
    this.syncCommandsMethod = locateSyncCommandsMethod(craftServerClass);
  }

  public boolean tryUnregisterCommand(Command command) {
    try {
      var commandMap = (CommandMap) commandMapField.get(command);

      if (commandMap == null)
        throw new IllegalStateException("Expected command " + command.getName() + " to be registered");

      command.unregister(commandMap);

      var targetNames = new ArrayList<String>();

      for (var commandEntry : commandMapCommands.entrySet()) {
        if (commandEntry.getValue() == command)
          targetNames.add(commandEntry.getKey());
      }

      for (var targetName : targetNames)
        commandMapCommands.remove(targetName);

      return true;
    } catch (Exception e) {
      logger.log(Level.SEVERE, "Could not unregister command " + command.getName(), e);
      return false;
    }
  }

  public boolean tryRegisterCommand(Command command) {
    try {
      commandMap.register(pluginPrefix, command);
      return true;
    } catch (Exception e) {
      logger.log(Level.SEVERE, "Could not register command " + command.getName(), e);
      return false;
    }
  }

  public void trySyncCommands() {
    try {
      syncCommandsMethod.invoke(Bukkit.getServer());
    } catch (Exception e) {
      logger.log(Level.SEVERE, "Could not sync commands", e);
    }
  }

  private SimpleCommandMap locateCommandMap(Class<?> craftServerClass) {
    try {
      for (var field : craftServerClass.getDeclaredFields()) {
        if (SimpleCommandMap.class.isAssignableFrom(field.getType())) {
          field.setAccessible(true);
          return (SimpleCommandMap) field.get(Bukkit.getServer());
        }
      }

      throw new IllegalStateException("No field was of type SimpleCommandMap");
    } catch (Exception e) {
      throw new IllegalStateException("Could not locate the CommandMap", e);
    }
  }

  @SuppressWarnings("unchecked")
  private Map<String, Command> locateCommandMapCommands(SimpleCommandMap commandMap) {
    try {
      for (var field : SimpleCommandMap.class.getDeclaredFields()) {
        if (!field.getName().equals("knownCommands"))
          continue;

        field.setAccessible(true);
        return (Map<String, Command>) field.get(commandMap);
      }

      throw new IllegalStateException("Encountered no field named knownCommands");
    } catch (Exception e) {
      throw new IllegalStateException("Could not locate commands within CommandMap");
    }
  }

  private Method locateSyncCommandsMethod(Class<?> craftServerClass) {
    try {
      return craftServerClass.getDeclaredMethod("syncCommands");
    } catch (Exception e) {
      throw new IllegalStateException("Could not locate CraftServer#syncCommands", e);
    }
  }

  private Class<?> locateCraftServerClass(String bukkitPackage) {
    try {
      return Class.forName(bukkitPackage + ".CraftServer");
    } catch (Exception e) {
      throw new IllegalStateException("Could not locate the CraftServer class", e);
    }
  }

  private Field locateCommandMapField() {
    for (var field : Command.class.getDeclaredFields()) {
      if (field.getType() != CommandMap.class)
        continue;

      field.setAccessible(true);
      return field;
    }

    throw new IllegalStateException("Could not locate command-map field");
  }
}
