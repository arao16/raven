package com.fulfilment.application.monolith.warehouses.domain.usecases;

import com.fulfilment.application.monolith.warehouses.domain.models.Warehouse;
import com.fulfilment.application.monolith.warehouses.domain.ports.ArchiveWarehouseOperation;
import com.fulfilment.application.monolith.warehouses.domain.ports.WarehouseStore;
import jakarta.enterprise.context.ApplicationScoped;
import java.time.LocalDateTime;

@ApplicationScoped
public class ArchiveWarehouseUseCase implements ArchiveWarehouseOperation {

  private final WarehouseStore warehouseStore;

  public ArchiveWarehouseUseCase(WarehouseStore warehouseStore) {
    this.warehouseStore = warehouseStore;
  }

  @Override
  public void archive(Warehouse warehouse) {
    if (warehouse == null || warehouse.businessUnitCode == null || warehouse.businessUnitCode.isBlank()) {
      throw new WarehouseOperationException(400, "businessUnitCode must be provided.");
    }

    Warehouse existing = warehouseStore.findByBusinessUnitCode(warehouse.businessUnitCode.trim());
    if (existing == null) {
      throw new WarehouseOperationException(
              404, "Warehouse with businessUnitCode " + warehouse.businessUnitCode + " does not exist.");
    }
    if (existing.archivedAt != null) {
      return; // idempotent archive
    }

    existing.archivedAt = LocalDateTime.now();
    warehouseStore.update(existing);
  }
}