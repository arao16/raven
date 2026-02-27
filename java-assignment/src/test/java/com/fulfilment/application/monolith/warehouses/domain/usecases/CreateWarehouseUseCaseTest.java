package com.fulfilment.application.monolith.warehouses.domain.usecases;

import static org.junit.jupiter.api.Assertions.*;

import com.fulfilment.application.monolith.warehouses.domain.models.Location;
import com.fulfilment.application.monolith.warehouses.domain.models.Warehouse;
import com.fulfilment.application.monolith.warehouses.domain.ports.LocationResolver;
import com.fulfilment.application.monolith.warehouses.domain.ports.WarehouseStore;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class CreateWarehouseUseCaseTest {

  @Test
  void create_whenBusinessUnitAlreadyExists_throws() {
    InMemoryWarehouseStore store = new InMemoryWarehouseStore();
    store.create(warehouse("BU-1", "ZWOLLE-001", 10, 5));

    CreateWarehouseUseCase useCase = new CreateWarehouseUseCase(store, id -> location(id, 2, 100));

    Warehouse request = warehouse("BU-1", "ZWOLLE-001", 10, 5);

    WarehouseOperationException ex =
        assertThrows(WarehouseOperationException.class, () -> useCase.create(request));

    assertEquals(400, ex.status);
  }

  @Test
  void create_whenLocationInvalid_throws() {
    InMemoryWarehouseStore store = new InMemoryWarehouseStore();
    LocationResolver resolver = id -> null;

    CreateWarehouseUseCase useCase = new CreateWarehouseUseCase(store, resolver);

    WarehouseOperationException ex =
        assertThrows(
            WarehouseOperationException.class,
            () -> useCase.create(warehouse("BU-2", "UNKNOWN", 10, 5)));

    assertEquals(400, ex.status);
  }

  @Test
  void create_whenMaxWarehousesAtLocationReached_throws() {
    InMemoryWarehouseStore store = new InMemoryWarehouseStore();
    // location maxNumberOfWarehouses = 1
    LocationResolver resolver = id -> location(id, 1, 100);

    store.create(warehouse("BU-1", "ZWOLLE-001", 10, 5)); // active at location already

    CreateWarehouseUseCase useCase = new CreateWarehouseUseCase(store, resolver);

    WarehouseOperationException ex =
        assertThrows(
            WarehouseOperationException.class,
            () -> useCase.create(warehouse("BU-2", "ZWOLLE-001", 10, 5)));

    assertEquals(400, ex.status);
  }

  @Test
  void create_whenLocationMaxCapacityExceeded_throws() {
    InMemoryWarehouseStore store = new InMemoryWarehouseStore();
    // maxCapacity = 15
    LocationResolver resolver = id -> location(id, 5, 15);

    store.create(warehouse("BU-1", "ZWOLLE-001", 10, 0)); // used = 10 already

    CreateWarehouseUseCase useCase = new CreateWarehouseUseCase(store, resolver);

    WarehouseOperationException ex =
        assertThrows(
            WarehouseOperationException.class,
            () -> useCase.create(warehouse("BU-2", "ZWOLLE-001", 10, 0))); // 10 + 10 > 15

    assertEquals(400, ex.status);
  }

  @Test
  void create_whenValid_persistsWarehouseAndNormalizesLocation() {
    InMemoryWarehouseStore store = new InMemoryWarehouseStore();
    LocationResolver resolver = id -> location("ZWOLLE-001", 5, 100);

    CreateWarehouseUseCase useCase = new CreateWarehouseUseCase(store, resolver);

    useCase.create(warehouse(" BU-9 ", "  zwolle-001  ", 10, 5));

    Warehouse created = store.findByBusinessUnitCode("BU-9");
    assertNotNull(created);
    assertEquals("BU-9", created.businessUnitCode);
    assertEquals("ZWOLLE-001", created.location);
    assertNotNull(created.createdAt);
    assertNull(created.archivedAt);
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
      data.removeIf(w -> w.businessUnitCode != null && w.businessUnitCode.equalsIgnoreCase(warehouse.businessUnitCode));
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
