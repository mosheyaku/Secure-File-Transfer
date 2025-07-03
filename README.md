# 🔐 Secure File Transfer (Client-Server Project)

A cross-language secure file transfer application built with **Java** (Client) and **Python** (Server). This project enables encrypted file exchange over a network using **RSA**, **AES**, and **CRC** mechanisms to ensure confidentiality and integrity.



## 📦 Features

- 🔑 **RSA**: Secure key exchange
- 🔒 **AES**: Symmetric encryption for file contents
- ✅ **CRC**: File integrity check using CRC32
- 🔁 Reliable retry mechanism
- 💬 Simple request/response protocol
- 🧰 Built using **Maven** for Java client management



## 📁 Project Structure

- server/ # Python server code
- client/ # Java Maven project (client-side)



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
mvn clean compile
mvn exec:java -Dexec.mainClass="com.moshe.client.Main"
```

## 🛠️ Technologies Used
- Java – Client logic & networking
- Python – Server logic & file handling
- Maven – Build & dependency management
- RSA/AES – Encryption
- CRC32 – File integrity check
- Socket Programming – Client-server communication
