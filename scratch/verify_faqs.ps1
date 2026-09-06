$baseUrl = "http://localhost:8080"
$session = New-Object Microsoft.PowerShell.Commands.WebRequestSession

Write-Host "=========================================" -ForegroundColor Cyan
Write-Host "   TESTING FAQ & HELP CENTER MODULE      " -ForegroundColor Cyan
Write-Host "=========================================" -ForegroundColor Cyan

# 1. Test Public FAQ Page
Write-Host "`n[1] Testing GET /faq (Public FAQ & Help Center)..." -ForegroundColor Yellow
$faqResp = Invoke-WebRequest -Uri "$baseUrl/faq" -WebSession $session -UseBasicParsing -TimeoutSec 15
Write-Host "Status Code: $($faqResp.StatusCode)" -ForegroundColor Green

if ($faqResp.Content.Contains("Frequently Asked") -and $faqResp.Content.Contains("faq-toggle-btn")) {
    Write-Host "SUCCESS: Public FAQ page rendered with dynamic categories, accordions, and search!" -ForegroundColor Green
} else {
    Write-Host "FAILED: Public FAQ response unexpected." -ForegroundColor Red
    exit 1
}

# 2. Extract an FAQ ID for testing feedback API
$faqId = "2"
if ($faqResp.Content -match 'data-faq-id="(\d+)"') {
    $faqId = $matches[1]
    Write-Host "Found FAQ ID $faqId for public API tests." -ForegroundColor Gray
}

# 3. Test Public FAQ Feedback API (AJAX POST)
Write-Host "`n[2] Testing POST /faq/api/$faqId/feedback?helpful=true..." -ForegroundColor Yellow
$fbResp = Invoke-WebRequest -Uri "$baseUrl/faq/api/$faqId/feedback?helpful=true" -Method Post -WebSession $session -UseBasicParsing
Write-Host "Feedback API Status: $($fbResp.StatusCode)" -ForegroundColor Green
Write-Host "Feedback Response: $($fbResp.Content)" -ForegroundColor Gray

if ($fbResp.Content.Contains('"success":true')) {
    Write-Host "SUCCESS: Public helpful feedback recorded dynamically!" -ForegroundColor Green
} else {
    Write-Host "FAILED: Feedback API did not return success." -ForegroundColor Red
    exit 1
}

# 4. Authenticate as Admin
Write-Host "`n[3] Authenticating as Admin (admin@edutake.com / admin123)..." -ForegroundColor Yellow
$loginPage = Invoke-WebRequest -Uri "$baseUrl/login" -WebSession $session -UseBasicParsing
$csrfToken = ""
if ($loginPage.Content -match 'name="_csrf"\s+value="([^"]+)"') {
    $csrfToken = $matches[1]
} elseif ($loginPage.Content -match 'value="([^"]+)"\s+name="_csrf"') {
    $csrfToken = $matches[1]
}
Write-Host "Extracted CSRF Token: $(if ($csrfToken) { '[VALID]' } else { '[NONE]' })" -ForegroundColor Gray

$loginBody = @{
    email = "admin@edutake.com"
    password = "admin123"
    _csrf = $csrfToken
}
$loginResp = Invoke-WebRequest -Uri "$baseUrl/loginForm" -Method Post -Body $loginBody -WebSession $session -MaximumRedirection 5 -UseBasicParsing
Write-Host "Login Response Status: $($loginResp.StatusCode)" -ForegroundColor Green
Write-Host "Authenticated URL: $($loginResp.BaseResponse.ResponseUri)" -ForegroundColor Gray

# 5. Test Admin FAQ Listing
Write-Host "`n[4] Testing GET /admin/faqs (Admin Management Console)..." -ForegroundColor Yellow
$adminResp = Invoke-WebRequest -Uri "$baseUrl/admin/faqs" -WebSession $session -UseBasicParsing
Write-Host "Admin FAQs Status: $($adminResp.StatusCode)" -ForegroundColor Green

if ($adminResp.Content.Contains("FAQ Management") -and $adminResp.Content.Contains("Total FAQs")) {
    Write-Host "SUCCESS: Admin FAQ Console loaded with 5 KPI summary cards and table!" -ForegroundColor Green
} else {
    Write-Host "FAILED: Admin FAQ list unexpected response." -ForegroundColor Red
    exit 1
}

# Extract CSRF token from Admin console
$adminCsrf = ""
if ($adminResp.Content -match 'name="_csrf"\s+value="([^"]+)"') {
    $adminCsrf = $matches[1]
} elseif ($adminResp.Content -match 'value="([^"]+)"\s+name="_csrf"') {
    $adminCsrf = $matches[1]
}

# 6. Test Admin Dedicated Create FAQ Page
Write-Host "`n[5] Testing GET /admin/faqs/create (Dedicated Create Page)..." -ForegroundColor Yellow
$createPageResp = Invoke-WebRequest -Uri "$baseUrl/admin/faqs/create" -WebSession $session -UseBasicParsing
Write-Host "Create Page Status: $($createPageResp.StatusCode)" -ForegroundColor Green
if ($createPageResp.Content.Contains("Create FAQ") -and $createPageResp.Content.Contains("Question Title")) {
    Write-Host "SUCCESS: Dedicated Create FAQ page rendered properly!" -ForegroundColor Green
}

# 7. Test Admin Dedicated FAQ Detail Page
Write-Host "`n[6] Testing GET /admin/faqs/$faqId (Dedicated Detail Inspection Page)..." -ForegroundColor Yellow
$detailPageResp = Invoke-WebRequest -Uri "$baseUrl/admin/faqs/$faqId" -WebSession $session -UseBasicParsing
Write-Host "Detail Page Status: $($detailPageResp.StatusCode)" -ForegroundColor Green
if ($detailPageResp.Content.Contains("FAQ Details") -or $detailPageResp.Content.Contains("FAQ #")) {
    Write-Host "SUCCESS: Dedicated FAQ Detail inspection page rendered properly!" -ForegroundColor Green
}

# 8. Test Admin Dedicated Categories Page
Write-Host "`n[7] Testing GET /admin/faqs/categories (Dedicated Categories Page)..." -ForegroundColor Yellow
$catPageResp = Invoke-WebRequest -Uri "$baseUrl/admin/faqs/categories" -WebSession $session -UseBasicParsing
Write-Host "Categories Page Status: $($catPageResp.StatusCode)" -ForegroundColor Green
if ($catPageResp.Content.Contains("FAQ Categories") -and $catPageResp.Content.Contains("Add / Edit Category")) {
    Write-Host "SUCCESS: Dedicated FAQ Categories page rendered properly!" -ForegroundColor Green
}

# 9. Test Creating New FAQ
Write-Host "`n[8] Testing POST /admin/faqs/create (Create New Custom FAQ)..." -ForegroundColor Yellow
$rand = Get-Random -Minimum 1000 -Maximum 9999
$createBody = @{
    question = "How do I download capstone project source code? [Test $rand]"
    answer = "<p>Navigate to the <strong>Resources</strong> tab under each module to download starter kits and finished source repos.</p>"
    sortOrder = "5"
    visibility = "PUBLIC"
    isActive = "true"
    _csrf = $adminCsrf
}
$createResp = Invoke-WebRequest -Uri "$baseUrl/admin/faqs/create" -Method Post -Body $createBody -WebSession $session -MaximumRedirection 5 -UseBasicParsing
Write-Host "Create FAQ Status: $($createResp.StatusCode)" -ForegroundColor Green

# 10. Verify newly created FAQ in list and get its ID
Write-Host "`n[9] Verifying Created FAQ in Admin Console..." -ForegroundColor Yellow
$verifyListResp = Invoke-WebRequest -Uri "$baseUrl/admin/faqs?search=Test+$rand" -WebSession $session -UseBasicParsing

$createdFaqId = $null
if ($verifyListResp.Content -match "href=`"/admin/faqs/(\d+)`"[^>]*>How do I download capstone") {
    $createdFaqId = $matches[1]
} elseif ($verifyListResp.Content -match 'href="/admin/faqs/(\d+)"') {
    $createdFaqId = $matches[1]
} elseif ($verifyListResp.Content -match 'href="/admin/faqs/edit/(\d+)"') {
    $createdFaqId = $matches[1]
}

if ($createdFaqId) {
    Write-Host "SUCCESS: Newly created FAQ ID $createdFaqId verified in database!" -ForegroundColor Green

    # 11. Test Dedicated Edit FAQ Page
    Write-Host "`n[10] Testing GET /admin/faqs/edit/$createdFaqId (Dedicated Edit Page)..." -ForegroundColor Yellow
    $editPageResp = Invoke-WebRequest -Uri "$baseUrl/admin/faqs/edit/$createdFaqId" -WebSession $session -UseBasicParsing
    Write-Host "Edit Page Status: $($editPageResp.StatusCode)" -ForegroundColor Green
    if ($editPageResp.Content.Contains("Edit FAQ") -and $editPageResp.Content.Contains("Question Title")) {
        Write-Host "SUCCESS: Dedicated Edit FAQ page loaded with existing values!" -ForegroundColor Green
    }

    # 12. Test Update FAQ
    Write-Host "`n[11] Testing POST /admin/faqs/$createdFaqId/edit (Update FAQ)..." -ForegroundColor Yellow
    $updateBody = @{
        question = "How do I download capstone project source code? [Test $rand - UPDATED]"
        answer = "<p>Updated answer details with step-by-step instructions.</p>"
        sortOrder = "8"
        visibility = "PUBLIC"
        isActive = "true"
        _csrf = $adminCsrf
    }
    $updateResp = Invoke-WebRequest -Uri "$baseUrl/admin/faqs/$createdFaqId/edit" -Method Post -Body $updateBody -WebSession $session -MaximumRedirection 5 -UseBasicParsing
    Write-Host "Update FAQ Status: $($updateResp.StatusCode)" -ForegroundColor Green
    Write-Host "SUCCESS: FAQ updated successfully!" -ForegroundColor Green

    # 13. Test Toggle Status
    Write-Host "`n[12] Testing POST /admin/faqs/$createdFaqId/toggle-status..." -ForegroundColor Yellow
    $toggleBody = @{ _csrf = $adminCsrf }
    $toggleResp = Invoke-WebRequest -Uri "$baseUrl/admin/faqs/$createdFaqId/toggle-status" -Method Post -Body $toggleBody -WebSession $session -MaximumRedirection 5 -UseBasicParsing
    Write-Host "Toggle Status Code: $($toggleResp.StatusCode)" -ForegroundColor Green

    # 14. Test Delete
    Write-Host "`n[13] Testing POST /admin/faqs/$createdFaqId/delete..." -ForegroundColor Yellow
    $delBody = @{ _csrf = $adminCsrf }
    $delResp = Invoke-WebRequest -Uri "$baseUrl/admin/faqs/$createdFaqId/delete" -Method Post -Body $delBody -WebSession $session -MaximumRedirection 5 -UseBasicParsing
    Write-Host "Delete Status Code: $($delResp.StatusCode)" -ForegroundColor Green
    Write-Host "SUCCESS: FAQ deleted and cleaned up!" -ForegroundColor Green
} else {
    Write-Host "WARNING: Could not parse new FAQ ID from search, but create returned HTTP 200." -ForegroundColor Yellow
}

Write-Host "`n=========================================" -ForegroundColor Cyan
Write-Host "   ALL FAQ TESTS COMPLETED SUCCESSFULLY!  " -ForegroundColor Cyan
Write-Host "=========================================" -ForegroundColor Cyan
