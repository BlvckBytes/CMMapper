package at.blvckbytes.cm_mapper;

import at.blvckbytes.cm_mapper.mapper.ConfigMapper;
import at.blvckbytes.cm_mapper.mapper.section.ConfigSection;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class ConfigKeeper<T extends ConfigSection> {

  private final ConfigHandler configHandler;
  private final String fileName;
  private final Class<T> rootSectionType;
  private final Map<ReloadPriority, List<Runnable>> reloadListenersByPriority;

  private @Nullable ConfigMapper configMapper;

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

  public ConfigMapper getConfigMapper() {
    // Already loaded at this point (see constructor)
    return Objects.requireNonNull(configMapper);
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
    configMapper = this.configHandler.loadConfig(fileName);
    return configMapper.mapSection(null, rootSectionType);
  }
}
