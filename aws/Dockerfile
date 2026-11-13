# Custom CodeBuild image — Quest Camera Suite
#
# Amazon Linux 2023 + JDK 17 + Android SDK 34 + Gradle 8.9, all baked in.
# Build once and push to ECR so CodeBuild never re-downloads the SDK:
#
#   docker build -t quest-cam-build aws/
#   docker tag quest-cam-build <account>.dkr.ecr.<region>.amazonaws.com/quest-cam-build:latest
#   docker push <account>.dkr.ecr.<region>.amazonaws.com/quest-cam-build:latest
#
# Then reference the ECR image in the CodeBuild project settings.

FROM amazonlinux:2023

RUN dnf install -y java-17-amazon-corretto-headless unzip curl git && dnf clean all

ENV JAVA_HOME=/usr/lib/jvm/java-17-amazon-corretto.x86_64
ENV ANDROID_HOME=/opt/android-sdk
ENV ANDROID_SDK_ROOT=/opt/android-sdk
ENV PATH=$PATH:$ANDROID_HOME/cmdline-tools/latest/bin:$ANDROID_HOME/platform-tools

# Android command-line tools
RUN mkdir -p $ANDROID_HOME/cmdline-tools && \
    cd $ANDROID_HOME/cmdline-tools && \
    curl -sSLO https://dl.google.com/android/repository/commandlinetools-linux-11076708_latest.zip && \
    unzip -q commandlinetools-linux-11076708_latest.zip && \
    mv cmdline-tools-linux latest && \
    rm commandlinetools-linux-11076708_latest.zip

# SDK packages (licenses accepted at image build time, not per build)
RUN yes | sdkmanager --licenses > /dev/null && \
    sdkmanager "platforms;android-34" "build-tools;34.0.0" "platform-tools"

# Gradle (matches the wrapper version used by GitHub Actions CI)
RUN curl -sSLO https://services.gradle.org/distributions/gradle-8.9-bin.zip && \
    unzip -q gradle-8.9-bin.zip -d /opt && \
    ln -s /opt/gradle-8.9/bin/gradle /usr/local/bin/gradle && \
    rm gradle-8.9-bin.zip

CMD ["gradle", "--version"]
