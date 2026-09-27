# Argos IDPS

An open source IDPS made as Computer Science graduation project at IMETRO.

# Description

**Argos** is an open source **Intrusion Detection and Prevention System** developed in Kotlin and Python.

The project has two main modules:

- **Argos Core:** The module responsible for capturing network traffic, organize the traffic in flows, save captured packages for auditing, block malicious traffic and notify through email.

- **Argos Analyst:** This module contains the two Machine Learning models responsible to detect and classify anomalies. This module is the core decision maker for every package.

Both modules run in different processes and communicate via TCP sockets in the loopback interface in the port `3469`.

# Instalation and Usage

## System Requirements

This project was developed and tested on **Fedora Workstation 42**, but it's expected to work on any Linux distro with the following dependencies:

- Libpcap
- JDK 17 (for those who want to build the project)
- JRE 17 (for those who just want to run the JAR file)
- Python 3

## Building the project from source code

Clone the repository

`git clone https://github.com/tesla-j/argos-idps`

Go to the following folder

`cd argos-idps/Argos_IDPS/`

Then build with Gradle

`./gradlew build`

Wait until the project finishes building.

## Running the project

You can download the latest JAR from [releases](https://github.com/Tesla-J/argos-idps/releases). If you compiled the source code, you can find the JAR in the `argos-idps/Argos_IDPS/build/libs/` folder.

Argos requires root permission to operate, by default it caputes packages in the loopback interface. All captured traffic is stored in a capture file in the `/var/log/argos/` folder.

## Configuring Argos

To configure argos, changes can be done in `/etc/argos/argos.conf` file. If not exists, Argos generates it with default settings. The default settings in the file are:

```
nif_addr=127.0.0.1
nif_netmask=255.0.0.0
admin_email=please@change.me
smtp_host=smtp.domain.com
smtp_port=25
smtp_username=argos@domain.com
smtp_passwd=secretpassword
```

`nif_addr` and `nif_netmask` must match the IP and net mask of the interface argos will capture the traffic.

`admin_email` is the email Argos will send alerts when an anomaly is detected. The email will also contain the blocked IP and the name of the capture file for auditing. Softwares what support `.pacap` files (eg: Wireshark) can be used to read the content.

`smtp_host` and `smtp_port` refers to the host and port of the SMTP server from which Argos will the the `smtp_username` username and `smtp_passwd` password to login and send alerts.
