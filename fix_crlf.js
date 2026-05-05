const fs = require('fs');
const path = 'G:\\workSpace\\telegram-media-downloader-java\\backend\\src\\main\\java\\com\\tgdownloader\\service\\DownloadCoreService.java';
let c = fs.readFileSync(path, 'utf8');
c = c.replace(/\r\n/g, '\n');
fs.writeFileSync(path, c, 'utf8');
console.log('done', c.length);
