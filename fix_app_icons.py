#!/usr/bin/env python3
"""
Fix Android app icons using both logo variants:
- pluct logo.jpeg (with background) for ic_launcher.png
- pluct logo transparent.png for ic_launcher_foreground.png
"""

import os
from PIL import Image

# Icon sizes for each density (in pixels)
ICON_SIZES = {
    'mipmap-mdpi': 48,
    'mipmap-hdpi': 72,
    'mipmap-xhdpi': 96,
    'mipmap-xxhdpi': 144,
    'mipmap-xxxhdpi': 192,
}

# Foreground sizes for adaptive icons (108dp safe zone)
FOREGROUND_SIZES = {
    'mipmap-mdpi': 108,
    'mipmap-hdpi': 162,
    'mipmap-xhdpi': 216,
    'mipmap-xxhdpi': 324,
    'mipmap-xxxhdpi': 432,
}

SOURCE_LOGO_JPEG = 'pluct logo.jpeg'
SOURCE_LOGO_TRANSPARENT = 'pluct logo transparent.png'
RES_BASE = 'app/src/main/res'

def generate_icons():
    """Generate all icon sizes from both logo variants."""
    
    # Load the JPEG logo (with background)
    if not os.path.exists(SOURCE_LOGO_JPEG):
        print(f"Error: Source logo not found: {SOURCE_LOGO_JPEG}")
        return False
    
    # Load the transparent logo
    if not os.path.exists(SOURCE_LOGO_TRANSPARENT):
        print(f"Error: Source transparent logo not found: {SOURCE_LOGO_TRANSPARENT}")
        return False
    
    try:
        # Convert JPEG to RGBA for processing
        logo_jpeg = Image.open(SOURCE_LOGO_JPEG)
        if logo_jpeg.mode != 'RGBA':
            logo_jpeg = logo_jpeg.convert('RGBA')
        print(f"Loaded JPEG logo: {logo_jpeg.size[0]}x{logo_jpeg.size[1]} pixels")
        
        logo_transparent = Image.open(SOURCE_LOGO_TRANSPARENT)
        if logo_transparent.mode != 'RGBA':
            logo_transparent = logo_transparent.convert('RGBA')
        print(f"Loaded transparent logo: {logo_transparent.size[0]}x{logo_transparent.size[1]} pixels")
    except Exception as e:
        print(f"Error loading logos: {e}")
        return False
    
    # Generate regular launcher icons from JPEG logo
    print("\n=== Generating ic_launcher.png from JPEG logo ===")
    for folder, size in ICON_SIZES.items():
        folder_path = os.path.join(RES_BASE, folder)
        os.makedirs(folder_path, exist_ok=True)
        
        # Resize with high-quality resampling
        resized = logo_jpeg.resize((size, size), Image.Resampling.LANCZOS)
        
        # Save as PNG
        output_path = os.path.join(folder_path, 'ic_launcher.png')
        resized.save(output_path, 'PNG', optimize=True)
        print(f"Generated: {output_path} ({size}x{size})")
    
    # Generate foreground icons from transparent logo for adaptive icons
    print("\n=== Generating ic_launcher_foreground.png from transparent logo ===")
    for folder, size in FOREGROUND_SIZES.items():
        folder_path = os.path.join(RES_BASE, folder)
        os.makedirs(folder_path, exist_ok=True)
        
        # Create a larger canvas for adaptive icon (108dp) with padding
        # The logo should be centered and scaled down to fit the safe zone (72dp)
        safe_zone_ratio = 72.0 / 108.0  # Safe zone is 66.67% of total
        logo_size = int(size * safe_zone_ratio)
        
        # Resize logo to fit safe zone
        resized_logo = logo_transparent.resize((logo_size, logo_size), Image.Resampling.LANCZOS)
        
        # Create transparent canvas at full size
        canvas = Image.new('RGBA', (size, size), (0, 0, 0, 0))
        
        # Center the logo on the canvas
        offset = (size - logo_size) // 2
        canvas.paste(resized_logo, (offset, offset), resized_logo)
        
        # Save as foreground
        output_path = os.path.join(folder_path, 'ic_launcher_foreground.png')
        canvas.save(output_path, 'PNG', optimize=True)
        print(f"Generated: {output_path} ({size}x{size})")
    
    # Also create round icons (same as regular)
    print("\n=== Generating ic_launcher_round.png ===")
    for folder, size in ICON_SIZES.items():
        folder_path = os.path.join(RES_BASE, folder)
        
        # Use same as regular launcher
        src_path = os.path.join(folder_path, 'ic_launcher.png')
        dst_path = os.path.join(folder_path, 'ic_launcher_round.png')
        
        if os.path.exists(src_path):
            img = Image.open(src_path)
            img.save(dst_path, 'PNG', optimize=True)
            print(f"Generated: {dst_path} ({size}x{size})")
    
    print("\n✅ All icons generated successfully!")
    return True

if __name__ == '__main__':
    success = generate_icons()
    exit(0 if success else 1)

