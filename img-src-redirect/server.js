
const express = require('express');
const app = express();

//aHR0cHM6Ly9pbWczLmRhdW1jZG4ubmV0L3RodW1iL1I2NTh4MC5xNzAvP2ZuYW1lPWh0dHBzOi8vdDEuZGF1bWNkbi5uZXQvbmV3cy8yMDI1MDcvMDIvc2VvdWwvMjAyNTA3MDIxMTM5MDM1MTN6am9lLmpwZw==
app.use(express.static('public'));

app.get('/', (req, res) => {
    res.sendFile(__dirname + '/public/index.html');
});

//---------------

app.get('/viewer/:hash', (req, res) => {
    const hash = req.params.hash; 
    const url = Buffer.from(hash, 'base64').toString('utf-8');
    res.redirect(302, url);
});

app.listen(3000, () => {
    console.log('서버가 3000번 포트에서 실행중입니다.');
});