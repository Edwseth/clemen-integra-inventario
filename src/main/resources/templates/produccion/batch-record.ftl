<!DOCTYPE html
  PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN"
  "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html xmlns="http://www.w3.org/1999/xhtml" xml:lang="es" lang="es">
<head>
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8" />
    <title>Batch Record - Registro de Lote</title>
    <style>
        body { font-family: Arial, sans-serif; font-size: 12px; color: #333; }
        h1 { text-align: center; font-size: 18px; margin-bottom: 8px; }
        h2 { font-size: 15px; margin-top: 16px; margin-bottom: 8px; }
        table { width: 100%; border-collapse: collapse; margin-bottom: 12px; }
        th, td { border: 1px solid #ccc; padding: 6px; text-align: left; }
        th { background-color: #f2f2f2; }
        .header-grid { display: grid; grid-template-columns: repeat(2, 1fr); gap: 6px; margin-bottom: 12px; }
        .label { font-weight: bold; }
    </style>
</head>
<body>
<h1>BATCH RECORD – REGISTRO DE LOTE</h1>

<h2>Datos de la orden</h2>
<div class="header-grid">
    <div><span class="label">Producto:</span> ${op.producto}</div>
    <div><span class="label">Código OP:</span> ${op.codigoOrden}</div>
    <div><span class="label">SKU:</span> ${op.codigoSku}</div>
    <div><span class="label">Lote:</span> ${op.lote}</div>
    <div><span class="label">Presentación:</span> ${op.presentacion}</div>
    <div><span class="label">Responsable:</span> ${op.responsable}</div>
    <div><span class="label">Cantidad programada:</span> ${op.cantidadProgramada}</div>
    <div><span class="label">Cantidad producida:</span> ${op.cantidadProducida}</div>
    <div><span class="label">Estado:</span> ${op.estado}</div>
    <div><span class="label">% Cumplimiento:</span> ${op.porcentajeCumplimiento}</div>
    <div><span class="label">Inicio:</span> ${op.fechaInicio}</div>
    <div><span class="label">Fin:</span> ${op.fechaFin}</div>
</div>

<h2>1. Materiales utilizados (consumos y reservas)</h2>
<table>
    <thead>
    <tr>
        <th>Producto</th>
        <th>Lote</th>
        <th>Almacén origen</th>
        <th>Cantidad</th>
        <th>Unidad</th>
        <th>Fecha movimiento</th>
    </tr>
    </thead>
    <tbody>${consumoRows}</tbody>
</table>

<table>
    <thead>
    <tr>
        <th>Lote reservado</th>
        <th>Cantidad reservada</th>
        <th>Cantidad consumida</th>
        <th>Estado</th>
    </tr>
    </thead>
    <tbody>${reservaRows}</tbody>
</table>

<h2>2. Controles en proceso</h2>
<table>
    <thead>
    <tr>
        <th>Etapa</th>
        <th>Parámetro</th>
        <th>Valor medido</th>
        <th>Unidad</th>
        <th>Cumple</th>
        <th>Observaciones</th>
        <th>Evaluado por</th>
        <th>Fecha registro</th>
    </tr>
    </thead>
    <tbody>${controlProcesoRows}</tbody>
</table>

<h2>3. Controles de empaque</h2>
<table>
    <thead>
    <tr>
        <th>Parámetro</th>
        <th>Valor medido</th>
        <th>Unidad</th>
        <th>Cumple</th>
        <th>Observaciones</th>
        <th>Evaluado por</th>
        <th>Fecha registro</th>
    </tr>
    </thead>
    <tbody>${controlEmpaqueRows}</tbody>
</table>

<h2>4. Producción final y calidad</h2>
<table>
    <thead>
    <tr>
        <th>Unidades producidas</th>
        <th>Unidades aprobadas</th>
        <th>Unidades rechazadas</th>
        <th>Rendimiento calculado</th>
    </tr>
    </thead>
    <tbody>
    <tr>
        <td>${produccionFinal.unidadesProducidas}</td>
        <td>${produccionFinal.unidadesAprobadas}</td>
        <td>${produccionFinal.unidadesRechazadas}</td>
        <td>${produccionFinal.rendimiento}</td>
    </tr>
    </tbody>
</table>

<table>
    <thead>
    <tr>
        <th>Lote PT</th>
        <th>Estado</th>
        <th>Fabricación</th>
        <th>Vencimiento</th>
        <th>Liberación</th>
        <th>Usuario liberador</th>
    </tr>
    </thead>
    <tbody>
    <tr>
        <td>${lotePT.codigoLote}</td>
        <td>${lotePT.estado}</td>
        <td>${lotePT.fechaFabricacion}</td>
        <td>${lotePT.fechaVencimiento}</td>
        <td>${lotePT.fechaLiberacion}</td>
        <td>${lotePT.usuarioLiberador}</td>
    </tr>
    </tbody>
</table>

<table>
    <thead>
    <tr>
        <th>Tipo evaluación</th>
        <th>Resultado</th>
        <th>Observaciones</th>
        <th>Evaluador</th>
        <th>Fecha</th>
    </tr>
    </thead>
    <tbody>${evaluacionRows}</tbody>
</table>

<table>
    <thead>
    <tr>
        <th>Estado</th>
        <th>Motivo</th>
        <th>Fecha retención</th>
        <th>Fecha liberación</th>
        <th>Aprobador</th>
    </tr>
    </thead>
    <tbody>${retencionRows}</tbody>
</table>

<h2>5. Revisión y aprobación de Calidad</h2>
<table>
    <tbody>
    <tr>
        <th>Estado Batch Record</th>
        <td>${batchRecord.estadoBatchRecord!'-'}</td>
    </tr>
    <tr>
        <th>Revisado por</th>
        <td>${batchRecord.revisadoPorNombre!'-'}</td>
    </tr>
    <tr>
        <th>Fecha de revisión</th>
        <td>${batchRecord.fechaRevision!'-'}</td>
    </tr>
    <tr>
        <th>Observaciones de Calidad</th>
        <td>${batchRecord.observacionesCalidad!'-'}</td>
    </tr>
    </tbody>
</table>

<h2>6. Observaciones</h2>
<table>
    <thead>
    <tr>
        <th>Tipo</th>
        <th>Descripción</th>
        <th>Registrado por</th>
        <th>Fecha registro</th>
    </tr>
    </thead>
    <tbody>${observacionRows}</tbody>
</table>

</body>
</html>
