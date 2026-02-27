package com.fulfilment.application.monolith.warehouses.domain.usecases;

import static org.junit.jupiter.api.Assertions.*;

import com.fulfilment.application.monolith.warehouses.domain.models.Location;
import com.fulfilment.application.monolith.warehouses.domain.models.Warehouse;
import com.fulfilment.application.monolith.warehouses.domain.ports.LocationResolver;
import com.fulfilment.application.monolith.warehouses.domain.ports.WarehouseStore;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class ReplaceWarehouseUseCaseTest {

  @Test
  void replace_whenNoActiveWarehouse_throws404() {
    InMemoryWarehouseStore store = new InMemoryWarehouseStore();
    LocationResolver resolver = id -> location(id, 5, 100);

    ReplaceWarehouseUseCase useCase = new ReplaceWarehouseUseCase(store, resolver);

    WarehouseOperationException ex =
        assertThrows(
            WarehouseOperationException.class,
            () -> useCase.replace(warehouse("BU-X", "ZWOLLE-001", 10, 5)));

    assertEquals(404, ex.status);
  }

  @Test
  void replace_whenStockDoesNotMatch_throws400() {
    InMemoryWarehouseStore store = new InMemoryWarehouseStore();
    LocationResolver resolver = id -> location(id, 5, 100);

    store.create(warehouse("BU-1", "ZWOLLE-001", 10, 5)); // current stock 5

    ReplaceWarehouseUseCase useCase = new ReplaceWarehouseUseCase(store, resolver);

    WarehouseOperationException ex =
        assertThrows(
            WarehouseOperationException.class,
            () -> useCase.replace(warehouse("BU-1", "ZWOLLE-001", 10, 6))); // mismatch

    assertEquals(400, ex.status);
  }

  @Test
  void replace_whenValid_archivesCurrentAndCreatesNewActive() {
    InMemoryWarehouseStore store = new InMemoryWarehouseStore();
    LocationResolver resolver = id -> location("AMSTERDAM-001", 5, 100);

    store.create(warehouse("BU-1", "ZWOLLE-001", 10, 5));

    ReplaceWarehouseUseCase useCase = new ReplaceWarehouseUseCase(store, resolver);

    useCase.replace(warehouse("BU-1", "AMSTERDAM-001", 20, 5));

    List<Warehouse> all = store.getAll();
    assertEquals(2, all.size(), "Should have archived old + created new");

    Warehouse archived = all.stream().filter(w -> w.archivedAt != null).findFirst().orElse(null);
    Warehouse active = all.stream().filter(w -> w.archivedAt == null).findFirst().orElse(null);

    assertNotNull(archived);
    assertNotNull(active);

    assertEquals("BU-1", archived.businessUnitCode);
    assertEquals("BU-1", active.businessUnitCode);

    assertEquals("AMSTERDAM-001", active.location);
    assertEquals(20, active.capacity);
    assertEquals(5, active.stock);

    assertNotNull(active.createdAt);
  }

  @Test
  void replace_whenNewLocationAtMaxWarehouses_throws400() {
    InMemoryWarehouseStore store = new InMemoryWarehouseStore();
    // maxNumberOfWarehouses = 1
    LocationResolver resolver = id -> location("ZWOLLE-001", 1, 100);

    store.create(warehouse("BU-1", "AMSTERDAM-001", 10, 5)); // current
    store.create(warehouse("BU-2", "ZWOLLE-001", 10, 0)); // another active already at target location

    ReplaceWarehouseUseCase useCase = new ReplaceWarehouseUseCase(store, resolver);

    WarehouseOperationException ex =
        assertThrows(
            WarehouseOperationException.class,
            () -> useCase.replace(warehouse("BU-1", "ZWOLLE-001", 10, 5)));

    assertEquals(400, ex.status);
  }

  private static Warehouse warehouse(String bu, String location, Integer capacity, Integer stock) {
    Warehouse w = new Warehouse();
    w.businessUnitCode = bu;
    w.location = location;
    w.capacity = capacity;
    w.stock = stock;
    return w;
  }

  private static Location location(String id, int maxWarehouses, int maxCapacity) {
    return new Location(id, maxWarehouses, maxCapacity);
  }

  /** Minimal in-memory implementation to unit test use cases. */
  private static final class InMemoryWarehouseStore implements WarehouseStore {
    private final List<Warehouse> data = new ArrayList<>();

    @Override
    public List<Warehouse> getAll() {
      return new ArrayList<>(data);
    }

    @Override
    public void create(Warehouse warehouse) {
      data.add(cloneWarehouse(warehouse));
    }

    @Override
    public void update(Warehouse warehouse) {
      // Replace first matching by BU code (close enough for use case unit tests)
      for (int i = 0; i < data.size(); i++) {
        Warehouse existing = data.get(i);
        if (existing.businessUnitCode != null
            && warehouse.businessUnitCode != null
            && existing.businessUnitCode.equalsIgnoreCase(warehouse.businessUnitCode)) {
          data.set(i, cloneWarehouse(warehouse));
          return;
        }
      }
      throw new IllegalArgumentException("Warehouse not found for update: " + warehouse.businessUnitCode);
    }

    @Override
    public void remove(Warehouse warehouse) {
      data.removeIf(
          w -> w.businessUnitCode != null && w.businessUnitCode.equalsIgnoreCase(warehouse.businessUnitCode));
    }

    @Override
    public Warehouse findByBusinessUnitCode(String buCode) {
      if (buCode == null) return null;
      return data.stream()
          .filter(w -> w.businessUnitCode != null && w.businessUnitCode.equalsIgnoreCase(buCode))
          .findFirst()
          .map(InMemoryWarehouseStore::cloneWarehouse)
          .orElse(null);
    }

    private static Warehouse cloneWarehouse(Warehouse w) {
      Warehouse c = new Warehouse();
      c.businessUnitCode = w.businessUnitCode;
      c.location = w.location;
      c.capacity = w.capacity;
      c.stock = w.stock;
      c.createdAt = w.createdAt;
      c.archivedAt = w.archivedAt;
      return c;
    }
  }
}
