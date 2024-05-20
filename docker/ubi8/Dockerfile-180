FROM registry.access.redhat.com/ubi8/python-38
#FROM registry.access.redhat.com/ubi8/ubi:8.3

LABEL maintainer="sig-platform@spinnaker.io"
ARG TARGETARCH

# KUBECTL_RELEASE kept one minor version behind latest to maximise compatibility overlap
ENV KUBECTL_RELEASE=1.22.0
ENV AWS_CLI_VERSION=1.18.152
ENV AWS_CLI_S3_CMD=2.0.2
ENV AWS_AIM_AUTHENTICATOR_VERSION=0.5.9
ENV GOOGLE_CLOUD_SDK_VERSION=412.0.0
ENV ECR_TOKEN_VERSION=v1.0.2

ENV PATH "$PATH:/usr/local/bin/:/opt/google-cloud-sdk/bin/:/usr/local/bin/aws-iam-authenticator"

USER root
RUN yum -y install bash jq  tar unzip wget procps java-11-openjdk java-11-openjdk-devel.x86_64 vim  net-tools curl git python2 tzdata-java

# AWS CLI

RUN yum -y install python2-pip && \
    pip2 install --upgrade awscli==${AWS_CLI_VERSION} s3cmd==${AWS_CLI_S3_CMD} python-magic \
     && yum -y remove  python2-pip && \
    yum clean all




# Google cloud SDK
#RUN [ $TARGETARCH == 'amd64' ] &&  export GCP_ARCH="x86_64" || export GCP_ARCH="arm"  \
RUN   wget -nv https://dl.google.com/dl/cloudsdk/channels/rapid/downloads/google-cloud-sdk-${GOOGLE_CLOUD_SDK_VERSION}-linux-x86_64.tar.gz \
  && mkdir -p /opt \
  && tar -xzf google-cloud-sdk-${GOOGLE_CLOUD_SDK_VERSION}-linux-x86_64.tar.gz -C /opt \
  && rm google-cloud-sdk-${GOOGLE_CLOUD_SDK_VERSION}-linux-x86_64.tar.gz \
  && CLOUDSDK_PYTHON="python3" /opt/google-cloud-sdk/install.sh --usage-reporting=false --bash-completion=false \
     --additional-components app-engine-java app-engine-go gke-gcloud-auth-plugin \
  && rm -rf ~/.config/gcloud \
  && rm -rf /opt/google-cloud-sdk/.install/.backup

# kubectl + AWS IAM authenticator
RUN wget https://storage.googleapis.com/kubernetes-release/release/v${KUBECTL_RELEASE}/bin/linux/${TARGETARCH}/kubectl \
  && chmod +x kubectl \
  && mv ./kubectl /usr/local/bin/kubectl \
  && wget -O aws-iam-authenticator https://github.com/kubernetes-sigs/aws-iam-authenticator/releases/download/v${AWS_AIM_AUTHENTICATOR_VERSION}/aws-iam-authenticator_${AWS_AIM_AUTHENTICATOR_VERSION}_linux_${TARGETARCH} \
	&& chmod +x ./aws-iam-authenticator \
	&& mv ./aws-iam-authenticator /usr/local/bin/aws-iam-authenticator\
    && ln -sf /usr/local/bin/aws-iam-authenticator /usr/local/bin/heptio-authenticator-aws


COPY clouddriver-web/build/install/clouddriver /opt/clouddriver


RUN adduser spinnaker

RUN mkdir -p /opt/clouddriver/plugins
RUN chown -R spinnaker:spinnaker /opt/

RUN mkdir -p /etc/pki/tls/certs
RUN chmod -R 777 /etc/pki/tls/certs


RUN chmod -R 777 /var/lib/
RUN chown -R spinnaker:spinnaker /var/lib/
USER spinnaker
CMD  sleep 180s ; "/opt/clouddriver/bin/clouddriver"
