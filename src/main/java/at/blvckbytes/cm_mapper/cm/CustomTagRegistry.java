package at.blvckbytes.cm_mapper.cm;

import at.blvckbytes.component_markup.markup.ast.tag.TagRegistry;
import at.blvckbytes.component_markup.markup.ast.tag.built_in.BuiltInTagRegistry;

public class CustomTagRegistry extends BuiltInTagRegistry {

  public static final TagRegistry INSTANCE = new CustomTagRegistry();

  public CustomTagRegistry() {
    register(new HoverStackTag());
  }
}
