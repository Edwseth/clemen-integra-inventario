param(
    [string]$ServiceName = "ClemenERP-Backend",
    [string]$HealthUrl = "http://localhost:8080/actuator/health",
    [int]$TimeoutSeconds = 10,
    [string]$LogFile = "C:\\ClemenERP\\logs\\health-watchdog.log"
)

$timestamp = (Get-Date).ToString("s")
$logPrefix = "[$timestamp]"

function Write-Log($message) {
    "$logPrefix $message" | Out-File -FilePath $LogFile -Append -Encoding utf8
}

try {
    $response = Invoke-RestMethod -Method Get -Uri $HealthUrl -TimeoutSec $TimeoutSeconds
    if ($response.status -eq "UP" -or $response.status -eq "up") {
        Write-Log "Health OK ($HealthUrl)."
        exit 0
    }
    Write-Log "Health en estado inesperado: $($response | ConvertTo-Json -Compress). Reiniciando $ServiceName"
}
catch {
    Write-Log "Error al consultar health: $($_.Exception.Message). Reiniciando $ServiceName"
}

# Si llegó aquí, reinicia el servicio
try {
    Write-Log "Reiniciando servicio $ServiceName"
    Restart-Service -Name $ServiceName -Force -ErrorAction Stop
    Write-Log "Servicio $ServiceName reiniciado"
}
catch {
    Write-Log "Fallo al reiniciar $ServiceName: $($_.Exception.Message)"
    exit 1
}
