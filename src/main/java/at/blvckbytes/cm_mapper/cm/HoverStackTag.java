package at.blvckbytes.cm_mapper.cm;

import at.blvckbytes.component_markup.constructor.SlotType;
import at.blvckbytes.component_markup.expression.ast.ExpressionNode;
import at.blvckbytes.component_markup.markup.ast.node.FunctionDrivenNode;
import at.blvckbytes.component_markup.markup.ast.node.MarkupNode;
import at.blvckbytes.component_markup.markup.ast.node.control.ContainerNode;
import at.blvckbytes.component_markup.markup.ast.node.terminal.RawNode;
import at.blvckbytes.component_markup.markup.ast.tag.*;
import at.blvckbytes.component_markup.markup.interpreter.MarkupInterpreter;
import at.blvckbytes.component_markup.markup.parser.token.TokenEmitter;
import at.blvckbytes.component_markup.util.InputView;
import net.kyori.adventure.text.Component;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashSet;
import java.util.List;

public class HoverStackTag extends TagDefinition {

  public HoverStackTag() {
    super(TagClosing.OPEN_CLOSE, TagPriority.NORMAL);
  }

  @Override
  public boolean matchName(InputView tagName) {
    return tagName.contentEquals("hover-stack", true);
  }

  @Override
  public @NotNull MarkupNode createNode(
    @Nullable TokenEmitter tokenEmitter,
    @NotNull InputView tagName,
    boolean selfClosing,
    @NotNull AttributeMap attributes,
    @Nullable LinkedHashSet<LetBinding> letBindings,
    @Nullable List<MarkupNode> children
  ) {
    ExpressionNode flagValue = attributes.getOptionalBoundFlagExpressionNode();
    ExpressionNode stackAttribute = flagValue == null ? attributes.getMandatoryExpressionNode("stack") : flagValue;

    return new FunctionDrivenNode(tagName, letBindings, interpreter -> {
      var stackValue = interpreter.evaluateAsPlainObject(stackAttribute);

      if (!(stackValue instanceof ItemStack itemStack)) {
        interpreter.getLogger().logErrorScreen(stackAttribute.getFirstMemberPositionProvider(), "Expected a value of type ItemStack, but found " + (stackValue == null ? "null" : stackValue.getClass()));
        return null;
      }

      var hoveredComponent = (Component) MarkupInterpreter.interpret(
        new ContainerNode(tagName, children, null),
        SlotType.SINGLE_LINE_CHAT,
        interpreter.getEnvironment(),
        interpreter.getComponentConstructor(),
        interpreter.getLogger()
      ).getFirst();

      return new RawNode(hoveredComponent.hoverEvent(itemStack.asHoverEvent()));
    });
  }
}
