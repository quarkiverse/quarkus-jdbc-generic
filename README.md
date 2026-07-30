# Quarkus JDBC Generic

Connect a Quarkus Agroal datasource to any database via a JDBC driver resolved with reflection **at runtime**.

The JDBC driver of an Agroal datasource is normally fixed at build time: it is either derived from
`db-kind` by a `quarkus-jdbc-*` extension or set explicitly with the build-time property
`quarkus.datasource.jdbc.driver` (`db-kind=other`). This prevents shipping a single build artifact
that is pointed at different databases per deployment, where the driver can only be decided at
startup, like the JDBC URL or the pool sizing already can.

Typical use cases (coming from the Apache Polaris needs):

* Resolve a driver in JVM mode which is not provided in the default image (think a Pod with an init
  container copying the driver into the classpath using an `emptyDir` volume or alike), for example
  because the license of the driver forbids providing it out of the box.
* Use the same image and switch the driver depending on the environment/customer (standard Java JDBC
  usage).

## Usage

```xml
<dependency>
    <groupId>io.github.rmannibucau</groupId>
    <artifactId>quarkus-jdbc-generic</artifactId>
    <version>${version}</version>
</dependency>
```

```properties
# build time: selects the runtime-resolved driver support
quarkus.datasource.db-kind=generic
# runtime: the driver class, loaded with reflection from the application classpath
quarkus.datasource.jdbc-generic.driver=oracle.jdbc.driver.OracleDriver
# the rest of the datasource configuration is unchanged
quarkus.datasource.jdbc.url=jdbc:oracle:thin:@192.168.1.12:1521/ORCL_SVC
quarkus.datasource.username=scott
quarkus.datasource.password=tiger
```

Named datasources work the same way with `quarkus.datasource."name".jdbc-generic.driver`.

Depending on the actual type of the configured class (`java.sql.Driver`, `javax.sql.DataSource`, or
`javax.sql.XADataSource` when `quarkus.datasource.jdbc.transactions=xa`), the matching Agroal
connection provider implementation is used. The driver JAR must be part of the application
classpath; only the *choice* of the driver class is deferred to runtime.

## Native executables

The closed-world assumption still applies: candidate drivers must be in the classpath at image build
time and only the class choice is deferred.

* When the `jdbc-generic.driver` property is visible at build time (e.g. `application.properties`),
  the configured class is registered for reflection automatically.
* `java.sql.Driver` service providers of the driver JAR are registered automatically in all cases.
* Otherwise (property only known at runtime and the class is a `DataSource`/`XADataSource`), register
  the candidate classes with `@RegisterForReflection(targets = ...)`.

Note that reflection registration is often not enough for a JDBC driver in native mode (runtime
initialization, resources, JNI); prefer a dedicated `quarkus-jdbc-*` extension for native builds.

## How it works

The extension registers the `generic` database kind with build-time placeholder driver classes that
satisfy the build-time validation of the Agroal extension. At runtime, before the datasource is
created, the placeholder entry recorded in `AgroalDataSourceSupport` is substituted with the
configured driver class:

1. eagerly at runtime init (fail fast: missing property, class not loadable or of the wrong type
   abort the boot with an actionable error), and
2. lazily from an inert `java.sql.Driver` service provider instantiated by Agroal right before it
   reads the driver class of the datasource it creates, which makes the substitution independent of
   the startup ordering of datasource consumers such as Flyway or Liquibase.

> [!NOTE]
> This standalone extension is the Quarkiverse-style port of the upstream proposal
> [quarkusio/quarkus#55677](https://github.com/quarkusio/quarkus/pull/55677), which adds a proper
> `AgroalDriverResolver` SPI to `quarkus-agroal`. Since a third-party extension cannot modify
> `quarkus-agroal`, this port achieves the same behavior by relying on Agroal runtime internals
> (`AgroalDataSourceSupport`); it is therefore coupled to the Quarkus version it is built and tested
> against (see the `quarkus.version` property, currently 3.38.0).

## Building

```bash
mvn verify
```

Modules: `runtime` (the extension), `deployment` (build steps + `QuarkusExtensionTest` tests),
`integration-tests` (`@QuarkusTest` against the packaged extension), `docs`.

## License

[Apache License 2.0](LICENSE)
