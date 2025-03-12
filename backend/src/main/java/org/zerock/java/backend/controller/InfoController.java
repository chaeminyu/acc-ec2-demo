package org.zerock.java.backend.controller;

import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.zerock.java.backend.model.ServerInfo;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.LocalDateTime;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class InfoController {

    @GetMapping("/info")
    public ServerInfo getServerInfo() {
        ServerInfo info = new ServerInfo();
        info.setTimestamp(LocalDateTime.now().toString());

        try {
            // EC2 메타데이터에서 인스턴스 ID 가져오기
            URL url = new URL("http://169.254.169.254/latest/meta-data/instance-id");
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(3000);

            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(connection.getInputStream()));
            String instanceId = reader.readLine();
            reader.close();

            info.setInstanceId(instanceId);
            info.setMessage("EC2 인스턴스에서 성공적으로 데이터를 가져왔습니다!");
        } catch (Exception e) {
            info.setInstanceId("알 수 없음");
            info.setMessage("EC2 메타데이터에 접근할 수 없습니다: " + e.getMessage());
        }

        return info;
    }
}