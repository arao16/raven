package com.fulfilment.application.monolith.warehouses.domain.usecases;

public class WarehouseOperationException extends RuntimeException {
    public final int status;

    public WarehouseOperationException(int status, String message) {
        super(message);
        this.status = status;
    }
}