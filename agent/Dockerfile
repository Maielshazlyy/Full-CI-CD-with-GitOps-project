FROM jenkins/inbound-agent:jdk21

USER root

ARG TRIVY_VERSION=0.76.1

RUN apt-get update \
    && apt-get install -y --no-install-recommends nodejs npm docker.io curl ca-certificates tar \
    && rm -rf /var/lib/apt/lists/*

RUN curl -fsSLo /tmp/trivy.tar.gz \
      "https://github.com/aquasecurity/trivy/releases/download/v${TRIVY_VERSION}/trivy_${TRIVY_VERSION}_Linux-64bit.tar.gz" \
    && tar -xzf /tmp/trivy.tar.gz -C /usr/local/bin trivy \
    && chmod 0755 /usr/local/bin/trivy \
    && rm -f /tmp/trivy.tar.gz

USER jenkins
