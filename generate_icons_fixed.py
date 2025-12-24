#!/usr/bin/env python3
"""
Generate Android app icons with proper aspect ratio handling.
Uses pluct logo.jpg (with background) and pluct logo transparent.png
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

# Foreground sizes for adaptive icons (108dp)
FOREGROUND_SIZES = {
    'mipmap-mdpi': 108,
    'mipmap-hdpi': 162,
    'mipmap-xhdpi': 216,
    'mipmap-xxhdpi': 324,
    'mipmap-xxxhdpi': 432,
}

SOURCE_LOGO_JPG = 'pluct logo.jpg'
SOURCE_LOGO_TRANSPARENT = 'pluct logo transparent.png'
RES_BASE = 'app/src/main/res'

def create_icon_from_square(source_img, target_size, add_padding_percent=0):
    """
    Create icon from a square source image.
    Optionally add padding around the logo for better visual balance.
    """
    # Calculate the logo size with padding
    if add_padding_percent > 0:
        logo_size = int(target_size * (1.0 - add_padding_percent / 100.0))
    else:
        logo_size = target_size
    
    # Resize the source image
    resized = source_img.resize((logo_size, logo_size), Image.Resampling.LANCZOS)
    
    if add_padding_percent > 0:
        # Create canvas with transparent background
        canvas = Image.new('RGBA', (target_size, target_size), (0, 0, 0, 0))
        # Center the logo
        offset = (target_size - logo_size) // 2
        canvas.paste(resized, (offset, offset), resized if resized.mode == 'RGBA' else None)
        return canvas
    else:
        return resized

def generate_icons():
    """Generate all icon sizes from both logo variants."""
    
    # Check for source files
    if not os.path.exists(SOURCE_LOGO_JPG):
        print(f"Error: Source logo not found: {SOURCE_LOGO_JPG}")
        return False
    
    if not os.path.exists(SOURCE_LOGO_TRANSPARENT):
        print(f"Error: Source transparent logo not found: {SOURCE_LOGO_TRANSPARENT}")
        return False
    
    try:
        # Load the JPG logo (with background)
        logo_jpg = Image.open(SOURCE_LOGO_JPG)
        if logo_jpg.mode != 'RGBA':
            logo_jpg = logo_jpg.convert('RGBA')
        print(f"Loaded JPG logo: {logo_jpg.size[0]}x{logo_jpg.size[1]} pixels")
        
        # Load the transparent logo
        logo_transparent = Image.open(SOURCE_LOGO_TRANSPARENT)
        if logo_transparent.mode != 'RGBA':
            logo_transparent = logo_transparent.convert('RGBA')
        print(f"Loaded transparent logo: {logo_transparent.size[0]}x{logo_transparent.size[1]} pixels")
    except Exception as e:
        print(f"Error loading logos: {e}")
        return False
    
    # Generate regular launcher icons from JPG logo
    # Add 5% padding for better visual balance
    print("\n=== Generating ic_launcher.png from JPG logo ===")
    for folder, size in ICON_SIZES.items():
        folder_path = os.path.join(RES_BASE, folder)
        os.makedirs(folder_path, exist_ok=True)
        
        # Create icon with slight padding
        icon = create_icon_from_square(logo_jpg, size, add_padding_percent=5)
        
        # Save as PNG
        output_path = os.path.join(folder_path, 'ic_launcher.png')
        icon.save(output_path, 'PNG', optimize=True)
        print(f"Generated: {output_path} ({size}x{size})")
    
    # Generate foreground icons from transparent logo for adaptive icons
    print("\n=== Generating ic_launcher_foreground.png from transparent logo ===")
    for folder, size in FOREGROUND_SIZES.items():
        folder_path = os.path.join(RES_BASE, folder)
        os.makedirs(folder_path, exist_ok=True)
        
        # For adaptive icons, the safe zone is 66dp out of 108dp (61%)
        # We'll use 55% to ensure the logo fits comfortably
        safe_zone_size = int(size * 0.55)
        
        # Resize transparent logo to fit safe zone
        resized_logo = logo_transparent.resize((safe_zone_size, safe_zone_size), Image.Resampling.LANCZOS)
        
        # Create transparent canvas at full size
        canvas = Image.new('RGBA', (size, size), (0, 0, 0, 0))
        
        # Center the logo on the canvas
        offset = (size - safe_zone_size) // 2
        canvas.paste(resized_logo, (offset, offset), resized_logo)
        
        # Save as foreground
        output_path = os.path.join(folder_path, 'ic_launcher_foreground.png')
        canvas.save(output_path, 'PNG', optimize=True)
        print(f"Generated: {output_path} ({size}x{size})")
    
    # Generate round icons (same as regular with padding)
    print("\n=== Generating ic_launcher_round.png ===")
    for folder, size in ICON_SIZES.items():
        folder_path = os.path.join(RES_BASE, folder)
        
        # Create icon with slight padding for round shape
        icon = create_icon_from_square(logo_jpg, size, add_padding_percent=8)
        
        output_path = os.path.join(folder_path, 'ic_launcher_round.png')
        icon.save(output_path, 'PNG', optimize=True)
        print(f"Generated: {output_path} ({size}x{size})")
    
    print("\n✅ All icons generated successfully!")
    print("\nNote: Icons have 5% padding to prevent edge clipping.")
    print("Adaptive icons use 55% safe zone for optimal display across all device shapes.")
    return True

if __name__ == '__main__':
    success = generate_icons()
    exit(0 if success else 1)

