# Vibey Lyrics: reports what plays on this PC (Spotify, browser, media player ...) through the Windows media
# controls (the same info as the volume flyout). One JSON line every ~0.4 s.
# Args: <pid of the game> <command file>. Writing "toggle", "next" or "previous" into the command file controls the
# player; the script ends by itself when the game is gone. The album art of each track is saved next to the command
# file (cover-<n>.img) and its path reported as "cover".
param([int]$GamePid = 0, [string]$CommandFile = '')
$ErrorActionPreference = 'Stop'
[Console]::OutputEncoding = New-Object System.Text.UTF8Encoding $false
try {
    Add-Type -AssemblyName System.Runtime.WindowsRuntime
    $null = [Windows.Media.Control.GlobalSystemMediaTransportControlsSessionManager, Windows.Media.Control, ContentType = WindowsRuntime]
} catch {
    [Console]::Out.WriteLine('{"error":"unsupported"}')
    exit 1
}
$asTask = [System.WindowsRuntimeSystemExtensions].GetMethods() | Where-Object {
    $_.Name -eq 'AsTask' -and $_.GetParameters().Count -eq 1 -and $_.GetParameters()[0].ParameterType.Name -eq 'IAsyncOperation`1'
} | Select-Object -First 1

function Await($op, [Type]$type) {
    $task = $asTask.MakeGenericMethod($type).Invoke($null, @($op))
    if (-not $task.Wait(3000)) { return $null }
    return $task.Result
}

$mgrType = [Windows.Media.Control.GlobalSystemMediaTransportControlsSessionManager]
$propsType = [Windows.Media.Control.GlobalSystemMediaTransportControlsSessionMediaProperties]
$streamType = [Windows.Storage.Streams.IRandomAccessStreamWithContentType, Windows.Storage.Streams, ContentType = WindowsRuntime]
$asStream = [System.IO.WindowsRuntimeStreamExtensions].GetMethods() | Where-Object {
    $_.Name -eq 'AsStreamForRead' -and $_.GetParameters().Count -eq 1
} | Select-Object -First 1
$coverDir = if ($CommandFile) { Split-Path -Parent $CommandFile } else { $env:TEMP }
$coverKey = ''
$coverPath = ''
$coverSeen = 0
$coverTries = 0
$coverN = 0

# Saves the track's thumbnail; returns its path or ''.
function SaveCover($props) {
    if (-not $props.Thumbnail) { return '' }
    $ras = Await ($props.Thumbnail.OpenReadAsync()) $streamType
    if (-not $ras) { return '' }
    # Called through reflection: PowerShell 5 cannot pick the overload for a WinRT stream itself.
    $in = $asStream.Invoke($null, @($ras))
    $ms = New-Object System.IO.MemoryStream
    $in.CopyTo($ms)
    $in.Dispose()
    if ($ms.Length -lt 64) { return '' }
    $script:coverN++
    Get-ChildItem -LiteralPath $coverDir -Filter 'cover-*.img' -ErrorAction SilentlyContinue | Where-Object {
        $_.LastWriteTime -lt (Get-Date).AddSeconds(-20)
    } | Remove-Item -ErrorAction SilentlyContinue
    $file = Join-Path $coverDir ('cover-' + [DateTimeOffset]::Now.ToUnixTimeMilliseconds() + '-' + $script:coverN + '.img')
    [System.IO.File]::WriteAllBytes($file, $ms.ToArray())
    return $file
}
$mgr = Await ($mgrType::RequestAsync()) $mgrType
if ($null -eq $mgr) {
    [Console]::Out.WriteLine('{"error":"unsupported"}')
    exit 1
}
$ErrorActionPreference = 'Continue'

function Pick() {
    foreach ($x in $mgr.GetSessions()) {
        if ($x.GetPlaybackInfo().PlaybackStatus -eq 'Playing') { return $x }
    }
    return $mgr.GetCurrentSession()
}

while ($true) {
    if ($GamePid -gt 0 -and -not (Get-Process -Id $GamePid -ErrorAction SilentlyContinue)) { exit 0 }
    if ($CommandFile -and (Test-Path -LiteralPath $CommandFile)) {
        $cmd = (Get-Content -LiteralPath $CommandFile -Raw).Trim()
        Remove-Item -LiteralPath $CommandFile -ErrorAction SilentlyContinue
        if ($cmd -eq 'quit') { exit 0 }
        $s = Pick
        if ($s) {
            if ($cmd -eq 'toggle') { $null = Await ($s.TryTogglePlayPauseAsync()) ([bool]) }
            if ($cmd -eq 'next') { $null = Await ($s.TrySkipNextAsync()) ([bool]) }
            if ($cmd -eq 'previous') { $null = Await ($s.TrySkipPreviousAsync()) ([bool]) }
        }
    }
    try {
        $session = Pick
        if (-not $session) {
            [Console]::Out.WriteLine('{"active":false}')
        } else {
            $info = $session.GetPlaybackInfo()
            $tl = $session.GetTimelineProperties()
            $props = Await ($session.TryGetMediaPropertiesAsync()) $propsType
            $playing = ($info.PlaybackStatus -eq 'Playing')
            $pos = $tl.Position.TotalMilliseconds
            # The player reports its position now and then; move it on by the time since that report.
            if ($playing -and $tl.LastUpdatedTime.Year -gt 2000) {
                $pos += ([DateTimeOffset]::Now - $tl.LastUpdatedTime).TotalMilliseconds
            }
            $dur = ($tl.EndTime - $tl.StartTime).TotalMilliseconds
            # Players change the title a moment before the picture: wait until the track has been the same for a
            # few reports, then fetch its art (a few tries, some players are slow to provide it).
            $key = [string]$props.Artist + '|' + [string]$props.Title
            if ($key -ne $coverKey) {
                $coverKey = $key
                $coverPath = ''
                $coverSeen = 0
                $coverTries = 0
            }
            $coverSeen++
            if (-not $coverPath -and $coverSeen -ge 3 -and $coverTries -lt 6 -and ($coverSeen % 3) -eq 0) {
                $coverTries++
                try { $coverPath = SaveCover $props } catch { $coverPath = '' }
            }
            if ($dur -gt 0 -and $pos -gt $dur) { $pos = $dur }
            $o = [ordered]@{
                active  = $true
                app     = [string]$session.SourceAppUserModelId
                title   = [string]$props.Title
                artist  = [string]$props.Artist
                album   = [string]$props.AlbumTitle
                playing = $playing
                pos     = [long]$pos
                dur     = [long]$dur
                cover   = [string]$coverPath
            }
            [Console]::Out.WriteLine(($o | ConvertTo-Json -Compress))
        }
    } catch {
        [Console]::Out.WriteLine('{"active":false}')
    }
    [Console]::Out.Flush()
    Start-Sleep -Milliseconds 400
}
