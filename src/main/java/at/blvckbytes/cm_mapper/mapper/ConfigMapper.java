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

package at.blvckbytes.cm_mapper.mapper;

import at.blvckbytes.cm_mapper.mapper.section.*;
import at.blvckbytes.component_markup.expression.interpreter.InterpretationEnvironment;
import at.blvckbytes.component_markup.util.logging.InterpreterLogger;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.*;
import java.util.*;
import java.util.stream.Collectors;

public class ConfigMapper implements IConfigMapper {

  private record Tuple<A, B>(A a, B b) {}

  private final IConfig config;
  private final InterpretationEnvironment baseEnvironment;
  private final InterpreterLogger interpreterLogger;
  private final ValueConverter valueConverter;

  public ConfigMapper(
    IConfig config,
    InterpretationEnvironment baseEnvironment,
    InterpreterLogger interpreterLogger,
    ValueConverter valueConverter
  ) {
    this.config = config;
    this.baseEnvironment = baseEnvironment;
    this.interpreterLogger = interpreterLogger;
    this.valueConverter = valueConverter;
  }

  @Override
  public IConfig getConfig() {
    return config;
  }

  @Override
  public <T extends ConfigSection> T mapSection(@Nullable String root, Class<T> type) throws Exception {
    return mapSectionSub(root, null, type);
  }

  /**
   * Recursive, parameterized subroutine for creating an empty config section and then assigning values
   * to its mapped fields automatically, based on their names and types by making use of
   * {@link #resolveFieldValue}. Fields of type object will be decided at
   * runtime, null values may get a default value assigned and incompatible values are tried to be
   * converted before invoking the field setter. If a value still is null after all calls, the field
   * remains unchanged.
   * @param root Root node of this section (null means config root)
   * @param source Alternative value source (map instead of config lookup)
   * @param type Class of the config section to instantiate
   * @return Instantiated class with mapped fields
   */
  private <T extends ConfigSection> T mapSectionSub(@Nullable String root, @Nullable Map<?, ?> source, Class<T> type) throws Exception {
      T instance = findStandardConstructor(type).newInstance(baseEnvironment, interpreterLogger);

      Tuple<List<Field>, Iterator<Field>> fields = findApplicableFields(type);

      while (fields.b().hasNext()) {
        Field f = fields.b().next();
        CSNamed nameAnnotation = f.getAnnotation(CSNamed.class);
        String fName = nameAnnotation == null ? f.getName() : nameAnnotation.name();

        try {
          Class<?> fieldType = f.getType();

          // Object fields trigger a call to runtime decide their type based on previous fields
          if (fieldType == Object.class || f.isAnnotationPresent(CSDecide.class)) {
            Class<?> decidedType = instance.runtimeDecide(fName);

            if (decidedType == null)
              throw new MappingError("Requesting plain objects is disallowed");

            fieldType = decidedType;
          }
          Object value = resolveFieldValue(root, source, f, fieldType);

          // Couldn't resolve a non-null value, try to ask for a default value
          if (value == null)
            value = instance.defaultFor(f);

          if (value != null && !fieldType.isInstance(value))
            value = valueConverter.convert(value, fieldType);

          // Only set if the value isn't null, as the default constructor
          // might have already assigned some default value earlier
          if (value == null)
            continue;

          f.set(instance, value);
        } catch (MappingError error) {
          IllegalStateException exception = new IllegalStateException(error.getMessage() + " (at path '" + joinPaths(root, fName) + "')");
          exception.addSuppressed(error);
          throw exception;
        }
      }

      // This instance won't have any more changes applied to it, call with the list of affected fields
      instance.afterParsing(fields.a());

      return instance;
  }

  /**
   * Find all fields of a class which automated mapping applies to, including inherited fields
   * @param type Class to look through
   * @return A tuple containing the unsorted list as well as an iterator of fields in
   *         the order that fields of type Object come after known types
   */
  private Tuple<List<Field>, Iterator<Field>> findApplicableFields(Class<?> type) {
    List<Field> affectedFields = new ArrayList<>();

    // Walk the class' hierarchy
    Class<?> c = type;
    while (c != Object.class) {
      for (Field f : c.getDeclaredFields()) {
        if (Modifier.isStatic(f.getModifiers()))
          continue;

        if (f.isAnnotationPresent(CSIgnore.class))
          continue;

        if (f.getType() == type)
          throw new IllegalStateException("Sections cannot use self-referencing fields (" + type + ", " + f.getName() + ")");

        f.setAccessible(true);
        affectedFields.add(f);
      }
      c = c.getSuperclass();
    }

    Iterator<Field> fieldI = affectedFields.stream()
      .sorted((a, b) -> {
        if (a.getType() == Object.class && b.getType() == Object.class)
          return 0;

        // Objects are "greater", so they'll be last when sorting ASC
        return a.getType() == Object.class ? 1 : -1;
      }).iterator();

    return new Tuple<>(affectedFields, fieldI);
  }

  /**
   * Resolve a path by either looking it up in the config itself or by resolving it
   * from a previous config response which occurred in the form of a map
   * @param path Path to resolve
   * @param source Map to resolve from instead of querying the config, optional
   * @return Resolved value, null if either the value was null or if it wasn't available
   */
  private @Nullable Object resolvePath(String path, @Nullable Map<?, ?> source) {
    // No object to look in specified, retrieve this path from the config
    if (source == null)
      return config.get(path);

    int dotIndex = path.indexOf('.');

    while (!path.isEmpty()) {
      String key = dotIndex < 0 ? path : path.substring(0, dotIndex);

      if (key.isBlank())
        throw new MappingError("Cannot resolve a blank key");

      path = dotIndex < 0 ? "" : path.substring(dotIndex + 1);
      dotIndex = path.indexOf('.');

      Object value = source.get(key);

      // Last iteration, respond with the current value
      if (path.isEmpty())
        return value;

      // Reached a dead end and not yet at the last iteration
      if (!(value instanceof Map))
        return null;

      // Swap out the current map reference to navigate forwards
      source = (Map<?, ?>) value;
    }

    // Path was blank, which means root
    return source;
  }

  private boolean isInstanceIgnoreBoxing(Class<?> type, Object instance) {
    if (type.isInstance(instance))
      return true;

    if (type == int.class && instance instanceof Integer)
      return true;

    if (type == long.class && instance instanceof Long)
      return true;

    if (type == byte.class && instance instanceof Byte)
      return true;

    if (type == double.class && instance instanceof Double)
      return true;

    if (type == float.class && instance instanceof Float)
      return true;

    if (type == char.class && instance instanceof Character)
      return true;

    return type == boolean.class && instance instanceof Boolean;
  }

  /**
   * Tries to convert the input object to the specified type, by either stringifying,
   * by converting to an enum-constant or by parsing a {@link ConfigSection} if the
   * input is of type map and returning null otherwise. Unsupported types throw.
   * @param input Input object to convert
   * @param type Type to convert to
   */
  private @Nullable Object convertType(@Nullable Object input, Class<?> type) throws Exception {
    if (input == null)
      return null;

    if (!type.isInstance(input))
      input = valueConverter.convert(input, type);

    if (isInstanceIgnoreBoxing(type, input))
      return input;

    if (type == Object.class)
      return input;

    if (type.isEnum()) {
      String upperInput = input.toString().toUpperCase(Locale.ROOT);
      Object[] enumConstants = type.getEnumConstants();

      for (Object enumConstant : enumConstants) {
        if (((Enum<?>)enumConstant).name().equals(upperInput))
          return enumConstant;
      }

      String existingConstants = Arrays.stream(enumConstants)
        .map(it -> ((Enum<?>) it).name())
        .collect(Collectors.joining(", "));

      throw new MappingError("Value \"" + input + "\" was not one of " + existingConstants);
    }

    if (ConfigSection.class.isAssignableFrom(type)) {
      if (!(input instanceof Map))
        input = new HashMap<>();

      Object value = mapSectionSub(null, (Map<?, ?>) input, type.asSubclass(ConfigSection.class));

      return convertType(value, type);
    }

    if (type == String.class)
      return String.valueOf(input);

    throw new MappingError("Unsupported type specified: " + type);
  }

  /**
   * Handles resolving a field of type map based on a previously looked up value
   * @param f Map field which has to be assigned to
   * @param value Previously looked up value
   * @return Value to assign to the field
   */
  private Object handleResolveMapField(Field f, Object value) throws Exception {
    List<Class<?>> genericTypes = getGenericTypes(f);
    assert genericTypes != null && genericTypes.size() == 2;

    Map<Object, Object> result = new LinkedHashMap<>();

    if (!(value instanceof Map))
      return result;

    for (Map.Entry<?, ?> entry : ((Map<?, ?>) value).entrySet()) {
      Object resultKey;
      try {
        resultKey = convertType(entry.getKey(), genericTypes.get(0));
      } catch (MappingError error) {
        throw new MappingError(error.getMessage() + " (at the key of a map)");
      }

      Object resultValue;
      try {
        resultValue = convertType(entry.getValue(), genericTypes.get(1));
      } catch (MappingError error) {
        throw new MappingError(error.getMessage() + " (at value for key=" + resultKey + " of a map)");
      }

      result.put(resultKey, resultValue);
    }

    return result;
  }

  /**
   * Handles resolving a field of type list based on a previously looked up value
   * @param f List field which has to be assigned to
   * @param value Previously looked up value
   * @return Value to assign to the field
   */
  private List<Object> handleResolveListField(Field f, Object value) throws Exception {
    List<Class<?>> genericTypes = getGenericTypes(f);
    assert genericTypes != null && genericTypes.size() == 1;

    List<Object> result = new ArrayList<>();

    if (!(value instanceof List<?> list))
      return result;

    for (int i = 0; i < list.size(); i++) {
      Object itemValue;
      try {
        itemValue = convertType(list.get(i), genericTypes.get(0));
      } catch (MappingError error) {
        throw new MappingError(error.getMessage() + " (at index " + i + " of a list)");
      }

      result.add(itemValue);
    }

    return result;
  }

  /**
   * Handles resolving a field of type array based on a previously looked up value
   * @param f List field which has to be assigned to
   * @param value Previously looked up value
   * @return Value to assign to the field
   */
  private Object handleResolveArrayField(Field f, Object value) throws Exception {
    Class<?> arrayType = f.getType().getComponentType();

    if (!(value instanceof List<?> list))
      return Array.newInstance(arrayType, 0);

    Object array = Array.newInstance(arrayType, list.size());

    for (int i = 0; i < list.size(); i++) {
      Object itemValue;
      try {
        itemValue = convertType(list.get(i), arrayType);
      } catch (MappingError error) {
        throw new MappingError(error.getMessage() + " (at index " + i + " of an array)");
      }

      Array.set(array, i, itemValue);
    }

    return array;
  }

  /**
   * Tries to resolve a field's value based on its type, its annotations, its name and
   * the source (either a path or a source map).
   * @param root Root node of this section (null means config root)
   * @param source Map to resolve from instead of querying the config, optional
   * @param f Field which has to be assigned to
   * @return Value to be assigned to the field
   */
  private @Nullable Object resolveFieldValue(@Nullable String root, @Nullable Map<?, ?> source, Field f, Class<?> type) throws Exception {
    CSNamed nameAnnotation = f.getAnnotation(CSNamed.class);
    String fieldName = nameAnnotation == null ? f.getName() : nameAnnotation.name();
    String path = f.isAnnotationPresent(CSInlined.class) ? root : joinPaths(root, fieldName);
    boolean always = f.isAnnotationPresent(CSAlways.class) || f.getDeclaringClass().isAnnotationPresent(CSAlways.class);

    Object value = resolvePath(path, source);

    // It's not marked as always and the current path doesn't exist: return null
    if (!always && value == null)
      return null;

    if (ConfigSection.class.isAssignableFrom(type))
      return mapSectionSub(path, source, type.asSubclass(ConfigSection.class));

    // Requested plain object
    if (type == Object.class)
      return value;

    if (Map.class.isAssignableFrom(type))
      return handleResolveMapField(f, value);

    if (List.class.isAssignableFrom(type) || Set.class.isAssignableFrom(type)) {
      var result = handleResolveListField(f, value);

      if (Set.class.isAssignableFrom(type))
        return new HashSet<>(result);

      return result;
    }

    if (type.isArray())
      return handleResolveArrayField(f, value);

    return convertType(value, type);
  }

  /**
   * Join two config paths and account for all possible cases
   * @param a Path A (or null/empty)
   * @param b Path B (or null/empty)
   * @return Path A joined with path B
   */
  private String joinPaths(@Nullable String a, @Nullable String b) {
    if (a == null || a.isBlank())
      return b;

    if (b == null || b.isBlank())
      return a;

    if (a.endsWith(".") && b.startsWith("."))
      return a + b.substring(1);

    if (a.endsWith(".") || b.startsWith("."))
      return a + b;

    return a + "." + b;
  }

  /**
   * Find the standard constructor of a class: constructor(EvaluationEnvironmentBuilder)
   * or throw a runtime exception otherwise.
   * @param type Type of the target class
   * @return Standard constructor
   */
  private<T> Constructor<T> findStandardConstructor(Class<T> type) {
    try {
      Constructor<T> constructor = type.getDeclaredConstructor(InterpretationEnvironment.class, InterpreterLogger.class);

      if (!Modifier.isPublic(constructor.getModifiers()))
        throw new IllegalStateException("The standard-constructor of a config-section has to be public");

      return constructor;
    } catch (NoSuchMethodException e) {
      throw new IllegalStateException("Please specify a standard-constructor of scheme (" + InterpretationEnvironment.class + ", " + InterpreterLogger.class + ") on " + type);
    }
  }

  /**
   * Get a list of generic types a field's type declares
   * @param f Target field
   * @return List of generic fields, null if the field's type is not generic
   */
  private @Nullable List<Class<?>> getGenericTypes(Field f) {
    Type genericType = f.getGenericType();

    if (!(genericType instanceof ParameterizedType))
      return null;

    Type[] types = ((ParameterizedType) genericType).getActualTypeArguments();
    List<Class<?>> result = new ArrayList<>();

    for (Type type : types)
      result.add(unwrapType(type));

    return result;
  }

  /**
   * Attempts to unwrap a given type to its raw type class
   * @param type Type to unwrap
   * @return Unwrapped type
   */
  private Class<?> unwrapType(Type type) {
    if (type instanceof Class)
      return (Class<?>) type;

    if (type instanceof ParameterizedType)
      return unwrapType(((ParameterizedType) type).getRawType());

    throw new MappingError("Cannot unwrap type of class=" + type.getClass());
  }
}
