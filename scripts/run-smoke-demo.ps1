param(
    [string]$BaseUrl = "http://localhost:8080/api/v1",
    [string]$AdminUsername = "admin",
    [string]$Password = "Password123!",
    [int]$OutboxWaitSeconds = 6
)

$ErrorActionPreference = "Stop"

function Invoke-Api {
    param(
        [string]$Method,
        [string]$Url,
        [object]$Body = $null,
        [hashtable]$Headers = @{}
    )

    $params = @{
        Method      = $Method
        Uri         = $Url
        Headers     = $Headers
        ErrorAction = "Stop"
    }

    if ($null -ne $Body) {
        $params["ContentType"] = "application/json"
        $params["Body"] = ($Body | ConvertTo-Json -Depth 10)
    }

    try {
        $response = Invoke-RestMethod @params
        return [pscustomobject]@{
            ok   = $true
            body = $response
        }
    }
    catch {
        $detail = if ($_.ErrorDetails -and $_.ErrorDetails.Message) {
            $_.ErrorDetails.Message
        }
        else {
            $_.Exception.Message
        }
        return [pscustomobject]@{
            ok   = $false
            body = $detail
        }
    }
}

function Assert-Step {
    param(
        [string]$Name,
        [object]$Result
    )

    if (-not $Result.ok) {
        throw "$Name failed: $($Result.body)"
    }

    return $Result.body
}

$suffix = Get-Date -Format "yyyyMMddHHmmss"
$ownerUsername = "smoke_owner_$suffix"
$customerUsername = "smoke_customer_$suffix"
$driverUsername = "smoke_driver_$suffix"
$summary = [ordered]@{}

$adminLogin = Assert-Step "admin login" (Invoke-Api -Method Post -Url "$BaseUrl/auth/login" -Body @{
        username = $AdminUsername
        password = $Password
    })
$adminToken = $adminLogin.data.accessToken
$summary.adminLogin = "ok"

$ownerRegister = Assert-Step "owner register" (Invoke-Api -Method Post -Url "$BaseUrl/auth/register" -Body @{
        username = $ownerUsername
        password = $Password
        role     = "RESTAURANT_OWNER"
    })
$ownerToken = $ownerRegister.data.accessToken
$summary.ownerUsername = $ownerUsername

$customerRegister = Assert-Step "customer register" (Invoke-Api -Method Post -Url "$BaseUrl/auth/register" -Body @{
        username = $customerUsername
        password = $Password
        role     = "CUSTOMER"
    })
$customerToken = $customerRegister.data.accessToken
$summary.customerUsername = $customerUsername

$driverCreate = Assert-Step "driver create" (Invoke-Api -Method Post -Url "$BaseUrl/users" -Headers @{
        Authorization = "Bearer $adminToken"
    } -Body @{
        username = $driverUsername
        password = $Password
        role     = "DRIVER"
    })
$driverId = $driverCreate.data.id
$summary.driverUsername = $driverUsername
$summary.driverId = $driverId

$driverLogin = Assert-Step "driver login" (Invoke-Api -Method Post -Url "$BaseUrl/auth/login" -Body @{
        username = $driverUsername
        password = $Password
    })
$driverToken = $driverLogin.data.accessToken

$restaurantCreate = Assert-Step "restaurant create" (Invoke-Api -Method Post -Url "$BaseUrl/restaurants" -Headers @{
        Authorization = "Bearer $ownerToken"
    } -Body @{
        name        = "Smoke Pizza $suffix"
        description = "Smoke demo restaurant"
        address     = "1 Demo Street"
        phone       = "0900000000"
    })
$restaurantId = $restaurantCreate.data.id
$summary.restaurantId = $restaurantId

$restaurantSearch = Assert-Step "restaurant search" (Invoke-Api -Method Get -Url "$BaseUrl/restaurants/search?keyword=Smoke&page=0&size=10")
$summary.restaurantSearchCount = $restaurantSearch.data.totalElements

$menuCreate = Assert-Step "menu create" (Invoke-Api -Method Post -Url "$BaseUrl/restaurants/$restaurantId/menu-items" -Headers @{
        Authorization = "Bearer $ownerToken"
    } -Body @{
        name              = "Smoke Burger $suffix"
        description       = "Inventory controlled demo item"
        price             = 120000.00
        available         = $true
        trackStock        = $true
        stockQuantity     = 20
        lowStockThreshold = 3
    })
$menuItemId = $menuCreate.data.id
$summary.menuItemId = $menuItemId

$menuSearch = Assert-Step "menu search" (Invoke-Api -Method Get -Url "$BaseUrl/menu-items/search?restaurantId=$restaurantId&available=true&keyword=Smoke&minPrice=100000&maxPrice=150000&page=0&size=10")
$summary.menuSearchCount = $menuSearch.data.totalElements

$orderKey = "order-smoke-$suffix"
$orderBody = @{
    items = @(
        @{
            menuItemId = $menuItemId
            quantity   = 2
        }
    )
}
$orderCreate = Assert-Step "order create" (Invoke-Api -Method Post -Url "$BaseUrl/orders" -Headers @{
        Authorization   = "Bearer $customerToken"
        "Idempotency-Key" = $orderKey
    } -Body $orderBody)
$orderRepeat = Assert-Step "order repeat" (Invoke-Api -Method Post -Url "$BaseUrl/orders" -Headers @{
        Authorization   = "Bearer $customerToken"
        "Idempotency-Key" = $orderKey
    } -Body $orderBody)
$orderId = $orderCreate.data.id
$trackingId = $orderCreate.data.trackingId
$summary.orderId = $orderId
$summary.trackingId = $trackingId
$summary.orderIdempotent = ($orderCreate.data.id -eq $orderRepeat.data.id)

$paymentKey = "payment-smoke-$suffix"
$paymentBody = @{
    approved = $true
}
$paymentCreate = Assert-Step "payment create" (Invoke-Api -Method Post -Url "$BaseUrl/orders/$orderId/payments/mock" -Headers @{
        Authorization   = "Bearer $customerToken"
        "Idempotency-Key" = $paymentKey
    } -Body $paymentBody)
$paymentRepeat = Assert-Step "payment repeat" (Invoke-Api -Method Post -Url "$BaseUrl/orders/$orderId/payments/mock" -Headers @{
        Authorization   = "Bearer $customerToken"
        "Idempotency-Key" = $paymentKey
    } -Body $paymentBody)
$summary.paymentIdempotent = ($paymentCreate.data.id -eq $paymentRepeat.data.id)
$summary.paymentStatus = $paymentCreate.data.status

$ownerOrders = Assert-Step "owner restaurant orders" (Invoke-Api -Method Get -Url "${BaseUrl}/orders/restaurants/${restaurantId}?page=0&size=20" -Headers @{
        Authorization = "Bearer $ownerToken"
    })
$summary.ownerOrderCount = $ownerOrders.data.totalElements

Assert-Step "order confirm" (Invoke-Api -Method Patch -Url "$BaseUrl/orders/$orderId/confirm" -Headers @{
        Authorization = "Bearer $ownerToken"
    }) | Out-Null
Assert-Step "order preparing" (Invoke-Api -Method Patch -Url "$BaseUrl/orders/$orderId/preparing" -Headers @{
        Authorization = "Bearer $ownerToken"
    }) | Out-Null
Assert-Step "order ready for delivery" (Invoke-Api -Method Patch -Url "$BaseUrl/orders/$orderId/ready-for-delivery" -Headers @{
        Authorization = "Bearer $ownerToken"
    }) | Out-Null

$assignKey = "delivery-smoke-$suffix"
$deliveryAssign = Assert-Step "delivery assign" (Invoke-Api -Method Post -Url "$BaseUrl/orders/$orderId/deliveries/assign" -Headers @{
        Authorization   = "Bearer $adminToken"
        "Idempotency-Key" = $assignKey
    } -Body @{
        driverId = $driverId
    })
$deliveryAssignRepeat = Assert-Step "delivery assign repeat" (Invoke-Api -Method Post -Url "$BaseUrl/orders/$orderId/deliveries/assign" -Headers @{
        Authorization   = "Bearer $adminToken"
        "Idempotency-Key" = $assignKey
    } -Body @{
        driverId = $driverId
    })
$deliveryId = $deliveryAssign.data.id
$summary.deliveryId = $deliveryId
$summary.deliveryAssignmentIdempotent = ($deliveryAssign.data.id -eq $deliveryAssignRepeat.data.id)

$driverDeliveries = Assert-Step "driver deliveries" (Invoke-Api -Method Get -Url "$BaseUrl/deliveries/my?page=0&size=20" -Headers @{
        Authorization = "Bearer $driverToken"
    })
$summary.driverDeliveryCount = $driverDeliveries.data.totalElements

Assert-Step "delivery start" (Invoke-Api -Method Patch -Url "$BaseUrl/deliveries/$deliveryId/start" -Headers @{
        Authorization = "Bearer $driverToken"
    }) | Out-Null
Assert-Step "delivery complete" (Invoke-Api -Method Patch -Url "$BaseUrl/deliveries/$deliveryId/complete" -Headers @{
        Authorization = "Bearer $driverToken"
    }) | Out-Null

$orderTracking = Assert-Step "order tracking" (Invoke-Api -Method Get -Url "$BaseUrl/orders/tracking/$trackingId" -Headers @{
        Authorization = "Bearer $customerToken"
    })
$summary.orderFinalStatus = $orderTracking.data.status

$statusHistory = Assert-Step "status history" (Invoke-Api -Method Get -Url "$BaseUrl/orders/$orderId/status-history" -Headers @{
        Authorization = "Bearer $customerToken"
    })
$summary.statusHistoryCount = @($statusHistory.data).Count

$orderPayments = Assert-Step "order payments" (Invoke-Api -Method Get -Url "$BaseUrl/orders/$orderId/payments?page=0&size=20" -Headers @{
        Authorization = "Bearer $customerToken"
    })
$summary.paymentListCount = $orderPayments.data.totalElements

$reportRevenue = Assert-Step "admin revenue report" (Invoke-Api -Method Get -Url "$BaseUrl/admin/reports/revenue?fromDate=2026-01-01T00:00:00Z&toDate=2026-12-31T23:59:59Z" -Headers @{
        Authorization = "Bearer $adminToken"
    })
$summary.completedOrderCount = $reportRevenue.data.completedOrderCount
$summary.paidAmountTotal = $reportRevenue.data.paidAmountTotal

$outboxSummaryBeforeWait = Assert-Step "outbox summary before wait" (Invoke-Api -Method Get -Url "$BaseUrl/admin/outbox/summary" -Headers @{
        Authorization = "Bearer $adminToken"
    })
$summary.outboxPendingBeforeWait = $outboxSummaryBeforeWait.data.pendingOutboxCount
$summary.outboxFailedBeforeWait = $outboxSummaryBeforeWait.data.failedOutboxCount

Start-Sleep -Seconds $OutboxWaitSeconds

$outboxSummaryAfterWait = Assert-Step "outbox summary after wait" (Invoke-Api -Method Get -Url "$BaseUrl/admin/outbox/summary" -Headers @{
        Authorization = "Bearer $adminToken"
    })
$summary.outboxPendingAfterWait = $outboxSummaryAfterWait.data.pendingOutboxCount
$summary.outboxFailedAfterWait = $outboxSummaryAfterWait.data.failedOutboxCount

$outboxFailed = Assert-Step "outbox failed list" (Invoke-Api -Method Get -Url "$BaseUrl/admin/outbox/failed" -Headers @{
        Authorization = "Bearer $adminToken"
    })
$summary.outboxFailedListCount = @($outboxFailed.data).Count
$summary.completedAt = (Get-Date).ToString("o")

$summary | ConvertTo-Json -Depth 10
