$cookieJar = New-Object Microsoft.PowerShell.Commands.WebRequestSession

# 1. Public Blogs
$resp = Invoke-WebRequest -Uri "http://localhost:8080/blogs" -WebSession $cookieJar -UseBasicParsing
[System.IO.File]::WriteAllText("d:\CRM Project\EducationCrmProject2\scratch\blogs_rendered.html", $resp.Content)

# 2. Login
$loginPage = Invoke-WebRequest -Uri "http://localhost:8080/login" -WebSession $cookieJar -UseBasicParsing
$csrfToken = ""
if ($loginPage.Content -match 'name="_csrf"\s+value="([^"]+)"') {
    $csrfToken = $matches[1]
}
$loginBody = @{
    email = "admin@edutake.com"
    password = "admin"
    _csrf = $csrfToken
}
$null = Invoke-WebRequest -Uri "http://localhost:8080/loginForm" -Method Post -Body $loginBody -WebSession $cookieJar -UseBasicParsing

# 3. Admin Blogs
$adminResp = Invoke-WebRequest -Uri "http://localhost:8080/admin/blogs" -WebSession $cookieJar -UseBasicParsing
[System.IO.File]::WriteAllText("d:\CRM Project\EducationCrmProject2\scratch\admin_blogs_rendered.html", $adminResp.Content)

Write-Host "Public blogs length: " $resp.Content.Length
Write-Host "Admin blogs length: " $adminResp.Content.Length
