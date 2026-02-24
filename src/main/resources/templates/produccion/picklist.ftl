<!DOCTYPE html>
<html lang="es">
<head>
  <meta charset="UTF-8" />
  <style>
    @page { size: A4; margin: 36px; }
    body { font-family: Arial, sans-serif; font-size: 9pt; color: #111; }
    h2 { margin: 0 0 8px 0; font-size: 12pt; }

    .meta { margin: 8px 0 14px; border-collapse: collapse; }
    .meta td { padding: 2px 0; vertical-align: top; }
    .meta .label { font-weight: bold; width: 110px; white-space: nowrap; padding-right: 10px; }
    .meta .value { width: auto; }

    .items { width: 100%; border-collapse: collapse; }
    .items th, .items td { border: 1px solid #cfcfcf; padding: 5px; text-align: left; }
    .items th { background: #f2f2f2; font-weight: bold; }
    .items td.num { text-align: right; }

    .firmas { width: 100%; border-collapse: collapse; margin-top: 20px; }
    .firmas td { width: 25%; text-align: center; border: none; }
    .firmas .linea { padding-bottom: 2px; }
    .firmas .label { padding-top: 2px; }
  </style>
</head>
<body>
  <h2>Picklist OP</h2>

  <table class="meta">
    <tr><td class="label">Solicitante:</td><td class="value">${solicitanteNombre}</td></tr>
    <tr><td class="label">OP:</td><td class="value">${codigoOP}</td></tr>
    <tr><td class="label">Producto:</td><td class="value">${productoNombre!"-"}</td></tr>
    <tr><td class="label">Fecha OP:</td><td class="value">${fechaOP}</td></tr>
  </table>

  <table class="items">
    <thead>
      <tr>
        <th>Producto</th>
        <th>Lote</th>
        <th>Cant.</th>
        <th>UM</th>
        <th>Alm. Origen</th>
        <th>Ubic. Origen</th>
        <th>Alm. Destino</th>
        <th>Ubic. Destino</th>
        <th>Observaciones</th>
      </tr>
    </thead>
    <tbody>
      ${itemsRows}
    </tbody>
  </table>

  <table class="firmas">
    <tr>
      <td class="linea">____________________</td>
      <td class="linea">____________________</td>
      <td class="linea">____________________</td>
      <td class="linea">____________________</td>
    </tr>
    <tr>
      <td class="label">Alistó</td>
      <td class="label">Verificó</td>
      <td class="label">Entregó</td>
      <td class="label">Recibió</td>
    </tr>
  </table>
</body>
</html>
