## 1. EC2 인스턴스 접속 및 환경 설정

EC2 인스턴스가 성공적으로 시작된 후:

1. 인스턴스 상태가 "실행 중"이 되면, 인스턴스를 선택하고 "연결(Connect)" 버튼을 클릭합니다.
2. SSH 또는 EC2 Instance Connect를 사용하여 인스턴스에 접속합니다.
3. **시스템을 업데이트하고 Docker를 설치합니다**:

### 시스템 업데이트

```bash
# 시스템 업데이트
sudo apt update
sudo apt upgrade -y

# Docker 설치에 필요한 패키지 설치
sudo apt install -y apt-transport-https ca-certificates curl software-properties-common

# Docker 공식 GPG 키 추가
curl -fsSL <https://download.docker.com/linux/ubuntu/gpg> | sudo apt-key add -

# Docker 공식 저장소 추가
sudo add-apt-repository "deb [arch=amd64] <https://download.docker.com/linux/ubuntu> $(lsb_release -cs) stable"

# 저장소 업데이트
sudo apt update

# Docker 설치
sudo apt install -y docker-ce

# Docker 서비스 시작
sudo systemctl start docker
sudo systemctl enable docker

# 현재 사용자를 docker 그룹에 추가 (sudo 없이 docker 명령어 실행 가능)
sudo usermod -aG docker ubuntu

```

- 로그아웃 후 다시 로그인하여 docker 그룹 권한을 적용합니다.

```java
exit
```

### Docker Compose 설치:

```bash
# Docker Compose 다운로드
sudo curl -L "<https://github.com/docker/compose/releases/latest/download/docker-compose-$>(uname -s)-$(uname -m)" -o /usr/local/bin/docker-compose

# 실행 권한 부여
sudo chmod +x /usr/local/bin/docker-compose

# 심볼릭 링크 생성
sudo ln -s /usr/local/bin/docker-compose /usr/bin/docker-compose

# 버전 확인
docker-compose --version

```

## 2. 도커 이미지 가져오기 및 실행하기

1. 제공된 도커 이미지들을 가져옵니다:

```bash
docker pull chaeminyu/ec2-iam-frontend:latest
docker pull chaeminyu/ec2-iam-backend:latest

```

2. docker-compose.yml 파일을 생성합니다:

```bash
mkdir -p ~/ec2-iam-workshop
cd ~/ec2-iam-workshop

```

3. 아래 내용으로 docker-compose.yml 파일을 생성합니다:

(또는 깃헙 레포에서 긁어서 EC2에서 vim으로 작성해도 됩니다)

```bash
cat > docker-compose.yml << 'EOF'
version: '3'

services:
  frontend:
    image: chaeminyu/ec2-iam-frontend:latest
    build: ./frontend
    ports:
      - "80:80"
    depends_on:
      - backend
    networks:
      - app-network
    environment:
      - BACKEND_URL=http://backend:8080

  backend:
    image: chaeminyu/ec2-iam-backend:latest
    build: ./backend
    ports:
      - "8080:8080"
    networks:
      - app-network

networks:
  app-network:
    driver: bridge
EOF

```

4. Docker Compose를 사용하여 컨테이너를 실행합니다:

```bash
docker-compose up -d
```
