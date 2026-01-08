package at.blvckbytes.cm_mapper.section.item;

import at.blvckbytes.cm_mapper.cm.ComponentExpression;
import at.blvckbytes.cm_mapper.cm.ComponentMarkup;
import at.blvckbytes.cm_mapper.mapper.section.ConfigSection;
import at.blvckbytes.component_markup.constructor.SlotType;
import at.blvckbytes.component_markup.expression.interpreter.InterpretationEnvironment;
import at.blvckbytes.component_markup.util.logging.InterpreterLogger;
import com.cryptomorin.xseries.XMaterial;
import com.destroystokyo.paper.profile.ProfileProperty;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public class ItemStackSection extends ConfigSection {

  private @Nullable ComponentMarkup type;
  private @Nullable ComponentMarkup name;
  private @Nullable ComponentMarkup lore;
  private @Nullable ComponentExpression amount;
  private @Nullable ComponentMarkup textures;

  public ItemStackSection(InterpretationEnvironment baseEnvironment, InterpreterLogger interpreterLogger) {
    super(baseEnvironment, interpreterLogger);
  }

  public void patch(ItemStack item, InterpretationEnvironment environment) {
    var meta = item.getItemMeta();

    if (amount != null) {
      var amountValue = amount.interpret(environment);

      if (amountValue != null)
        item.setAmount((int) environment.getValueInterpreter().asLong(amountValue));
    }

    if (name != null)
      meta.displayName(name.interpret(SlotType.ITEM_NAME, environment).get(0));

    if (lore != null) {
      var finalLore = meta.lore();
      var additionalLore = lore.interpret(SlotType.ITEM_LORE, environment);

      if (finalLore == null)
        finalLore = additionalLore;
      else
        finalLore.addAll(additionalLore);

      meta.lore(finalLore);
    }

    if (textures != null) {
      var texturesValue = textures.asPlainString(environment);

      if (!texturesValue.isBlank() && meta instanceof SkullMeta skullMeta) {
        var profile = Bukkit.createProfile(UUID.randomUUID(), null);
        profile.setProperty(new ProfileProperty("textures", texturesValue));
        skullMeta.setPlayerProfile(profile);
      }
    }

    item.setItemMeta(meta);
  }

  public ItemStack build(InterpretationEnvironment environment) {
    var result = new ItemStack(getMaterial(environment));

    patch(result, environment);

    return result;
  }

  private Material getMaterial(InterpretationEnvironment environment) {
    if (type != null) {
      var typeName = type.asPlainString(environment);

      if (typeName.isBlank())
        return Material.BARRIER;

      var xMaterial = XMaterial.matchXMaterial(typeName);

      if (xMaterial.isPresent())
        return xMaterial.get().get();

      type.log("Could not locate an XMeterial called \"" + typeName + "\"", null);
    }

    return Material.BARRIER;
  }
}
