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
import at.blvckbytes.cm_mapper.mapper.IConfigMapper;
import at.blvckbytes.cm_mapper.mapper.ValueConverter;
import at.blvckbytes.cm_mapper.mapper.YamlConfig;
import at.blvckbytes.component_markup.expression.interpreter.InterpretationEnvironment;
import at.blvckbytes.component_markup.util.logging.InterpreterLogger;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.function.Executable;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

public class TestHelper {

  private final InterpretationEnvironment emptyEnvironment = new InterpretationEnvironment();
  private final InterpreterLogger nullLogger = (view, position, message, e) -> {};

  /**
   * Create a new config instance and load its contents from a yaml file
   * @param fileName Input file within the resources folder, null to not load at all
   * @return Loaded yaml configuration instance
   */
  public YamlConfig makeConfig(@Nullable String fileName) throws FileNotFoundException {
    YamlConfig config = new YamlConfig();

    if (fileName != null)
      config.load(new FileReader("src/test/resources/" + fileName));

    return config;
  }

  /**
   * Create a new config instance on the provided path and then create a
   * new mapper instance on top of that configuration instance
   * @param fileName Input file within the resources folder
   * @return Mapper instance, operating on the configuration instance
   */
  public IConfigMapper makeMapper(String fileName) throws FileNotFoundException {
    return makeMapper(fileName, (input, type) -> input);
  }

  /**
   * Create a new config instance on the provided path and then create a
   * new mapper instance on top of that configuration instance
   * @param fileName Input file within the resources folder
   * @param valueConverter Value converter to be used
   * @return Mapper instance, operating on the configuration instance
   */
  public IConfigMapper makeMapper(String fileName, ValueConverter valueConverter) throws FileNotFoundException {
    YamlConfig config = makeConfig(fileName);
    return new ConfigMapper(config, emptyEnvironment, nullLogger, valueConverter);
  }

  /**
   * Assert that the provided yaml config saves without throwing and that the saved
   * lines equal to the line contents of the provided comparison file
   * @param fileName Comparison file name within the resources/save folder
   * @param config Configuration to save and compare
   */
  public void assertSave(String fileName, YamlConfig config) throws Exception {
    StringWriter writer = new StringWriter();
    assertDoesNotThrow(() -> config.save(writer));
    List<String> fileContents = Files.readAllLines(Paths.get("src/test/resources/save/" + fileName));
    String writerString = writer.toString();
    List<String> writerContents = Arrays.asList(writerString.split("\n"));
    assertLinesMatch(fileContents, writerContents, "written: \n" + writerString);
  }

  /**
   * Asserts that a given value was present before removal (if existing is true) and
   * that it's absent afterward.
   * @param path Path to remove
   * @param existing Whether the key actually exists
   * @param config Configuration instance to remove on
   */
  public void assertRemovalInMemory(String path, boolean existing, YamlConfig config) {
    assertTrue(!existing || config.exists(path));
    config.remove(path);
    assertFalse(config.exists(path));
  }

  /**
   * Asserts that a given key's comment lines do not match the lines about to append before
   * calling attach as well as their presence afterward.
   * @param path Path to attach at
   * @param lines Lines of comments to attach
   * @param self Whether to attach to the key itself or its value
   * @param config Configuration instance to attach on
   */
  public void assertAttachCommentInMemory(String path, List<String> lines, boolean self, YamlConfig config) {
    assertNotEquals(lines, config.readComment(path, self));
    config.attachComment(path, lines, self);
    assertEquals(lines, config.readComment(path, self));
  }

  /**
   * Asserts that a given key's value does not match the value about to set before
   * calling set and assures the key's value equality with the set value afterward.
   * @param path Path to set at
   * @param value Value to set
   * @param config Configuration instance to set on
   */
  public void assertSetInMemory(String path, Object value, YamlConfig config) {
    assertNotEquals(config.get(path), value);
    config.set(path, value);
    assertEquals(config.get(path), value);
  }

  /**
   * Creates an ordered map of values by joining every even indexed value as a
   * key with an odd indexed value as a corresponding value
   * @param values Key value pairs
   * @return Ordered map
   */
  public Map<Object, Object> map(Object... values) {
    if (values.length % 2 != 0)
      throw new IllegalStateException("Every key needs to be mapped to a value");

    Map<Object, Object> result = new LinkedHashMap<>();

    for (int i = 0; i < values.length; i += 2)
      result.put(values[i], values[i + 1]);

    return result;
  }

  /**
   * Creates an ordered list of values by adding all values to a list
   * @param values Values to add to the list
   * @return Ordered list
   */
  public List<Object> list(Object... values) {
    return new ArrayList<>(Arrays.asList(values));
  }

  /**
   * Asserts that an executable throws an exception of a certain type with a specific message
   * @param expectedType Expected exception type
   * @param executable Executable to run
   * @param containedMessagePart Expected contained part in exception message
   */
  public <T extends Throwable> void assertThrowsWithMsg(Class<T> expectedType, Executable executable, String containedMessagePart) {
    T exception = assertThrows(expectedType, executable);
    assertTrue(exception.getMessage().contains(containedMessagePart), "Exception message did not contain expected message part");
  }
}
