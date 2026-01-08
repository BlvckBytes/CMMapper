package at.blvckbytes.cm_mapper;

import java.util.List;

public enum ReloadPriority {
  HIGHEST,
  HIGH,
  MEDIUM,
  LOW,
  LOWEST
  ;

  public static final List<ReloadPriority> VALUES_IN_CALL_ORDER = List.of(values());
}
