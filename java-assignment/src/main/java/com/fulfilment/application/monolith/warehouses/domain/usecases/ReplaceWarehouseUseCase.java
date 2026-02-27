package com.fulfilment.application.monolith.warehouses.domain.usecases;

import com.fulfilment.application.monolith.warehouses.domain.models.Location;
import com.fulfilment.application.monolith.warehouses.domain.models.Warehouse;
import com.fulfilment.application.monolith.warehouses.domain.ports.LocationResolver;
import com.fulfilment.application.monolith.warehouses.domain.ports.ReplaceWarehouseOperation;
import com.fulfilment.application.monolith.warehouses.domain.ports.WarehouseStore;
import jakarta.enterprise.context.ApplicationScoped;
import java.time.LocalDateTime;

@ApplicationScoped
public class ReplaceWarehouseUseCase implements ReplaceWarehouseOperation {

  private final WarehouseStore warehouseStore;
  private final LocationResolver locationResolver;

  public ReplaceWarehouseUseCase(WarehouseStore warehouseStore, LocationResolver locationResolver) {
    this.warehouseStore = warehouseStore;
    this.locationResolver = locationResolver;
  }

  @Override
  public void replace(Warehouse newWarehouse) {
    if (newWarehouse == null) {
      throw new WarehouseOperationException(400, "Request body must be provided.");
    }
    if (newWarehouse.businessUnitCode == null || newWarehouse.businessUnitCode.isBlank()) {
      throw new WarehouseOperationException(400, "businessUnitCode must be provided.");
    }

    Warehouse current = warehouseStore.findByBusinessUnitCode(newWarehouse.businessUnitCode.trim());
    if (current == null || current.archivedAt != null) {
      throw new WarehouseOperationException(
              404, "Active warehouse with businessUnitCode " + newWarehouse.businessUnitCode + " was not found.");
    }

    if (newWarehouse.location == null || newWarehouse.location.isBlank()) {
      throw new WarehouseOperationException(400, "location must be provided.");
    }
    if (newWarehouse.capacity == null || newWarehouse.capacity <= 0) {
      throw new WarehouseOperationException(400, "capacity must be a positive integer.");
    }
    if (newWarehouse.stock == null || newWarehouse.stock < 0) {
      throw new WarehouseOperationException(400, "stock must be a non-negative integer.");
    }
    if (newWarehouse.capacity < newWarehouse.stock) {
      throw new WarehouseOperationException(400, "capacity must be greater than or equal to stock.");
    }

    // Location Validation
    Location location = locationResolver.resolveByIdentifier(newWarehouse.location.trim());
    if (location == null) {
      throw new WarehouseOperationException(400, "Invalid warehouse location: " + newWarehouse.location);
    }

    // Additional Validations for Replacing a Warehouse
    if (current.stock == null || !current.stock.equals(newWarehouse.stock)) {
      throw new WarehouseOperationException(400, "Replacement warehouse stock must match the current warehouse stock.");
    }
    if (newWarehouse.capacity < current.stock) {
      throw new WarehouseOperationException(400, "Replacement warehouse capacity must accommodate current warehouse stock.");
    }

    // Feasibility at location (exclude current warehouse since it will be archived)
    var activeAll =
            warehouseStore.getAll().stream().filter(w -> w.archivedAt == null).toList();

    int activeCountAtNewLocation =
            (int)
                    activeAll.stream()
                            .filter(w -> w.location != null && w.location.equalsIgnoreCase(location.identification))
                            .filter(w -> !w.businessUnitCode.equalsIgnoreCase(current.businessUnitCode))
                            .count();

    if (activeCountAtNewLocation >= location.maxNumberOfWarehouses) {
      throw new WarehouseOperationException(
              400, "Maximum number of warehouses reached for location " + location.identification + ".");
    }

    int usedCapacityAtNewLocation =
            activeAll.stream()
                    .filter(w -> w.location != null && w.location.equalsIgnoreCase(location.identification))
                    .filter(w -> !w.businessUnitCode.equalsIgnoreCase(current.businessUnitCode))
                    .map(w -> w.capacity == null ? 0 : w.capacity)
                    .reduce(0, Integer::sum);

    if (usedCapacityAtNewLocation + newWarehouse.capacity > location.maxCapacity) {
      throw new WarehouseOperationException(
              400, "Warehouse capacity exceeds max capacity for location " + location.identification + ".");
    }

    // Archive current + create new (reuse BU code)
    current.archivedAt = LocalDateTime.now();
    warehouseStore.update(current);

    newWarehouse.createdAt = LocalDateTime.now();
    newWarehouse.archivedAt = null;
    newWarehouse.businessUnitCode = current.businessUnitCode;
    newWarehouse.location = location.identification;

    warehouseStore.create(newWarehouse);
  }
}