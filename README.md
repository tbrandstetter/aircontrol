# Getting Started
The aircontrol service allows you to monitor and manage Drexel & Weiss home ventilation systems via REST (GUI comes in a later version). Aircontrol uses the USB service interface of the Drexel & Weiss devices in debug mode, so you don't need the additional modbus adapter. A list of registers can be found on the Drexel & Weiss [homepage](http://filter.drexel-weiss.at/HP/Upload/Dateien/900.6667_00_TI_Modbus_Parameter_V4.01_DE.pdf).

**Important: The service has been tested on my own ventilation system (Silent Stratos) but it should be clear that the Drexel & Weiss warranty doesn't cover damages for this case of usage. Use it at your own responsibility!**

## Changelog
0.1 Initial release

## Supported Devices

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

## Prerequisite on the device
For communication between the device and Aircontrol you have to connect an USB-Cable (USB Type B) to the service connector of the device. The location of the service connector can be different from device to device, so please make sure to read the documentation of your device, before you go on.

Additionally you have to set the "Serial Interface Operation Mode" to "Debug".

## Build
``git clone https://github.com/tbrandstetter/aircontrol.git``

``mvn clean``

``mvn package``

## Installation
Copy binary (in aircontrol-service/target/)

``cp aircontrol-service-0.0.1-SNAPSHOT.jar /usr/local/sbin/aircontrol-0.0.1.jar``

#### Create Symlink:

``ln -s /usr/local/sbin/aircontrol-service-0.0.1-SNAPSHOT.jar /usr/local/sbin/aircontrol.jar``

#### Create service file:
``vi aircontrol.service``
```
[Unit]
Description=Aircontrol
After=syslog.target

[Service]
User=root
ExecStart=/usr/local/sbin/aircontrol.jar
SuccessExitStatus=143

[Install]
WantedBy=multi-user.target
```

#### Copy configuration file to systemd directory
``cp aircontrol.service /etc/systemd/system/aircontrol.service``

``systemctl daemon-reload``

## Configuration
``vi /usr/local/sbin/application.properties``
```
# Default settings
spring.profiles.active=prod

# Modbus serial settings
modbus.baudrate=115200
modbus.port=/dev/ttyUSB0
modbus.retrycount=20
modbus.retrytimeout=3

# Database settings
spring.datasource.url=jdbc:h2:mem:testdb
spring.datasource.driverClassName=org.h2.Driver
spring.datasource.username=sa
spring.datasource.password=password
spring.jpa.database-platform=org.hibernate.dialect.H2Dialect
```
You only have to change the modbus.port!

### Run
``systemctl start aircontrol``

### Monitor
``journalctl -f ``

### Use
Register id's an`d description can be found in this [PDF](http://filter.drexel-weiss.at/HP/Upload/Dateien/900.6667_00_TI_Modbus_Parameter_V4.01_DE.pdf).

#### Use Swagger
``http://"ip of your system":8080/swagger-ui/index.html``

#### Get register value by id
``GET http://"ip of your system":8080/api/v1/registers/"id"``

#### Get all registers
``GET http://"ip of your system":8080/api/v1/registers``

#### Write register
``PUT http://"ip of your system":8080/api/v1/registers/"id"``

Payload in Body: ``{"value" : "value you want to change"}``

