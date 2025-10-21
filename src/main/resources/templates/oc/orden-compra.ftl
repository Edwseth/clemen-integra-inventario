<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE html
  PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN"
  "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html xmlns="http://www.w3.org/1999/xhtml" xml:lang="es" lang="es">
<head>
  <meta http-equiv="Content-Type" content="text/html; charset=UTF-8" />
  <style>
    @page {
      size: A4;
      margin: 18mm 12mm 16mm 12mm;
      @top-center { content: element(doc-header) }
    }
    body { font-family: Arial, Helvetica, sans-serif; font-size: 11px; color:#000; }

    .header { position: running(doc-header); }
    .empresa { text-align:center; line-height:1.2; font-size:12px; }
    .empresa b { font-size:14px; }

    .row { display:flex; justify-content:space-between; margin-top:8px; }
    .box { border:1px solid #000; padding:6px 8px; }

    table { width:100%; border-collapse:collapse; }
    th, td { border:1px solid #000; padding:4px 6px; vertical-align:top; }
    th { background:#eee; }

    .small { font-size:10px; }
    .right { text-align:right; }
    .mt-8 { margin-top:8px; }
    .mt-12 { margin-top:12px; }
    .mb-6 { margin-bottom:6px; }
  </style>
</head>
<body>

<!-- HEADER (repite en cada página) -->
<div class="header">
  <div class="empresa">
    <b>${empresa.nombre}</b><br/>
    Nit: ${empresa.nit}<br/>
    ${empresa.direccion}<br/>
    ${empresa.telefono}
  </div>
</div>

<!-- Fecha y número de OC -->
<div class="row mt-12">
  <div class="box" style="width:48%;">
    <table>
      <tr><th colspan="3" class="small">Fecha</th></tr>
      <tr><th>Día</th><th>Mes</th><th>Año</th></tr>
      <tr>
        <td class="right">${fechaDia}</td>
        <td class="right">${fechaMes}</td>
        <td class="right">${fechaAnio}</td>
      </tr>
    </table>
  </div>
  <div class="box" style="width:48%;">
    <table>
      <tr><th class="small">Orden de Compra</th></tr>
      <tr><td class="right">No. ${numero}</td></tr>
      <tr><td class="small right">Citar éste número en el documento de entrega de la mercancía.</td></tr>
    </table>
  </div>
</div>

<!-- Proveedor / Despachar a -->
<div class="row mt-8">
  <div class="box" style="width:48%;">
    <b>Proveedor:</b><br/>
    ${proveedor.nombre}<br/>
    <span class="small">Nit: ${proveedor.nit}</span><br/>
    <span class="small">Dirección: ${proveedor.direccion}</span><br/>
    <span class="small">Ciudad: ${proveedor.ciudad}${prov_tel}${prov_web}</span>
  </div>

  <div class="box" style="width:48%;">
    <b>Despachar a:</b><br/>
    SEDE PRINCIPAL<br/>
    Carrera 41 D # 46-40 Union de Vivienda, Cali Valle del Cauca<br/>
    Colombia<br/>
    Teléfono / Movil 3175081762
  </div>
</div>

<!-- Condiciones de pago -->
<div class="box mt-8">
  <b>Condiciones de Pago</b><br/>
  ${condicionesPago}
</div>

<!-- Tabla de ítems -->
<div class="mt-8">
  <table>
    <thead>
      <tr>
        <th>Código</th>
        <th>Descripción</th>
        <th>Fecha Necesidad</th>
        <th>Udm</th>
        <th class="right">Cantidad</th>
        <th class="right">Precio Unitario</th>
        <th class="right">IVA%</th>
        <th class="right">ICUI%</th>
        <th class="right">Valor Total</th>
      </tr>
    </thead>
    <tbody>
      ${itemsRows}
    </tbody>
  </table>
</div>

<!-- Condiciones y/o especificaciones + totales -->
<div class="row mt-8">
  <div class="box" style="width:64%;">
    <div style="text-align:center;"><b>Condiciones y/o especificaciones</b></div>
    <ol class="small">
      <li>El proveedor se compromete a garantizar que los bienes y/o servicios objeto de compra, cumplen con las especificaciones requeridas.</li>
      <li>Debe entregarse Factura original con copia de la misma y copia de la Orden de Compra.</li>
      <li>La Factura debe citar el No. de la Orden de Compra. No asociar más de una Orden a una sola Factura.</li>
      <li>No se aceptan sobrantes superiores al 10% de la cantidad solicitada; la Compañía se reserva el derecho a devolver excedentes.</li>
      <li>El pago de Facturas se realizará según programación de pagos de la Compañía y sólo al beneficiario.</li>
      <li>Para retenciones de ICA e Industria y Comercio, indicar ciudad de despacho correcta en la Factura.</li>
    </ol>
  </div>
  <div class="box" style="width:34%;">
    <table>
      <tr><th>Descuento:</th><td class="right">0</td></tr>
      <tr><th>Valor IVA:</th><td class="right">${valorIva}</td></tr>
      <tr><th>Valor ICUI:</th><td class="right">${valorIcui}</td></tr>
      <tr><th><b>Total:</b></th><td class="right"><b>${total}</b></td></tr>
    </table>
  </div>
</div>

<!-- Observaciones -->
<div class="box mt-8">
  <b>Observaciones:</b><br/>
  ${observaciones}
</div>

<!-- Firma comprador -->
<div class="mt-12">
  <table>
    <tr>
      <td style="height:38mm; border:1px solid #000; vertical-align:bottom; text-align:center;">
        Comprador<br/>${comprador}
      </td>
    </tr>
  </table>
</div>

</body>
</html>

