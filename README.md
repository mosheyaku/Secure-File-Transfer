# 🔐 Secure File Transfer (Client-Server Project)

A cross-language secure file transfer application built with **Java** (Client) and **Python** (Server).  
This project enables encrypted file exchange over a network using **RSA**, **AES**, and **CRC** mechanisms  
to ensure confidentiality and integrity.
<br><br> 


## 📦 Features

🔑 **RSA**: Secure key exchange  
🔒 **AES**: Symmetric encryption for file contents  
✅ **CRC**: File integrity check using CRC32  
🔁 Reliable retry mechanism  
💬 Simple request/response protocol  
🧰 Built using **Maven** for Java client management
<br><br>


## 📁 Project Structure

- server/ # Python server code
- client/ # Java Maven project (client-side)
<br><br>


## ⚙️ Configuration Files

The project includes two configuration files:

📄 **`transfer.info`**  
  Contains the server IP and port, client name, and the file name to send.

📄 **`port.info`**  
  Specifies the port number the server listens on.

---

#### Example contents:

```text
transfer.info:
127.0.0.1:8888
Moshe
Screenshot1.png

port.info:
8888
```
---

You can use these template files as-is or modify their contents as needed to fit your environment.  
🔔 Important: The port in transfer.info must match the port in port.info for the client and server to connect correctly.
<br><br> 


## 🚀 How to Run

### ✅ Prerequisites

- Java 8+ installed
- Maven installed
- Python 3.6+


### 🖥️ Start the Server

```bash
cd server
python main.py
```


### 🖥️ Start the Client
```bash
cd client/client-project
mvn clean compile exec:java -Dexec.mainClass="com.moshe.client.Main"
```
<br><br>


## 🛠️ Technologies Used
- Java – Client logic & networking
- Python – Server logic & file handling
- Maven – Build & dependency management
- RSA/AES – Encryption
- CRC32 – File integrity check
- Socket Programming – Client-server communication
