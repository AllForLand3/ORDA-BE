param(
    [Parameter(Mandatory = $true)]
    [string]$KeyPath
)

$ErrorActionPreference = 'Stop'
if (-not (Test-Path -LiteralPath $KeyPath -PathType Leaf)) {
    throw 'SSH key file does not exist.'
}
Write-Output 'Keep this terminal open while using the AWS database. Press Ctrl+C to close the tunnel.'
& ssh.exe -i $KeyPath -N -L '127.0.0.1:15432:127.0.0.1:55432' `
    -o StrictHostKeyChecking=yes -o ExitOnForwardFailure=yes `
    -o ServerAliveInterval=30 -o ServerAliveCountMax=3 ubuntu@43.203.221.58
if ($LASTEXITCODE -ne 0) { throw 'SSH tunnel stopped with an error.' }
