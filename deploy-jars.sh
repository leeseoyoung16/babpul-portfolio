#!/bin/bash
KEY="${DEPLOY_KEY:?DEPLOY_KEY 환경변수에 PEM 키 경로를 지정하세요}"
HOST="${DEPLOY_HOST:?DEPLOY_HOST 환경변수에 user@host를 지정하세요}"
REMOTE="/home/ec2-user/bappul-backend/bappul-msa"

scp -i $KEY eureka-server/build/libs/*.jar $HOST:$REMOTE/eureka-server/build/libs/
scp -i $KEY user-service/build/libs/*.jar $HOST:$REMOTE/user-service/build/libs/
scp -i $KEY store-service/build/libs/*.jar $HOST:$REMOTE/store-service/build/libs/
scp -i $KEY order-service/build/libs/*.jar $HOST:$REMOTE/order-service/build/libs/
scp -i $KEY store-ai-service/build/libs/*.jar $HOST:$REMOTE/store-ai-service/build/libs/
scp -i $KEY matching-service/build/libs/*.jar $HOST:$REMOTE/matching-service/build/libs/

echo "전송 완료"