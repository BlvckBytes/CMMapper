package at.blvckbytes.cm_mapper;

public interface ValueConverter {

  Object convert(Object input, Class<?> fieldType);

}
