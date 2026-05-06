ARG RUNTIME_IMAGE=eclipse-temurin:17.0.14_7-jre-jammy
FROM ${RUNTIME_IMAGE}



# UTF-8 避免 LibreOffice/日志中文异常；时区与业务一致
ENV LANG=C.UTF-8 LC_ALL=C.UTF-8 TZ=Asia/Shanghai

ARG LIBREOFFICE_VERSION=1:7.3.7-0ubuntu0.22.04.10

# fonts-noto-cjk：容器内默认无中文字体，转 PDF 会成「方框」；fontconfig + fc-cache 供 LibreOffice 识别字库
RUN apt-get update \
    && apt-get install -y --no-install-recommends \
        libreoffice=${LIBREOFFICE_VERSION} \
        fonts-noto-cjk \
        fontconfig \
    && fc-cache -fv \
    && rm -rf /var/lib/apt/lists/*


#ADD ./ /code
ADD doc_server.jar /code/doc_server.jar
ADD ./config /code/config
# 用来备份配置文件
ADD ./config /code/config_back
WORKDIR /code
#VOLUME /logs
#EXPOSE 8080

# 不同系统有兼容问题
# RUN /bin/cp /usr/share/zoneinfo/Asia/Shanghai /etc/localtime && echo 'Asia/Shanghai' >/etc/timezone

#-Xmx ：jvm的最大值	-XX:MaxHeapSize 的简写
#-Xms ：jvm的最小值	-XX:InitialHeapSize 的简写
#-Xss             -XX:ThreadStackSize 的简写  Stack 栈,最小328
# jinfo -flags pid,jinfo -flag name pid,jmap -heap pid
# 如果是大项目修改xms和xmx的值,越大越好,设置jdk编码为utf8解决打印中文,或者拉取nacos配置文件的时候出现乱码问题
# 国产机器上加xms配置有可能会造成服务启动失败
ENTRYPOINT ["java","-server","-Xms1024M","-Xmx1024M","-Dfile.encoding=UTF-8","-Duser.timezone=GMT+08","-jar","doc_server.jar","--Dspring.config.location=config/*"]
# ENTRYPOINT ["java","-Dfile.encoding=UTF-8","-Duser.timezone=GMT+08","-jar","doc_server.jar","--Dspring.config.location=config/*"]
