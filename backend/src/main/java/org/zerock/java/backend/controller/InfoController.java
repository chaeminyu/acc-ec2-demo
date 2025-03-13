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

    private String getMetadataToken() {
        try {
            URL tokenUrl = new URL("http://169.254.169.254/latest/api/token");
            HttpURLConnection tokenConnection = (HttpURLConnection) tokenUrl.openConnection();
            tokenConnection.setRequestMethod("PUT");
            tokenConnection.setRequestProperty("X-aws-ec2-metadata-token-ttl-seconds", "21600");
            tokenConnection.setConnectTimeout(3000);

            BufferedReader tokenReader = new BufferedReader(
                    new InputStreamReader(tokenConnection.getInputStream()));
            String token = tokenReader.readLine();
            tokenReader.close();

            return token;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private String getMetadata(String path, String token) {
        try {
            URL url = new URL("http://169.254.169.254/latest/meta-data/" + path);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            if (token != null) {
                connection.setRequestProperty("X-aws-ec2-metadata-token", token);
            }
            connection.setConnectTimeout(3000);

            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(connection.getInputStream()));
            String result = reader.readLine();
            reader.close();

            return result;
        } catch (Exception e) {
            e.printStackTrace();
            return "Unknown";
        }
    }

    @GetMapping("/info")
    public ServerInfo getServerInfo() {
        ServerInfo info = new ServerInfo();
        info.setTimestamp(LocalDateTime.now().toString());

        try {
            // IMDSv2 토큰 가져오기
            String token = getMetadataToken();
            if (token == null) {
                throw new Exception("Failed to get IMDSv2 token");
            }

            // EC2 메타데이터에서 인스턴스 정보 가져오기
            String instanceId = getMetadata("instance-id", token);
            String availabilityZone = getMetadata("placement/availability-zone", token);
            String instanceType = getMetadata("instance-type", token);

            // 리전은 가용 영역에서 마지막 문자를 제외한 부분
            String region = "Unknown";
            if (!availabilityZone.equals("Unknown")) {
                region = availabilityZone.substring(0, availabilityZone.length() - 1);
            }

            info.setInstanceId(instanceId);
            info.setRegion(region);
            info.setAvailabilityZone(availabilityZone);
            info.setInstanceType(instanceType);
            info.setMessage("EC2 인스턴스에서 성공적으로 데이터를 가져왔습니다!");
        } catch (Exception e) {
            e.printStackTrace();
            info.setInstanceId("알 수 없음");
            info.setRegion("알 수 없음");
            info.setAvailabilityZone("알 수 없음");
            info.setInstanceType("알 수 없음");
            info.setMessage("EC2 메타데이터에 접근할 수 없습니다: " + e.getMessage());
        }

        return info;
    }
}