<#
.SYNOPSIS
  Builds the Fabric 1.21.11 jar and installs it into a "Vibey Dev" instance of the local Vibe Launcher.

.DESCRIPTION
  1. Runs `gradlew :fabric-1.21.11:build` (skip with -SkipBuild).
  2. Finds the "Vibey Dev" instance in the Vibe Launcher data dir (%APPDATA%\VibeLauncher, or
     $env:VIBE_LAUNCHER_HOME) or creates it exactly like the launcher does: instances/<id>/instance.json
     (Fabric, Minecraft 1.21.11, loader version resolved by the launcher on first start) plus minecraft/.
  3. Adds Fabric API the way the launcher's content installer does: newest compatible Modrinth release of
     project P7dR8mSH downloaded into minecraft/mods/ (SHA-1/SHA-512 checked) and recorded in content.json.
  4. Turns the launcher's per-instance "Vibey Client" switch OFF (vibey-client.json "enabled": false), so the
     launcher never replaces the local jar, removes every other vibey-client*/vibeyclient* jar from mods/ and
     copies the fresh jar in.

  Only files of the "Vibey Dev" instance are written. The launcher has no global instance index (it scans
  instances/*/instance.json at startup), so nothing else is touched. No process is ever stopped.

  Windows PowerShell 5.1 compatible. Keep this file ASCII-only (5.1 reads BOM-less scripts as ANSI).

.PARAMETER SkipBuild
  Do not run Gradle; install the jar that is already in versions/fabric-1.21.11/build/libs.

.PARAMETER LauncherHome
  Launcher data dir. Default: $env:VIBE_LAUNCHER_HOME, else %APPDATA%\VibeLauncher (same rule as the launcher).

.PARAMETER InstanceName
  Display name of the dev instance. Default: "Vibey Dev".

.EXAMPLE
  powershell -ExecutionPolicy Bypass -File scripts/dev-install.ps1
  powershell -ExecutionPolicy Bypass -File scripts/dev-install.ps1 -SkipBuild
#>
[CmdletBinding()]
param(
    [switch]$SkipBuild,
    [string]$LauncherHome,
    [string]$InstanceName = 'Vibey Dev'
)

Set-StrictMode -Version 2.0
$ErrorActionPreference = 'Stop'
$ProgressPreference = 'SilentlyContinue'

# ------------------------------------------------------------------ constants

$Target = 'fabric-1.21.11'
$McVersion = '1.21.11'
$FabricApiProject = 'P7dR8mSH'
$ModrinthApi = 'https://api.modrinth.com/v2'
$LauncherProcess = 'vibe-launcher'   # Tauri mainBinaryName of the Vibe Launcher
$Utf8NoBom = New-Object System.Text.UTF8Encoding $false
$RepoRoot = Split-Path -Parent $PSScriptRoot

function Info([string]$m) { Write-Host "[dev-install] $m" }
function Warn([string]$m) { Write-Host "[dev-install] WARNING: $m" -ForegroundColor Yellow }
function Fail([string]$m) { Write-Host "[dev-install] ERROR: $m" -ForegroundColor Red; exit 1 }

# ------------------------------------------------------------------ JSON (serde_json pretty format)

# The launcher writes JSON with serde_json::to_vec_pretty: 2-space indent, "key": value, no BOM, no trailing
# newline. ConvertTo-Json in 5.1 uses a different layout, so this writes the same layout as the launcher and
# keeps every field (and its order) of files it updates.
function ConvertTo-JsonString([string]$s) {
    $sb = New-Object System.Text.StringBuilder
    [void]$sb.Append('"')
    foreach ($ch in $s.ToCharArray()) {
        $c = [int]$ch
        if ($ch -eq '"') { [void]$sb.Append('\"') }
        elseif ($ch -eq '\') { [void]$sb.Append('\\') }
        elseif ($c -eq 10) { [void]$sb.Append('\n') }
        elseif ($c -eq 13) { [void]$sb.Append('\r') }
        elseif ($c -eq 9) { [void]$sb.Append('\t') }
        elseif ($c -eq 8) { [void]$sb.Append('\b') }
        elseif ($c -eq 12) { [void]$sb.Append('\f') }
        elseif ($c -lt 32) { [void]$sb.Append(('\u{0:x4}' -f $c)) }
        else { [void]$sb.Append($ch) }
    }
    [void]$sb.Append('"')
    return $sb.ToString()
}

function ConvertTo-PrettyJson($value, [int]$depth = 0) {
    $ind = '  ' * ($depth + 1)
    $end = '  ' * $depth
    if ($null -eq $value) { return 'null' }
    if ($value -is [bool]) { if ($value) { return 'true' } else { return 'false' } }
    if ($value -is [string]) { return (ConvertTo-JsonString $value) }
    if ($value -is [double] -or $value -is [single]) { return $value.ToString('R', [Globalization.CultureInfo]::InvariantCulture) }
    if ($value -is [ValueType]) { return ([IFormattable]$value).ToString($null, [Globalization.CultureInfo]::InvariantCulture) }
    if ($value -is [System.Collections.IDictionary]) {
        $keys = @($value.Keys)
        if ($keys.Count -eq 0) { return '{}' }
        $parts = foreach ($k in $keys) { $ind + (ConvertTo-JsonString ([string]$k)) + ': ' + (ConvertTo-PrettyJson $value[$k] ($depth + 1)) }
        return "{`n" + ($parts -join ",`n") + "`n$end}"
    }
    if ($value -is [System.Management.Automation.PSCustomObject]) {
        $props = @($value.PSObject.Properties)
        if ($props.Count -eq 0) { return '{}' }
        $parts = foreach ($p in $props) { $ind + (ConvertTo-JsonString $p.Name) + ': ' + (ConvertTo-PrettyJson $p.Value ($depth + 1)) }
        return "{`n" + ($parts -join ",`n") + "`n$end}"
    }
    if ($value -is [System.Collections.IEnumerable]) {
        $items = @($value)
        if ($items.Count -eq 0) { return '[]' }
        $parts = foreach ($i in $items) { $ind + (ConvertTo-PrettyJson $i ($depth + 1)) }
        return "[`n" + ($parts -join ",`n") + "`n$end]"
    }
    return (ConvertTo-JsonString ([string]$value))
}

function Read-JsonFile([string]$path) {
    $text = [IO.File]::ReadAllText($path, [Text.Encoding]::UTF8)   # also strips a BOM, if any
    return ($text | ConvertFrom-Json)
}

# Like the launcher's write_json_atomic: write <file>.tmp, then rename over the target.
function Write-JsonFile([string]$path, $value) {
    $tmp = "$path.tmp"
    [IO.File]::WriteAllText($tmp, (ConvertTo-PrettyJson $value), $Utf8NoBom)
    # [NullString]::Value: a plain $null would arrive as "" (invalid backup path)
    if (Test-Path -LiteralPath $path) { [IO.File]::Replace($tmp, $path, [NullString]::Value) } else { [IO.File]::Move($tmp, $path) }
}

function Set-Prop($obj, [string]$name, $value) {
    if ($obj.PSObject.Properties[$name]) { $obj.$name = $value }
    else { $obj | Add-Member -NotePropertyName $name -NotePropertyValue $value }
}

function Get-Prop($obj, [string]$name) {
    if ($null -ne $obj -and $obj.PSObject.Properties[$name]) { return $obj.$name }
    return $null
}

# ------------------------------------------------------------------ helpers

# Same rule as the launcher's slugify() for the instance id (folder name).
function Get-Slug([string]$name) {
    $sb = New-Object System.Text.StringBuilder
    $dash = $false
    foreach ($ch in $name.ToCharArray()) {
        $c = [int]$ch
        $rep = $null
        if ($c -eq 0xE4 -or $c -eq 0xC4) { $rep = 'ae' }
        elseif ($c -eq 0xF6 -or $c -eq 0xD6) { $rep = 'oe' }
        elseif ($c -eq 0xFC -or $c -eq 0xDC) { $rep = 'ue' }
        elseif ($c -eq 0xDF) { $rep = 'ss' }
        elseif ($c -lt 128) { $rep = ([string]$ch).ToLowerInvariant() }
        else { $rep = [string]$ch }
        foreach ($x in $rep.ToCharArray()) {
            if (($x -match '^[a-z0-9._]$') -and [int]$x -lt 128) { [void]$sb.Append($x); $dash = $false }
            elseif (-not $dash -and $sb.Length -gt 0) { [void]$sb.Append('-'); $dash = $true }
        }
    }
    $s = $sb.ToString().Trim('-', '.')
    if ($s.Length -eq 0) { return 'instance' }
    if ($s.Length -gt 48) { $s = $s.Substring(0, 48) }
    return $s
}

function Get-FileHashHex([string]$path, [string]$alg) {
    return (Get-FileHash -LiteralPath $path -Algorithm $alg).Hash.ToLowerInvariant()
}

# $true if another process holds the file open (the running game keeps its mod jars open).
function Test-FileLocked([string]$path) {
    try {
        $fs = [IO.File]::Open($path, [IO.FileMode]::Open, [IO.FileAccess]::ReadWrite, [IO.FileShare]::None)
        $fs.Close()
        return $false
    } catch [System.IO.IOException] {
        return $true
    } catch {
        return $false   # e.g. read-only file: not held open by anyone
    }
}

function Get-JsonFromWeb([string]$url) {
    $headers = @{ 'User-Agent' = 'vibey-client-dev-install' }
    return (Invoke-RestMethod -Uri $url -Headers $headers -UseBasicParsing)
}

function Test-VibeyJarName([string]$fileName) {
    $n = $fileName.ToLowerInvariant()
    return (($n.StartsWith('vibey-client') -or $n.StartsWith('vibeyclient')) -and ($n.EndsWith('.jar') -or $n.EndsWith('.jar.disabled')))
}

# ------------------------------------------------------------------ 1. build

if (-not $SkipBuild) {
    $gradleArgs = @(":$Target`:build")
    $props = Join-Path $RepoRoot 'gradle.properties'
    $capped = (Test-Path -LiteralPath $props) -and (Select-String -LiteralPath $props -Pattern '^\s*org\.gradle\.jvmargs\s*=.*-Xmx' -Quiet)
    if (-not $capped) { $gradleArgs = @('-Dorg.gradle.jvmargs=-Xmx3G') + $gradleArgs }
    Info "Building: gradlew $($gradleArgs -join ' ')"
    Push-Location $RepoRoot
    try {
        & (Join-Path $RepoRoot 'gradlew.bat') @gradleArgs
        $code = $LASTEXITCODE
    } finally {
        Pop-Location
    }
    if ($code -ne 0) { Fail "Gradle build failed (exit code $code)." }
}

$libs = Join-Path $RepoRoot "versions\$Target\build\libs"
if (-not (Test-Path -LiteralPath $libs)) { Fail "No build output in $libs. Run without -SkipBuild." }
$modVersion = $null
$baseName = 'vibey-client'
foreach ($line in [IO.File]::ReadAllLines((Join-Path $RepoRoot 'gradle.properties'))) {
    if ($line -match '^\s*mod_version\s*=\s*(.+?)\s*$') { $modVersion = $Matches[1] }
    if ($line -match '^\s*archives_base_name\s*=\s*(.+?)\s*$') { $baseName = $Matches[1] }
}
$candidates = @(Get-ChildItem -LiteralPath $libs -File -Filter '*.jar' | Where-Object { $_.Name -notmatch '-(sources|javadoc|dev)\.jar$' })
$jar = $null
if ($modVersion) { $jar = $candidates | Where-Object { $_.Name -eq "$baseName-$Target-$modVersion.jar" } | Select-Object -First 1 }
if (-not $jar) { $jar = $candidates | Sort-Object LastWriteTimeUtc -Descending | Select-Object -First 1 }
if (-not $jar) { Fail "No jar in $libs." }
Info "Jar: $($jar.FullName) ($([math]::Round($jar.Length / 1MB, 2)) MB, built $($jar.LastWriteTime))"

# ------------------------------------------------------------------ 2. launcher + instance

if (-not $LauncherHome) {
    if ($env:VIBE_LAUNCHER_HOME) { $LauncherHome = $env:VIBE_LAUNCHER_HOME }
    else { $LauncherHome = Join-Path ([Environment]::GetFolderPath('ApplicationData')) 'VibeLauncher' }
}
$instancesDir = Join-Path $LauncherHome 'instances'
if (-not (Test-Path -LiteralPath $instancesDir)) {
    Fail "Vibe Launcher data not found at $LauncherHome. Start the Vibe Launcher once (or pass -LauncherHome)."
}

$launcher = @(Get-Process -Name $LauncherProcess -ErrorAction SilentlyContinue)
$launcherRunning = $launcher.Count -gt 0
$launcherStart = $null
if ($launcherRunning) {
    try { $launcherStart = ($launcher | ForEach-Object { $_.StartTime } | Sort-Object | Select-Object -First 1) } catch { $launcherStart = $null }
}

# Find the instance by display name (the folder name is the id).
$instDir = $null
$inst = $null
foreach ($d in Get-ChildItem -LiteralPath $instancesDir -Directory) {
    $j = Join-Path $d.FullName 'instance.json'
    if (-not (Test-Path -LiteralPath $j)) { continue }
    try { $o = Read-JsonFile $j } catch { continue }
    $n = Get-Prop $o 'name'
    if ($n -and $n.Trim() -ieq $InstanceName) { $instDir = $d.FullName; $inst = $o; break }
}

$created = $false
if (-not $inst) {
    # Same as InstanceStore::create in the launcher: unique slug id, instance.json, minecraft/ dir.
    $base = Get-Slug $InstanceName
    $id = $base
    $i = 2
    while (Test-Path -LiteralPath (Join-Path $instancesDir $id)) { $id = "$base-$i"; $i++ }
    $instDir = Join-Path $instancesDir $id
    $nowMs = [long]([DateTimeOffset]::UtcNow.ToUnixTimeMilliseconds())
    $inst = [ordered]@{
        id = $id
        name = $InstanceName
        icon = $null
        accent = '#8b6cff'
        group = $null
        mc_version = $McVersion
        # version null = the launcher resolves the recommended Fabric loader on first start
        loader = [ordered]@{ kind = 'fabric'; version = $null }
        java = [ordered]@{ path = $null; min_memory_mb = $null; max_memory_mb = $null; jvm_args = $null; env = [ordered]@{} }
        window = [ordered]@{ width = $null; height = $null; fullscreen = $null }
        source = $null
        install_state = 'not_installed'
        version_id = $null
        created_at = $nowMs
        last_played = $null
        play_time_secs = 0
        notes = 'Local Vibey Client dev build, updated by scripts/dev-install.ps1 in the vibey-client repo.'
        auto_update_content = $null
    }
    [void](New-Item -ItemType Directory -Force -Path (Join-Path $instDir 'minecraft\mods'))
    Write-JsonFile (Join-Path $instDir 'instance.json') $inst
    $created = $true
    Info "Created instance '$InstanceName' ($instDir)"
} else {
    $id = Split-Path -Leaf $instDir
    Info "Instance '$InstanceName' found ($instDir)"
    # Keep it on Fabric + 1.21.11. Changing instance.json while the launcher runs is unsafe: it keeps all
    # instances in memory and rewrites instance.json from that copy on its next change (e.g. after playing).
    $loader = Get-Prop $inst 'loader'
    $kind = Get-Prop $loader 'kind'
    if ((Get-Prop $inst 'mc_version') -ne $McVersion -or $kind -ne 'fabric') {
        if ($launcherRunning) {
            Fail "'$InstanceName' is set to $kind $(Get-Prop $inst 'mc_version'), not fabric $McVersion. Close the Vibe Launcher and run this script again (or change it back in the launcher)."
        }
        Set-Prop $inst 'mc_version' $McVersion
        if ($kind -ne 'fabric') { Set-Prop $inst 'loader' ([ordered]@{ kind = 'fabric'; version = $null }) }
        # Same as InstanceStore::modify when version or loader change
        Set-Prop $inst 'install_state' 'not_installed'
        Set-Prop $inst 'version_id' $null
        Write-JsonFile (Join-Path $instDir 'instance.json') $inst
        Info "Reset '$InstanceName' to Fabric $McVersion"
    }
}

$gameDir = Join-Path $instDir 'minecraft'
$modsDir = Join-Path $gameDir 'mods'
[void](New-Item -ItemType Directory -Force -Path $modsDir)

# ------------------------------------------------------------------ game running?

$gameProcs = @()
try {
    $gameProcs = @(Get-CimInstance Win32_Process -Filter "Name like 'java%'" -ErrorAction Stop |
        Where-Object { $_.CommandLine -and $_.CommandLine.IndexOf($gameDir, [StringComparison]::OrdinalIgnoreCase) -ge 0 })
} catch { $gameProcs = @() }
$lockedJars = @(Get-ChildItem -LiteralPath $modsDir -File | Where-Object { (Test-VibeyJarName $_.Name) -and (Test-FileLocked $_.FullName) })
if ($gameProcs.Count -gt 0 -or $lockedJars.Count -gt 0) {
    $what = if ($lockedJars.Count -gt 0) { "locked: $(($lockedJars | ForEach-Object { $_.Name }) -join ', ')" } else { "java PID $(($gameProcs | ForEach-Object { $_.ProcessId }) -join ', ')" }
    Fail "Minecraft of '$InstanceName' is running ($what). Quit the game, then run this script again."
}

# ------------------------------------------------------------------ 3. Vibey Client switch OFF

$vcPath = Join-Path $instDir 'vibey-client.json'
if (Test-Path -LiteralPath $vcPath) {
    $vc = Read-JsonFile $vcPath
    if ((Get-Prop $vc 'enabled') -ne $false -or (Get-Prop $vc 'file') -or (Get-Prop $vc 'version') -or (Get-Prop $vc 'sha256')) {
        # Same as the launcher's "switch off": enabled false, managed file forgotten, requires kept.
        Set-Prop $vc 'enabled' $false
        Set-Prop $vc 'file' $null
        Set-Prop $vc 'version' $null
        Set-Prop $vc 'sha256' $null
        Write-JsonFile $vcPath $vc
        Info 'Vibey Client switch turned OFF (vibey-client.json)'
    }
} else {
    $vc = [ordered]@{
        enabled = $false; version = $null; channel = $null; file = $null; sha256 = $null
        mc_version = $null; loader = $null; installed_at = $null; requires = @()
    }
    Write-JsonFile $vcPath $vc
    Info 'Vibey Client switch set OFF (vibey-client.json)'
}

# ------------------------------------------------------------------ 4. Fabric API (like the launcher's content installer)

$contentPath = Join-Path $instDir 'content.json'
$content = $null
if (Test-Path -LiteralPath $contentPath) {
    try { $content = Read-JsonFile $contentPath } catch { Warn "content.json unreadable, leaving it alone: $_" }
}
$haveApi = $false
$staleKeys = @()
if ($content -and (Get-Prop $content 'files')) {
    foreach ($p in $content.files.PSObject.Properties) {
        if ((Get-Prop $p.Value 'source') -eq 'modrinth' -and (Get-Prop $p.Value 'project_id') -eq $FabricApiProject) {
            $f = Join-Path $gameDir ($p.Name -replace '/', '\')
            if ((Test-Path -LiteralPath $f) -or (Test-Path -LiteralPath "$f.disabled")) { $haveApi = $true } else { $staleKeys += $p.Name }
        }
    }
}
if (-not $haveApi) {
    $manual = @(Get-ChildItem -LiteralPath $modsDir -File | Where-Object { $_.Name -match '^fabric-api-.*\.jar(\.disabled)?$' })
    if ($manual.Count -gt 0) { $haveApi = $true; Info "Fabric API present ($($manual[0].Name), not from the launcher index)" }
}
if ($haveApi) {
    Info 'Fabric API already installed'
} else {
    Info "Installing Fabric API for $McVersion from Modrinth"
    try { [Net.ServicePointManager]::SecurityProtocol = [Net.ServicePointManager]::SecurityProtocol -bor [Net.SecurityProtocolType]::Tls12 } catch { }
    $project = Get-JsonFromWeb "$ModrinthApi/project/$FabricApiProject"
    $q = '?loaders=' + [Uri]::EscapeDataString('["fabric"]') + '&game_versions=' + [Uri]::EscapeDataString("[`"$McVersion`"]")
    $versions = @(Get-JsonFromWeb "$ModrinthApi/project/$($project.id)/version$q")
    # pick_best: newest release with a matching loader, else newest with a matching loader
    $versions = @($versions | Where-Object { @($_.game_versions) -contains $McVersion -and @($_.loaders) -contains 'fabric' } | Sort-Object { [string]$_.date_published } -Descending)
    $ver = $versions | Where-Object { $_.version_type -eq 'release' } | Select-Object -First 1
    if (-not $ver) { $ver = $versions | Select-Object -First 1 }
    if (-not $ver) { Fail "Modrinth has no Fabric API version for Minecraft $McVersion." }
    $file = @($ver.files) | Where-Object { $_.primary } | Select-Object -First 1
    if (-not $file) { $file = @($ver.files) | Select-Object -First 1 }
    $fileName = $file.filename -replace '[<>:"/\\|?*]', '_'
    $tmp = Join-Path $env:TEMP "vibey-dev-install-$([guid]::NewGuid().ToString('N')).jar"
    Invoke-WebRequest -Uri $file.url -OutFile $tmp -UseBasicParsing -Headers @{ 'User-Agent' = 'vibey-client-dev-install' }
    if ((Get-FileHashHex $tmp 'SHA1') -ne ([string]$file.hashes.sha1).ToLowerInvariant() -or
        (Get-FileHashHex $tmp 'SHA512') -ne ([string]$file.hashes.sha512).ToLowerInvariant()) {
        Remove-Item -LiteralPath $tmp -Force
        Fail "Fabric API download $($file.url) failed the hash check."
    }
    Move-Item -LiteralPath $tmp -Destination (Join-Path $modsDir $fileName) -Force

    $side = 'unknown'
    $on = @('required', 'optional')
    $cs = [string](Get-Prop $project 'client_side'); $ss = [string](Get-Prop $project 'server_side')
    if (-not $cs) { $cs = 'unknown' }; if (-not $ss) { $ss = 'unknown' }
    if ($cs -eq 'unsupported' -and $on -contains $ss) { $side = 'server' }
    elseif ($ss -eq 'unsupported' -and $on -contains $cs) { $side = 'client' }
    elseif (($on -contains $cs -or $cs -eq 'unknown') -and ($on -contains $ss -or $ss -eq 'unknown') -and -not ($cs -eq 'unknown' -and $ss -eq 'unknown')) { $side = 'both' }
    $releaseType = 'release'
    if ($ver.version_type -eq 'beta' -or $ver.version_type -eq 'alpha') { $releaseType = $ver.version_type }

    $entry = [ordered]@{
        source = 'modrinth'
        project_id = $project.id
        version_id = $ver.id
        sha1 = $file.hashes.sha1
        sha512 = $file.hashes.sha512
        title = $project.title
        icon_url = Get-Prop $project 'icon_url'
        version_number = $ver.version_number
        url = $file.url
        size = [long]$file.size
        date = [string]$ver.date_published
        release_type = $releaseType
        side = $side
    }
    if (-not $content) {
        $content = [pscustomobject][ordered]@{ format = 1; files = [pscustomobject]@{}; local = [pscustomobject]@{}; unknown = [pscustomobject]@{}; pinned = @() }
    }
    if (-not (Get-Prop $content 'files')) { Set-Prop $content 'files' ([pscustomobject]@{}) }
    foreach ($k in $staleKeys) { $content.files.PSObject.Properties.Remove($k) }
    Set-Prop $content.files "mods/$fileName" ([pscustomobject]$entry)
    Set-Prop $content 'format' 1
    Write-JsonFile $contentPath $content
    Info "Fabric API $($ver.version_number) installed ($fileName)"
}

# ------------------------------------------------------------------ 5. copy the jar

foreach ($old in @(Get-ChildItem -LiteralPath $modsDir -File | Where-Object { Test-VibeyJarName $_.Name })) {
    if ($old.Name -ne $jar.Name) { Info "Removing $($old.Name)" }
    try { Remove-Item -LiteralPath $old.FullName -Force } catch { Fail "Could not remove $($old.FullName) (is the game running?): $_" }
}
$dest = Join-Path $modsDir $jar.Name
Copy-Item -LiteralPath $jar.FullName -Destination $dest -Force
if ((Get-FileHashHex $dest 'SHA256') -ne (Get-FileHashHex $jar.FullName 'SHA256')) { Fail "Copy of $($jar.Name) is corrupt." }
Info "Installed $($jar.Name) into $modsDir"

# ------------------------------------------------------------------ launcher hints

if ($launcherRunning) {
    $createdAt = Get-Prop $inst 'created_at'
    $knownToLauncher = $false
    if ($launcherStart -and $createdAt -and -not $created) {
        $createdTime = [DateTimeOffset]::FromUnixTimeMilliseconds([long]$createdAt).LocalDateTime
        $knownToLauncher = $launcherStart -gt $createdTime
    }
    if ($knownToLauncher) {
        Info "The Vibe Launcher is running and already lists '$InstanceName'; the new jar is picked up on the next start of the game."
    } else {
        Warn "The Vibe Launcher is running and reads instances only at startup: restart it to see '$InstanceName'."
    }
}
Info 'Done.'
