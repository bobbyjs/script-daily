FROM jrrwll/oraclejre

LABEL maintainer="jrriwll@gmail.com" \
      version="1.0" \
      description="$IMAGE_NAME image"

WORKDIR /usr/local/src
COPY $IMAGE_NAME.jar .

EXPOSE 8080

CMD java -Xmx\${JVM_MAX_MEMORY} \
         -Xss1M -server \
         -Dfile.encoding=UTF-8  \
         -Duser.timezone=GMT+08 \
         -Dspring.profiles.active=\${SPRING_PROFILE} \
         -jar $IMAGE_NAME.jar