package com.fulfilment.application.monolith.warehouses.adapters.database;

import com.fulfilment.application.monolith.warehouses.domain.models.Warehouse;
import com.fulfilment.application.monolith.warehouses.domain.ports.WarehouseStore;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;
import java.time.LocalDateTime;
import java.util.List;

@ApplicationScoped
public class WarehouseRepository implements WarehouseStore, PanacheRepository<DbWarehouse> {

  @Override
  public List<Warehouse> getAll() {
    return this.listAll().stream().map(DbWarehouse::toWarehouse).toList();
  }

  @Override
  public void create(Warehouse warehouse) {
    if (warehouse == null) {
      throw new IllegalArgumentException("warehouse must not be null");
    }
    DbWarehouse entity = DbWarehouse.fromWarehouse(warehouse);
    if (entity.createdAt == null) {
      entity.createdAt = LocalDateTime.now();
    }
    persist(entity);
  }

  @Override
  public void update(Warehouse warehouse) {
    if (warehouse == null) {
      throw new IllegalArgumentException("warehouse must not be null");
    }
    DbWarehouse entity = find("businessUnitCode = ?1 and createdAt = ?2", warehouse.businessUnitCode, warehouse.createdAt)
        .firstResult();

    // Fallback: if createdAt isn't set, update "active" row for the BU.
    if (entity == null) {
      entity = find("businessUnitCode = ?1 and archivedAt is null", warehouse.businessUnitCode).firstResult();
    }

    if (entity == null) {
      throw new IllegalArgumentException(
          "Warehouse with businessUnitCode=" + warehouse.businessUnitCode + " does not exist");
    }

    entity.applyFrom(warehouse);
    persist(entity);
  }

  @Override
  public void remove(Warehouse warehouse) {
    if (warehouse == null) {
      throw new IllegalArgumentException("warehouse must not be null");
    }
    delete("businessUnitCode", warehouse.businessUnitCode);
  }

  @Override
  public Warehouse findByBusinessUnitCode(String buCode) {
    if (buCode == null || buCode.isBlank()) {
      return null;
    }
    DbWarehouse entity = find("businessUnitCode", buCode.trim()).firstResult();
    return entity == null ? null : entity.toWarehouse();
  }

  public DbWarehouse findActiveDbByBusinessUnitCode(String buCode) {
    if (buCode == null || buCode.isBlank()) {
      return null;
    }
    return find("businessUnitCode = ?1 and archivedAt is null", buCode.trim()).firstResult();
  }

  public List<DbWarehouse> listActiveByLocation(String location) {
    return find("location = ?1 and archivedAt is null", location).list();
  }

  public List<DbWarehouse> listActiveAll() {
    return find("archivedAt is null").list();
  }
}
