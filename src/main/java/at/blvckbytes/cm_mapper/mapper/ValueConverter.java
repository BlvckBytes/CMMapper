package at.blvckbytes.cm_mapper.mapper;

public interface ValueConverter {

  Object convert(Object input, Class<?> fieldType);

}
