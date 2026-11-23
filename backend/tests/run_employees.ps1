param(
    [string]$PhpExe = 'php',
    [Parameter(Mandatory=$true)][string]$MariaDbBin,
    [switch]$NoV012
)
# Windows local lab only. New data directory per run; never reuse a running server or install a service.
$ErrorActionPreference='Stop'
$repoRoot=[IO.Path]::GetFullPath((Join-Path $PSScriptRoot '../..'))
$phpCommand=(Get-Command $PhpExe -ErrorAction Stop).Source
$dbBin=(Resolve-Path -LiteralPath $MariaDbBin).Path
$serverExe=Join-Path $dbBin 'mysqld.exe'
$installerExe=Join-Path $dbBin 'mysql_install_db.exe'
$clientExe=Join-Path $dbBin 'mysql.exe'
$shutdownExe=Join-Path $dbBin 'mysqladmin.exe'
foreach($exe in @($serverExe,$installerExe,$clientExe,$shutdownExe)){if(-not(Test-Path -LiteralPath $exe -PathType Leaf)){throw "Missing MariaDB tool: $exe"}}
& $phpCommand (Join-Path $PSScriptRoot 'check_repro.php')
if($LASTEXITCODE -ne 0){throw 'Preflight failed; no database initialized'}
$probe=New-Object Net.Sockets.TcpClient
try{
    $attempt=$probe.BeginConnect('127.0.0.1',13317,$null,$null)
    if($attempt.AsyncWaitHandle.WaitOne(400)){
        try{$probe.EndConnect($attempt)}catch{}
        if($probe.Connected){throw 'Port 13317 is occupied. Stop your own previous test server first. This script will not use or stop it.'}
    }
}finally{$probe.Dispose()}
$storage=Join-Path $repoRoot 'backend/storage'
[IO.Directory]::CreateDirectory($storage)|Out-Null
$runId=[Guid]::NewGuid().ToString('N')
$labData=Join-Path $storage ('mysql-repro-'+$runId)
$logRoot=Join-Path $storage ('repro-'+$runId)
[IO.Directory]::CreateDirectory($logRoot)|Out-Null
& $installerExe "--datadir=$labData" --port=13317 --silent *> (Join-Path $logRoot 'initialize.log')
if($LASTEXITCODE -ne 0){throw 'Local initialization failed; inspect the private initialize.log'}
$iniFile=Join-Path $labData 'my.ini'
$ini="[mysqld]`ndatadir="+$labData.Replace('\','/')+"`nport=13317`nbind-address=127.0.0.1`n[client]`nport=13317`n"
[IO.File]::WriteAllText($iniFile,$ini)
$envNames=@('WSP_TEST_PORT','WSP_TEST_DATA_DIR','WSP_TEST_ADMIN','WSP_TEST_PASSWORD','WSP_TEST_FACE_LOGIN_OPT_IN','WSP_NATIVE_PUBLIC','WSP_TEST_NO_V012')
$oldEnv=@{};foreach($name in $envNames){$oldEnv[$name]=[Environment]::GetEnvironmentVariable($name,'Process')}
$labProcess=$null
try{
    [Environment]::SetEnvironmentVariable('WSP_TEST_PORT','13317','Process')
    [Environment]::SetEnvironmentVariable('WSP_TEST_DATA_DIR',$labData,'Process')
    [Environment]::SetEnvironmentVariable('WSP_TEST_ADMIN','root','Process')
    [Environment]::SetEnvironmentVariable('WSP_TEST_PASSWORD','','Process')
    [Environment]::SetEnvironmentVariable('WSP_TEST_FACE_LOGIN_OPT_IN','0','Process')
    [Environment]::SetEnvironmentVariable('WSP_NATIVE_PUBLIC',$null,'Process')
    [Environment]::SetEnvironmentVariable('WSP_TEST_NO_V012',$(if($NoV012){'1'}else{$null}),'Process')
    $labProcess=Start-Process -FilePath $serverExe -ArgumentList @("--defaults-file=`"$iniFile`"",'--console') -WindowStyle Hidden -PassThru -RedirectStandardOutput (Join-Path $logRoot 'mysql-out.log') -RedirectStandardError (Join-Path $logRoot 'mysql-error.log')
    $ready=$false
    for($i=0;$i -lt 60;$i++){
        if($labProcess.HasExited){throw 'The new lab server stopped unexpectedly'}
        & $shutdownExe --no-defaults --protocol=TCP --host=127.0.0.1 --port=13317 --user=root --password= ping *> $null
        if($LASTEXITCODE -eq 0){$ready=$true;break};Start-Sleep -Milliseconds 250
    }
    if(-not $ready){throw 'The new lab server did not start'}
    & $phpCommand (Join-Path $PSScriptRoot 'labor_transition_validation_unit.php')
    if($LASTEXITCODE -ne 0){throw 'Validation unit tests failed'}
    & $phpCommand (Join-Path $PSScriptRoot 'integration.php') *> (Join-Path $logRoot 'integration.log')
    if($LASTEXITCODE -ne 0){throw 'Integration failed; inspect private integration.log'}
    Get-Content -LiteralPath (Join-Path $logRoot 'integration.log') -Tail 1
    Write-Output "Private test logs: $logRoot"
}finally{
    try{
        if($labProcess -and -not $labProcess.HasExited){
            $actual=(& $clientExe --no-defaults --protocol=TCP --host=127.0.0.1 --port=13317 --user=root --password= --batch --raw --skip-column-names -e 'SELECT @@datadir')
            if($LASTEXITCODE -ne 0 -or -not $actual){throw 'Cannot verify lab ownership; no shutdown attempted'}
            $actualPath=[IO.Path]::GetFullPath($actual.Trim()).TrimEnd('\','/')
            if($actualPath -ine [IO.Path]::GetFullPath($labData).TrimEnd('\','/')){throw 'Unexpected datadir; no shutdown attempted'}
            & $shutdownExe --no-defaults --protocol=TCP --host=127.0.0.1 --port=13317 --user=root --password= shutdown
            if($LASTEXITCODE -ne 0){throw 'Owned lab shutdown failed'}
        }
    }finally{foreach($name in $envNames){[Environment]::SetEnvironmentVariable($name,$oldEnv[$name],'Process')}}
}
