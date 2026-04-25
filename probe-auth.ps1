param(
  [string]$BaseUrl = "http://localhost:8080",
  [string]$Username = "admin",
  [string]$Password = "admin123"
)

$body = @{ username = $Username; password = $Password } | ConvertTo-Json
$login = Invoke-RestMethod -Method Post -Uri ($BaseUrl + "/auth/login") -ContentType "application/json" -Body $body
$token = $login.token

Write-Host ("role=" + $login.role)

$headers = @{ Authorization = ("Bearer " + $token) }
$urls = @(
  "/packages/my",
  "/wallet/me",
  "/deliveries/reward/pending",
  "/deliveries/pending",
  "/users",
  "/auth/me"
)

foreach ($u in $urls) {
  try {
    Invoke-RestMethod -Method Get -Uri ($BaseUrl + $u) -Headers $headers | Out-Null
    Write-Host ($u + " OK")
  } catch {
    $resp = $_.Exception.Response
    if ($resp) {
      Write-Host ($u + " FAIL " + [int]$resp.StatusCode)
    } else {
      Write-Host ($u + " FAIL no-response")
    }
  }
}

