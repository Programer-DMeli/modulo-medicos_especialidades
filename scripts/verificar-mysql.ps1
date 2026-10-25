param(
    [string]$MysqlBin = 'C:\xampp\mysql\bin',
    [string]$Username = 'root'
)
$ErrorActionPreference = 'Stop'
$projectDirectory = Split-Path -Parent $PSScriptRoot
$database = 'medicos_integracion_' + [guid]::NewGuid().ToString('N').Substring(0, 8)
$environmentNames = @('SPRING_DATASOURCE_URL', 'SPRING_DATASOURCE_USERNAME', 'SPRING_DATASOURCE_PASSWORD',
    'SPRING_DATASOURCE_DRIVER_CLASS_NAME', 'SPRING_JPA_DATABASE_PLATFORM', 'SPRING_JPA_HIBERNATE_DDL_AUTO', 'MYSQL_PWD')
$previousEnvironment = @{}
foreach ($name in $environmentNames) { $previousEnvironment[$name] = [Environment]::GetEnvironmentVariable($name, 'Process') }
Push-Location $projectDirectory
try {
    New-Item -ItemType Directory -Force -Path target | Out-Null
    $schema = Get-Content docs/schema.sql -Raw
    $schema = $schema.Replace('CREATE DATABASE IF NOT EXISTS medico_especialidad', "CREATE DATABASE $database")
    $schema = $schema.Replace('USE medico_especialidad;', "USE $database;")
    $schemaPath = Join-Path $projectDirectory 'target/esquema-integracion.sql'
    Set-Content -LiteralPath $schemaPath -Value $schema -Encoding utf8
    $env:MYSQL_PWD = $env:TEST_DB_PASSWORD
    & "$MysqlBin\mysql.exe" --protocol=tcp --host=127.0.0.1 --port=3306 "--user=$Username" "--execute=source $($schemaPath.Replace('\','/'))"
    if ($LASTEXITCODE -ne 0) { throw 'No se pudo crear la base aislada. No se ejecutaron pruebas.' }
    $env:SPRING_DATASOURCE_URL = "jdbc:mysql://localhost:3306/$database"
    $env:SPRING_DATASOURCE_USERNAME = $Username
    $env:SPRING_DATASOURCE_PASSWORD = $env:TEST_DB_PASSWORD
    $env:SPRING_DATASOURCE_DRIVER_CLASS_NAME = 'com.mysql.cj.jdbc.Driver'
    $env:SPRING_JPA_DATABASE_PLATFORM = 'org.hibernate.dialect.MariaDBDialect'
    $env:SPRING_JPA_HIBERNATE_DDL_AUTO = 'validate'
    $maven = Get-Command mvn.cmd -ErrorAction SilentlyContinue
    if ($maven) { & $maven.Source -o test *> target/entrega-mysql.log }
    else { & .\mvnw.cmd test *> target/entrega-mysql.log }
    $testExitCode = $LASTEXITCODE
    $evidenceDirectory = Join-Path $projectDirectory 'docs/evidencias/mysql'
    New-Item -ItemType Directory -Force -Path $evidenceDirectory | Out-Null
    $databaseInfo = & "$MysqlBin\mysql.exe" --protocol=tcp --host=127.0.0.1 --port=3306 "--user=$Username" "--database=$database" --batch --execute="SELECT DATABASE() AS base_pruebas, VERSION() AS version_servidor; SELECT TABLE_NAME,COLUMN_NAME,REFERENCED_TABLE_NAME,REFERENCED_COLUMN_NAME FROM information_schema.KEY_COLUMN_USAGE WHERE TABLE_SCHEMA=DATABASE() AND REFERENCED_TABLE_NAME IS NOT NULL;"
    @("Fecha: $(Get-Date -Format 'yyyy-MM-dd HH:mm:ss')", "Base aislada: $database", 'Esquema validado sin alteraciones automáticas.',
        'Cada prueba transaccional revierte sus registros. Las filas EVIDENCIA_BD se consultan por JDBC antes del rollback.',
        '', $databaseInfo, '', (Get-Content target/entrega-mysql.log | Select-String 'EVIDENCIA_BD|Tests run:|BUILD SUCCESS|BUILD FAILURE' | ForEach-Object { $_.Line })) |
        Set-Content -LiteralPath (Join-Path $evidenceDirectory 'resultado.txt') -Encoding utf8
    Copy-Item target/surefire-reports/*.txt -Destination $evidenceDirectory -Force
    Get-Content (Join-Path $evidenceDirectory 'resultado.txt')
    if ($testExitCode -ne 0) { throw 'Fallaron las pruebas. Consulte target/entrega-mysql.log.' }
} finally {
    foreach ($name in $environmentNames) { [Environment]::SetEnvironmentVariable($name, $previousEnvironment[$name], 'Process') }
    Pop-Location
}
