# eudiw-verifier-demo / Digdir demo brukarstad

> [!NOTE]
> This application is part of the National Sandbox for Digital Wallet.
> See https://docs.digdir.no/docs/lommebok/lommebok_om.html for more information.


Digdir demo brukarstad for verification of verifiable credentials for the digital wallet. This application is built to explore the [OpenID for Verifiable Presentations](https://openid.net/specs/openid-4-verifiable-presentations-1_0.html) specification, but it is not a complete implementation of the full specification.

## Requirements

* Java 25
* Redis

> [!WARNING]
> Access to Digitaliseringsdirektoratet infrastructure is required to run the application.

## Running the application locally

### Maven (dev profile)

The local hosts file should include:
```
127.0.0.1 abr.vc.local
```

Start Redis locally by running Docker compose (see below).

The application can be started with Maven:

```
docker-compose up redis
```

Then run the application from IntelliJ (with `dev` profile) or with Maven:

```
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

The application will run on https://abr.vc.local:8080/ with the `dev` profile.

### Docker (docker profile)
This setup is currently not fully working for local development...

The application can be started with Docker compose:
```
docker compose up --build
```

The application will run on https://abr.vc.local:8082 with the `docker` profile.
