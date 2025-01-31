package com.example.fcmmessage.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.io.InputStream;

@Configuration
public class FcmConfig {
    @PostConstruct
    public void initialize() throws IOException {
        String keyPath = "fcm-key/firebase-adminsdk.json";  //resources 폴더에 넣어둔 key 경로

//        InputStream serviceAccount = new ClassPathResource(keyPath).getInputStream();
        InputStream serviceAccount = getClass().getClassLoader().getResourceAsStream(keyPath);

        FirebaseOptions options = new FirebaseOptions.Builder()
                .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                .build();

        FirebaseApp.initializeApp(options);
    }
}
