<!DOCTYPE html>
<html lang="es">
<head>
    <meta charset="UTF-8" />
    <title>Auditoría de Lote</title>
    <style>
        @page { size: A4; margin: 20px; }
        body { font-family: Arial, sans-serif; font-size: 11px; margin: 0; padding: 0; }
        h1 { text-align: center; }
        table { width: 100%; border-collapse: collapse; margin-top: 10px; }
        th, td { border: 1px solid #ccc; padding: 6px; }
        th { background: #f0f0f0; }
    </style>
</head>
<body>
<h1>AUDITORÍA DE LOTE</h1>
<p>Fecha de generación: ${fechaGeneracion}</p>

<h3>Datos del lote</h3>
<table>
    <tr><th>Código Lote</th><td>${codigoLote}</td><th>Producto</th><td>${producto}</td></tr>
    <tr><th>Categoría</th><td>${categoria}</td><th>Tipo análisis</th><td>${tipoAnalisis}</td></tr>
    <tr><th>Estado lote</th><td>${estadoLote}</td><th>Stock</th><td>${stockLote}</td></tr>
    <tr><th>Fecha fabricación</th><td>${fechaFabricacion}</td><th>Fecha vencimiento</th><td>${fechaVencimiento}</td></tr>
    <tr><th>Almacén</th><td>${almacen}</td><th>Ubicación</th><td>${ubicacion}</td></tr>
</table>

<h3>Estado de calidad</h3>
<table>
    <tr><th>Estado</th><td>${estadoCalidad}</td><th>Retención activa</th><td>${retencionActiva}</td></tr>
    <tr><th>Motivo retención</th><td colspan="3">${motivoRetencion}</td></tr>
</table>

<h3>No conformidades / Desviaciones</h3>
<table>
    <thead>
    <tr>
        <th>Código</th>
        <th>Tipo</th>
        <th>Severidad</th>
        <th>Estado</th>
        <th>Fecha Apertura</th>
        <th>Fecha Cierre</th>
        <th>CAPA</th>
    </tr>
    </thead>
    <tbody>
    ${tablaIncidentes}
    </tbody>
</table>

<h3>Movimientos de inventario</h3>
<table>
    <thead>
    <tr>
        <th>Fecha</th>
        <th>Tipo</th>
        <th>Clasificación</th>
        <th>Cantidad</th>
        <th>Almacén Origen</th>
        <th>Almacén Destino</th>
        <th>Motivo</th>
        <th>Usuario</th>
        <th>O.P.</th>
    </tr>
    </thead>
    <tbody>
    ${tablaMovimientos}
    </tbody>
</table>

</body>
</html>
