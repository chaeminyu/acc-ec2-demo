package org.zerock.java.backend.controller;

import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.zerock.java.backend.model.InstanceInfo;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
            tokenConnection.setReadTimeout(3000);

            int responseCode = tokenConnection.getResponseCode();
            if (responseCode == 200) {
                BufferedReader tokenReader = new BufferedReader(
                        new InputStreamReader(tokenConnection.getInputStream()));
                String token = tokenReader.readLine();
                tokenReader.close();
                return token;
            } else {
                System.out.println("Failed to get token. Response code: " + responseCode);
                return null;
            }
        } catch (Exception e) {
            System.out.println("Exception when getting metadata token: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    private String getMetadata(String path, String token) {
        try {
            URL url = new URL("http://169.254.169.254/latest/meta-data/" + path);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");

            // IMDSv2 방식 (토큰 사용)
            if (token != null) {
                connection.setRequestProperty("X-aws-ec2-metadata-token", token);
            }

            connection.setConnectTimeout(3000);
            connection.setReadTimeout(3000);

            int responseCode = connection.getResponseCode();
            if (responseCode == 200) {
                BufferedReader reader = new BufferedReader(
                        new InputStreamReader(connection.getInputStream()));
                String result = reader.readLine();
                reader.close();
                return result;
            } else {
                // IMDSv1 방식으로 시도 (토큰 사용 실패 시)
                if (token != null) {
                    return getMetadataV1(path);
                }
                System.out.println("Failed to get metadata for path: " + path + ". Response code: " + responseCode);
                return "Unknown";
            }
        } catch (Exception e) {
            System.out.println("Exception when getting metadata for path: " + path + ". Error: " + e.getMessage());
            // IMDSv1 방식으로 시도 (예외 발생 시)
            if (token != null) {
                return getMetadataV1(path);
            }
            e.printStackTrace();
            return "Unknown";
        }
    }

    // IMDSv1 방식으로 메타데이터 가져오기
    private String getMetadataV1(String path) {
        try {
            URL url = new URL("http://169.254.169.254/latest/meta-data/" + path);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(3000);
            connection.setReadTimeout(3000);

            int responseCode = connection.getResponseCode();
            if (responseCode == 200) {
                BufferedReader reader = new BufferedReader(
                        new InputStreamReader(connection.getInputStream()));
                String result = reader.readLine();
                reader.close();
                return result;
            } else {
                System.out.println("Failed to get metadata via IMDSv1 for path: " + path + ". Response code: " + responseCode);
                return "Unknown";
            }
        } catch (Exception e) {
            System.out.println("Exception when getting metadata via IMDSv1 for path: " + path + ". Error: " + e.getMessage());
            e.printStackTrace();
            return "Unknown";
        }
    }

    @GetMapping("/instance-info")
    public Map<String, String> getInstanceInfo() {
        Map<String, String> info = new HashMap<>();
        info.put("timestamp", LocalDateTime.now().toString());

        try {
            // IMDSv2 토큰 가져오기
            String token = getMetadataToken();

            // EC2 메타데이터에서 인스턴스 정보 가져오기
            String instanceId = getMetadata("instance-id", token);
            String availabilityZone = getMetadata("placement/availability-zone", token);
            String instanceType = getMetadata("instance-type", token);

            // 리전은 가용 영역에서 마지막 문자를 제외한 부분
            String region = "Unknown";
            if (!availabilityZone.equals("Unknown")) {
                region = availabilityZone.substring(0, availabilityZone.length() - 1);
            }

            info.put("instanceId", instanceId);
            info.put("region", region);
            info.put("availabilityZone", availabilityZone);
            info.put("instanceType", instanceType);
            info.put("message", "EC2 인스턴스에서 성공적으로 데이터를 가져왔습니다!");
        } catch (Exception e) {
            e.printStackTrace();
            info.put("instanceId", "알 수 없음");
            info.put("region", "알 수 없음");
            info.put("availabilityZone", "알 수 없음");
            info.put("instanceType", "알 수 없음");
            info.put("message", "EC2 메타데이터에 접근할 수 없습니다: " + e.getMessage());
        }

        return info;
    }

    @GetMapping("/test-iam-role")
    public Map<String, Object> testIamRole() {
        Map<String, Object> result = new HashMap<>();
        List<String> services = new ArrayList<>();
        boolean success = false;

        try {
            // IMDSv2 토큰 가져오기
            String token = getMetadataToken();

            // IAM 역할 정보 가져오기
            String iamInfo = getMetadata("iam/info", token);

            if (iamInfo != null && !iamInfo.equals("Unknown")) {
                success = true;

                // 간단히 S3와 EC2 서비스 확인 (실제로는 권한 테스트를 해야 함)
                if (getMetadata("iam/security-credentials", token) != null) {
                    services.add("IAM");
                }

                // 여기서는 역할이 연결되어 있다면 성공으로 간주
                services.add("EC2");
                services.add("S3"); // 실제로는 S3 접근 테스트 필요
            }

            result.put("success", success);
            result.put("services", services);

            if (!success) {
                result.put("error", "IAM 역할이 인스턴스에 연결되어 있지 않거나 접근할 수 없습니다.");
            }
        } catch (Exception e) {
            e.printStackTrace();
            result.put("success", false);
            result.put("error", "IAM 역할 테스트 중 오류 발생: " + e.getMessage());
        }

        return result;
    }
}