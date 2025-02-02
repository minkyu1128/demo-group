## FCM 이란?

Firebase Cloud Messaging의 약자로 구글에서 제공하는 메시징 서비스로,   
FCM은 서버에서 클라이언트로 안정적으로 메시지를 전달 할 수 있도록 도와주는 서비스입니다.
![img_7.png](img_7.png)

## 1. 구현 해보기

### 1.1 FCM 메시지 흐름

![img.png](img.png)

### 1.2 FCM 사용을 위한 서버의 조건

1. 서버는 FCM에서 지정한 형식의 메시지 요청을 보낼 수 있어야 합니다.
2. **지수 백오프**를 사용해 요청을 처리하고 다시 보낼 수 있어야 합니다.
    - [지수 백오프]: 요청이 실패할 때마다 다음 요청까지의 유휴시간 간격을 n배씩 늘리면서 재요청을 지연시키는 알고리즘
    - 임의 지연을 사용해 연쇄 충돌을 방지하기 위해서 사용
3. **서버 승인 사용자의 인증 정보**와 **클라이언트의 등록 토큰**을 안전하게 저장 할 수 있어야 합니다.
    - 서버 승인 사용자의 인증 정보: 메시지를 보낼 앱 서버가 인증된 서버라는 것을 증명하는 정보
    - 클라이언트의 등록 토큰: 메시지를 보내고자 하는 디바이스의 정보

### 1.3 워크플로우

1. 사용자가 앱을 실행하면, 앱은 FCM 메시지를 수신합니다.
2. 앱은 FCM 메시지를 처리하고, 사용자에게 알림을 표시합니다.
3. 사용자가 알림을 클릭하면, 앱은 알림에 대한 정보를 표시합니다.
4. 앱은 알림에 대한 정보를 서버로 전송합니다.
5. 서버는 알림에 대한 정보를 저장합니다.
6. 서버는 알림에 대한 정보를 사용자에게 전송합니다.
7. 사용자는 알림에 대한 정보를 확인합니다.
8. 사용자는 알림에 대한 정보를 삭제합니다.
9. 서버는 알림에 대한 정보를 삭제합니다.
10. 사용자는 앱을 종료합니다.
11. 앱은 종료됩니다.

## 2. 실습

### 2.1 Firebase 프로젝트 생성

### 2.2 Firebase Admin SDK 설정

[공식사이트](https://firebase.google.com/docs/admin/setup?hl=ko&_gl=1*68beem*_up*MQ..*_ga*MTk1MTk3OTE0Mi4xNzM4Mjg0MTYy*_ga_CW55HF8NVT*MTczODI4NDE2Mi4xLjAuMTczODI4NDE2Mi4wLjAuMA..)
를 참고하여 Firebase Admin SDK 설정을 진행합니다.

서버에서 FCM 메시지를 보내기 위한 Firebase Admin SDK 라이브러리 의존성을 추가해 줍니다.

``` groovy
dependencies {
  implementation 'com.google.firebase:firebase-admin:9.4.3'
}
```

SDK 초기화에 앞서 `서비스 계정의 비공개 키 파일(.json)`이 필요 합니다.   
[서비스 계정 생성](https://console.firebase.google.com/u/0/project/_/settings/serviceaccounts/adminsdk?hl=ko&_gl=1*62ca0n*_up*MQ..*_ga*ODM1ODkwNTYwLjE3MzgyODY4NTI.*_ga_CW55HF8NVT*MTczODI4Njg1Mi4xLjAuMTczODI4Njg1Mi4wLjAuMA..)
으로 접속 후 다음 절차를 따라 프로젝트 생성 후 비공개 키를 발급 받도록 합니다.

``` text
1. Firebase Console에서 설정 > 서비스 계정을 엽니다.
2. 새 비공개 키 생성을 클릭한 다음 키 생성을 클릭하여 확인합니다.
3. 키가 들어 있는 JSON 파일을 안전하게 저장합니다.
```

프로젝트를 생성하게 되면 친절하게 Admin SDK 초기화 코드를 제공해 줍니다.  
여기서 `새 비공개 키 생성` 버튼을 클릭하여 JSON 파일을 다운로드 받습니다. `키 파일`은 기밀 데이터이므로 공개저장소에 업로드 하지 않도록 주의합니다.   
.gitignore 파일에 `*.json` 을 추가하여 업로드 되지 않도록 설정합니다.
![img_2.png](img_2.png)

다운로드 받은 JSON 파일을 프로젝트의 `resources` 디렉토리에 저장 후 SDK 를 초기화 하기 위해 다음과 같이 코드를 작성합니다.

``` java
@Configuration
public class FcmConfig {
    @PostConstruct
    public void initialize() throws IOException {
        String keyPath = "fcm-key/firebase-adminsdk.json";  //resources 폴더에 넣어둔 key 경로

        InputStream serviceAccount = new ClassPathResource(keyPath).getInputStream();

        FirebaseOptions options = new FirebaseOptions.Builder()
                .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                .build();

        FirebaseApp.initializeApp(options);
    }
}
```

### 2.3 서버에서 FCM 메시지 보내기

메시지를 전송하는 방법은 다음과 같으며, 스니펫은 링크를 참고하시길 바랍니다.

- [특정 기기에 메시지 전송](https://firebase.google.com/docs/cloud-messaging/send-message?hl=ko#send-messages-to-specific-devices) -
  client의 token 기반으로 메시지를 발행 합니다.
- [여러 기기에 메시지 전송](https://firebase.google.com/docs/cloud-messaging/send-message?hl=ko#send-messages-to-multiple-devices) -
  client의 token 목록을 기반으로 메시지를 발행 합니다.
- [주제(Topic)로 메시지 전송](https://firebase.google.com/docs/cloud-messaging/send-message?hl=ko#send-messages-to-topics) -
  topic 기반으로 메시지를 발행 합니다.
- [기기 그룹에 메시지 전송](https://firebase.google.com/docs/cloud-messaging/send-message?hl=ko#send-messages-to-device-groups)

메시지 필드는 플랫폼(Android, iOS, 웹)의 `공통 필드`와 `플랫폼별 필드` 가 있으며,    
플랫폼별 필드를
사용해 [플랫폼에 맞게 맞춤 설정](https://firebase.google.com/docs/cloud-messaging/send-message?hl=ko#customize-messages-across-platforms)
이 가능 합니다.   
플랫폼별 블록에서 제공하는 키에 관한 자세한
내용은 [HTTP v1 참조 문서](https://firebase.google.com/docs/reference/fcm/rest/v1/projects.messages?hl=ko&_gl=1*1xmtpjp*_up*MQ..*_ga*NDM4NjI0MjY5LjE3MzgzODg2MTM.*_ga_CW55HF8NVT*MTczODM4ODYxMi4xLjAuMTczODM4ODYxMi4wLjAuMA..#resource:-message)
에서 확인 하세요.

``` json
[ 공통 필드 ]
message.notification.title
message.notification.body
message.data
[ 플랫폼별 필드 ]
message.android   //안드로이드
message.webpush   //Web
message.apns      //Apple
```

서버에서 클라이언트의 Topic 구독과 취소를 관리 할 수 있으며, 주제(Topic) 메시지 전송을 사용 할 경우 유용하게 활용 할 수 있습니다.   
자세한 내용과
스니펫은 [서버 Topic 관리](https://firebase.google.com/docs/cloud-messaging/manage-topics?hl=ko&_gl=1*1kg8otd*_up*MQ..*_ga*MTA3NTAxODI2My4xNzM4Mzk3NTkw*_ga_CW55HF8NVT*MTczODM5NzU5MC4xLjAuMTczODM5NzU5MC4wLjAuMA..)
문서에서 확인 할 수 있습니다.

### 2.4 FCM 클라이언트 설정

메시지를 수신하기 위해서 클라이언트 설정이 선행 되어야 하며, FCM을 웹에서 사용 할 경우 `HTTPS 연결이 필수`로 요구 됩니다.   
개발 환경에서 localhost 는 HTTP 를 사용할 수 있도록 예외를 두고 있지만, 이번 실습에서는 Https를 무료로 기본 제공해주는 firebase Host를 사용해 진행 해보고자 합니다.   
     
`앱 생성 > 웹 푸시 인증서 키 쌍(key pair)생성 > firebase 앱 초기화 > 토큰 생성 > 메시지 수신 설정` 순으로 진행 합니다.   
관련
문서는 [클라이언트 설정(웹 앱)](https://firebase.google.com/docs/cloud-messaging/js/client?hl=ko&_gl=1*2msja3*_up*MQ..*_ga*MjgyOTIxNTE1LjE3Mzg0MTg5OTk.*_ga_CW55HF8NVT*MTczODQyODM3Ny4yLjAuMTczODQyODM3Ny4wLjAuMA..)
과 [메시지 수신(웹 앱)](https://firebase.google.com/docs/cloud-messaging/js/receive?hl=ko&_gl=1*8v83hv*_up*MQ..*_ga*MjgyOTIxNTE1LjE3Mzg0MTg5OTk.*_ga_CW55HF8NVT*MTczODQ3OTM4NC4zLjAuMTczODQ3OTM4NC4wLjAuMA..)
을 참고하실 수 있습니다.

먼저 앱을 생성하기
위해 [계정 생성](https://console.firebase.google.com/u/0/project/_/settings/serviceaccounts/adminsdk?hl=ko&_gl=1*62ca0n*_up*MQ..*_ga*ODM1ODkwNTYwLjE3MzgyODY4NTI.*_ga_CW55HF8NVT*MTczODI4Njg1Mi4xLjAuMTczODI4Njg1Mi4wLjAuMA..)
에 접속해 프로젝트>일반 페이지에서 "앱 생성"을 진행 합니다.   
본 예제는 WebApp 으로 생성 했습니다.

![img_1.png](img_1.png)

앱 생성이 완료되면 다음과 같이 **firebase 앱 초기화** 코드에 대한 친절한 스니펫을 확인 할 수 있습니다.
![img_3.png](img_3.png)

이후 "Firebase 호스팅 사이트에 연결" 버튼을 클릭해 호스팅을 생성하도록 합니다.
![img_4.png](img_4.png)

다음 커맨드는 프로젝트 디렉터리를 생성할 **Root 경로에서 진행**하시길 권장 드립니다.    
프로젝트 생성 단계에서 나열되는 목록 중 **"( ) Hosting: Configure files for Firebase Hosting and (optionally) set up GitHub Action
deploys
"** 를 선택 하도록 합니다.

```shell
1) Firebase CLI 설치
  $ npm install -g firebase-tools
2) 프로젝트 초기화
  $ firebase login  //로그인. 웹브라우저에서 구글 계정으로 로그인이 진행 됩니다.
  $ firebase init   //프로젝트 생성. 
3) 호이스팅에 배포
  $ firebase deploy //나열되는 프로젝트 목록 중 배포할 프로젝트를 선택 합니다.
``` 

배포까지 성공하면 다음 링크를 통해 배포된 페이지를 확인 할 수 있습니다.
![img_5.png](img_5.png) ![img_6.png](img_6.png)

### 2.5 클라이언트에서 FCM 메시지 수신하기

메시지 수신은 `앱 생성 > 웹 푸시 인증서 키 쌍(key pair)생성 > firebase 앱 초기화 > 토큰 생성 > 메시지 수신 설정` 중 firebase 앱 초기화부터 진행 합니다.   
관련
문서는 [메시지 수신(웹 앱)](https://firebase.google.com/docs/cloud-messaging/js/receive?hl=ko&_gl=1*8v83hv*_up*MQ..*_ga*MjgyOTIxNTE1LjE3Mzg0MTg5OTk.*_ga_CW55HF8NVT*MTczODQ3OTM4NC4zLjAuMTczODQ3OTM4NC4wLjAuMA..)
을 참고 하시기 바랍니다.

메시지 수신 방법에는 포그라운드와 백그라운드 두가지 방법이 있으며,   
포그라운드(foreground)는 브라우저 **화면이 활성화 되어있을 경우에 동작**하고,   
백그라운드(background)는 브라우저 **화면이 비활성화 되어있는 경우에 동작** 합니다.   
(이 사실을 간과하고 테스트 진행하다 포그라운드 핸들러가 동작하지 않아 한참을 삽질 했습니다...OTL)    
firebase 에서는 수신 방법에 따라 `onMessage` 와 `onBackgroundMessage` 함수를 제공 합니다.  
함수에 관한 인자(arguments)
는 [Messaging API reference 문서](https://firebase.google.com/docs/reference/js/messaging_.md?authuser=0&hl=ko&_gl=1*rhfyhf*_up*MQ..*_ga*MjgyOTIxNTE1LjE3Mzg0MTg5OTk.*_ga_CW55HF8NVT*MTczODQ3MTk4MC43LjEuMTczODQ3MjA4Ni4wLjAuMA..#gettoken_b538f38)
에서 확인 하실 수 있습니다.

public path에 html 페이지를 하나 만들고 다음 순으로 진행 합니다

**1) firebase 앱 초기화**

firebaseConfig 정보는 앞서 생성한 앱의 SDK 설정 및 구성 스니펫에서 확인 하시기 바랍니다.

```javascript
<script type="module">
    import {initializeApp} from "https://www.gstatic.com/firebasejs/11.2.0/firebase-app.js";
    import {getAnalytics} from "https://www.gstatic.com/firebasejs/11.2.0/firebase-analytics.js";
    import {getMessaging, deleteToken, getToken, onMessage} from
    "https://www.gstatic.com/firebasejs/11.2.0/firebase-messaging.js";
    const firebaseConfig = {
    apiKey: "AIzaSyBeN6Z6tKcgx208Dd94yovWgYT9JLwVnZk",
    authDomain: "fir-17abf.firebaseapp.com",
    projectId: "fir-17abf",
    storageBucket: "fir-17abf.firebasestorage.app",
    messagingSenderId: "968604268680",
    appId: "1:968604268680:web:372a91794c18cd80da073d",
    measurementId: "G-758XGRLK6L"
};
    // Firebase 초기화
    const firebaseApp = initializeApp(firebaseConfig);
    const analytics = getAnalytics(firebaseApp);
    const messaging = getMessaging(firebaseApp);
</script>
```

**서비스 워커** 동작을 위해 Root Path에 `firebase-messaging-sw.js` 파일을 생성하고 앱 초기화 정보를 아래와 같이 다시 한번 작성해줍니다.   
서비스 워커 초기화는 토큰 발급과 백그라운드 메시지 핸들러의 경우 필수 사항이며, 포그라운드 메시지 핸들러는 필수 사항이 아니지만   
저의 경우 firebase-messaging-sw.js 를 빈(empty) 파일로 사용 했을 때 포그라운드 메시지가 핸들러가 동작하지 않았습니다...

```javascript
< firebase-messaging-sw.js>
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
```

위 내용까지 작성이 완료 되었다면 백그라운드 핸들러가 동작하게 되며,    
백그라운드로 수신된 메시지의 message.notification 의 title과 body 내용을 알림으로 띄워줍니다.    
만약, 백그라운드 핸들러를 커스텀 하고자 할 경우 아래와 같이 처리 할 수 있습니다.

```javascript
< firebase-messaging-sw.js>
    // ...
    messaging.onBackgroundMessage((payload) => {
    console.log('백그라운드 메시지:', payload);
    fnNotification(payload);
});

    const fnNotification = (payload) => {
    const notificationTitle = `(커스텀)payload.notification.title`;
    const notificationOptions = {
    body: `(커스텀)${payload.notification.body}`,
    icon: '/firebase-logo.png'
};

    self.registration.showNotification(notificationTitle, notificationOptions);
}
```

**2) 토큰 생성(등록)**   
Messaging 인스턴스의 푸시 알림을 구독하는 절차 입니다.   
token 을 발행하려면 vapiKey 값이 필요한데 [서비스 계정 생성 > 프로젝트 선택 > 클라우드 메시지 탭 > 웹 구성 ] 에서 웹 푸시 인증서를 생성 후 키 쌍(key pair) 값을 설정 합니다.

```javascript
<script type="module">
    // ...
    getToken(messaging, {vapidKey: '<웹 푸시 인증서 키 쌍>'})
    .then((currentToken) => {
        if (currentToken) {
            // 필요한 경우 토큰을 서버로 전송하고 UI를 업데이트하세요
            console.log(`현재 토큰은 [${currentToken}] 입니다`);
            // ...
        } else {
            // 권한 요청 UI 표시
            console.log('사용 가능한 등록 토큰이 없습니다. 토큰 생성을 위한 권한을 요청하세요.');
            // ...
        }
    }).catch((err) => {
        console.log('토큰을 가져오는 중 오류가 발생했습니다. ', err);
        // ...
    });
</script>
```

만약 구독(subscribe) 취소를 하고자 할 경우 `deleteToken` 함수를 통해 token 을 무효화할 수 있으며,   
서버에서는 무효화된 토큰으로 메시지 전송 시 `UNREGISTRED` 에러를 응답받게 됩니다.

```javascript
<script type="module">
    // ...
    //토큰 무효화
    deleteToken(messaging);
</script>
```

**3) 메시지 수신**   
포그라운드 메시지 수신은 onMessage 함수를 사용 합니다.   
구독 인스턴스에서 메시지를 발행하면 onMessage 가 실행 됩니다.

```javascript
<script type="module">
    // ... 
    onMessage(messaging, (payload) => {
    console.log('메시지가 수신되었습니다. ', payload);
    // ... 
});
```
### 2.7 서버에서 FCM 메시지 전송하기

### 2.6 클라이언트에서 FCM 메시지 확인하기

### 2.8 서버에서 FCM 메시지 삭제하기

### 2.9 서버에서 FCM 메시지 확인하기


### 2.11 서버에서 FCM 메시지 전송 결과 확인하기

### 2.12 서버에서 FCM 메시지 전송 결과 처리하기

### 2.13 서버에서 FCM 메시지 전송 결과 저장하기

### 2.14 서버에서 FCM 메시지 전송 결과 삭제하기

## 99. 참고 자료

### 99.1 사이트

- [Firebase Cloud Messaging 공식문서](https://firebase.google.com/docs/cloud-messaging)
- [FCM 알아보기](https://musma.github.io/2023/09/06/FCM-%EC%95%8C%EC%95%84%EB%B3%B4%EA%B8%B0.html#fcm-%EC%9D%B4%EB%9E%80)