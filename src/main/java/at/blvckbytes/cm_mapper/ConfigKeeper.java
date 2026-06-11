package at.blvckbytes.cm_mapper;

import at.blvckbytes.cm_mapper.mapper.ConfigMapper;
import at.blvckbytes.cm_mapper.mapper.section.ConfigSection;
import org.bukkit.Bukkit;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class ConfigKeeper<T extends ConfigSection> {

  private final ConfigHandler configHandler;
  private final String fileName;
  private final Class<T> rootSectionType;

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
    this.rootSection = loadRootSection();
  }

  public ConfigMapper getConfigMapper() {
    // Already loaded at this point (see constructor)
    return Objects.requireNonNull(configMapper);
  }

  public void reload() throws Exception {
    this.rootSection = loadRootSection();
    Bukkit.getPluginManager().callEvent(new ConfigKeeperReloadEvent(this));
  }

  private T loadRootSection() throws Exception {
    configMapper = this.configHandler.loadConfig(fileName);
    return configMapper.mapSection(null, rootSectionType);
  }
}
