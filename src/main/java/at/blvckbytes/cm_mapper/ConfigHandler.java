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

import at.blvckbytes.cm_mapper.cm.ComponentExpression;
import at.blvckbytes.cm_mapper.cm.ComponentMarkup;
import at.blvckbytes.cm_mapper.mapper.ConfigMapper;
import at.blvckbytes.cm_mapper.mapper.YamlConfig;
import at.blvckbytes.component_markup.expression.interpreter.InterpretationEnvironment;
import at.blvckbytes.component_markup.markup.ast.tag.built_in.BuiltInTagRegistry;
import at.blvckbytes.component_markup.markup.parser.MarkupParseException;
import at.blvckbytes.component_markup.markup.parser.MarkupParser;
import at.blvckbytes.component_markup.util.ErrorScreen;
import at.blvckbytes.component_markup.util.InputView;
import at.blvckbytes.component_markup.util.logging.InterpreterLogger;
import com.google.common.base.Charsets;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.Nullable;

import java.io.*;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ConfigHandler implements InterpreterLogger {

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

      var baseEnvironment = new InterpretationEnvironment();

      var globalLookupTable = new HashMap<String, Object>();
      baseEnvironment.withVariable("lut", globalLookupTable);

      if (config.get("cLut") instanceof Map<?,?> map) {
        for (var entry : map.entrySet()) {
          var key = String.valueOf(entry.getKey());
          var view = InputView.of(String.valueOf(entry.getValue()));

          try {
            globalLookupTable.put(key, MarkupParser.parse(view, BuiltInTagRegistry.INSTANCE));
          } catch (MarkupParseException e) {
            log(view, e.position, e.getErrorMessage(), null);
          }
        }
      }

      if (config.get("sLut") instanceof Map<?,?> map) {
        for (var entry : map.entrySet()) {
          var key = String.valueOf(entry.getKey());

          if (globalLookupTable.keySet().stream().anyMatch(key::equalsIgnoreCase))
            logger.warning("Duplicate s-lut-entry \"" + key + "\" in " + fileName);

          globalLookupTable.put(key, entry.getValue());
        }
      }

      ConfigMapper mapper = new ConfigMapper(config, (input, type) -> {
        if (type == ComponentMarkup.class)
          return new ComponentMarkup(String.valueOf(input), baseEnvironment, this);

        if (type == ComponentExpression.class)
          return new ComponentExpression(String.valueOf(input), baseEnvironment, this);

        return input;
      });

      mapperByFileName.put(fileName.toLowerCase(), mapper);
      return mapper;
    }
  }

  @Override
  public void log(InputView view, int position, String message, @Nullable Throwable e) {
    for (var line : ErrorScreen.make(view, position, message))
      logger.log(Level.WARNING, line);

    if (e != null)
      logger.log(Level.WARNING, "The following error occurred:", e);
  }
}
