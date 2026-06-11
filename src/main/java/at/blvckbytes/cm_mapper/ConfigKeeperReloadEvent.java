package at.blvckbytes.cm_mapper;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

public class ConfigKeeperReloadEvent extends Event {

  private static final HandlerList handlers = new HandlerList();

  public final ConfigKeeper<?> configKeeper;

  public ConfigKeeperReloadEvent(ConfigKeeper<?> configKeeper) {
    this.configKeeper = configKeeper;
  }

  @Override
  public @NotNull HandlerList getHandlers() {
    return handlers;
  }

  @NotNull
  public static HandlerList getHandlerList() {
    return handlers;
  }
}
