package com.willyes.clemenintegra.shared.security.testsupport;

import org.springframework.security.test.context.support.WithMockUser;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Usuario de pruebas con rol administrativo y permisos canónicos transversales
 * para evitar falsos 403 en tests de negocio/controlador.
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@WithMockUser(authorities = {
        "ROL_SUPER_ADMIN",
        "INV_READ", "INV_WRITE", "INV_EXPORT", "INV_WORKFLOW", "INV_WORKFLOW_START", "INV_WORKFLOW_FINISH",
        "INV_CONTEOS_READ", "INV_CONTEOS_WRITE", "INV_CONTEOS_APPLY", "INV_CONTEOS_CLOSE", "INV_CONTEOS_START",
        "QC_READ", "QC_WRITE", "QC_EXPORT", "QC_WORKFLOW", "QC_WORKFLOW_FINISH", "QC_DECIDE",
        "PROD_READ", "PROD_WRITE", "PROD_WORKFLOW", "PROD_WORKFLOW_FINISH",
        "PO_READ", "PO_WRITE", "PO_EXPORT",
        "DOC_READ", "DOC_WRITE",
        "BOM_READ", "BOM_WRITE"
})
public @interface WithTestSuperAdmin {
}

