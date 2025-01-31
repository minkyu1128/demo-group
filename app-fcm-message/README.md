## FCM 이란?

Firebase Cloud Messaging의 약자로 구글에서 제공하는 메시징 서비스로,   
FCM은 서버에서 클라이언트로 안정적으로 메시지를 전달 할 수 있도록 도와주는 서비스입니다.

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

### 2.4 클라이언트에서 FCM 메시지 수신하기

### 2.5 클라이언트에서 FCM 메시지 응답하기

### 2.6 클라이언트에서 FCM 메시지 삭제하기

### 2.7 클라이언트에서 FCM 메시지 확인하기

### 2.8 서버에서 FCM 메시지 삭제하기

### 2.9 서버에서 FCM 메시지 확인하기

### 2.10 서버에서 FCM 메시지 전송하기

### 2.11 서버에서 FCM 메시지 전송 결과 확인하기

### 2.12 서버에서 FCM 메시지 전송 결과 처리하기

### 2.13 서버에서 FCM 메시지 전송 결과 저장하기

### 2.14 서버에서 FCM 메시지 전송 결과 삭제하기

## 99. 참고 자료

### 99.1 사이트

- [Firebase Cloud Messaging 공식문서](https://firebase.google.com/docs/cloud-messaging)
- [FCM 알아보기](https://musma.github.io/2023/09/06/FCM-%EC%95%8C%EC%95%84%EB%B3%B4%EA%B8%B0.html#fcm-%EC%9D%B4%EB%9E%80)