package com.fulfilment.application.monolith.warehouses.adapters.restapi;

import com.fulfilment.application.monolith.warehouses.adapters.database.DbWarehouse;
import com.fulfilment.application.monolith.warehouses.adapters.database.WarehouseRepository;
import com.fulfilment.application.monolith.warehouses.domain.usecases.WarehouseOperationException;
import com.fulfilment.application.monolith.warehouses.domain.ports.ArchiveWarehouseOperation;
import com.fulfilment.application.monolith.warehouses.domain.ports.CreateWarehouseOperation;
import com.fulfilment.application.monolith.warehouses.domain.ports.ReplaceWarehouseOperation;
import com.warehouse.api.WarehouseResource;
import com.warehouse.api.beans.Warehouse;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import java.util.List;

@RequestScoped
public class WarehouseResourceImpl implements WarehouseResource {

  @Inject WarehouseRepository warehouseRepository;

  @Inject CreateWarehouseOperation createWarehouseOperation;
  @Inject ReplaceWarehouseOperation replaceWarehouseOperation;
  @Inject ArchiveWarehouseOperation archiveWarehouseOperation;

  @Override
  public List<Warehouse> listAllWarehousesUnits() {
    // Only ACTIVE warehouses
    return warehouseRepository.listActiveAll().stream()
        .map(DbWarehouse::toWarehouse)
        .map(this::toWarehouseResponse)
        .toList();
  }

  @Override
  @Transactional
  public Warehouse createANewWarehouseUnit(@NotNull Warehouse data) {
    var domain = toDomainWarehouse(data);
    createWarehouseOperation.create(domain);

    var created = warehouseRepository.findByBusinessUnitCode(domain.businessUnitCode);
    if (created == null) {
      throw new WebApplicationException("Warehouse was created but could not be retrieved.", 500);
    }
    return toWarehouseResponse(created);
  }

  @Override
  public Warehouse getAWarehouseUnitByID(String id) {
    Long dbId = parseIdAsLong(id);

    DbWarehouse entity = warehouseRepository.findById(dbId);
    if (entity == null) {
      throw new WebApplicationException("Warehouse with id " + id + " does not exist.", 404);
    }

    return toWarehouseResponse(entity.toWarehouse());
  }

  @Override
  @Transactional
  public void archiveAWarehouseUnitByID(String id) {
    Long dbId = parseIdAsLong(id);

    DbWarehouse entity = warehouseRepository.findById(dbId);
    if (entity == null) {
      throw new WebApplicationException("Warehouse with id " + id + " does not exist.", 404);
    }

    var request = new com.fulfilment.application.monolith.warehouses.domain.models.Warehouse();
    request.businessUnitCode = entity.businessUnitCode;

    archiveWarehouseOperation.archive(request);
  }

  @Override
  @Transactional
  public Warehouse replaceTheCurrentActiveWarehouse(String businessUnitCode, @NotNull Warehouse data) {
    if (businessUnitCode == null || businessUnitCode.isBlank()) {
      throw new WebApplicationException("businessUnitCode must be provided.", 400);
    }

    var domain = toDomainWarehouse(data);
    domain.businessUnitCode = businessUnitCode.trim();

    replaceWarehouseOperation.replace(domain);

    // After replace, the "active" record is the newest for that BU.
    var active = warehouseRepository.findByBusinessUnitCode(domain.businessUnitCode);
    if (active == null) {
      throw new WebApplicationException(
          "Warehouse was replaced but active warehouse could not be retrieved.", 500);
    }

    return toWarehouseResponse(active);
  }

  private Long parseIdAsLong(String id) {
    if (id == null || id.isBlank()) {
      throw new WebApplicationException("id must be provided.", 400);
    }
    try {
      return Long.parseLong(id.trim());
    } catch (NumberFormatException e) {
      throw new WebApplicationException("Invalid id: " + id, 400);
    }
  }

  private com.fulfilment.application.monolith.warehouses.domain.models.Warehouse toDomainWarehouse(Warehouse data) {
    if (data == null) {
      return null;
    }
    var w = new com.fulfilment.application.monolith.warehouses.domain.models.Warehouse();
    w.businessUnitCode = data.getBusinessUnitCode();
    w.location = data.getLocation();
    w.capacity = data.getCapacity();
    w.stock = data.getStock();
    return w;
  }

  private Warehouse toWarehouseResponse(
      com.fulfilment.application.monolith.warehouses.domain.models.Warehouse warehouse) {
    var response = new Warehouse();
    response.setBusinessUnitCode(warehouse.businessUnitCode);
    response.setLocation(warehouse.location);
    response.setCapacity(warehouse.capacity);
    response.setStock(warehouse.stock);
    return response;
  }

  @Provider
  public static class WarehouseOperationExceptionMapper implements ExceptionMapper<WarehouseOperationException> {
    @Override
    public Response toResponse(WarehouseOperationException exception) {
      // Keep it simple: return the message as plain text (fine for tests; can be JSON later if desired)
      return Response.status(exception.status)
          .type(MediaType.TEXT_PLAIN_TYPE)
          .entity(exception.getMessage() == null ? "" : exception.getMessage())
          .build();
    }
  }
}
