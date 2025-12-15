<#
.SYNOPSIS
    Realiza un backup diario de MySQL con rotación y hash SHA256.
.DESCRIPTION
    - Usa mysql_config_editor (login-path) o un archivo defaults-extra-file externo al repositorio.
    - Genera un archivo .sql, lo comprime en ZIP y crea un archivo .sha256.
    - Elimina respaldos más antiguos que el número de días definido en $RetentionDays.
    - Registra la ejecución en un archivo de log por día.
#>

param(
    [string]$DatabaseName = "clemen_integra_erp",
    [string]$BackupDirectory = (Join-Path $PSScriptRoot "data"),
    [int]$RetentionDays = 30,
    [string]$LoginPath = "clemen_backup",
    [string]$MysqlDumpPath = "mysqldump",
    [string]$DefaultsExtraFile = ""
)

$ErrorActionPreference = "Stop"
$timestamp = Get-Date -Format "yyyyMMdd_HHmm"
$today = Get-Date -Format "yyyyMMdd"
$logDirectory = $BackupDirectory
$logFile = Join-Path $logDirectory "backup_${today}.log"

function Write-Log {
    param(
        [string]$Message,
        [ValidateSet("INFO","WARN","ERROR")]
        [string]$Level = "INFO"
    )
    $line = "$(Get-Date -Format 'yyyy-MM-dd HH:mm:ss') [$Level] $Message"
    Write-Output $line
    Add-Content -Path $logFile -Value $line
}

try {
    if (-not (Test-Path -Path $BackupDirectory)) {
        Write-Log "Creando directorio de backups en '$BackupDirectory'" "INFO"
        New-Item -Path $BackupDirectory -ItemType Directory -Force | Out-Null
    }

    $dumpFile = Join-Path $BackupDirectory "${DatabaseName}_${timestamp}.sql"
    $zipFile = Join-Path $BackupDirectory "${DatabaseName}_${timestamp}.zip"
    $hashFile = "$zipFile.sha256"

    $dumpArgs = @("--single-transaction","--routines","--triggers","--events","--hex-blob","--default-character-set=utf8mb4")

    if ([string]::IsNullOrWhiteSpace($DefaultsExtraFile)) {
        $dumpArgs += "--login-path=$LoginPath"
    }
    else {
        $dumpArgs += "--defaults-extra-file=$DefaultsExtraFile"
    }

    $dumpArgs += $DatabaseName

    Write-Log "Iniciando backup de base de datos '$DatabaseName'" "INFO"
    $dumpArgs += @("--result-file=$dumpFile")

    & $MysqlDumpPath @($dumpArgs) 2>&1 | ForEach-Object { Write-Log $_ "INFO" }

    if ($LASTEXITCODE -ne 0 -or -not (Test-Path $dumpFile)) {
        throw "mysqldump falló con código $LASTEXITCODE"
    }

    Write-Log "Compresion ZIP -> '$zipFile'" "INFO"
    Compress-Archive -Path $dumpFile -DestinationPath $zipFile -Force

    if (-not (Test-Path $zipFile)) {
        throw "No se generó el archivo ZIP"
    }

    Remove-Item -Path $dumpFile -Force

    $fileHash = Get-FileHash -Algorithm SHA256 -Path $zipFile
    "${fileHash.Hash} $(Split-Path -Leaf $zipFile)" | Set-Content -Path $hashFile -Encoding ASCII
    Write-Log "Hash SHA256 generado en '$hashFile'" "INFO"

    Write-Log "Iniciando rotación de backups (retención: $RetentionDays días)" "INFO"
    $threshold = (Get-Date).AddDays(-$RetentionDays)
    $oldBackups = Get-ChildItem -Path $BackupDirectory -File | Where-Object { $_.LastWriteTime -lt $threshold -and ($_.Extension -in '.zip','.sha256','.log') }
    foreach ($item in $oldBackups) {
        Write-Log "Eliminando backup antiguo '${item.FullName}'" "WARN"
        Remove-Item -Path $item.FullName -Force
    }

    Write-Log "Backup finalizado exitosamente" "INFO"
    exit 0
}
catch {
    Write-Log $_.Exception.Message "ERROR"
    exit 1
}
