# PowerShell script to resize logo using .NET
Add-Type -AssemblyName System.Drawing

$inputFile = "logo (3).png"
$sizes = @(
    @{folder="android/app/src/main/res/mipmap-mdpi"; size=48}
    @{folder="android/app/src/main/res/mipmap-hdpi"; size=72}
    @{folder="android/app/src/main/res/mipmap-xhdpi"; size=96}
    @{folder="android/app/src/main/res/mipmap-xxhdpi"; size=144}
    @{folder="android/app/src/main/res/mipmap-xxxhdpi"; size=192}
)

Write-Host "🎨 1STQR Logo Resize Başlıyor..."

# Load original image
$originalImage = [System.Drawing.Image]::FromFile((Resolve-Path $inputFile))

foreach ($item in $sizes) {
    $folder = $item.folder
    $size = $item.size
    
    # Create directory if not exists
    if (!(Test-Path $folder)) {
        New-Item -ItemType Directory -Path $folder -Force | Out-Null
    }
    
    # Resize image
    $resizedImage = New-Object System.Drawing.Bitmap($size, $size)
    $graphics = [System.Drawing.Graphics]::FromImage($resizedImage)
    $graphics.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic
    $graphics.DrawImage($originalImage, 0, 0, $size, $size)
    
    # Save ic_launcher.png
    $outputPath1 = Join-Path $folder "ic_launcher.png"
    $resizedImage.Save($outputPath1, [System.Drawing.Imaging.ImageFormat]::Png)
    
    # Save ic_launcher_round.png (same image)
    $outputPath2 = Join-Path $folder "ic_launcher_round.png"
    $resizedImage.Save($outputPath2, [System.Drawing.Imaging.ImageFormat]::Png)
    
    Write-Host "✅ $folder : ${size}x${size}px"
    
    $graphics.Dispose()
    $resizedImage.Dispose()
}

$originalImage.Dispose()
Write-Host "🎉 Logo Resize Tamamlandı!"







































