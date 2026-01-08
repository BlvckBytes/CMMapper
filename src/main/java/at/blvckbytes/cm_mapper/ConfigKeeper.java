package at.blvckbytes.cm_mapper;

import at.blvckbytes.cm_mapper.mapper.section.AConfigSection;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class ConfigKeeper<T extends AConfigSection> {

  private final ConfigHandler configHandler;
  private final String fileName;
  private final Class<T> rootSectionType;
  private final Map<ReloadPriority, List<Runnable>> reloadListenersByPriority;

  public T rootSection;

  public ConfigKeeper(
    ConfigHandler configHandler,
    String fileName,
    Class<T> rootSectionType
  ) throws Exception {
    this.configHandler = configHandler;
    this.fileName = fileName;
    this.rootSectionType = rootSectionType;
    this.reloadListenersByPriority = new HashMap<>();
    this.rootSection = loadRootSection();
  }

  public void registerReloadListener(Runnable listener, ReloadPriority priority) {
    reloadListenersByPriority.computeIfAbsent(priority, key -> new ArrayList<>()).add(listener);
  }

  public void registerReloadListener(Runnable listener) {
    registerReloadListener(listener, ReloadPriority.MEDIUM);
  }

  public void reload() throws Exception {
    this.rootSection = loadRootSection();

    for (var priority : ReloadPriority.VALUES_IN_CALL_ORDER) {
      var listeners = reloadListenersByPriority.get(priority);

      if (listeners == null)
        continue;

      for (var listener : listeners)
        listener.run();
    }
  }

  private T loadRootSection() throws Exception {
    return this.configHandler.loadConfig(fileName).mapSection(null, rootSectionType);
  }
}
