import os
from base64 import b64decode, b64encode

from Crypto.Cipher import AES
from Crypto.Cipher import PKCS1_v1_5 as Cipher_PKCS1_v1_5
from Crypto.PublicKey import RSA


def generate_aes_key():
    """
    Generates a random 128-bit AES key and returns it as a Base64-encoded string.
    """
    return b64encode(os.urandom(16)).decode("utf-8")


def rsa_encrypt(public_key, plain_text):
    """
    Encrypts a given plain text string using the client’s RSA public key
    and returns it Base64-encoded.
    """
    der = b64decode(public_key)

    rsa_key = RSA.importKey(der)

    rsa_cipher = Cipher_PKCS1_v1_5.new(rsa_key)

    result = rsa_cipher.encrypt(plain_text.encode("utf-8"))

    return b64encode(result)


def aes_decrypt(encoded_aes_key, cipher_text):
    """
    Decrypts an AES-encrypted message using the given
    Base64-encoded AES key, assuming CBC mode with a zero IV.
    Removes PKCS#7 padding.
    """
    aes_key = b64decode(encoded_aes_key)
    iv = AES.block_size * b'\x00'
    cipher = AES.new(aes_key, AES.MODE_CBC, iv)
    decrypted = cipher.decrypt(b64decode(cipher_text))

    pad_len = decrypted[-1]
    if pad_len < 1 or pad_len > AES.block_size:
        raise ValueError("Invalid padding length")

    if decrypted[-pad_len:] != bytes([pad_len]) * pad_len:
        raise ValueError("Invalid padding bytes")

    return decrypted[:-pad_len]
