package com.fulfilment.application.monolith.location;

import static org.junit.jupiter.api.Assertions.*;

import com.fulfilment.application.monolith.warehouses.domain.models.Location;
import org.junit.jupiter.api.Test;

public class LocationGatewayTest {

  @Test
  public void resolveByIdentifier_whenNull_returnsNull() {
    LocationGateway gateway = new LocationGateway();
    assertNull(gateway.resolveByIdentifier(null));
  }

  @Test
  public void resolveByIdentifier_whenBlank_returnsNull() {
    LocationGateway gateway = new LocationGateway();
    assertNull(gateway.resolveByIdentifier("   "));
  }

  @Test
  public void resolveByIdentifier_whenExisting_isCaseInsensitiveAndTrims() {
    LocationGateway gateway = new LocationGateway();

    Location location = gateway.resolveByIdentifier("  zwolle-001  ");

    assertNotNull(location);
    assertEquals("ZWOLLE-001", location.identification);
  }

  @Test
  public void resolveByIdentifier_whenUnknown_returnsNull() {
    LocationGateway gateway = new LocationGateway();
    assertNull(gateway.resolveByIdentifier("NOPE-999"));
  }
}
