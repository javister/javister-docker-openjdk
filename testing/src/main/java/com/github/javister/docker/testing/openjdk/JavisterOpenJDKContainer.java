package com.github.javister.docker.testing.openjdk;

import com.github.dockerjava.api.command.InspectContainerResponse;
import com.github.javister.docker.testing.IllegalImageVariantException;
import com.github.javister.docker.testing.base.JavisterBaseContainerImpl;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.output.OutputFrame;
import org.testcontainers.containers.output.WaitingConsumer;

import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.concurrent.Future;
import java.util.concurrent.TimeoutException;

import static com.github.javister.docker.testing.base.JavisterBaseContainer.getImageRepository;
import static com.github.javister.docker.testing.base.JavisterBaseContainer.getImageTag;

/**
 * Обёртка над контейнером
 * <a href="https://github.com/javister/javister-docker-openjdk">
 * javister-docker-docker.bintray.io/javister/javister-docker-openjdk
 * </a>.
 *
 * <p>Образ данного контейнера содержит OpenJDK.
 *
 * @param <SELF> параметр, необходимый для организации паттерна fluent API.
 */
@SuppressWarnings({"squid:S00119", "UnusedReturnValue", "unused", "java:S2160"})
public class JavisterOpenJDKContainer<SELF extends JavisterOpenJDKContainer<SELF>> extends JavisterBaseContainerImpl<SELF> {
    private static final Logger LOGGER = LoggerFactory.getLogger(JavisterOpenJDKContainer.class);

    private final WaitingConsumer waitConsumer = new WaitingConsumer();
    private final Variant variant;
    private boolean doDebugWait;
    private boolean doDebug;
    private int debugPort = 8787;
    private boolean isCustomDebugPort;
    private StringBuilder jvmOptions = new StringBuilder();
    private StringBuilder toolOptions = new StringBuilder("-Djava.awt.headless=true -Djava.net.preferIPv4Stack=true");

    /**
     * Создаёт контейнер из образа
     * <a href="https://github.com/javister/javister-docker-openjdk">
     * javister-docker-docker.bintray.io/javister/javister-docker-openjdk
     * </a>.
     *
     * @param variant версия релиза OpenJDK
     */
    @SuppressWarnings("unchecked")
    public JavisterOpenJDKContainer(Variant variant) {
        super(
                getImageRepository(JavisterOpenJDKContainer.class, variant.getValue()),
                getImageTag(JavisterOpenJDKContainer.class, variant.getValue())
        );
        this.variant = variant;
        init();
    }

    /**
     * Создаёт контейнер с базой данных OpenJDK для JUnit тестирования.
     *
     * <p>Объект класса необходим для нахождения рабочего каталога тестов.
     *
     * @param variant   версия релиза OpenJDK
     * @param testClass класс JUnit теста для которого создаётся контейнер.
     */
    @SuppressWarnings("unchecked")
    public JavisterOpenJDKContainer(Variant variant, Class<?> testClass) {
        super(
                getImageRepository(JavisterOpenJDKContainer.class, variant.getValue()),
                getImageTag(JavisterOpenJDKContainer.class, variant.getValue()),
                testClass
        );
        this.variant = variant;
        init();
    }

    /**
     * Создаёт контейнер с базой данных OpenJDK для JUnit тестирования.
     *
     * <p>Объект класса необходим для нахождения рабочего каталога тестов.
     *
     * @param testClass класс JUnit теста для которого создаётся контейнер.
     */
    @SuppressWarnings("squid:S1699")
    public JavisterOpenJDKContainer(Class<?> testClass) {
        this(Variant.V8, testClass);
    }

    public JavisterOpenJDKContainer() {
        this(Variant.V8);
    }

    public JavisterOpenJDKContainer(Variant variant, String tag) {
        super(tag);
        this.variant = variant;
        init();
    }

    public JavisterOpenJDKContainer(Variant variant, String dockerImageName, String tag) {
        super(dockerImageName, tag);
        this.variant = variant;
        init();
    }

    public JavisterOpenJDKContainer(Variant variant, Class<?> testClass, Future<String> image) {
        super(testClass, image);
        this.variant = variant;
        init();
    }

    public JavisterOpenJDKContainer(Variant variant, String tag, Class<?> testClass) {
        super(tag, testClass);
        this.variant = variant;
        init();
    }

    public JavisterOpenJDKContainer(Variant variant, String dockerImageName, String tag, Class<?> testClass) {
        super(dockerImageName, tag, testClass);
        this.variant = variant;
        init();
    }

    @Nullable
    @Override
    public String getVariant() {
        return variant.getValue();
    }

    public SELF withJvmOptions(String options) {
        jvmOptions = new StringBuilder(options);
        return self();
    }

    public SELF addJvmOptions(String options) {
        jvmOptions.append(' ').append(options.trim());
        return self();
    }

    public SELF withToolOptions(String options) {
        toolOptions = new StringBuilder(options);
        return self();
    }

    public SELF addToolOptions(String options) {
        toolOptions.append(' ').append(options.trim());
        return self();
    }

    /**
     * Определяет, запущен ли текущий процесс JVM в режиме отладки.
     *
     * <p>Данный тест необходим, например, для определения того, что JUnit тест запущен под отладкой.
     * В таком случае и тестируемое приложение тоже будет запущено под отладкой.
     *
     * @return true, если у процесса влючена отладка и false в противном случае.
     */
    public boolean isInDebug() {
        RuntimeMXBean runtimeMxBean = ManagementFactory.getRuntimeMXBean();
        List<String> arguments = runtimeMxBean.getInputArguments();
        if (arguments.contains("-Xdebug")) {
            return true;
        }
        return arguments.stream().anyMatch(arg -> arg.contains("jdwp="));
    }

    /**
     * Задает размер кучи JVM при старте приложения.
     * <p>Примеры:
     * <pre>
     * 200m
     * 2g
     * </pre>
     *
     * @param xms размер кучи.
     * @return возвращает this для fluent API.
     */
    public final SELF withXMS(String xms) {
        this.withEnv("JAVA_XMS", xms);
        return self();
    }

    /**
     * Задаёт максимально разрешённый размер кучи JVM.
     * <p>Примеры:
     * <pre>
     * 200m
     * 2g
     * </pre>
     *
     * @param xmx размер кучи.
     * @return возвращает this для fluent API.
     */
    public final SELF withXMX(String xmx) {
        this.withEnv("JAVA_XMX", xmx);
        return self();
    }

    /**
     * Включает режим отладки для запускаемого приложения.
     *
     * <p>В JVM бкдкт переданы соответствующие флаги, а так же из контейнера будет открыт порт для подключения
     * отладчиком.
     *
     * @param debug true, если необходимо включить отладку и false (по умолчанию) в противном случае.
     * @return возвращает this для fluent API.
     */
    public SELF withDebug(boolean debug) {
        this.withEnv("DEBUG", Boolean.toString(debug));
        this.withExposedPorts(8787);
        doDebug = true;
        return self();
    }

    /**
     * Включает ожидание подключения отлядчика после старта JVM.
     *
     * <p>В силу того, что JUnit тесты запускаются сразу после старта приложения можно не успеть подключиться отладкой
     * и поймать интересующую ситуацию. В таком случае можно взвести данный флаг. Тогда JVM дождётся подключения
     * отладчика и только потом продолжит выполнение.
     *
     * @param debugSuspend true, если необходимо дожидаться подключение отладчика
     *                     и false (по умолчанию) в противном случае.
     * @return возвращает this для fluent API.
     */
    public SELF withDebugSuspend(boolean debugSuspend) {
        this.withEnv("DEBUG_SUSPEND", Boolean.toString(debugSuspend));
        doDebugWait = debugSuspend;
        return self();
    }

    /**
     * Устанавливает номер порта, который должен быть доступен снаружи контейнера для отладки.
     *
     * <p>По умолчанию для отладки выделяется случайный свободный порт. Но если задать эту опцию,
     * то номер порта зафиксируется на конкретный заданный.
     *
     * @param debugPort номер порта для подключения отладчика.
     * @return возвращает this для fluent API.
     */
    public SELF withDebugPort(int debugPort) {
        this.withExposedPorts(debugPort);
        this.debugPort = debugPort;
        isCustomDebugPort = true;
        return self();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        JavisterOpenJDKContainer<?> that = (JavisterOpenJDKContainer<?>) o;

        if (doDebugWait != that.doDebugWait) return false;
        if (debugPort != that.debugPort) return false;
        if (isCustomDebugPort != that.isCustomDebugPort) return false;
        return variant == that.variant;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + variant.hashCode();
        result = 31 * result + (doDebugWait ? 1 : 0);
        result = 31 * result + debugPort;
        result = 31 * result + (isCustomDebugPort ? 1 : 0);
        return result;
    }

    @Override
    protected void configure() {
        super.configure();
        this
                .withEnv("JVM_OPTS", jvmOptions.toString())
                .withEnv("JAVA_TOOL_OPTIONS", toolOptions.toString());
        if (isInDebug()) {
            if (isCustomDebugPort) {
                this.addFixedExposedPort(debugPort, 8787);
            } else {
                this.withExposedPorts(8787);
            }
        }
    }

    @Override
    protected void containerIsStarting(InspectContainerResponse containerInfo) {
        if (doDebug) {
            Integer dPort = this.getMappedPort(8787);
            if (dPort == null && isCustomDebugPort) {
                dPort = debugPort;
            }
            String address = this.getContainerIpAddress();
            if (dPort != null) {
                if (doDebugWait) {
                    try {
                        waitConsumer.waitUntil((OutputFrame frame) ->
                                frame.getUtf8String().contains("Listening for transport dt_socket at address"));
                    } catch (TimeoutException e) {
                        LOGGER.debug("Timed out of container startup", e);
                    }
                }
                String debugPortMessage = "\n"
                        + "****************************************************\n"
                        + "******  DEBUG ADDRESS: " + address + ":" + dPort + " *************\n"
                        + "****************************************************\n";
                logger().info(debugPortMessage);
            }
        }
    }

    private void init() {
        if (isInDebug()) {
            this
                    .withDebug(true)
                    .withStartupTimeout(Duration.of(10, ChronoUnit.MINUTES))
                    .withLogConsumer(waitConsumer);

            String debugSuspend = System.getenv("DOCKER_WILDFLY_DEBUG_SUSPEND");
            if (debugSuspend != null) {
                this.withDebugSuspend(Boolean.parseBoolean(debugSuspend));
            }

            String debugPortEnv = System.getenv("DOCKER_WILDFLY_DEBUG_PORT");
            if (debugPortEnv != null) {
                this.withDebugPort(Integer.parseInt(debugPortEnv));
            }
        } else {
            this.addJvmOptions("-XX:+UseShenandoahGC "
                    + "-XX:ShenandoahGCHeuristics=compact "
                    + "-XX:+UnlockExperimentalVMOptions "
                    + "-XX:ShenandoahGuaranteedGCInterval=20000 "
                    + "-XX:ShenandoahUncommitDelay=10000");
        }
    }

    /**
     * Варианты OpenJDK, соответствующие её мажорным версиям.
     */
    public enum Variant {
        V8("8"),
        V11("11");
        private final String value;

        Variant(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }

        @SuppressWarnings("unchecked")
        public static Variant get(String value) {
            switch (value) {
                case "8":
                    return V8;
                case "11":
                    return V11;
                default:
                    throw new IllegalImageVariantException(
                            "Unexpected Docker variant " +
                                    value +
                                    " for image " +
                                    getImageRepository(JavisterOpenJDKContainer.class, V8.getValue()));
            }
        }
    }
}
