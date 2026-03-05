FROM mcr.microsoft.com/devcontainers/base:ubuntu-24.04

# Install JDK 17 (needed by sdkmanager and Gradle) and Python 3
RUN apt-get update \
    && apt-get install -y --no-install-recommends openjdk-17-jdk-headless python3 python3-pip python3-venv \
    && rm -rf /var/lib/apt/lists/*
ENV JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64

ARG ANDROID_HOME=/opt/android-sdk
ENV ANDROID_HOME=${ANDROID_HOME}
ENV PATH="${ANDROID_HOME}/cmdline-tools/latest/bin:${ANDROID_HOME}/platform-tools:${PATH}"

# Install Android command-line tools
ARG CMDLINE_TOOLS_URL=https://dl.google.com/android/repository/commandlinetools-linux-11076708_latest.zip
RUN mkdir -p ${ANDROID_HOME}/cmdline-tools \
    && curl -fsSL ${CMDLINE_TOOLS_URL} -o /tmp/cmdline-tools.zip \
    && unzip -q /tmp/cmdline-tools.zip -d /tmp/cmdline-tools \
    && mv /tmp/cmdline-tools/cmdline-tools ${ANDROID_HOME}/cmdline-tools/latest \
    && rm -rf /tmp/cmdline-tools.zip /tmp/cmdline-tools

# Accept licenses and install SDK components
RUN yes | sdkmanager --licenses > /dev/null 2>&1 \
    && sdkmanager --install \
        "platform-tools" \
        "platforms;android-35" \
        "build-tools;36.0.0"

# Make SDK writable by vscode user so Gradle/AGP can download extra components
RUN chown -R vscode:vscode ${ANDROID_HOME}

# Install postgresql-client
RUN apt-get update \
    && apt-get install -y --no-install-recommends postgresql-client \
    && rm -rf /var/lib/apt/lists/*

# Install API Python dependencies
COPY api/requirements.txt api/requirements-dev.txt /tmp/
RUN pip install --break-system-packages -r /tmp/requirements.txt -r /tmp/requirements-dev.txt \
    && rm /tmp/requirements*.txt

# Install Claude Code and add to PATH
RUN curl -fsSL https://claude.ai/install.sh | bash
ENV PATH="/root/.local/bin:${PATH}"
