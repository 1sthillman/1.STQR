const sharp = require('sharp');
const fs = require('fs');
const path = require('path');

async function generateIcons() {
  const inputImage = 'logo (3).png';
  
  // Android icon sizes
  const sizes = [
    { folder: 'mipmap-mdpi', size: 48 },
    { folder: 'mipmap-hdpi', size: 72 },
    { folder: 'mipmap-xhdpi', size: 96 },
    { folder: 'mipmap-xxhdpi', size: 144 },
    { folder: 'mipmap-xxxhdpi', size: 192 }
  ];

  console.log('🎨 1STQR İcon\'ları Oluşturuluyor...');
  
  for (const { folder, size } of sizes) {
    const outputDir = `android/app/src/main/res/${folder}`;
    
    // Create directory if it doesn't exist
    if (!fs.existsSync(outputDir)) {
      fs.mkdirSync(outputDir, { recursive: true });
    }
    
    // Generate ic_launcher.png
    await sharp(inputImage)
      .resize(size, size, { 
        fit: 'contain',
        background: { r: 0, g: 0, b: 0, alpha: 0 }
      })
      .png()
      .toFile(path.join(outputDir, 'ic_launcher.png'));
    
    // Generate ic_launcher_round.png  
    await sharp(inputImage)
      .resize(size, size, { 
        fit: 'contain',
        background: { r: 0, g: 0, b: 0, alpha: 0 }
      })
      .png()
      .toFile(path.join(outputDir, 'ic_launcher_round.png'));
      
    console.log(`✅ ${folder}: ${size}x${size}px`);
  }
  
  console.log('🎉 Tüm İcon\'lar Hazır!');
}

generateIcons().catch(console.error);







































