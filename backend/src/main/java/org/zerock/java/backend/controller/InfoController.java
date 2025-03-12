package org.zerock.java.backend.controller;

import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.zerock.java.backend.model.ServerInfo;
import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.ec2.model.DescribeInstancesRequest;
import software.amazon.awssdk.services.ec2.model.DescribeInstancesResponse;
import software.amazon.awssdk.services.ec2.model.Instance;
import software.amazon.awssdk.services.ec2.model.Reservation;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.LocalDateTime;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class InfoController {

    private String getMetadata(String path) {
        try {
            // 먼저 토큰 가져오기
            URL tokenUrl = new URL("http://169.254.169.254/latest/api/token");
            HttpURLConnection tokenConnection = (HttpURLConnection) tokenUrl.openConnection();
            tokenConnection.setRequestMethod("PUT");
            tokenConnection.setRequestProperty("X-aws-ec2-metadata-token-ttl-seconds", "21600");
            tokenConnection.setConnectTimeout(3000);

            BufferedReader tokenReader = new BufferedReader(
                    new InputStreamReader(tokenConnection.getInputStream()));
            String token = tokenReader.readLine();
            tokenReader.close();

            // 토큰을 사용하여 메타데이터 가져오기
            URL url = new URL("http://169.254.169.254/latest/meta-data/" + path);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setRequestProperty("X-aws-ec2-metadata-token", token);
            connection.setConnectTimeout(3000);

            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(connection.getInputStream()));
            String result = reader.readLine();
            reader.close();

            return result;
        } catch (Exception e) {
            return "Unknown";
        }
    }

    @GetMapping("/info")
    public ServerInfo getServerInfo() {
        ServerInfo info = new ServerInfo();
        info.setTimestamp(LocalDateTime.now().toString());

        try {
            Ec2Client ec2Client = Ec2Client.create();

            // 인스턴스 ID는 환경 변수를 통해 가져오기 시도
            String instanceId = System.getenv("HOSTNAME");
            info.setInstanceId(instanceId != null ? instanceId : "알 수 없음");

            // EC2 인스턴스 설명 요청
            DescribeInstancesRequest request = DescribeInstancesRequest.builder()
                    .instanceIds(instanceId)
                    .build();

            DescribeInstancesResponse response = ec2Client.describeInstances(request);

            if (response.hasReservations() && !response.reservations().isEmpty()) {
                Reservation reservation = response.reservations().get(0);
                if (!reservation.instances().isEmpty()) {
                    Instance instance = reservation.instances().get(0);
                    info.setInstanceType(instance.instanceTypeAsString());
                    info.setAvailabilityZone(instance.placement().availabilityZone());
                    info.setRegion(instance.placement().availabilityZone().substring(0, instance.placement().availabilityZone().length() - 1));
                }
            }

            info.setMessage("EC2 인스턴스에서 성공적으로 데이터를 가져왔습니다!");
        } catch (Exception e) {
            info.setInstanceId("알 수 없음");
            info.setRegion("알 수 없음");
            info.setAvailabilityZone("알 수 없음");
            info.setInstanceType("알 수 없음");
            info.setMessage("EC2 정보를 가져올 수 없습니다: " + e.getMessage());
        }

        return info;
    }
}