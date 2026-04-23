const https = require('https');

// Thay URL backend của bạn vào đây
const BACKEND_URL = process.env.BACKEND_URL || 'https://okabe.onrender.com/api/v1/health';

console.log(`[${new Date().toISOString()}] Bắt đầu dịch vụ Keep-Alive cho: ${BACKEND_URL}`);

function ping() {
  https.get(BACKEND_URL, (res) => {
    console.log(`[${new Date().toISOString()}] Ping thành công! Status Code: ${res.statusCode}`);
  }).on('error', (err) => {
    console.error(`[${new Date().toISOString()}] Ping thất bại: ${err.message}`);
  });
}

// Ping ngay lập tức khi chạy
ping();

// Cứ 10 phút (600.000 ms) ping một lần
const INTERVAL = 10 * 60 * 1000;
setInterval(ping, INTERVAL);
