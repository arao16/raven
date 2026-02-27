package com.fulfilment.application.monolith.warehouses.domain.usecases;

import com.fulfilment.application.monolith.warehouses.domain.models.Location;
import com.fulfilment.application.monolith.warehouses.domain.models.Warehouse;
import com.fulfilment.application.monolith.warehouses.domain.ports.CreateWarehouseOperation;
import com.fulfilment.application.monolith.warehouses.domain.ports.LocationResolver;
import com.fulfilment.application.monolith.warehouses.domain.ports.WarehouseStore;
import jakarta.enterprise.context.ApplicationScoped;
import java.time.LocalDateTime;

@ApplicationScoped
public class CreateWarehouseUseCase implements CreateWarehouseOperation {

  private final WarehouseStore warehouseStore;
  private final LocationResolver locationResolver;

  public CreateWarehouseUseCase(WarehouseStore warehouseStore, LocationResolver locationResolver) {
    this.warehouseStore = warehouseStore;
    this.locationResolver = locationResolver;
  }

  @Override
  public void create(Warehouse warehouse) {
    if (warehouse == null) {
      throw new WarehouseOperationException(400, "Request body must be provided.");
    }
    if (warehouse.businessUnitCode == null || warehouse.businessUnitCode.isBlank()) {
      throw new WarehouseOperationException(400, "businessUnitCode must be provided.");
    }
    if (warehouse.location == null || warehouse.location.isBlank()) {
      throw new WarehouseOperationException(400, "location must be provided.");
    }
    if (warehouse.capacity == null || warehouse.capacity <= 0) {
      throw new WarehouseOperationException(400, "capacity must be a positive integer.");
    }
    if (warehouse.stock == null || warehouse.stock < 0) {
      throw new WarehouseOperationException(400, "stock must be a non-negative integer.");
    }
    if (warehouse.capacity < warehouse.stock) {
      throw new WarehouseOperationException(400, "capacity must be greater than or equal to stock.");
    }

    // Business Unit Code Verification (must not exist in history)
    if (warehouseStore.findByBusinessUnitCode(warehouse.businessUnitCode.trim()) != null) {
      throw new WarehouseOperationException(400, "Warehouse businessUnitCode already exists.");
    }

    // Location Validation
    Location location = locationResolver.resolveByIdentifier(warehouse.location.trim());
    if (location == null) {
      throw new WarehouseOperationException(400, "Invalid warehouse location: " + warehouse.location);
    }

    // Warehouse Creation Feasibility (max number)
    var activeAtLocation =
        warehouseStore.getAll().stream()
            .filter(w -> w.archivedAt == null)
            .filter(w -> w.location != null && w.location.equalsIgnoreCase(location.identification))
            .toList();

    if (activeAtLocation.size() >= location.maxNumberOfWarehouses) {
      throw new WarehouseOperationException(
          400, "Maximum number of warehouses reached for location " + location.identification + ".");
    }

    // Capacity and Stock Validation (location maxCapacity)
    int usedCapacity =
        activeAtLocation.stream().map(w -> w.capacity == null ? 0 : w.capacity).reduce(0, Integer::sum);

    if (usedCapacity + warehouse.capacity > location.maxCapacity) {
      throw new WarehouseOperationException(
          400, "Warehouse capacity exceeds max capacity for location " + location.identification + ".");
    }

    // if all went well, create the warehouse
    warehouse.createdAt = warehouse.createdAt == null ? LocalDateTime.now() : warehouse.createdAt;
    warehouse.archivedAt = null;
    warehouse.businessUnitCode = warehouse.businessUnitCode.trim();
    warehouse.location = location.identification;

    warehouseStore.create(warehouse);
  }
}
