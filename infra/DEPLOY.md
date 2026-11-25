# Despliegue en AWS Academy (2 instancias EC2)

| Instancia | Tipo sugerido | Contenido | Compose |
|---|---|---|---|
| `pedidos-mensajeria` | t3.large (8 GB) | RabbitMQ (2 nodos), Kafka (3 brokers + 3 Zookeeper), Kafka-UI | `docker-compose.rabbitmq.yml`, `docker-compose.kafka.yml`, `docker-compose.kafka-ui.yml` |
| `pedidos-app` | t3.medium (4 GB) | PostgreSQL, backend, 3 microservicios, frontend, Caddy (HTTPS) | `docker-compose.app.yml` |

Learner Lab suele limitar los tipos a nano..large. Ambas con Ubuntu 22.04, 30 GB de disco.

## 1. Crear las instancias
1. EC2 → Launch instance (una por fila de la tabla), con tu key pair `vockey`.
2. **Elastic IP** a cada una (EC2 → Elastic IPs → Allocate → Associate). Sin esto, la IP cambia cuando el Lab se apaga.
3. Security groups (misma VPC):
   - `pedidos-app`: entrada 22 (tu IP), 80 y 443 (0.0.0.0/0), 8080 (0.0.0.0/0: lo llama el API Gateway), 8083 (tu IP).
   - `pedidos-mensajeria`: entrada 22 (tu IP), 5672-5673, 9092-9094 (solo desde el SG de `pedidos-app`), 15672-15673 y 8090 (tu IP, para mostrar las consolas).

## 2. Instalar Docker (en cada instancia)
```bash
sudo apt-get update && sudo apt-get install -y docker.io docker-compose-v2 git
sudo usermod -aG docker ubuntu && newgrp docker
```

## 3. Nodo de mensajería
```bash
git clone <repo-backend-pedidos360> && cd backend-pedidos360/infra
export KAFKA_EXTERNAL_HOST=<IP PRIVADA de esta instancia>
docker compose -f docker-compose.rabbitmq.yml up -d
docker compose -f docker-compose.kafka.yml up -d
docker compose -f docker-compose.kafka-ui.yml up -d
docker exec rabbit1 rabbitmqctl cluster_status        # deben verse rabbit1 y rabbit2
```
Kafka-UI: `http://<IP pública mensajería>:8090`.

## 4. Nodo de aplicación
```bash
# Clonar los 5 repos en la MISMA carpeta
git clone <repo-backend-pedidos360>; git clone <repo-ms-notificaciones>
git clone <repo-ms-rabbit-admin>; git clone <repo-ms-kafka-admin>; git clone <repo-frontend-pedidos360>
cd backend-pedidos360/infra
cp .env.example .env && nano .env      # MESSAGING_HOST, PUBLIC_HOST, API_BASE_URL, DB_PASSWORD
docker compose -f docker-compose.app.yml up -d --build
docker compose -f docker-compose.app.yml ps
```
`ms-kafka-admin` crea al iniciar los tópicos `pedidos.eventos` y `pedidos.estado` (3 particiones, 3 réplicas).

## 5. Azure AD
En el registro de la app del **frontend** (Authentication → Single-page application) agregar la URI de redirección `https://<PUBLIC_HOST>`.

## 6. API Gateway
Pendiente (siguiente etapa): HTTP API con JWT authorizer (issuer del tenant de Azure AD) apuntando a `http://<IP pública app>:8080`.
Después, poner la URL del gateway en `API_BASE_URL` y reconstruir el frontend: `docker compose -f docker-compose.app.yml up -d --build frontend`.

## Después de reiniciar el Learner Lab
Los contenedores tienen `restart: always`; si algo no sube: `docker compose -f <archivo> up -d` en cada instancia.
