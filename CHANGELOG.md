# Changelog

Formato basado en [Keep a Changelog](https://keepachangelog.com/es-ES/1.1.0/) y versionado [SemVer](https://semver.org/lang/es/).

## [Unreleased]

### Agregado
- Persistencia en PostgreSQL configurable por variables de entorno (`DB_URL`, `DB_USERNAME`, `DB_PASSWORD`).
- Topología RabbitMQ: exchanges `topic` y `direct`, 3 colas con su DLQ, nombres centralizados en `application.yml`.
- Productor Kafka (`pedidos.eventos`, `pedidos.estado`) con DTO tipado, `acks=all` e idempotencia.
- Dockerfile y carpeta `infra/`: clúster RabbitMQ de 2 nodos, Kafka con 3 brokers, Kafka-UI y compose de aplicación.

### Cambiado
- H2 queda solo para tests (perfil `test`); se elimina la consola H2.
- Los IDs de Azure AD se leen de variables de entorno con valores por defecto.
