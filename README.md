# CMMapper

## Custom Tags

### hover-stack

This tag accepts a bukkit `ItemStack`-value as the `stack`-attribute which is then
turned into a hover-event (using the Paper API) that is bound to the tag's content.

```
<hover-stack [stack]="your.bukkit_item.here">This text will display the stack when hovered!</>
```

For convenience, the `stack`-attribute also supports a bound expression of arbitrary name.

```
<hover-stack [your.bukkit_item.here]>This text will display the stack when hovered!</>
```
