FROM jenkins/inbound-agent:jdk21

USER root

ARG KUBECTL_VERSION=v1.37.1
ARG TRIVY_VERSION=0.76.1

RUN apt-get update \
    && apt-get install -y --no-install-recommends nodejs npm docker.io curl ca-certificates tar \
    && rm -rf /var/lib/apt/lists/*

RUN curl -fsSLo /usr/local/bin/kubectl \
      "https://dl.k8s.io/release/${KUBECTL_VERSION}/bin/linux/amd64/kubectl" \
    && chmod 0755 /usr/local/bin/kubectl

RUN curl -fsSLo /tmp/trivy.tar.gz \
      "https://github.com/aquasecurity/trivy/releases/download/v${TRIVY_VERSION}/trivy_${TRIVY_VERSION}_Linux-64bit.tar.gz" \
    && tar -xzf /tmp/trivy.tar.gz -C /usr/local/bin trivy \
    && chmod 0755 /usr/local/bin/trivy \
    && rm -f /tmp/trivy.tar.gz

USER jenkins