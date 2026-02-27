package com.fulfilment.application.monolith.warehouses.domain.usecases;

import static org.junit.jupiter.api.Assertions.*;

import com.fulfilment.application.monolith.warehouses.domain.models.Warehouse;
import com.fulfilment.application.monolith.warehouses.domain.ports.WarehouseStore;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class ArchiveWarehouseUseCaseTest {

  @Test
  void archive_whenRequestIsNull_throws400() {
    WarehouseStore store = new InMemoryWarehouseStore();
    ArchiveWarehouseUseCase useCase = new ArchiveWarehouseUseCase(store);

    WarehouseOperationException ex =
        assertThrows(WarehouseOperationException.class, () -> useCase.archive(null));

    assertEquals(400, ex.status);
  }

  @Test
  void archive_whenBusinessUnitCodeMissing_throws400() {
    WarehouseStore store = new InMemoryWarehouseStore();
    ArchiveWarehouseUseCase useCase = new ArchiveWarehouseUseCase(store);

    Warehouse request = new Warehouse();
    request.businessUnitCode = "   ";

    WarehouseOperationException ex =
        assertThrows(WarehouseOperationException.class, () -> useCase.archive(request));

    assertEquals(400, ex.status);
  }

  @Test
  void archive_whenWarehouseDoesNotExist_throws404() {
    WarehouseStore store = new InMemoryWarehouseStore();
    ArchiveWarehouseUseCase useCase = new ArchiveWarehouseUseCase(store);

    Warehouse request = new Warehouse();
    request.businessUnitCode = "BU-404";

    WarehouseOperationException ex =
        assertThrows(WarehouseOperationException.class, () -> useCase.archive(request));

    assertEquals(404, ex.status);
  }

  @Test
  void archive_whenAlreadyArchived_isIdempotentAndDoesNotChangeArchivedAt() {
    InMemoryWarehouseStore store = new InMemoryWarehouseStore();

    Warehouse existing = new Warehouse();
    existing.businessUnitCode = "BU-1";
    existing.archivedAt = LocalDateTime.now().minusDays(1);
    store.create(existing);

    ArchiveWarehouseUseCase useCase = new ArchiveWarehouseUseCase(store);

    Warehouse request = new Warehouse();
    request.businessUnitCode = "BU-1";

    LocalDateTime archivedAtBefore = store.findByBusinessUnitCode("BU-1").archivedAt;

    useCase.archive(request);

    LocalDateTime archivedAtAfter = store.findByBusinessUnitCode("BU-1").archivedAt;
    assertEquals(archivedAtBefore, archivedAtAfter);
  }

  @Test
  void archive_whenActive_setsArchivedAtAndPersistsUpdate() {
    InMemoryWarehouseStore store = new InMemoryWarehouseStore();

    Warehouse existing = new Warehouse();
    existing.businessUnitCode = "BU-1";
    existing.archivedAt = null;
    store.create(existing);

    ArchiveWarehouseUseCase useCase = new ArchiveWarehouseUseCase(store);

    Warehouse request = new Warehouse();
    request.businessUnitCode = "BU-1";

    useCase.archive(request);

    Warehouse updated = store.findByBusinessUnitCode("BU-1");
    assertNotNull(updated.archivedAt);
  }

  /** Minimal in-memory WarehouseStore for unit testing. */
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
          w -> w.businessUnitCode != null && warehouse.businessUnitCode != null
              && w.businessUnitCode.equalsIgnoreCase(warehouse.businessUnitCode));
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
