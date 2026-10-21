FROM eclipse-temurin:21-jdk-noble

ARG CMDLINE_TOOLS=16111833
ARG ANDROID_PLATFORM=android-37.0
ARG BUILD_TOOLS=36.0.0
ARG KTLINT=1.8.0
ARG DETEKT=1.23.8

ENV ANDROID_HOME=/opt/android-sdk
ENV PATH=${PATH}:/opt/android-sdk/cmdline-tools/latest/bin:/opt/android-sdk/platform-tools

RUN apt-get update \
    && apt-get install -y --no-install-recommends unzip git \
    && rm -rf /var/lib/apt/lists/*

RUN mkdir -p ${ANDROID_HOME}/cmdline-tools \
    && curl -fsSL -o /opt/cmdline-tools.zip https://dl.google.com/android/repository/commandlinetools-linux-${CMDLINE_TOOLS}_latest.zip \
    && unzip -q /opt/cmdline-tools.zip -d ${ANDROID_HOME}/cmdline-tools \
    && mv ${ANDROID_HOME}/cmdline-tools/cmdline-tools ${ANDROID_HOME}/cmdline-tools/latest \
    && rm /opt/cmdline-tools.zip

RUN yes | sdkmanager --licenses > /dev/null \
    && sdkmanager "platforms;${ANDROID_PLATFORM}" "build-tools;${BUILD_TOOLS}" "platform-tools" > /dev/null

RUN curl -fsSL -o /usr/local/bin/ktlint https://github.com/pinterest/ktlint/releases/download/${KTLINT}/ktlint \
    && chmod +x /usr/local/bin/ktlint

RUN curl -fsSL -o /opt/detekt-cli.jar https://github.com/detekt/detekt/releases/download/v${DETEKT}/detekt-cli-${DETEKT}-all.jar

WORKDIR /workspace
