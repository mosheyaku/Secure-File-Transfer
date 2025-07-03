import binascii
import sqlite3
import struct
import shortuuid

from crypto_utils import *
from db_utils import *
from pakcet_header import *


class Server:
    def __init__(self, sock):
        self.client_id = ""
        self.client_name = ""
        self.public_key = ""
        self.aes_key = ""
        self.file_id = ""
        self.file_name = ""

        self.db_connection = sqlite3.connect(DB_FILE_NAME)
        create_tables(self.db_connection)

        self.retry = False
        self.socket = sock

    def register_client(self, request_header, request_payload):
        """
        This method handles a client's registration request:
        It checks if the client name already exists in the database,
        and if not-generates a new client ID, stores it, and sends
        a success response. If it already exists, it sends a failure response.
        """
        respond = RespondHeader()

        name = request_payload.decode("utf-8").replace('\x00', '')
        exists = is_exist_client(self.db_connection, name)
        registered = False

        if exists:
            self.client_id, self.client_name, self.public_key, last_seen, self.aes_key \
                = get_client(self.db_connection, name)
            registered = True

        if registered:
            respond.code = RESPOND_REGISTER_FAIL
            self.socket.send(respond.pack_to_bytes())
            print("[REGISTER] : Fail - Client already registered.")
            return False
        else:
            client_id = shortuuid.ShortUUID().random(length=16)

            self.client_id = client_id
            self.client_name = name
            add_client(self.db_connection, client_id, name, "", "")

            respond.code = RESPOND_REGISTER_SUCCESS
            respond.payload_size = 16
            self.socket.send(respond.pack_to_bytes())
            self.socket.send(self.client_id.encode())

            print("[REGISTER] : Success.")
            return True

    def share_key(self, request_header, request_payload):
        """
        Receives the client's RSA public key, generates an AES key, encrypts it
        using the public key, stores both in the database, and sends the encrypted
        AES key back to the client.
        """
        respond = RespondHeader()
        name = request_payload[0:255].decode("utf-8").replace('\x00', '')
        pubkey = request_payload[255:].decode("utf-8").replace('\x00', '')

        self.aes_key = generate_aes_key()
        self.public_key = pubkey

        update_client(self.db_connection, self.client_id, self.client_name,
                      self.public_key, self.aes_key)

        encrypted_key = rsa_encrypt(pubkey, self.aes_key)

        respond_payload = self.client_id.encode()
        respond_payload += encrypted_key

        respond.code = RESPOND_KEY_SHARE
        respond.payload_size = len(respond_payload)

        self.socket.send(respond.pack_to_bytes())
        self.socket.send(respond_payload)

        print("[SHARE KEY] : Success. Aes key sent.")
        return True

    def login(self, request_header, request_payload):
        """
        Checks if the client exists in the database by name. If yes, generates a new
        AES key, updates the database, encrypts the AES key with the stored public key,
        and sends it back. Otherwise, sends a login rejection.
        """
        name = request_payload.decode("utf-8").replace('\x00', '')

        exists = is_exist_client(self.db_connection, name)
        registered = True

        if exists:
            self.client_id, self.client_name, self.public_key, last_seen, self.aes_key \
                = get_client(self.db_connection, name)
        else:
            registered = False

        respond = RespondHeader()
        if not registered:
            respond.code = RESPOND_LOGIN_REJECTED
            respond.payload_size = 16
            self.socket.send(respond.pack_to_bytes())
            self.socket.send(request_header.id.encode())

            print("[LOGIN] : Fail. Client not registered.")
            return False
        else:
            respond.code = RESPOND_LOGIN_CONFIRMED
            self.aes_key = generate_aes_key()

            update_client(self.db_connection, self.client_id, self.client_name,
                          self.public_key, self.aes_key)

            encrypted_key = rsa_encrypt(self.public_key, self.aes_key)

            respond_payload = self.client_id.encode()
            respond_payload += encrypted_key

            respond.payload_size = len(respond_payload)
            self.socket.send(respond.pack_to_bytes())
            self.socket.send(respond_payload)

            print("[LOGIN] : Success. Aes key sent.")
            return True

    def receive_file(self, request_header, request_payload):
        """
        Receives a file from the client: decrypts it using the AES key,
        verifies its integrity with a CRC32 checksum, saves it to disk,
        and sends back the checksum and metadata.
        """

        data_size = struct.unpack("<I", request_payload[0:4])[0]

        file_name = struct.unpack("=255s", request_payload[4:259])[0].decode("utf-8").rstrip('\x00')

        encrypted_base64 = request_payload[259:].decode("ascii").rstrip('\x00')

        decrypted_bytes = bytearray(aes_decrypt(self.aes_key, encrypted_base64))

        crc = binascii.crc32(decrypted_bytes[0:data_size])

        file_path = OUT_FILE_PATH + file_name
        with open(file_path, "wb") as out_file:
            out_file.write(decrypted_bytes[0:data_size])

        self.file_name = file_name
        verified = False
        if not self.retry:
            self.file_id = shortuuid.ShortUUID().random(length=16)
            add_file(self.db_connection, self.file_id, self.file_name, OUT_FILE_PATH, verified)

        respond_payload = struct.pack("<16sI255s", self.client_id.encode(), data_size, file_name.encode())
        respond_payload += struct.pack(">I", crc)

        respond = RespondHeader()
        respond.code = RESPOND_FILE_ACCEPTED
        respond.payload_size = len(respond_payload)
        self.socket.send(respond.pack_to_bytes())
        self.socket.send(respond_payload)

        print("[RECEIVE FILE] : Success. CRC sent.")

        return True

    def confirm_valid_crc(self, request_header, request_payload):
        """
        Marks the file as verified in the database after the client confirms
        the CRC check passed, and sends back a confirmation response.
        """
        verified = True
        update_file(self.db_connection, self.file_id, self.file_name, OUT_FILE_PATH, verified)

        # Reset retry state.
        self.retry = False

        respond = RespondHeader()
        respond.code = RESPOND_MESSAGE_CONFIRMED
        respond.payload_size = 16

        payload = bytes()
        payload += struct.pack("=16s", str.encode(self.client_id))

        self.socket.send(respond.pack_to_bytes())
        self.socket.send(payload)

        print("[CHECK CRC] : Success. File transfer success\n")

        return False

    def confirm_invalid_crc(self, request_header, request_payload):
        """
        Triggered when the client reports a CRC failure.
        Prepares the server to retry the file transfer by setting a retry flag.
        """
        # Logs failure, sets retry flag, and keeps connection open.
        print("[CHECK CRC] : Fail. Retry to receive.")
        self.retry = True
        return True

    def confirm_last_invalid_crc(self):
        """
        Handles the final CRC failure after all retries. Sends a confirmation
        that the server is ending the transfer due to persistent errors.
        """
        respond = RespondHeader()
        self.retry = False
        respond.code = RESPOND_MESSAGE_CONFIRMED
        self.socket.send(respond.pack_to_bytes())

        print("[CHECK CRC] : Fail. Last CRC error. Ending transfer\n")

        return False
