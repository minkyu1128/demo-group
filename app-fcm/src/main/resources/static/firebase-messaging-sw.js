importScripts('https://www.gstatic.com/firebasejs/11.2.0/firebase-app-compat.js');
importScripts('https://www.gstatic.com/firebasejs/11.2.0/firebase-messaging-compat.js');

firebase.initializeApp({
    apiKey: "AIzaSyBeN6Z6tKcgx208Dd94yovWgYT9JLwVnZk",
    authDomain: "fir-17abf.firebaseapp.com",
    projectId: "fir-17abf",
    messagingSenderId: "968604268680",
    appId: "1:968604268680:web:372a91794c18cd80da073d"
});

const messaging = firebase.messaging();

messaging.onBackgroundMessage((payload) => {
    console.log('백그라운드 메시지:', payload);
    fnNotification(payload);
});

const fnNotification = (payload) => {
    const notificationTitle = `(커스텀)payload.notification.title`;
    const notificationOptions = {
        body: `${payload.notification.body}`,
        icon: '/firebase-logo.png'
    };

    self.registration.showNotification(notificationTitle, notificationOptions);
}

