$ErrorActionPreference = 'Stop'
Set-Location $PSScriptRoot
if (-not $env:JAVA_HOME) {
  $jbr = Join-Path $env:ProgramFiles 'Android\Android Studio\jbr'
  if (Test-Path "$jbr\bin\java.exe") { $env:JAVA_HOME = $jbr }
}
if ($env:JAVA_HOME) { $env:Path = "$env:JAVA_HOME\bin;$env:Path" }
if (-not (Get-Command java -ErrorAction SilentlyContinue)) { throw 'Instala Android Studio o JDK 17.' }
if (-not $env:ANDROID_HOME) {
  if ($env:ANDROID_SDK_ROOT) { $env:ANDROID_HOME = $env:ANDROID_SDK_ROOT }
  else { $env:ANDROID_HOME = Join-Path $env:LOCALAPPDATA 'Android\Sdk' }
}
if (-not (Test-Path (Join-Path $env:ANDROID_HOME 'platforms\android-35\android.jar'))) { throw 'Abre Android Studio > SDK Manager e instala Android SDK Platform 35 y Build Tools 35.' }
$cache = Join-Path $env:LOCALAPPDATA 'NexusVR-build'
$gradle = Join-Path $cache 'gradle-8.9\bin\gradle.bat'
if (-not (Test-Path $gradle)) {
  New-Item -ItemType Directory -Force $cache | Out-Null
  $archive = Join-Path $cache 'gradle-8.9-bin.zip'
  Invoke-WebRequest 'https://services.gradle.org/distributions/gradle-8.9-bin.zip' -OutFile $archive
  Expand-Archive $archive -DestinationPath $cache -Force
}
& $gradle --no-daemon wrapper --gradle-version 8.9 --distribution-type bin
if ($LASTEXITCODE -ne 0) { throw 'No se pudo crear el wrapper de Gradle.' }
& .\gradlew.bat --no-daemon :app:assembleDebug
if ($LASTEXITCODE -ne 0) { throw 'Falló la compilación. Revisa los errores de Gradle.' }
Write-Host 'APK: app\build\outputs\apk\debug\app-debug.apk'
