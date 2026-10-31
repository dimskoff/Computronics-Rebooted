# Server Rack Cryptographic Co-Processor

A high-performance cryptographic ASIC coprocessor board for the OpenComputers **Server Rack**.

Accelerates hashing and encryption routines in hardware with zero machine cycle delays, making secure communications, transaction signing, and key verification virtually instantaneous.

## Component Name
`co_processor`

## Methods
* `md5(data: string): string` - Computes MD5 message digest.
* `sha256(data: string): string` - Computes SHA-256 cryptographic hash.
* `crc32(data: string): number` - Computes CRC32 checksum.
* `encrypt(data: string, key: string): string` - Encrypts data using AES-128, returns Base64.
* `decrypt(base64: string, key: string): string` - Decrypts AES-128 Base64 data.
* `generateKey(bytes?: number): string` - Generates a secure random hexadecimal key (8 to 64 bytes, default 16).

## Example
```lua
local component = require("component")
local crypto = component.co_processor

local secretKey = crypto.generateKey(16)
local encrypted = crypto.encrypt("CONFIDENTIAL DATA", secretKey)
print("Encrypted payload: " .. encrypted)
local decrypted = crypto.decrypt(encrypted, secretKey)
print("Decrypted payload: " .. decrypted)
```
