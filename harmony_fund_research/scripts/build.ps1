$ErrorActionPreference = "Stop"

$projectRoot = Resolve-Path (Join-Path $PSScriptRoot "..")
$devecoRoot = "D:\HarmonyOS\DevEco Studio"
$env:DEVECO_SDK_HOME = Join-Path $devecoRoot "sdk"
$hvigor = Join-Path $devecoRoot "tools\hvigor\bin\hvigorw.bat"

Push-Location $projectRoot
try {
  & $hvigor assembleHap --no-daemon
} finally {
  Pop-Location
}
