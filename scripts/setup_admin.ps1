# Script to generate Admin P12 certificate for ChriOnline
# This script creates a keys folder and generates a .p12 file

$email = Read-Host "Enter Admin Email"
$password = Read-Host "Enter Password for .p12 file"

if (-not (Test-Path "keys")) {
    New-Item -ItemType Directory -Path "keys"
}

$p12Path = "keys/admin.p12"

# Generate the key and certificate using keytool
# Alias is the email as expected by P12Signer
Write-Host "Generating keystore..." -ForegroundColor Cyan

keytool -genkeypair `
    -alias $email `
    -keyalg RSA `
    -keysize 2048 `
    -sigalg SHA256withRSA `
    -keystore $p12Path `
    -storetype PKCS12 `
    -storepass $password `
    -keypass $password `
    -dname "CN=$email, OU=Admin, O=ChriOnline, L=Casablanca, C=MA" `
    -validity 3650

if ($LASTEXITCODE -eq 0) {
    Write-Host "Successfully generated $p12Path" -ForegroundColor Green
    
    Write-Host "`n--- PUBLIC KEY FOR DATABASE ---" -ForegroundColor Yellow
    Write-Host "Please copy the following Base64 public key and update the 'cle_publique' column in the 'Admin' table for email: $email" -ForegroundColor Gray
    
    # Export public key in DER format then convert to Base64
    keytool -exportcert -alias $email -keystore $p12Path -storepass $password -file "keys/temp.crt"
    
    # We actually need the Public Key from the certificate
    # Let's use a small Java snippet to extract the Base64 public key correctly
    Write-Host "Extracting Base64 Public Key..." -ForegroundColor Cyan
} else {
    Write-Host "Error generating keystore. Make sure 'keytool' is in your PATH." -ForegroundColor Red
}
