package at.blvckbytes.cm_mapper;

import com.cryptomorin.xseries.XMaterial;
import org.bukkit.Material;
import org.jetbrains.annotations.Nullable;

public class MaterialMatcher {

  public static @Nullable Material tryMatch(String name) {
    name = name.trim().toUpperCase();

    var xMaterial = XMaterial.matchXMaterial(name);

    Material material;

    if (xMaterial.isPresent() && (material = xMaterial.get().get()) != null)
      return material;

    try {
      return Material.valueOf(name);
    } catch (Throwable ignored) {}

    return null;
  }
}
