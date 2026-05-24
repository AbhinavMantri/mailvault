param(
    [switch]$SkipInfrastructure,
    [switch]$SkipBuild,
    [switch]$KeepServices,
    [int]$StartupTimeoutSeconds = 300,
    [string]$DbUrl = "jdbc:postgresql://localhost:5432/mailvault",
    [string]$DbUsername = "mailvault",
    [string]$DbPassword = "mailvault",
    [string]$KafkaBootstrapServers = "localhost:9092",
    [string]$MinioEndpoint = "http://localhost:9000",
    [string]$MinioAccessKey = "mailvault",
    [string]$MinioSecretKey = "mailvault-secret",
    [string]$MinioBucket = "mailvault-emails",
    [string]$OpenSearchBaseUrl = "http://localhost:9200",
    [string]$RedisHost = "localhost",
    [string]$RedisPort = "6379"
)

$ErrorActionPreference = "Stop"

$root = Split-Path -Parent $PSScriptRoot
$logsDir = Join-Path $root ".e2e\logs"
New-Item -ItemType Directory -Force $logsDir | Out-Null

function Write-Step($Message) {
    Write-Host "[e2e] $Message"
}

function Assert-True($Condition, $Message) {
    if (-not $Condition) {
        throw $Message
    }
}

function Wait-Http($Url, $Name, $TimeoutSeconds) {
    $deadline = (Get-Date).AddSeconds($TimeoutSeconds)
    do {
        try {
            Invoke-RestMethod -Uri $Url -TimeoutSec 3 | Out-Null
            Write-Step "$Name is ready"
            return
        } catch {
            Start-Sleep -Seconds 2
        }
    } while ((Get-Date) -lt $deadline)

    throw "$Name did not become ready at $Url"
}

function Wait-Until($Name, $TimeoutSeconds, [scriptblock]$Condition) {
    $deadline = (Get-Date).AddSeconds($TimeoutSeconds)
    do {
        $result = & $Condition
        if ($result) {
            Write-Step "$Name verified"
            return $result
        }
        Start-Sleep -Seconds 2
    } while ((Get-Date) -lt $deadline)

    throw "$Name was not verified within $TimeoutSeconds seconds"
}

function Invoke-Json($Method, $Url, $Body = $null) {
    $params = @{
        Method = $Method
        Uri = $Url
        TimeoutSec = 15
    }
    if ($null -ne $Body) {
        $params.ContentType = "application/json"
        $params.Body = ($Body | ConvertTo-Json -Depth 10)
    }
    Invoke-RestMethod @params
}

$services = @(
    @{ Name = "ingestion-service"; Path = "ingestion-service"; Health = "http://localhost:8081/actuator/health" },
    @{ Name = "mailbox-service"; Path = "mailbox-service"; Health = "http://localhost:8082/actuator/health" },
    @{ Name = "quota-service"; Path = "quota-service"; Health = "http://localhost:8083/actuator/health" },
    @{ Name = "search-indexer"; Path = "search-indexer"; Health = "http://localhost:8084/actuator/health" },
    @{ Name = "search-service"; Path = "search-service"; Health = "http://localhost:8085/actuator/health" },
    @{ Name = "attachment-worker"; Path = "attachment-worker"; Health = "http://localhost:8086/actuator/health" }
)

$processes = @()
$previousEnv = @{
    DB_URL = $env:DB_URL
    DB_USERNAME = $env:DB_USERNAME
    DB_PASSWORD = $env:DB_PASSWORD
    KAFKA_BOOTSTRAP_SERVERS = $env:KAFKA_BOOTSTRAP_SERVERS
    MINIO_ENDPOINT = $env:MINIO_ENDPOINT
    MINIO_ACCESS_KEY = $env:MINIO_ACCESS_KEY
    MINIO_SECRET_KEY = $env:MINIO_SECRET_KEY
    MINIO_BUCKET = $env:MINIO_BUCKET
    OPENSEARCH_BASE_URL = $env:OPENSEARCH_BASE_URL
    REDIS_HOST = $env:REDIS_HOST
    REDIS_PORT = $env:REDIS_PORT
}

function Set-EnvOrRemove($Name, $Value) {
    if ($null -eq $Value) {
        Remove-Item "Env:$Name" -ErrorAction SilentlyContinue
    } else {
        Set-Item "Env:$Name" $Value
    }
}

function Normalize-ProcessPathEnv() {
    $pathValue = [Environment]::GetEnvironmentVariable("Path", "Process")
    if ([string]::IsNullOrWhiteSpace($pathValue)) {
        $pathValue = [Environment]::GetEnvironmentVariable("PATH", "Process")
    }
    if (-not [string]::IsNullOrWhiteSpace($pathValue)) {
        [Environment]::SetEnvironmentVariable("PATH", $null, "Process")
        [Environment]::SetEnvironmentVariable("Path", $pathValue, "Process")
    }
}

try {
    Normalize-ProcessPathEnv

    $env:DB_URL = $DbUrl
    $env:DB_USERNAME = $DbUsername
    $env:DB_PASSWORD = $DbPassword
    $env:KAFKA_BOOTSTRAP_SERVERS = $KafkaBootstrapServers
    $env:MINIO_ENDPOINT = $MinioEndpoint
    $env:MINIO_ACCESS_KEY = $MinioAccessKey
    $env:MINIO_SECRET_KEY = $MinioSecretKey
    $env:MINIO_BUCKET = $MinioBucket
    $env:OPENSEARCH_BASE_URL = $OpenSearchBaseUrl
    $env:REDIS_HOST = $RedisHost
    $env:REDIS_PORT = $RedisPort

    if (-not $SkipInfrastructure) {
        Write-Step "starting Docker infrastructure"
        Push-Location $root
        docker compose up -d
        Pop-Location
    }

    Wait-Http "http://localhost:9200/_cluster/health" "OpenSearch" $StartupTimeoutSeconds

    foreach ($service in $services) {
        $serviceDir = Join-Path $root $service.Path
        if (-not $SkipBuild) {
            Write-Step "packaging $($service.Name)"
            Push-Location $serviceDir
            mvn -DskipTests package
            Pop-Location
        }

        $jar = Get-ChildItem -Path (Join-Path $serviceDir "target") -Filter "*.jar" |
            Where-Object { $_.Name -notlike "*.original" } |
            Select-Object -First 1
        Assert-True $jar "$($service.Name) jar was not found"

        $stdout = Join-Path $logsDir "$($service.Name).out.log"
        $stderr = Join-Path $logsDir "$($service.Name).err.log"

        Write-Step "starting $($service.Name)"
        $javaArgs = @(
            "-Xms64m",
            "-Xmx192m",
            "-jar",
            $jar.FullName,
            "--spring.datasource.url=$DbUrl",
            "--spring.datasource.username=$DbUsername",
            "--spring.datasource.password=$DbPassword",
            "--spring.kafka.bootstrap-servers=$KafkaBootstrapServers",
            "--mailvault.storage.endpoint=$MinioEndpoint",
            "--mailvault.storage.access-key=$MinioAccessKey",
            "--mailvault.storage.secret-key=$MinioSecretKey",
            "--mailvault.storage.bucket=$MinioBucket",
            "--mailvault.search.base-url=$OpenSearchBaseUrl",
            "--spring.data.redis.host=$RedisHost",
            "--spring.data.redis.port=$RedisPort"
        )
        $process = Start-Process `
            -FilePath "java" `
            -ArgumentList $javaArgs `
            -WorkingDirectory $serviceDir `
            -RedirectStandardOutput $stdout `
            -RedirectStandardError $stderr `
            -PassThru `
            -WindowStyle Hidden
        $processes += $process
        Wait-Http $service.Health $service.Name $StartupTimeoutSeconds
    }

    $runId = [guid]::NewGuid().ToString("N").Substring(0, 8)
    $userId = "e2e-user-$runId"
    $recipient = "$userId@mailvault.local"
    $ccRecipient = "$userId-cc@mailvault.local"
    $bccRecipient = "$userId-bcc@mailvault.local"
    $subject = "MailVault E2E $runId"
    $textBody = "This is an end-to-end email for $runId"
    $htmlBody = "<p>This is an end-to-end email for $runId</p>"
    $attachmentText = "hello from MailVault e2e $runId"
    $attachmentBytes = [System.Text.Encoding]::UTF8.GetBytes($attachmentText)

    Write-Step "initiating attachment upload"
    $initiate = Invoke-Json "POST" "http://localhost:8081/attachments/initiate" @{
        userId = $userId
        filename = "e2e-$runId.txt"
        contentType = "text/plain"
        sizeBytes = $attachmentBytes.Length
    }
    Assert-True $initiate.attachmentId "attachmentId was not returned"
    Assert-True $initiate.uploadUrl "uploadUrl was not returned"

    Write-Step "uploading attachment to MinIO presigned URL"
    Invoke-WebRequest `
        -Method Put `
        -Uri $initiate.uploadUrl `
        -Body $attachmentBytes `
        -ContentType "text/plain" `
        -UseBasicParsing `
        -TimeoutSec 30 | Out-Null

    Write-Step "completing attachment upload"
    $complete = Invoke-Json "POST" "http://localhost:8081/attachments/$($initiate.attachmentId)/complete"
    Assert-True ($complete.status -eq "UPLOADED") "attachment completion did not return UPLOADED"

    Write-Step "importing email with uploaded attachment"
    $email = Invoke-Json "POST" "http://localhost:8081/emails/import" @{
        userId = $userId
        from = "billing@mailvault.local"
        to = @($recipient)
        cc = @($ccRecipient)
        bcc = @($bccRecipient)
        subject = $subject
        textBody = $textBody
        htmlBody = $htmlBody
        attachmentIds = @($initiate.attachmentId)
    }
    Assert-True $email.emailId "emailId was not returned"
    Assert-True ($email.status -eq "ACCEPTED") "email import did not return ACCEPTED"

    Write-Step "checking mailbox inbox"
    $inbox = Wait-Until "mailbox inbox contains imported email" 60 {
        $items = Invoke-Json "GET" "http://localhost:8082/mailboxes/$userId/inbox?limit=10"
        @($items) | Where-Object { $_.id -eq $email.emailId }
    }
    Assert-True ($inbox.attachmentCount -eq 1) "inbox attachment count was not 1"

    Write-Step "checking email detail and attachment worker result"
    $detail = Wait-Until "attachment is visible and processed" 60 {
        $current = Invoke-Json "GET" "http://localhost:8082/emails/$($email.emailId)?userId=$userId"
        $attachment = @($current.attachments) | Where-Object { $_.id -eq $initiate.attachmentId }
        if ($attachment -and $attachment.status -eq "READY") {
            return $current
        }
        return $null
    }
    Assert-True ($detail.subject -eq $subject) "email detail subject did not match"
    Assert-True ($detail.textBody -eq $textBody) "email detail text body did not match"
    Assert-True ($detail.htmlBody -eq $htmlBody) "email detail html body did not match"
    $ccMatches = @(@($detail.recipients) | Where-Object { $_.address -eq $ccRecipient -and $_.type -eq "CC" })
    $bccMatches = @(@($detail.recipients) | Where-Object { $_.address -eq $bccRecipient -and $_.type -eq "BCC" })
    Assert-True ($ccMatches.Count -eq 1) "email detail CC recipient was not returned"
    Assert-True ($bccMatches.Count -eq 1) "email detail BCC recipient was not returned"

    Write-Step "checking thread mailbox view"
    $thread = Wait-Until "thread list contains imported email" 60 {
        $threads = Invoke-Json "GET" "http://localhost:8082/mailboxes/$userId/threads?folder=INBOX&limit=10"
        @($threads) | Where-Object { $_.subject -eq $subject } | Select-Object -First 1
    }
    Assert-True ($thread.messageCount -eq 1) "thread message count was not 1"
    Assert-True ($thread.unreadCount -eq 1) "thread unread count was not 1"

    Write-Step "checking thread detail"
    $threadDetail = Invoke-Json "GET" "http://localhost:8082/threads/$($thread.id)?userId=$userId"
    $threadMessages = @($threadDetail.messages)
    Assert-True ($threadMessages.Count -eq 1) "thread detail message count was not 1"
    Assert-True ($threadMessages[0].emailId -eq $email.emailId) "thread detail email id did not match"
    Assert-True ($threadMessages[0].textBody -eq $textBody) "thread detail text body did not match"

    Write-Step "checking async quota usage"
    $quota = Wait-Until "quota usage updated from email.received" 60 {
        $current = Invoke-Json "GET" "http://localhost:8083/users/$userId/storage"
        if ($current.usedBytes -ge $email.logicalSizeBytes) {
            return $current
        }
        return $null
    }

    Write-Step "checking OpenSearch read model"
    $searchResult = Wait-Until "search returns imported email" 90 {
        $results = Invoke-Json "GET" "http://localhost:8085/emails/search?userId=$userId&q=$runId&limit=10"
        @($results) | Where-Object { $_.emailId -eq $email.emailId }
    }

    Write-Host ""
    Write-Host "MVP E2E PASS"
    Write-Host "  userId:        $userId"
    Write-Host "  emailId:       $($email.emailId)"
    Write-Host "  attachmentId:  $($initiate.attachmentId)"
    Write-Host "  quotaBytes:    $($quota.usedBytes)"
    Write-Host "  searchSubject: $($searchResult.subject)"
    Write-Host "  logs:          $logsDir"
} finally {
    if (-not $KeepServices.IsPresent) {
        foreach ($process in $processes) {
            $process.Refresh()
            if ($process -and -not $process.HasExited) {
                Write-Step "stopping process $($process.Id)"
                Stop-Process -Id $process.Id -Force -ErrorAction SilentlyContinue
            }
        }
    }
    foreach ($entry in $previousEnv.GetEnumerator()) {
        Set-EnvOrRemove $entry.Key $entry.Value
    }
}
