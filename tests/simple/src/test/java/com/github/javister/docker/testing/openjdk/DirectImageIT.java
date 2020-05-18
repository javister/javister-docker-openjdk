package com.github.javister.docker.testing.openjdk;

import com.github.dockerjava.api.command.InspectContainerResponse;
import com.github.javister.docker.testing.base.JavisterBaseContainer;
import com.github.javister.docker.testing.openjdk.JavisterOpenJDKContainer.Variant;
import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockserver.client.MockServerClient;
import org.mockserver.mock.Expectation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.MockServerContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.containers.output.WaitingConsumer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.images.builder.ImageFromDockerfile;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.io.File;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static com.github.javister.docker.testing.base.JavisterBaseContainer.getImageName;
import static org.mockserver.model.HttpRequest.request;
import static org.testcontainers.containers.output.OutputFrame.OutputType.STDOUT;

@Testcontainers
class DirectImageIT {
    private static final Logger LOGGER = LoggerFactory.getLogger(DirectImageIT.class);
    private static final Network externalNetwork = Network.newNetwork();
    private static final Network internalNetwork = Network.newNetwork();

    @ParameterizedTest(name = "OpenJDK release: {0}")
    @EnumSource(Variant.class)
    void proxyRequestTest(Variant variant) throws TimeoutException {
        try (Stand stand = new Stand(variant, DirectImageIT.class)) {
            stand.start();

            WaitingConsumer consumer = new WaitingConsumer();
            stand.getClient().followOutput(consumer, STDOUT);
            consumer.waitUntil(frame ->
                    frame.getUtf8String().contains("Hello, world!"), 15, TimeUnit.SECONDS);

            MockServerContainer proxyServer = stand.getProxyServer();
            MockServerClient proxyClient = new MockServerClient(proxyServer.getContainerIpAddress(), proxyServer.getMappedPort(1080));
            Expectation[] expectations = proxyClient.retrieveRecordedExpectations(request("/"));
            Assertions.assertEquals("Hello, world!", expectations[0].getHttpResponse().getBodyAsString());
            Assertions.assertFalse(expectations[0].getHttpRequest().containsHeader("Proxy-Authorization"), "Авторизация на прокси должна отсутствовать");
        }
    }

    @ParameterizedTest(name = "OpenJDK release: {0}")
    @EnumSource(Variant.class)
    void debugTest(Variant variant) {
        try (JavisterOpenJDKContainer<?> mserver = getMserver(variant, DirectImageIT.class).withDebug(true)) {
            mserver.start();

            String logs = mserver.getLogs(STDOUT);
            MatcherAssert.assertThat(
                    "Java приложение должно быть запущено под отладкой",
                    logs.contains("-Xdebug -Xrunjdwp:transport=dt_socket,address=8787,server=y,suspend=n") ||
                            logs.contains("-agentlib:jdwp=transport=dt_socket,server=y,address=*:8787,suspend=n")
            );
        }
    }

    public static class Stand implements AutoCloseable {
        private final JavisterOpenJDKContainer<?> mserver;
        private final MockServerContainer proxyServer;
        private final JavisterOpenJDKContainer<?> client;

        public Stand(Variant variant, Class<?> testClass) {
            mserver = DirectImageIT.getMserver(variant, testClass);

            proxyServer = new MockServerContainer("5.10.0") {
                // Workaround for https://github.com/moby/moby/issues/40740
                @Override
                protected void containerIsStarting(InspectContainerResponse containerInfo) {
                    getDockerClient().connectToNetworkCmd()
                            .withContainerId(getContainerId())
                            .withNetworkId(externalNetwork.getId())
                            .exec();
                    super.containerIsStarting(containerInfo);
                }

                @Override
                public InspectContainerResponse getContainerInfo() {
                    return getCurrentContainerInfo();
                }
            }
                    .dependsOn(mserver)
                    .withNetworkAliases("proxy")
                    .withNetwork(internalNetwork)
                    .withExposedPorts(1080)
                    .waitingFor(Wait.forHttp("/").forPort(1080).forStatusCode(404))
                    .withLogConsumer(new Slf4jLogConsumer(LOGGER).withPrefix("proxy").withRemoveAnsiCodes(false));

            client = new JavisterOpenJDKContainer<>(
                    variant,
                    DirectImageIT.class,
                    new ImageFromDockerfile()
                            .withFileFromFile(
                                    "simple.jar",
                                    new File(JavisterBaseContainer.getTestPath(DirectImageIT.class) + "/simple.jar")
                            )
                            .withDockerfileFromBuilder(builder ->
                                    builder
                                            .from(getImageName(JavisterOpenJDKContainer.class, variant.getValue()))
                                            .add("simple.jar", "/simple.jar")
                                            .build()
                            )
            )
                    .withHttpProxy("http://proxy:1080")
                    .withNoProxy("")
                    .withImagePullPolicy(__ -> false)
                    .withNetwork(internalNetwork)
                    .dependsOn(proxyServer)
                    .withCommand(
                            "my_init",
                            "--skip-runit",
                            "--",
                            "bash",
                            "-c",
                            "java -cp /simple.jar ${JVM_OPTS} com.github.javister.docker.testing.openjdk.GetUrl http://mserver:8080/"
                    );
        }

        public JavisterOpenJDKContainer<?> getMserver() {
            return mserver;
        }

        public MockServerContainer getProxyServer() {
            return proxyServer;
        }

        public JavisterOpenJDKContainer<?> getClient() {
            return client;
        }

        public void start() {
            mserver.start();
            proxyServer.start();
            client.start();
        }

        @Override
        public void close() {
            client.close();
            proxyServer.close();
            mserver.close();
        }
    }

    public static JavisterOpenJDKContainer<?> getMserver(Variant variant, Class<?> testClass) {
        return new JavisterOpenJDKContainer<>(
                variant,
                testClass,
                new ImageFromDockerfile()
                        .withFileFromFile(
                                "app.jar",
                                new File(JavisterBaseContainer.getTestPath(DirectImageIT.class) + "/openjdk-test-app.jar")
                        )
                        .withFileFromString(
                                "run",
                                "#!/bin/sh\necho \"JVM_OPTS=${JVM_OPTS}\"\nexec setuser system java -jar /app.jar"
                        )
                        .withDockerfileFromBuilder(builder ->
                                builder
                                        .from(getImageName(JavisterOpenJDKContainer.class, variant.getValue()))
                                        .add("app.jar", "/app.jar")
                                        .add("run", "/etc/service/app/run")
                                        .run("mkdir", "/etc/service/app/tmp")
                                        .run("chmod", "a+w+x", "/etc/service/app/run")
                                        .expose(8080)
                                        .build()
                        )
        )
                .withExposedPorts(8080)
                .withNetworkAliases("mserver")
                .withNetwork(externalNetwork)
                .withLogPrefix("mserver")
                .waitingFor(Wait.forHttp("/").forPort(8080));
    }
}
