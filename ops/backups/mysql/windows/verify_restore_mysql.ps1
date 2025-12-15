<#
.SYNOPSIS
    Verifica semanalmente que el backup más reciente puede restaurarse en una base temporal.
.DESCRIPTION
    - Valida integridad del ZIP usando el archivo .sha256 generado por el backup.
    - Restaura en <DatabaseName>_restorecheck, ejecuta consultas de salud y elimina la base temporal.
    - Usa login-path de mysql_config_editor o defaults-extra-file externo.
#>

param(
    [string]$DatabaseName = "clemen_integra_erp",
    [string]$BackupDirectory = (Join-Path $PSScriptRoot "data"),
    [string]$LoginPath = "clemen_backup",
    [string]$MysqlClientPath = "mysql",
    [string]$DefaultsExtraFile = ""
)

$ErrorActionPreference = "Stop"
$today = Get-Date -Format "yyyyMMdd"
$logFile = Join-Path $BackupDirectory "verify_restore_${today}.log"
$tempDbName = "${DatabaseName}_restorecheck"
$tempDir = Join-Path ([System.IO.Path]::GetTempPath()) "mysql_restorecheck"

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

function Invoke-MysqlCommand {
    param(
        [string]$Command,
        [string]$Database
    )
    $args = @()
    if ([string]::IsNullOrWhiteSpace($DefaultsExtraFile)) {
        $args += "--login-path=$LoginPath"
    }
    else {
        $args += "--defaults-extra-file=$DefaultsExtraFile"
    }

    if (-not [string]::IsNullOrWhiteSpace($Database)) {
        $args += "--database=$Database"
    }

    $args += "-e"
    $args += $Command

    & $MysqlClientPath @($args)

    if ($LASTEXITCODE -ne 0) {
        throw "mysql falló con código $LASTEXITCODE al ejecutar: $Command"
    }
}

try {
    if (-not (Test-Path -Path $BackupDirectory)) {
        throw "Directorio de backups no encontrado: $BackupDirectory"
    }

    $latestBackup = Get-ChildItem -Path $BackupDirectory -Filter "${DatabaseName}_*.zip" -File | Sort-Object LastWriteTime -Descending | Select-Object -First 1
    if (-not $latestBackup) {
        throw "No hay backups ZIP disponibles para restaurar"
    }

    $hashFile = "$($latestBackup.FullName).sha256"
    if (-not (Test-Path $hashFile)) {
        Write-Log "No se encontró el archivo de hash para validar integridad ($hashFile)." "WARN"
    }
    else {
        $expected = (Get-Content -Path $hashFile -ErrorAction Stop).Split(" ")[0]
        $actual = (Get-FileHash -Algorithm SHA256 -Path $latestBackup.FullName).Hash
        if ($expected -ne $actual) {
            throw "Hash SHA256 no coincide para ${latestBackup.Name}"
        }
        Write-Log "Integridad del backup verificada mediante SHA256." "INFO"
    }

    if (Test-Path $tempDir) { Remove-Item -Path $tempDir -Recurse -Force }
    New-Item -Path $tempDir -ItemType Directory | Out-Null

    Write-Log "Descomprimiendo ${latestBackup.Name} en $tempDir" "INFO"
    Expand-Archive -Path $latestBackup.FullName -DestinationPath $tempDir -Force

    $sqlFile = Get-ChildItem -Path $tempDir -Filter "*.sql" -File | Select-Object -First 1
    if (-not $sqlFile) { throw "No se encontró archivo SQL dentro del ZIP" }

    Write-Log "Recreando base temporal '$tempDbName'" "INFO"
    Invoke-MysqlCommand -Command "DROP DATABASE IF EXISTS \`$tempDbName\`;" -Database ""
    Invoke-MysqlCommand -Command "CREATE DATABASE \`$tempDbName\` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;" -Database ""

    Write-Log "Restaurando desde ${sqlFile.FullName}" "INFO"
    $mysqlArgs = @()
    if ([string]::IsNullOrWhiteSpace($DefaultsExtraFile)) {
        $mysqlArgs += "--login-path=$LoginPath"
    }
    else {
        $mysqlArgs += "--defaults-extra-file=$DefaultsExtraFile"
    }
    $mysqlArgs += "--database=$tempDbName"

    $restoreCmd = "\"$MysqlClientPath\" ${($mysqlArgs -join ' ')} < \"$($sqlFile.FullName)\""
    cmd.exe /c $restoreCmd
    if ($LASTEXITCODE -ne 0) {
        throw "Restauración falló con código $LASTEXITCODE"
    }

    $healthQueries = @(
        @{ Description = "Conteo de productos"; Query = "SELECT COUNT(*) AS total_productos FROM productos;" },
        @{ Description = "Conteo de lotes"; Query = "SELECT COUNT(*) AS total_lotes FROM lotes_productos;" },
        @{ Description = "Conteo de movimientos"; Query = "SELECT COUNT(*) AS total_movimientos FROM movimientos_inventario;" }
    )

    foreach ($check in $healthQueries) {
        Write-Log "Ejecutando chequeo: $($check.Description)" "INFO"
        $result = & $MysqlClientPath @($mysqlArgs) "-e" $check.Query
        if ($LASTEXITCODE -ne 0) {
            throw "Consulta de salud falló: $($check.Description)"
        }
        $result | ForEach-Object { Write-Log $_ "INFO" }
    }

    Write-Log "Verificación de restauración completada exitosamente" "INFO"
}
catch {
    Write-Log $_.Exception.Message "ERROR"
    exit 1
}
finally {
    try {
        Write-Log "Eliminando base temporal '$tempDbName'" "INFO"
        Invoke-MysqlCommand -Command "DROP DATABASE IF EXISTS \`$tempDbName\`;" -Database ""
    }
    catch {
        Write-Log "No se pudo limpiar la base temporal: $($_.Exception.Message)" "WARN"
    }

    if (Test-Path $tempDir) { Remove-Item -Path $tempDir -Recurse -Force }
}
