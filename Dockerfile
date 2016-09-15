FROM clojure:alpine

RUN apk add --update git && \
    rm -rf /var/cache/apk

ARG git_commit=unknown
ARG version=unknown

LABEL org.cyverse.git-ref="$git_commit"
LABEL org.cyverse.version="$version"

COPY . /usr/src/app

WORKDIR /usr/src/app

RUN lein uberjar && \
    cp target/porklock-standalone.jar .

RUN ln -s "/usr/bin/java" "/bin/porklock"

ENTRYPOINT ["porklock", "-jar", "/usr/src/app/porklock-standalone.jar"]
CMD ["--help"]
