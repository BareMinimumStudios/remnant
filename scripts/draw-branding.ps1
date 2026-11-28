$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.Drawing

function Brush([string]$hex) {
    [Drawing.SolidBrush]::new([Drawing.ColorTranslator]::FromHtml("#$hex"))
}

function Block($g, [string]$color, [int]$x, [int]$y, [int]$w, [int]$h) {
    $ink = Brush $color
    try { $g.FillRectangle($ink, $x, $y, $w, $h) } finally { $ink.Dispose() }
}

function Draw-Mark($g, [int]$x, [int]$y, [int]$unit) {
    $g.TranslateTransform($x, $y)
    $g.ScaleTransform($unit, $unit)
    Block $g '111313' 0 0 32 32
    Block $g '353938' 1 1 30 30
    Block $g '191C1B' 2 2 28 28
    Block $g '101211' 5 8 22 19
    Block $g 'F4EFE2' 5 6 22 4
    Block $g 'C3BEB1' 5 11 22 4
    Block $g 'F4EFE2' 5 17 22 4
    Block $g 'C3BEB1' 5 22 22 4
    Block $g '191C1B' 21 6 2 20
    Block $g 'DF5551' 23 6 4 4
    Block $g 'EFB64F' 23 11 4 4
    Block $g '81BA7B' 23 17 4 4
    $g.ResetTransform()
}

function New-Icon([int]$size, [string[]]$paths) {
    $canvas = [Drawing.Bitmap]::new($size, $size)
    $g = [Drawing.Graphics]::FromImage($canvas)
    try {
        Draw-Mark $g 0 0 ($size / 32)
        foreach ($path in $paths) { $canvas.Save($path, [Drawing.Imaging.ImageFormat]::Png) }
    } finally { $g.Dispose(); $canvas.Dispose() }
}

function Label($g, $value, $color, $family, $size, $style, $x, $y) {
    $font = [Drawing.Font]::new($family, $size, $style, [Drawing.GraphicsUnit]::Pixel)
    $ink = Brush $color
    try { $g.DrawString($value, $font, $ink, [single]$x, [single]$y) }
    finally { $font.Dispose(); $ink.Dispose() }
}

$root = Split-Path -Parent $PSScriptRoot
$assets = Join-Path $root 'docs/assets'
New-Icon 256 @((Join-Path $assets 'remnant-logo.png'), (Join-Path $root 'src/main/resources/assets/remnant/icon.png'))
New-Icon 1024 @((Join-Path $assets 'remnant-logo-1024.png'))

$canvas = [Drawing.Bitmap]::new(1920, 640)
$g = [Drawing.Graphics]::FromImage($canvas)
try {
    Block $g '141615' 0 0 1920 640
    Block $g '262A28' 44 44 1832 552
    Block $g '181B19' 46 46 1828 548
    Block $g '303431' 552 106 2 428
    Draw-Mark $g 112 128 12
    $g.TextRenderingHint = [Drawing.Text.TextRenderingHint]::AntiAliasGridFit
    Label $g 'BARE MINIMUM STUDIOS' 'A8B4AB' 'Consolas' 27 Bold 614 134
    Label $g 'REMNANT' 'F4EFE2' 'Arial Black' 144 Regular 584 179
    Label $g 'Offline player data, saved on logout' 'C6CEC7' 'Segoe UI' 46 Regular 611 352
    Block $g 'DF5551' 616 449 80 6
    Block $g 'EFB64F' 696 449 80 6
    Block $g '81BA7B' 776 449 80 6
    Label $g 'MINECRAFT 1.21.1  /  FABRIC + NEOFORGE' '9CAA9F' 'Consolas' 27 Bold 614 482
    $canvas.Save((Join-Path $assets 'remnant-banner.png'), [Drawing.Imaging.ImageFormat]::Png)
} finally { $g.Dispose(); $canvas.Dispose() }
