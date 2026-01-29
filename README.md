# argos-idps
An open source IDPS made as Computer Science graduation project at IMETRO

# Description
This software analyses internet traffic looking for invasion atempts.
It acts like a proxy for network devices and blocks any syspect traffic in the network.
An AI model is used to detect this kind of abnormal behaviour.

# Instalation

Before downloading the jar file, make sure **JRE 17** or above and **Python 3** is installed. Follow respective doccumentation to configure these in your system.

- Download the **.jar** file
- In the folder you downloaded it, execute `java -jar jarfile.jar`, this will start the IDPS on port **3964** by default.
- Configure your networks devices to use the machine you're running the IDPS as a SOCKS4 Proxy.

*Note: this project still in ealry stage of development, the internet performance can drop drastically, or simply stop working because the IDPS stopped due to an internal error. All feedback is wellcome, please open an issue*
