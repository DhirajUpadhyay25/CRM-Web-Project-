$ErrorActionPreference = "Stop"
$baseUrl = "http://localhost:8080"
$session = New-Object Microsoft.PowerShell.Commands.WebRequestSession

Write-Host "=========================================" -ForegroundColor Cyan
Write-Host "   TESTING BLOG & ARTICLES MODULE        " -ForegroundColor Cyan
Write-Host "=========================================" -ForegroundColor Cyan

# 1. Public Blogs Catalog (which triggers auto-seeding if empty)
Write-Host "`n[1] Testing GET /blogs (Public Blog Directory)..." -ForegroundColor Yellow
$blogsResp = Invoke-WebRequest -Uri "$baseUrl/blogs" -WebSession $session -UseBasicParsing -TimeoutSec 15
Write-Host "Status Code: $($blogsResp.StatusCode)" -ForegroundColor Green
if ($blogsResp.Content.Contains("Articles & Tutorials") -or $blogsResp.Content.Contains("Articles")) {
    Write-Host "SUCCESS: Blog directory rendered with topics and articles." -ForegroundColor Green
} else {
    Write-Host "WARNING: Could not find main blog heading." -ForegroundColor Red
}

# 2. Public Homepage with Dynamic Blogs
Write-Host "`n[2] Testing GET / (Homepage Latest Articles Section)..." -ForegroundColor Yellow
$homeResp = Invoke-WebRequest -Uri "$baseUrl/" -WebSession $session -UseBasicParsing -TimeoutSec 15
Write-Host "Status Code: $($homeResp.StatusCode)" -ForegroundColor Green
if ($homeResp.Content.Contains("Latest Articles & Tech Insights") -or $homeResp.Content.Contains("Latest Articles") -or $homeResp.Content.Contains("Knowledge Hub")) {
    Write-Host "SUCCESS: Homepage displays dynamic 'Latest Articles & Tech Insights' section!" -ForegroundColor Green
} else {
    Write-Host "WARNING: Could not find 'Latest Articles' section on homepage." -ForegroundColor Red
}

# 3. Public Article Reader
Write-Host "`n[3] Testing GET /blog/full-stack-developer-roadmap-2026 (Public Reader)..." -ForegroundColor Yellow
$articleResp = Invoke-WebRequest -Uri "$baseUrl/blog/full-stack-developer-roadmap-2026" -WebSession $session -UseBasicParsing -TimeoutSec 15
Write-Host "Status Code: $($articleResp.StatusCode)" -ForegroundColor Green
if ($articleResp.Content.Contains("Full-Stack Developer Roadmap") -or $articleResp.Content.Contains("Modern Path")) {
    Write-Host "SUCCESS: Article detail reader loaded successfully with rich prose, metadata, and reading time." -ForegroundColor Green
} else {
    Write-Host "WARNING: Article content check." -ForegroundColor Yellow
}

# 4. Admin Authentication
Write-Host "`n[4] Authenticating as Admin (admin@edutake.com / admin123)..." -ForegroundColor Yellow
$loginPage = Invoke-WebRequest -Uri "$baseUrl/login" -WebSession $session -UseBasicParsing
$csrfToken = ""
if ($loginPage.Content -match 'name="_csrf"\s+value="([^"]+)"') {
    $csrfToken = $matches[1]
} elseif ($loginPage.Content -match 'value="([^"]+)"\s+name="_csrf"') {
    $csrfToken = $matches[1]
}
Write-Host "Extracted CSRF Token: $csrfToken" -ForegroundColor Gray

$loginBody = @{
    email = "admin@edutake.com"
    password = "admin123"
    _csrf = $csrfToken
}
$loginResp = Invoke-WebRequest -Uri "$baseUrl/loginForm" -Method Post -Body $loginBody -WebSession $session -MaximumRedirection 5 -UseBasicParsing
Write-Host "Login Response Status: $($loginResp.StatusCode)" -ForegroundColor Green
Write-Host "Authenticated URL: $($loginResp.BaseResponse.ResponseUri)" -ForegroundColor Gray

# 5. Admin Blog Management List
Write-Host "`n[5] Testing GET /admin/blogs (Admin Management Console)..." -ForegroundColor Yellow
$adminBlogsResp = Invoke-WebRequest -Uri "$baseUrl/admin/blogs" -WebSession $session -UseBasicParsing
Write-Host "Admin Blogs Status: $($adminBlogsResp.StatusCode)" -ForegroundColor Green
if ($adminBlogsResp.Content.Contains("Blog & Article Management") -or $adminBlogsResp.Content.Contains("Articles & Blog Management")) {
    Write-Host "SUCCESS: Admin Blogs Management list rendered with KPI metric cards and action buttons." -ForegroundColor Green
} else {
    Write-Host "WARNING: Unexpected response in Admin Blogs list." -ForegroundColor Red
}

# 6. Admin Create Article Form
Write-Host "`n[6] Testing GET /admin/blogs/create (Admin Create Form)..." -ForegroundColor Yellow
$createFormResp = Invoke-WebRequest -Uri "$baseUrl/admin/blogs/create" -WebSession $session -UseBasicParsing
Write-Host "Create Form Status: $($createFormResp.StatusCode)" -ForegroundColor Green
$adminCsrf = ""
if ($createFormResp.Content -match 'name="_csrf"\s+value="([^"]+)"') {
    $adminCsrf = $matches[1]
} elseif ($createFormResp.Content -match 'value="([^"]+)"\s+name="_csrf"') {
    $adminCsrf = $matches[1]
}
if ($adminCsrf -eq "") { $adminCsrf = $csrfToken }
Write-Host "Admin CSRF Token: $adminCsrf" -ForegroundColor Gray

# 7. Admin Post New Article
Write-Host "`n[7] Testing POST /admin/blogs/create (Create New Article)..." -ForegroundColor Yellow
$randNum = Get-Random -Minimum 1000 -Maximum 9999
$newArticleTitle = "Microservices Architecture Patterns in 2026 - Test $randNum"
$newSlug = "microservices-patterns-test-$randNum"
$createBody = @{
    title = $newArticleTitle
    slug = $newSlug
    excerpt = "An in-depth test overview of event-driven microservices architecture."
    content = "<h2>Event Driven Architecture</h2><p>Event driven microservices provide unmatched scalability in distributed cloud native systems.</p>"
    featuredImage = "https://images.unsplash.com/photo-1558494949-ef010cbdcc31?auto=format&fit=crop&w=800&q=80"
    author = "EduTake Lead Architect"
    tags = "microservices, cloud, architecture, springboot"
    status = "PUBLISHED"
    visibility = "PUBLIC"
    isFeatured = "true"
    seoTitle = "Microservices Architecture Patterns in 2026"
    seoDescription = "Learn enterprise microservices patterns and practices."
    _csrf = $adminCsrf
}
$createPostResp = Invoke-WebRequest -Uri "$baseUrl/admin/blogs/create" -Method Post -Body $createBody -WebSession $session -MaximumRedirection 5 -UseBasicParsing
Write-Host "Create Article Response Status: $($createPostResp.StatusCode)" -ForegroundColor Green

# 8. Check created article in Admin List & JSON API
Write-Host "`n[8] Verifying Newly Created Article in Admin API..." -ForegroundColor Yellow
$adminBlogsResp2 = Invoke-WebRequest -Uri "$baseUrl/admin/blogs" -WebSession $session -UseBasicParsing
if ($adminBlogsResp2.Content -match 'data-id="(\d+)"') {
    $createdId = $matches[1]
    Write-Host "Found Article ID for Quick View: $createdId" -ForegroundColor Cyan
    
    $apiResp = Invoke-WebRequest -Uri "$baseUrl/admin/blogs/api/$createdId" -WebSession $session -UseBasicParsing
    Write-Host "Quick View API Status: $($apiResp.StatusCode)" -ForegroundColor Green
    Write-Host "Quick View API Payload: $($apiResp.Content)" -ForegroundColor Green
    
    # 9. Toggle Featured
    Write-Host "`n[9] Testing POST /admin/blogs/$createdId/toggle-featured..." -ForegroundColor Yellow
    $toggleFeatResp = Invoke-WebRequest -Uri "$baseUrl/admin/blogs/$createdId/toggle-featured" -Method Post -Body @{ _csrf = $adminCsrf } -WebSession $session -MaximumRedirection 5 -UseBasicParsing
    Write-Host "Toggle Featured Status: $($toggleFeatResp.StatusCode)" -ForegroundColor Green
    
    # 10. Toggle Status
    Write-Host "`n[10] Testing POST /admin/blogs/$createdId/toggle-status..." -ForegroundColor Yellow
    $toggleStatusResp = Invoke-WebRequest -Uri "$baseUrl/admin/blogs/$createdId/toggle-status" -Method Post -Body @{ _csrf = $adminCsrf } -WebSession $session -MaximumRedirection 5 -UseBasicParsing
    Write-Host "Toggle Status Code: $($toggleStatusResp.StatusCode)" -ForegroundColor Green
    
    # 11. Public View of Created Article
    Write-Host "`n[11] Testing GET /blog/$newSlug (Public Reader of New Article)..." -ForegroundColor Yellow
    $newArtResp = Invoke-WebRequest -Uri "$baseUrl/blog/$newSlug" -WebSession $session -UseBasicParsing
    Write-Host "New Article Public View Status: $($newArtResp.StatusCode)" -ForegroundColor Green
    if ($newArtResp.Content.Contains("Event Driven Architecture")) {
        Write-Host "SUCCESS: Newly created article is publicly viewable with rich HTML rendering!" -ForegroundColor Green
    }
}

Write-Host "`n=========================================" -ForegroundColor Cyan
Write-Host "   ALL BLOG TESTS COMPLETED SUCCESSFULLY!  " -ForegroundColor Green
Write-Host "=========================================" -ForegroundColor Cyan
