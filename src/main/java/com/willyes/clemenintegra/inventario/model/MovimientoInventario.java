package com.willyes.clemenintegra.inventario.model;

import com.willyes.clemenintegra.inventario.model.enums.CausaDevolucionPT;
import com.willyes.clemenintegra.inventario.model.enums.ClasificacionMovimientoInventario;
import com.willyes.clemenintegra.inventario.model.enums.CondicionProductoDevuelto;
import com.willyes.clemenintegra.inventario.model.enums.TipoMovimiento;
import com.willyes.clemenintegra.shared.model.Usuario;
import com.willyes.clemenintegra.produccion.model.EtapaProduccion;
import com.willyes.clemenintegra.produccion.model.OrdenProduccion;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "movimientos_inventario")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class MovimientoInventario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal cantidad;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_mov", nullable = false, length = 20)
    private TipoMovimiento tipoMovimiento;

    @Enumerated(EnumType.STRING)
    @Column(name = "clasificacion", length = 50)
    private ClasificacionMovimientoInventario clasificacion;

    @Column(name = "fecha_ingreso", nullable = false, updatable = false)
    private LocalDateTime fechaIngreso;

    @Column(name = "doc_referencia", length = 45)
    private String docReferencia;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "causa_devolucion_pt", length = 30)
    private CausaDevolucionPT causaDevolucionPt;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "condicion_producto_devuelto", length = 20)
    private CondicionProductoDevuelto condicionProductoDevuelto;

    @Column(name = "cliente_nombre", length = 255)
    private String clienteNombre;

    @Column(name = "lote_legacy", nullable = false)
    private boolean loteLegacy;

    @Column(name = "idempotency_key", length = 64, unique = true)
    private String idempotencyKey;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "registrado_por_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_movimientos_inventario_registrado_por"))
    private Usuario registradoPor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "productos_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_movimientos_inventario_productos1"))
    private Producto producto;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lotes_productos_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_movimientos_inventario_lotes_productos1"))
    private LoteProducto lote;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "almacen_origen_id", nullable = true,
            foreignKey = @ForeignKey(name = "fk_movimientos_inventario_origen"))
    private Almacen almacenOrigen;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "almacen_destino_id", nullable = true,
            foreignKey = @ForeignKey(name = "fk_movimientos_inventario_destino"))
    private Almacen almacenDestino;

    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "proveedores_id", nullable = true,
            foreignKey = @ForeignKey(name = "fk_movimientos_inventario_proveedores1"))
    private Proveedor proveedor;

    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "ordenes_compra_id", nullable = true,
            foreignKey = @ForeignKey(name = "fk_movimientos_inventario_ordenes_compra1"))
    private OrdenCompra ordenCompra;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "motivos_movimiento_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_movimientos_inventario_motivos_movimiento1"))
    private MotivoMovimiento motivoMovimiento;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "orden_produccion_id")
    private OrdenProduccion ordenProduccion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "orden_produccion_etapa_id")
    private EtapaProduccion ordenProduccionEtapa;

    @ManyToOne
    @JoinColumn(name = "tipos_movimiento_detalle_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_mov_inv_tipo_mov_detalle"))
    private TipoMovimientoDetalle tipoMovimientoDetalle;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "orden_compra_detalle_id", nullable = true,
            foreignKey = @ForeignKey(name = "fk_movimientos_inventario_orden_compra_detalle"))
    private OrdenCompraDetalle ordenCompraDetalle;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "solicitud_movimiento_id")
    private SolicitudMovimiento solicitudMovimiento;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recepcion_oc_id", foreignKey = @ForeignKey(name = "fk_movimientos_inventario_recepcion_oc"))
    private RecepcionOC recepcionOc;

    @Column(name = "codigo_recepcion", length = 32)
    private String codigoRecepcion;

    @Column(name = "costo_unitario_aplicado", precision = 19, scale = 6)
    private BigDecimal costoUnitarioAplicado;

    @Column(name = "costo_total_aplicado", precision = 19, scale = 6)
    private BigDecimal costoTotalAplicado;

    @PrePersist
    public void prePersist() {
        if (this.fechaIngreso == null) {
            this.fechaIngreso = LocalDateTime.now();
        }
    }

    public TipoMovimiento getTipoMovimiento() {return tipoMovimiento;}
    public void setTipoMovimiento(TipoMovimiento tipoMovimiento) {this.tipoMovimiento = tipoMovimiento;}
    public LocalDateTime getFechaIngreso() {return fechaIngreso;}
    public void setFechaIngreso(LocalDateTime fechaIngreso) {this.fechaIngreso = fechaIngreso;}
    public Almacen getAlmacenOrigen() {return almacenOrigen;}
    public void setAlmacenOrigen(Almacen almacenOrigen) {this.almacenOrigen = almacenOrigen;}
    public Almacen getAlmacenDestino() {return almacenDestino;}
    public void setAlmacenDestino(Almacen almacenDestino) {this.almacenDestino = almacenDestino;}

    public Long getId() {return id;}
    public void setId(Long id) {this.id = id;}
    public BigDecimal getCantidad() {return cantidad;}
    public void setCantidad(BigDecimal cantidad) {this.cantidad = cantidad;}
    public String getDocReferencia() {return docReferencia;}
    public void setDocReferencia(String docReferencia) {this.docReferencia = docReferencia;}
    public CausaDevolucionPT getCausaDevolucionPt() {return causaDevolucionPt;}
    public void setCausaDevolucionPt(CausaDevolucionPT causaDevolucionPt) {this.causaDevolucionPt = causaDevolucionPt;}
    public CondicionProductoDevuelto getCondicionProductoDevuelto() {return condicionProductoDevuelto;}
    public void setCondicionProductoDevuelto(CondicionProductoDevuelto condicionProductoDevuelto) {this.condicionProductoDevuelto = condicionProductoDevuelto;}
    public String getClienteNombre() {return clienteNombre;}
    public void setClienteNombre(String clienteNombre) {this.clienteNombre = clienteNombre;}
    public boolean isLoteLegacy() {return loteLegacy;}
    public void setLoteLegacy(boolean loteLegacy) {this.loteLegacy = loteLegacy;}
    public Usuario getRegistradoPor() {return registradoPor;}
    public void setRegistradoPor(Usuario registradoPor) {this.registradoPor = registradoPor;}
    public Producto getProducto() {return producto;}
    public void setProducto(Producto producto) {this.producto = producto;}
    public LoteProducto getLote() {return lote;}
    public void setLote(LoteProducto lote) {this.lote = lote;}
    public Proveedor getProveedor() {return proveedor;}
    public void setProveedor(Proveedor proveedor) {this.proveedor = proveedor;}
    public OrdenCompra getOrdenCompra() {return ordenCompra;}
    public void setOrdenCompra(OrdenCompra ordenCompra) {this.ordenCompra = ordenCompra;}
    public MotivoMovimiento getMotivoMovimiento() {return motivoMovimiento;}
    public void setMotivoMovimiento(MotivoMovimiento motivoMovimiento) {this.motivoMovimiento = motivoMovimiento;}
    public TipoMovimientoDetalle getTipoMovimientoDetalle() {return tipoMovimientoDetalle;}
    public void setTipoMovimientoDetalle(TipoMovimientoDetalle tipoMovimientoDetalle) {this.tipoMovimientoDetalle = tipoMovimientoDetalle;}
    public OrdenCompraDetalle getOrdenCompraDetalle() {return ordenCompraDetalle;}
    public void setOrdenCompraDetalle(OrdenCompraDetalle ordenCompraDetalle) {this.ordenCompraDetalle = ordenCompraDetalle;}
    public SolicitudMovimiento getSolicitudMovimiento() {return solicitudMovimiento;}
    public void setSolicitudMovimiento(SolicitudMovimiento solicitudMovimiento) {this.solicitudMovimiento = solicitudMovimiento;}

    public OrdenProduccion getOrdenProduccion() {return ordenProduccion;}
    public void setOrdenProduccion(OrdenProduccion ordenProduccion) {this.ordenProduccion = ordenProduccion;}
    public EtapaProduccion getOrdenProduccionEtapa() {return ordenProduccionEtapa;}
    public void setOrdenProduccionEtapa(EtapaProduccion ordenProduccionEtapa) {this.ordenProduccionEtapa = ordenProduccionEtapa;}
    public RecepcionOC getRecepcionOc() {return recepcionOc;}
    public void setRecepcionOc(RecepcionOC recepcionOc) {this.recepcionOc = recepcionOc;}
    public String getCodigoRecepcion() {return codigoRecepcion;}
    public void setCodigoRecepcion(String codigoRecepcion) {this.codigoRecepcion = codigoRecepcion;}
    public BigDecimal getCostoUnitarioAplicado() {return costoUnitarioAplicado;}
    public void setCostoUnitarioAplicado(BigDecimal costoUnitarioAplicado) {this.costoUnitarioAplicado = costoUnitarioAplicado;}
    public BigDecimal getCostoTotalAplicado() {return costoTotalAplicado;}
    public void setCostoTotalAplicado(BigDecimal costoTotalAplicado) {this.costoTotalAplicado = costoTotalAplicado;}
}
