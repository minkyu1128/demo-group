importScripts('https://www.gstatic.com/firebasejs/11.2.0/firebase-app-compat.js');
importScripts('https://www.gstatic.com/firebasejs/11.2.0/firebase-messaging-compat.js');

firebase.initializeApp({
    apiKey: "AIzaSyBeN6Z6tKcgx208Dd94yovWgYT9JLwVnZk",
    authDomain: "fir-17abf.firebaseapp.com",
    projectId: "fir-17abf",
    messagingSenderId: "968604268680",
    appId: "1:968604268680:web:372a91794c18cd80da073d",
    measurementId: "G-758XGRLK6L"
});

const messaging = firebase.messaging();

messaging.onBackgroundMessage((payload) => {
    console.log('백그라운드 메시지:', payload);
    // fnNotification(payload);


    // 메인 페이지로 메시지 전달
    const messageData = encodeURIComponent(JSON.stringify(payload));
    clients.matchAll({
        type: 'window',
        includeUncontrolled: true
    }).then((windowClients) => {
        for (let client of windowClients) {
            if (client.url.includes('index.html')) {
                client.postMessage({
                    type: 'FCM_MESSAGE',
                    payload: payload
                });
            }
        }
    });
});

// const fnNotification = (payload) => {
//     const notificationTitle = `(커스텀)payload.notification.title`;
//     const notificationOptions = {
//         body: `${payload.notification.body}`,
//         icon: '/firebase-logo.png'
//     };
//
//     self.registration.showNotification(notificationTitle, notificationOptions);
// }


// 알림 클릭 처리
self.addEventListener('notificationclick', function(event) {
    event.notification.close();

    // 메시지 데이터를 URL 파라미터로 전달
    const messageData = encodeURIComponent(JSON.stringify(event.notification.data));
    const url = `/index.html?fcm_message=${messageData}`;

    event.waitUntil(
        clients.matchAll({type: 'window'}).then(windowClients => {
            // 이미 열린 창이 있다면 포커스
            for (let client of windowClients) {
                if (client.url.includes('index.html') && 'focus' in client) {
                    return client.focus();
                }
            }
            // 열린 창이 없다면 새 창 열기
            if (clients.openWindow) {
                return clients.openWindow(url);
            }
        })
    );
});


