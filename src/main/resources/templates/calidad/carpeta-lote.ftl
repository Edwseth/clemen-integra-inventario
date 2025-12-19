<!DOCTYPE html>
<html lang="es">
<head>
    <meta charset="UTF-8" />
    <title>Carpeta de Lote</title>
    <style>
        @page { size: A4; margin: 20px; }
        body { font-family: Arial, sans-serif; font-size: 11px; margin: 0; padding: 0; }
        h1, h2 { color: #1d3557; }
        table { width: 100%; border-collapse: collapse; margin-bottom: 16px; }
        th, td { border: 1px solid #b5c3d3; padding: 8px; font-size: 12px; }
        th { background: #e9f0f7; text-align: left; }
    </style>
</head>
<body>
<h1>Carpeta de Lote</h1>
<p>Generado: ${fechaGeneracion}</p>
<p><strong>Lote:</strong> ${codigoLote} | <strong>Producto:</strong> ${producto} | <strong>Estado:</strong> ${estadoLote}</p>

<h2>Evaluaciones y anexos</h2>
<table>
    <thead>
    <tr>
        <th>Fecha evaluación</th>
        <th>Tipo</th>
        <th>Resultado</th>
        <th>Evaluador</th>
        <th>Anexos</th>
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
</body>
</html>
