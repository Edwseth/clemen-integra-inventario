<!DOCTYPE html>
<html lang="es">
<head>
    <meta charset="UTF-8" />
    <style>
        @page { size: A4; margin: 20px; }
        body { font-family: Arial, sans-serif; font-size: 11px; margin: 0; padding: 0; }
        table { width: 100%; border-collapse: collapse; }
        th, td { border: 1px solid #000; padding: 4px; text-transform: uppercase; font-size: 11px; }
        .no-border td { border: none; }
        .header-title { font-weight: bold; text-align: center; }
        .section-title { font-weight: bold; margin-top: 10px; text-transform: uppercase; }
        .spacer { height: 6px; }
    </style>
</head>
<body>
<table>
    <tr>
        <td colspan="2" class="header-title">LABORATORIO DE MICROBIOLOGÍA CLEMEN</td>
        <td colspan="2">
            FECHA DE ELABORACIÓN : 07/02/2025<br/>
            FECHA DE REVISIÓN: 07/02/2029
        </td>
    </tr>
    <tr>
        <td colspan="3">FORMATO DE INFORME DE MUESTRAS</td>
        <td>VERSIÓN: 02</td>
    </tr>
    <tr>
        <td colspan="3">CÓDIGO: FOR-MIC-009</td>
        <td>PÁGINA: 1 DE 1</td>
    </tr>
</table>
<div class="spacer"></div>
<table>
    <tr>
        <th>Fecha de Muestra</th>
        <th>Código del Producto</th>
        <th>Descripción del producto</th>
        <th>Cantidad Inspeccionada</th>
    </tr>
    <tr>
        <td>${fechaMuestra}</td>
        <td>${codigoProducto}</td>
        <td>${descripcionProducto}</td>
        <td>${cantidadInspeccionada}</td>
    </tr>
</table>
<div class="spacer"></div>
<table>
    <tr>
        <th>Fecha de Resultado</th>
        <th>OP / OC</th>
        <th>Lote</th>
    </tr>
    <tr>
        <td>${fechaResultado}</td>
        <td>${opOc}</td>
        <td>${lote}</td>
    </tr>
</table>
<div class="spacer"></div>
<table>
    <tr>
        <th>Análisis</th>
        <th>Método</th>
        <th>Especificación</th>
        <th>Resultados</th>
    </tr>
    ${tablaResultados}
</table>
<div class="spacer"></div>
<div class="section-title">Observaciones</div>
<div>${observaciones}</div>
<div class="spacer"></div>
<div class="section-title">Conclusiones</div>
<div>${conclusiones}</div>
<div class="spacer"></div>
<table>
    <tr>
        <th>Elaboró</th>
        <th>Revisó</th>
        <th>Aprobó</th>
    </tr>
    <tr>
        <td height="40">Firma:</td>
        <td>Firma:</td>
        <td>Firma:</td>
    </tr>
    <tr>
        <td>Nombre: ${nombreElabora}</td>
        <td>Nombre: Natali Salinas Ortiz</td>
        <td>Nombre: Victor Capote</td>
    </tr>
    <tr>
        <td>Cargo: Dt. Microbiología</td>
        <td>Cargo: Dt. Tec. Calidad</td>
        <td>Cargo: Gerente general</td>
    </tr>
    <tr>
        <td>Fecha: ${fechaEvaluacion}</td>
        <td>Fecha:</td>
        <td>Fecha:</td>
    </tr>
</table>
</body>
</html>
