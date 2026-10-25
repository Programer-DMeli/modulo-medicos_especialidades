param([ValidateRange(1024,65535)][int]$Port = 8081)
$ErrorActionPreference = 'Stop'
$projectDirectory = Split-Path -Parent $PSScriptRoot
$javaExecutable = if ($env:JAVA_HOME) { Join-Path $env:JAVA_HOME 'bin/java.exe' } else { 'C:\Program Files\Java\jdk-21.0.12.1\bin\java.exe' }
$jar = Join-Path $projectDirectory 'target/medicos_especialidades04-0.0.1-SNAPSHOT.jar'
if (-not (Test-Path -LiteralPath $javaExecutable)) { throw 'Configure JAVA_HOME con un JDK 21.' }
if (-not (Test-Path -LiteralPath $jar)) { throw 'Primero empaquete el proyecto con mvn package -DskipTests.' }
Push-Location $projectDirectory
try {
    & $javaExecutable -jar $jar --spring.profiles.active=local --spring.datasource.url=jdbc:mysql://127.0.0.1:3306/medico_especialidad --spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver --spring.jpa.hibernate.ddl-auto=validate "--server.port=$Port" --server.address=127.0.0.1
} finally { Pop-Location }
