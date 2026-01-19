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
import at.blvckbytes.cm_mapper.mapper.MappingError;
import at.blvckbytes.cm_mapper.mapper.YamlConfig;
import at.blvckbytes.component_markup.expression.interpreter.InterpretationEnvironment;
import at.blvckbytes.component_markup.markup.ast.tag.built_in.BuiltInTagRegistry;
import at.blvckbytes.component_markup.markup.parser.MarkupParseException;
import at.blvckbytes.component_markup.markup.parser.MarkupParser;
import at.blvckbytes.component_markup.util.ErrorScreen;
import at.blvckbytes.component_markup.util.InputView;
import at.blvckbytes.component_markup.util.logging.InterpreterLogger;
import com.cryptomorin.xseries.XMaterial;
import com.google.common.base.Charsets;
import org.bukkit.Material;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.Nullable;

import java.io.*;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ConfigHandler {

  private final Logger logger;
  private final Plugin plugin;

  private final String folderName;
  private final File folder;

  public ConfigHandler(Plugin plugin, String folderName) {
    this.plugin = plugin;
    this.logger = plugin.getLogger();
    this.folderName = folderName.charAt(0) == '/' ? folderName : ("/" + folderName);

    this.folder = new File(plugin.getDataFolder(), folderName);

    if (!this.folder.exists()) {
      if (!this.folder.mkdirs())
        throw new IllegalStateException("Could not create directories for " + this.folder);
    }
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

      var interpreterLogger = new InterpreterLogger() {
        @Override
        public void log(InputView view, int position, String message, @Nullable Throwable e) {
          for (var line : ErrorScreen.make(view, position, message))
            logger.log(Level.WARNING, "[" + fileName + "] " + line);

          if (e != null)
            logger.log(Level.WARNING, "[" + fileName + "] " + "The following error occurred:", e);
        }
      };

      var baseEnvironment = new InterpretationEnvironment();

      var globalLookupTable = new HashMap<String, Object>();
      baseEnvironment.withVariable("lut", globalLookupTable);

      if (config.get("cLut") instanceof Map<?,?> map) {
        for (var entry : map.entrySet()) {
          var key = String.valueOf(entry.getKey());

          globalLookupTable.put(key, parseLeafNodes(entry.getValue(), interpreterLogger));
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

      return new ConfigMapper(config, baseEnvironment, interpreterLogger, (input, type) -> {
        if (type == ComponentMarkup.class)
          return new ComponentMarkup(String.valueOf(input), baseEnvironment, interpreterLogger);

        if (type == ComponentExpression.class)
          return new ComponentExpression(String.valueOf(input), baseEnvironment, interpreterLogger);

        if (type == Material.class) {
          var materialExpression = new ComponentMarkup(String.valueOf(input), baseEnvironment, interpreterLogger);
          var materialString = materialExpression.asPlainString(null);
          var xMaterial = XMaterial.matchXMaterial(materialString);

          if (xMaterial.isEmpty())
            throw new MappingError("The material \"" + materialString + "\" is not a valid XMaterial-constant");

          return xMaterial.get().get();
        }

        if (type == int.class || type == Integer.class) {
          var numberExpression = new ComponentExpression(String.valueOf(input), baseEnvironment, interpreterLogger);
          return ComponentExpression.asInt(numberExpression, null);
        }

        if (type == double.class || type == Double.class) {
          var numberExpression = new ComponentExpression(String.valueOf(input), baseEnvironment, interpreterLogger);
          return ComponentExpression.asDouble(numberExpression, null);
        }

        return input;
      });
    }
  }

  private Object parseLeafNodes(Object input, InterpreterLogger logger) {
    if (input instanceof List<?> list) {
      for (var index = 0; index < list.size(); ++index) {
        //noinspection unchecked
        ((List<Object>) list).set(index, parseLeafNodes(list.get(index), logger));
      }
      return input;
    }

    if (input instanceof Map<?, ?> map) {
      //noinspection unchecked
      for (var entry : ((Map<?, Object>) map).entrySet())
        entry.setValue(parseLeafNodes(entry.getValue(), logger));

      return input;
    }

    var view = InputView.of(String.valueOf(input));

    try {
      return MarkupParser.parse(view, BuiltInTagRegistry.INSTANCE);
    } catch (MarkupParseException e) {
      logger.log(view, e.position, e.getErrorMessage(), null);
    }

    return input;
  }
}
