docker pull glucacxe/chatgpt-api:latest # 拉取镜像

docker run -p 8080:8080 \
--name chatgpt-api \
-d glucacxe/chatgpt-api   # 启动容器

