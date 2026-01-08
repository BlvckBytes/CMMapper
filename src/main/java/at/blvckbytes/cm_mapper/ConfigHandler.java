/*
 * MIT License
 *
 * Copyright (c) 2023 BlvckBytes
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package at.blvckbytes.cm_mapper;

import at.blvckbytes.cm_mapper.mapper.ConfigMapper;
import at.blvckbytes.cm_mapper.mapper.YamlConfig;
import com.google.common.base.Charsets;
import org.bukkit.plugin.Plugin;

import java.io.*;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ConfigHandler {

  private final Map<String, ConfigMapper> mapperByFileName;

  private final Logger logger;
  private final Plugin plugin;

  private final String folderName;
  private final File folder;

  public ConfigHandler(Plugin plugin, String folderName) {
    this.mapperByFileName = new HashMap<>();

    this.plugin = plugin;
    this.logger = plugin.getLogger();
    this.folderName = folderName.charAt(0) == '/' ? folderName : ("/" + folderName);

    this.folder = new File(plugin.getDataFolder(), folderName);

    if (!this.folder.exists()) {
      if (!this.folder.mkdirs())
        throw new IllegalStateException("Could not create directories for " + this.folder);
    }
  }

  public ConfigMapper getMapper(String fileName) throws FileNotFoundException {
    var mapper = mapperByFileName.get(fileName.toLowerCase());

    if (mapper == null)
      throw new FileNotFoundException("Could not find the config at " + folderName + "/" + fileName);

    return mapper;
  }

  private String getPluginResourcePath(String fileName) {
    return folderName.substring(1) + "/" + fileName;
  }

  private int extendConfig(String fileName, YamlConfig config) throws Exception {
    var resourcePath = getPluginResourcePath(fileName);

    try (
      InputStream resourceStream = this.plugin.getResource(resourcePath)
    ) {
      if (resourceStream == null)
        throw new IllegalStateException("Could not load resource file at " + resourcePath);

      YamlConfig resourceConfig = new YamlConfig();

      try (
        var resourceStreamReader = new InputStreamReader(resourceStream, Charsets.UTF_8)
      ) {
        resourceConfig.load(resourceStreamReader);
      }

      return config.extendMissingKeys(resourceConfig);
    }
  }

  private void saveConfig(YamlConfig config, String fileName) throws Exception {
    File file = new File(this.folder, fileName);

    if (file.exists() && !file.isFile())
      throw new IllegalStateException("Tried to write file; unexpected directory at " + file);

    try (
      FileOutputStream outputStream = new FileOutputStream(file);
      OutputStreamWriter outputWriter = new OutputStreamWriter(outputStream)
    ) {
      config.save(outputWriter);
    }
  }

  public ConfigMapper loadConfig(String fileName) throws Exception {
    boolean hasBeenCreated = false;

    File file = new File(this.folder, fileName);

    if (file.exists()) {
      if (file.isDirectory())
        throw new IllegalStateException("Tried to read file; unexpected directory at " + file);
    } else {
      this.plugin.saveResource(getPluginResourcePath(fileName), true);
      hasBeenCreated = true;
    }

    try (
      var inputStream = new FileInputStream(file);
      var inputStreamReader = new InputStreamReader(inputStream, Charsets.UTF_8)
    ) {
      YamlConfig config = new YamlConfig();

      config.load(inputStreamReader);

      if (!hasBeenCreated) {
        int numExtendedKeys = extendConfig(fileName, config);

        if (numExtendedKeys > 0) {
          this.logger.log(Level.INFO, "Extended " + numExtendedKeys + " new keys on the configuration " + fileName);
          saveConfig(config, fileName);
        }
      }

      ConfigMapper mapper = new ConfigMapper(config, this::configValueConverter);
      mapperByFileName.put(fileName.toLowerCase(), mapper);
      return mapper;
    }
  }

  private Object configValueConverter(Object input, Class<?> fieldType) {
    return input;
  }
}
