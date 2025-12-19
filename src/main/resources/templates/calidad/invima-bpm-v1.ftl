<!DOCTYPE html>
<html lang="es">
<head>
    <meta charset="UTF-8"/>
    <title>Reporte INVIMA/BPM v1</title>
    <style>
        body { font-family: Arial, sans-serif; margin: 16px; }
        h1, h2 { color: #1d3557; margin-bottom: 8px; }
        table { width: 100%; border-collapse: collapse; margin-bottom: 16px; }
        th, td { border: 1px solid #b5c3d3; padding: 8px; font-size: 12px; }
        th { background: #e9f0f7; text-align: left; }
    </style>
</head>
<body>
<h1>Reporte INVIMA/BPM v1</h1>
<p>Generado: ${fechaGeneracion}</p>

<h2>Identificación</h2>
<p><strong>Lote:</strong> ${codigoLote}</p>
<p><strong>Producto:</strong> ${producto}</p>
<p><strong>Categoría:</strong> ${categoria}</p>
<p><strong>Almacén:</strong> ${almacen} | <strong>Ubicación:</strong> ${ubicacion}</p>
<p><strong>Proveedor:</strong> ${proveedor}</p>

<h2>Certificado de liberación</h2>
<table>
    <thead>
    <tr>
        <th>Estado lote</th>
        <th>Fecha liberación</th>
        <th>Usuario liberador</th>
    </tr>
    </thead>
    <tbody>
    <tr>
        <td>${estadoLiberacion}</td>
        <td>${fechaLiberacion}</td>
        <td>${usuarioLiberador}</td>
    </tr>
    </tbody>
</table>

<h2>Evaluaciones (resumen)</h2>
<table>
    <thead>
    <tr>
        <th>Tipo</th>
        <th>Resultado</th>
        <th>Fecha</th>
        <th>Evaluador</th>
        <th>Resultados micro</th>
        <th>Conforme micro</th>
    </tr>
    </thead>
    <tbody>
    ${tablaEvaluaciones}
    </tbody>
</table>

<h2>No conformidades / Desviaciones</h2>
<table>
    <thead>
    <tr>
        <th>Código</th>
        <th>Tipo</th>
        <th>Severidad</th>
        <th>Estado</th>
        <th>Apertura</th>
        <th>Cierre</th>
        <th>CAPA</th>
    </tr>
    </thead>
    <tbody>
    ${tablaIncidentes}
    </tbody>
</table>

<h2>Retenciones</h2>
<table>
    <thead>
    <tr>
        <th>Motivo</th>
        <th>Descripción</th>
        <th>Estado</th>
        <th>ID</th>
    </tr>
    </thead>
    <tbody>
    ${tablaRetenciones}
    </tbody>
</table>

<h2>Condiciones de uso</h2>
<table>
    <thead>
    <tr>
        <th>Tipo</th>
        <th>Fecha parámetro</th>
        <th>Descripción</th>
    </tr>
    </thead>
    <tbody>
    ${tablaCondiciones}
    </tbody>
</table>

<h2>Movimientos de inventario</h2>
<table>
    <thead>
    <tr>
        <th>Fecha</th>
        <th>Tipo</th>
        <th>Clasificación</th>
        <th>Cantidad</th>
        <th>Almacén origen</th>
        <th>Almacén destino</th>
        <th>Motivo</th>
        <th>Registrado por</th>
        <th>Orden producción</th>
    </tr>
    </thead>
    <tbody>
    ${tablaMovimientos}
    </tbody>
</table>

<h2>Control documental asociado</h2>
<table>
    <thead>
    <tr>
        <th>Tipo</th>
        <th>Código</th>
        <th>Nombre</th>
        <th>Versión</th>
        <th>Fecha versión</th>
    </tr>
    </thead>
    <tbody>
    ${tablaDocumentos}
    </tbody>
</table>

<h2>Sanitización</h2>
<table>
    <thead>
    <tr>
        <th>Tipo</th>
        <th>Código</th>
        <th>Nombre</th>
        <th>Versión</th>
        <th>Fecha versión</th>
    </tr>
    </thead>
    <tbody>
    ${tablaSanitizacion}
    </tbody>
</table>

<h2>Calibración</h2>
<table>
    <thead>
    <tr>
        <th>Tipo</th>
        <th>Código</th>
        <th>Nombre</th>
        <th>Versión</th>
        <th>Fecha versión</th>
    </tr>
    </thead>
    <tbody>
    ${tablaCalibracion}
    </tbody>
</table>
</body>
</html>
