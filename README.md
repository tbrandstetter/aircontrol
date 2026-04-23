# Aircontrol

Aircontrol monitors and controls Drexel & Weiss ventilation systems over the service USB interface. It talks to the device in debug mode, exposes a small REST API, keeps recent register values in a local cache, and reconnects automatically if the serial link goes stale or drops.

**Important:** This project talks directly to the device's service interface. It is tested on a private installation and is used at your own risk.

## Current state

- Java 21
- Spring Boot 3.4.x
- Cache-first register reads with background refresh
- Connection status endpoint and stale/reconnect detection
- Designed for trusted private LAN deployments

## Supported devices

- Aerosilent Bianco
- Aerosilent Business
- Aerosilent Centro
- Aerosilent Exos
- Aerosilent Micro
- Aerosilent Primus
- Aerosilent Stratos
- Aerosilent Topo
- Aerosmart L
- Aerosmart M
- Aerosmart S
- Aerosmart Mono
- Aerosmart XLS
- Termosmart sc: 9
- X²
- X² Plus

## Device prerequisites

Before using Aircontrol:

1. Connect a USB Type-B cable to the device service connector.
2. Enable `Debug` for the serial interface operation mode on the device.
3. Identify the correct serial port on the host, usually `/dev/ttyUSB0`.

The exact connector location depends on the device model, so check the device documentation first.

## Build

```bash
git clone https://github.com/tbrandstetter/aircontrol.git
cd aircontrol
./mvnw clean package
```

The packaged application is created in `target/`, for example:

```bash
target/aircontrol-0.9.0-SNAPSHOT.jar
```

## Configuration

For production-style deployment, provide configuration outside the jar and override the default local-dev profile.

Example `application.properties`:

```properties
spring.profiles.active=prod

modbus.baudrate=115200
modbus.port=/dev/ttyUSB0
modbus.retrycount=20
modbus.retrytimeout=3
modbus.updaterange=240
modbus.stale-timeout=2m
modbus.reconnect-delay=5s
modbus.stale-check-delay=10s

spring.datasource.url=jdbc:h2:file:/var/lib/aircontrol/aircontrol
spring.datasource.driverClassName=org.h2.Driver
spring.datasource.username=sa
spring.datasource.password=password
spring.jpa.open-in-view=false

duw.deviceregognition=true
duw.devicetype=17
```

If you already know your device type and do not want automatic detection, set:

```properties
duw.deviceregognition=false
duw.devicetype=17
```

## Installation

Example installation to `/usr/local/sbin`:

```bash
cp target/aircontrol-0.9.0-SNAPSHOT.jar /usr/local/sbin/aircontrol-0.9.0.jar
ln -sf /usr/local/sbin/aircontrol-0.9.0.jar /usr/local/sbin/aircontrol.jar
```

Example `systemd` unit:

```ini
[Unit]
Description=Aircontrol
After=network.target

[Service]
User=root
ExecStart=/usr/bin/java -jar /usr/local/sbin/aircontrol.jar --spring.config.location=file:/usr/local/sbin/application.properties
SuccessExitStatus=143
Restart=on-failure

[Install]
WantedBy=multi-user.target
```

Then:

```bash
cp aircontrol.service /etc/systemd/system/aircontrol.service
systemctl daemon-reload
systemctl enable --now aircontrol
```

## Security model

Aircontrol is currently intended for **trusted private LANs only**.

- The REST API is intentionally open inside that network.
- `PUT` requests do not require a CSRF token.
- If you ever expose the service outside a trusted LAN, put authentication in front of it first.

## Monitoring

```bash
journalctl -u aircontrol -f
```

## Register reference

Drexel & Weiss register descriptions can be found in this PDF:

[Modbus parameter list](http://filter.drexel-weiss.at/HP/Upload/Dateien/900.6667_00_TI_Modbus_Parameter_V4.01_DE.pdf)

## REST API

### List cached registers

```http
GET /api/v1/registers
```

### Read one cached register

```http
GET /api/v1/registers/{id}
```

Returns the cached value if available. If the value is missing or stale, Aircontrol schedules a background refresh instead of blocking the request path.

### Read one register with connection and freshness state

```http
GET /api/v1/registers/{id}/status
```

### Read connection state

```http
GET /api/v1/connection
```

Example response fields:

- `state`
- `port`
- `lastDataAt`
- `staleTimeout`
- `reconnectAttempts`
- `lastError`

### Write a register

```http
PUT /api/v1/registers/{id}
Content-Type: application/json
```

Payload:

```json
{"value":"3"}
```

Response:

- `200 OK` with body `true` if the write was accepted
- `409 Conflict` with body `false` if the connection is not ready or the register is unsupported for the detected device

## Notes

- Swagger UI is not part of the current build.
- The H2 console is only intended for local development.
