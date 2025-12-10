<!DOCTYPE html>
<html lang="es">
<head>
    <meta charset="UTF-8" />
    <style>
        body { font-family: Arial, sans-serif; font-size: 12px; }
        h1 { text-align: center; }
        table { width: 100%; border-collapse: collapse; }
        th, td { border: 1px solid #000; padding: 4px; }
        th { background: #f0f0f0; }
    </style>
</head>
<body>
<h1>Informe de Análisis Microbiológico</h1>
<p><strong>Empresa:</strong> ${empresa.nombre} - ${empresa.nit}</p>
<p><strong>Dirección:</strong> ${empresa.direccion} - ${empresa.telefono}</p>
<p><strong>Producto:</strong> ${producto}</p>
<p><strong>Lote:</strong> ${lote}</p>
<p><strong>Fecha evaluación:</strong> ${fecha}</p>
<p><strong>Analista:</strong> ${analista}</p>

<table>
    <thead>
    <tr>
        <th>Ensayo</th>
        <th>Unidad</th>
        <th>Especificación</th>
        <th>Resultado</th>
        <th>Cumple</th>
        <th>Observaciones</th>
    </tr>
    </thead>
    <tbody>
    ${tablaResultados}
    </tbody>
</table>

</body>
</html>
