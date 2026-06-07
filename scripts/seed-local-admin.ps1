param(
    [string]$ContainerName = "nova-food-postgres",
    [string]$Database = "nova_food",
    [string]$DbUser = "nova_food",
    [string]$AdminUsername = "admin"
)

$ErrorActionPreference = "Stop"

$passwordHash = '$2a$10$vgZrZcz62vxSbMR2Wo2SZeg9mMtdACeIIx1J3gDUPgM0qIsbNPDsa'
$sql = @"
INSERT INTO users (id, username, password_hash, role, created_at)
VALUES (
    '00000000-0000-0000-0000-000000000001',
    '$AdminUsername',
    '$passwordHash',
    'ADMIN',
    CURRENT_TIMESTAMP
)
ON CONFLICT (username) DO UPDATE
SET password_hash = EXCLUDED.password_hash,
    role = EXCLUDED.role;
"@

docker exec -i $ContainerName psql -U $DbUser -d $Database -c $sql | Out-Null

Write-Host "Seeded admin user '$AdminUsername' with password Password123! in container '$ContainerName'."
