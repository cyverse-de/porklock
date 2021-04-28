FROM clojure:openjdk-17-lein-alpine

WORKDIR /usr/src/app

RUN apk add --no-cache git

RUN ln -s "/opt/openjdk-17/bin/java" "/bin/porklock"

COPY project.clj /usr/src/app/
RUN lein deps

COPY . /usr/src/app

RUN lein uberjar && \
    cp target/porklock-standalone.jar .

ENTRYPOINT ["porklock", "-jar", "/usr/src/app/porklock-standalone.jar"]
CMD ["--help"]

ARG git_commit=unknown
ARG version=unknown
ARG descriptive_version=unknown

LABEL org.cyverse.git-ref="$git_commit"
LABEL org.cyverse.version="$version"
LABEL org.cyverse.descriptive-version="$descriptive_version"
LABEL org.label-schema.vcs-ref="$git_commit"
LABEL org.label-schema.vcs-url="https://github.com/cyverse-de/porklock"
LABEL org.label-schema.version="$descriptive_version"
