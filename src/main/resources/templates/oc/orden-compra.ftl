<!DOCTYPE html
  PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN"
  "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html xmlns="http://www.w3.org/1999/xhtml" xml:lang="es" lang="es">
<head>
  <meta http-equiv="Content-Type" content="text/html; charset=UTF-8" />
  <style>
    @page {
      size: A4;
      /* margen superior mayor para el encabezado fijo */
      margin: 18mm 10mm 12mm 10mm;
      /* repetir encabezado en cada página */
      @top-center { content: element(doc-header) }
    }

    body { font-family: Helvetica, Arial, sans-serif; font-size: 11px; color:#000; }
    h1, h2, h3, p { margin: 0; padding: 0; }
    .w-100{width:100%}
    .mt-4{margin-top:4px} .mt-6{margin-top:6px} .mt-8{margin-top:8px}
    .mb-4{margin-bottom:4px} .mb-6{margin-bottom:6px} .mb-8{margin-bottom:8px}
    .pt-6{padding-top:6px} .pb-6{padding-bottom:6px} .pl-6{padding-left:6px} .pr-6{padding-right:6px}
    .small{font-size:10px}

    /* encabezado corporativo repetido */
    .header { position: running(doc-header); }
    .empresa { text-align:center; line-height:1.25; }
    .empresa b { font-size:14px; }

    /* fila superior: fecha + nro OC */
    .top { display: table; width:100%; }
    .cell { display: table-cell; vertical-align: top; }

    .fecha-box { width: 38mm; border: 1px solid #000; padding: 6px; }
    .fecha-box .titulo { font-weight: bold; text-transform: uppercase; text-align:center; margin-bottom: 6px; }
    .fecha-grid { width:100%; border-collapse: collapse; }
    .fecha-grid td, .fecha-grid th { border: 1px solid #000; padding: 4px; text-align:center; font-size: 10px; }
    .fecha-grid th { background:#f2f2f2; }

    .oc-meta { padding-left: 12px; }
    .oc-meta h1 { font-size: 18px; text-align: right; }
    .oc-num { text-align: right; margin-top: 4px; font-weight: bold; }
    .oc-tip { text-align: right; font-size: 10px; margin-top: 2px; }

    .bloques { margin-top: 10px; display: table; width: 100%; table-layout: fixed; }
    .bloque { display: table-cell; width: 50%; vertical-align: top; }
    .card { border: 1px solid #000; padding: 8px; min-height: 36mm; }
    .card h3 { font-size: 12px; margin-bottom: 6px; }
    .line { margin-bottom: 2px; }

    .band { border: 1px solid #000; border-top: none; display: table; width: 100%; table-layout: fixed; }
    .band .col { display: table-cell; padding: 6px 8px; }
    .band .col.title  { width: 35%; font-weight: bold; }
    .band .col.value  { width: 40%; }
    .band .col.moneda { width: 25%; text-align: right; font-weight: bold; }

    table.detalle { width:100%; border-collapse: collapse; margin-top: 6px; }
    table.detalle th, table.detalle td { border: 1px solid #000; padding: 5px; }
    table.detalle th { background:#f2f2f2; font-size:10px; }
    table.detalle td { font-size:10px; }
    .txt-right { text-align: right; }
    .txt-center{ text-align: center; }
    .nowrap { white-space: nowrap; }

    .terms-totals { margin-top: 8px; display: table; width: 100%; table-layout: fixed; }
    .terms { display: table-cell; width: 68%; vertical-align: top; border:1px solid #000; padding:8px; }
    .totals { display: table-cell; width: 32%; vertical-align: top; border:1px solid #000; border-left:none; padding:8px; }

    .totals .row { display: table; width: 100%; margin-bottom: 6px; }
    .totals .row .label { display: table-cell; }
    .totals .row .val { display: table-cell; text-align: right; font-weight: bold; }
    .totals .total { border-top:1px solid #000; padding-top:6px; margin-top:6px; font-size: 12px; }

    .observ { margin-top: 8px; border:1px solid #000; padding:8px; min-height: 18mm; }

    .firmas { margin-top: 14mm; text-align: center; }
    .firma-line { border-top:1px solid #000; width: 60mm; margin: 0 auto 4px; }

    /* Evitar cortes feos */
    table.detalle tr { page-break-inside: avoid; }
    .card, .terms, .totals, .observ { page-break-inside: avoid; }
  </style>
</head>
<body>

  <!-- Encabezado que se repite en todas las páginas -->
  <div class="header">
    <div class="empresa">
      <b>${empresa.nombre}</b><br />
      Nit: ${empresa.nit}<br />
      ${empresa.direccion}<br />
      ${empresa.telefono}
    </div>
  </div>

  <!-- Fila superior: Fecha + Nro OC -->
  <div class="top">
    <div class="cell">
      <div class="fecha-box">
        <div class="titulo">Fecha</div>
        <table class="fecha-grid">
          <tr>
            <th>Día</th><th>Mes</th><th>Año</th>
          </tr>
          <tr>
            <td>${fechaDia}</td>
            <td>${fechaMes}</td>
            <td>${fechaAnio}</td>
          </tr>
        </table>
      </div>
    </div>
    <div class="cell oc-meta">
      <h1>Orden de Compra</h1>
      <div class="oc-num">No. ${numero}</div>
      <div class="oc-tip small">Citar éste número en el documento de entrega de la mercancía.</div>
    </div>
  </div>

  <!-- Proveedor / Despachar a -->
  <div class="bloques">
    <div class="bloque pr-6">
      <div class="card">
        <h3>Proveedor:</h3>
        <div class="line"><b>${proveedor.nombre}</b></div>
        <div class="line small">Nit: ${proveedor.nit}</div>
        <div class="line small">Dirección: ${proveedor.direccion}</div>
        <div class="line small">Ciudad: ${proveedor.ciudad}</div>
        <div class="line small">${prov_tel}</div>
        <div class="line small">${prov_web}</div>
      </div>
    </div>
    <div class="bloque pl-6">
      <div class="card">
        <h3>Despachar a:</h3>
        SEDE PRINCIPAL<br />
        Carrera 41 D # 46-40 Union de Vivienda, Cali Valle del Cauca<br />
        Colombia<br />
        Teléfono / Movil 3176446404
      </div>
    </div>
  </div>

  <!-- Condiciones de pago -->
  <div class="band">
    <div class="col title">Condiciones de Pago</div>
    <div class="col value">${condicionesPago}</div>
    <div class="col moneda">Moneda COP</div>
  </div>

  <!-- Detalle -->
  <table class="detalle">
    <colgroup>
      <col style="width:9%" />   <!-- Código -->
      <col style="width:31%" />  <!-- Descripción -->
      <col style="width:11%" />  <!-- Fecha Necesidad -->
      <col style="width:8%" />   <!-- Udm -->
      <col style="width:10%" />  <!-- Cantidad -->
      <col style="width:12%" />  <!-- Precio Unitario -->
      <col style="width:7%" />   <!-- IVA% -->
      <col style="width:6%" />   <!-- ICUI% -->
      <col style="width:12%" />  <!-- Valor Total -->
    </colgroup>
    <thead>
      <tr>
        <th class="txt-center">Código</th>
        <th class="txt-center">Descripción</th>
        <th class="txt-center">Fecha<br />Necesidad</th>
        <th class="txt-center">Udm</th>
        <th class="txt-center">Cantidad</th>
        <th class="txt-center">Precio<br />Unitario</th>
        <th class="txt-center">IVA%</th>
        <th class="txt-center">ICUI%</th>
        <th class="txt-center">Valor Total</th>
      </tr>
    </thead>
    <tbody>
      ${itemsRows}
    </tbody>
  </table>

  <!-- Condiciones + Totales -->
  <div class="terms-totals">
    <div class="terms">
      <h3 class="mb-6">Condiciones y/o especificaciones</h3>
      <ol class="small" style="padding-left: 14px; line-height: 1.35;">
        <li>El proveedor se compromete a garantizar que los bienes y/o servicios objeto de compra, cumplen con las especificaciones requeridas. Los precios y las condiciones de pago aquí aparecen se entienden aceptados por el proveedor. Cualquier inconformidad debe ser resuelta previo a la ejecución de la orden.</li>
        <li>Debe entregarse Factura original, con copia de la misma y copia de la Orden de Compra. Si se trata de Cuenta de Cobro, incluir copia del Registro Único Tributario.</li>
        <li>La Factura o Cuenta de Cobro debe: (a) especificar NIT, razón social y dirección de la Compañía (ver encabezado de Orden de Compra), (b) relacionar número de este documento. No asociar más de una Orden de Compra en una sola Factura, (c) condiciones exactas de pedido (no se aceptan cambios), cantidad y precio.</li>
        <li>No se reciben Facturas o Cuentas de Cobro sin Orden de Compra. Los clientes de la Compañía deben estar notificados por la Compañía.</li>
        <li>La Compañía se reserva el derecho, sin obligación de dar un aviso, de tomar muestras de la mercancía y rechazar el material que no cumpla con las especificaciones.</li>
        <li>No se reciben cantidades superiores a las consignadas en esta Orden de Compra. El sistema no permite su recepción; cualquier exceso será rechazado y, de requerirse, deberá tramitarse una nueva Orden de Compra.</li>
        <li>El pago de la Factura se realizará de acuerdo con la política de programación de pagos de la Compañía, y sólo al beneficiario relacionado en el encabezado de la misma.</li>
        <li>Para efectos de aplicar correctamente las retenciones de Industria y Comercio, se solicita indicar en la Factura la ciudad donde presta el servicio, o la contratación se aplica la retención en el municipio de su sede principal.</li>
      </ol>
    </div>
    <div class="totals">
      <div class="row"><div class="label">Subtotal:</div><div class="val">$${subtotal}</div></div>
      <div class="row"><div class="label">Descuento:</div><div class="val">$${descuento}</div></div>
      <div class="row"><div class="label">Valor IVA:</div><div class="val">$${valorIva}</div></div>
      <div class="row"><div class="label">Valor ICUI:</div><div class="val">$${valorIcui}</div></div>
      <div class="row total"><div class="label">Total:</div><div class="val">$${total}</div></div>
    </div>
  </div>

  <!-- Observaciones -->
  <div class="observ">
    <b>Observaciones:</b>
    <div class="mt-8">${observaciones}</div>
  </div>

  <!-- Firma -->
  <div class="firmas">
    <div class="firma-line"></div>
    <div class="small">Comprador</div>
    <div class="small">${comprador}</div>
  </div>

</body>
</html>



