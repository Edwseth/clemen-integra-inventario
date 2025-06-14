package com.willyes.clemenintegra.inventario.application.exception;

public class LoteProductoNotFoundException extends RuntimeException {
    public LoteProductoNotFoundException(String message) {
        super(message);
    }
}
