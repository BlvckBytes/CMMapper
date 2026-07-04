package at.blvckbytes.cm_mapper.section.item;

import at.blvckbytes.cm_mapper.MaterialMatcher;
import at.blvckbytes.cm_mapper.cm.ComponentExpression;
import at.blvckbytes.cm_mapper.cm.ComponentMarkup;
import at.blvckbytes.cm_mapper.mapper.section.ConfigSection;
import at.blvckbytes.component_markup.constructor.SlotType;
import at.blvckbytes.component_markup.expression.interpreter.InterpretationEnvironment;
import at.blvckbytes.component_markup.util.logging.InterpreterLogger;
import com.destroystokyo.paper.profile.ProfileProperty;
import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.TooltipDisplay;
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
  private @Nullable ComponentExpression glint;
  private @Nullable ComponentExpression hideAdditionalTooltips;

  public ItemStackSection(InterpretationEnvironment baseEnvironment, InterpreterLogger interpreterLogger) {
    super(baseEnvironment, interpreterLogger);
  }

  public void patch(ItemStack item, InterpretationEnvironment environment) {
    var meta = item.getItemMeta();

    if (meta == null)
      return;

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

    if (glint != null) {
      var glintValue = glint.interpret(environment);

      if (glintValue != null)
        meta.setEnchantmentGlintOverride(environment.getValueInterpreter().asBoolean(glintValue));
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

    if (hideAdditionalTooltips != null) {
      var hideValue = hideAdditionalTooltips.interpret(environment);

      if (hideValue != null && environment.getValueInterpreter().asBoolean(hideValue))
        hideAdditionalTooltips(item);
    }
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

      var material = MaterialMatcher.tryMatch(typeName);

      if (material != null)
        return material;

      type.log("Could not locate an XMaterial called \"" + typeName + "\"", null);
    }

    return Material.BARRIER;
  }

  @SuppressWarnings("UnstableApiUsage")
  private void hideAdditionalTooltips(ItemStack item) {
    TooltipDisplay.Builder builder = TooltipDisplay.tooltipDisplay();

    for (var type : item.getDataTypes()) {
      if (type == DataComponentTypes.CUSTOM_NAME || type == DataComponentTypes.LORE)
        continue;

      builder.addHiddenComponents(type);
    }

    item.setData(DataComponentTypes.TOOLTIP_DISPLAY, builder.build());
  }
}
