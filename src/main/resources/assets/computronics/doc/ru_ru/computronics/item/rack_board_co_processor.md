# Аппаратный криптосопроцессор для серверной стойки

Высокопроизводительная специализированная плата аппаратного шифрования (ASIC) для **Серверной стойки** OpenComputers.

Обеспечивает мгновенное аппаратное хэширование и шифрование данных без задержек и расхода тактов процессора, позволяя строить защищённые сетевые протоколы, цифровые подписи и безопасные транзакции.

## Имя компонента
`co_processor`

## Методы
* `md5(data: string): string` — вычисляет хэш MD5.
* `sha256(data: string): string` — вычисляет криптографический хэш SHA-256.
* `crc32(data: string): number` — вычисляет контрольную сумму CRC32.
* `encrypt(data: string, key: string): string` — шифрует данные алгоритмом AES-128 и возвращает строку Base64.
* `decrypt(base64: string, key: string): string` — дешифрует данные AES-128 из строки Base64.
* `generateKey(bytes?: number): string` — генерирует криптографически стойкий случайный ключ в формате HEX (от 8 до 64 байт, по умолчанию 16).

## Пример
```lua
local component = require("component")
local crypto = component.co_processor

local key = crypto.generateKey(16)
local encrypted = crypto.encrypt("СЕКРЕТНЫЕ ДАННЫЕ", key)
print("Зашифровано: " .. encrypted)
local decrypted = crypto.decrypt(encrypted, key)
print("Расшифровано: " .. decrypted)
```
