$ErrorActionPreference = "Stop"
$session = New-Object Microsoft.PowerShell.Commands.WebRequestSession

# Login
$loginPage = Invoke-WebRequest -Uri "http://localhost:8080/login" -SessionVariable session -UseBasicParsing
$csrfToken = ""
if ($loginPage.Content -match 'name="_csrf"\s+value="([^"]+)"') { $csrfToken = $matches[1] }

$loginBody = @{
    email = "admin@edutake.com"
    password = "admin123"
    _csrf = $csrfToken
}
$null = Invoke-WebRequest -Uri "http://localhost:8080/loginForm" -Method Post -Body $loginBody -WebSession $session -MaximumRedirection 5 -UseBasicParsing

$resp = Invoke-WebRequest -Uri "http://localhost:8080/admin/blogs" -WebSession $session -UseBasicParsing
Write-Host "Response length: " $resp.Content.Length
Write-Host "First 500 chars: `n" $resp.Content.Substring(0, [Math]::Min(500, $resp.Content.Length))

[System.IO.File]::WriteAllText("d:\CRM Project\EducationCrmProject2\scratch\admin_blogs_debug.html", $resp.Content)
