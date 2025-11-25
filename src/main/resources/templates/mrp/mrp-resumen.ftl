<!DOCTYPE html>
<html lang="es">
<head>
    <meta charset="UTF-8" />
    <style>
        body { font-family: Arial, sans-serif; margin: 24px; }
        h2 { margin-bottom: 4px; }
        .meta { margin-bottom: 16px; }
        .meta span { display: inline-block; margin-right: 12px; }
        table { width: 100%; border-collapse: collapse; }
        th, td { border: 1px solid #ccc; padding: 6px; font-size: 12px; }
        th { background: #f2f2f2; }
        .right { text-align: right; }
    </style>
</head>
<body>
<h2>Resumen corrida MRP</h2>
<div class="meta">
    <span><strong>Número:</strong> ${numeroCorrida}</span>
    <span><strong>Plan semanal:</strong> ${planId}</span>
    <span><strong>Fecha ejecución:</strong> ${fechaEjecucion}</span>
    <div><strong>Rango:</strong> ${fechaInicio} - ${fechaFin}</div>
</div>
<table>
    <thead>
    <tr>
        <th>Código insumo</th>
        <th>Nombre</th>
        <th>Categoría</th>
        <th>Requerimiento bruto</th>
        <th>Inventario disponible</th>
        <th>Requerimiento neto</th>
        <th>Tipo sugerencia</th>
    </tr>
    </thead>
    <tbody>
    ${detalleRows}
    </tbody>
</table>
</body>
</html>
