param(
    [string]$Jdk = 'C:\Program Files\Java\jdk-17',
    [string]$Sdk = 'D:\CodexData\cache\android-sdk',
    [string]$Gradle = (Join-Path $PSScriptRoot '..\gradlew.bat'),
    [string]$Cache = 'D:\CodexData\cache\gradle',
    [string]$Temporary = 'D:\CodexData\temp\limbus-build\jvm'
)
$ErrorActionPreference = 'Stop'
$env:JAVA_HOME = $Jdk
$env:PATH = "$Jdk\bin;$env:PATH"
$env:JAVA_TOOL_OPTIONS = ''
$env:HOST_OS = 'windows'
$env:HOST_ARCH = 'x86_64'
$env:ANDROID_HOME = $Sdk
$env:ANDROID_SDK_ROOT = $Sdk
$env:GRADLE_USER_HOME = $Cache
$env:TEMP = $Temporary
$env:TMP = $Temporary
New-Item -ItemType Directory -Force -Path $Temporary, $Cache | Out-Null
Push-Location (Split-Path $PSScriptRoot)
try {
    python scripts/fetch-build-assets.py
    if ($LASTEXITCODE -ne 0) { throw 'Pinned build assets could not be verified.' }
    if (!(Test-Path -LiteralPath 'signing\keystore.properties')) {
        throw 'A private signing/keystore.properties is required for a signed release; see docs/llc-launcher.md.'
    }
    & $Gradle --console=plain --no-daemon --max-workers=4 "-Dorg.gradle.jvmargs=-Xmx4096m -Dfile.encoding=UTF-8 -Djava.io.tmpdir=$Temporary" :app:testDebugUnitTest :app:assembleDebug :app:assembleRelease
    if ($LASTEXITCODE -ne 0) { throw "Gradle failed with exit code $LASTEXITCODE" }
} finally {
    Pop-Location
}
