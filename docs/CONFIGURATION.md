# Computronics Rebooted - Configuration Guide / Руководство по настройке

Мод **Computronics Rebooted** предоставляет глубокую и гибкую двухуровневую систему конфигурации:
1. **`config/computronics-common.toml`** — общая/серверная конфигурация (механики, баланс, лимиты, анти-гриф, энергопотребление, буферы, тайминги).
2. **`config/computronics-client.toml`** — клиентская конфигурация (графика, отсечение дистанции рендеринга для FPS, размытие, звук).

Файлы создаются автоматически в папке `.minecraft/config/` (или `config/` на выделенном сервере). Все параметры имеют комментарии и безопасные диапазоны значений.

---

## 1. Серверная конфигурация (`computronics-common.toml`)

### `[features]` — Включение / Отключение компонентов
Позволяет создателям сборок выборочно выключать ненужные блоки, карты или модули стойки без удаления мода:
```toml
[features]
    # Блоки и периферия
    enableControlPanel = true       # Модульная панель управления
    enableChatBox = true            # Чат-бокс
    enableSpeechBox = true          # Синтезатор речи (Speech Box)
    enableTapeDrive = true          # Магнитофон и аудиокассеты
    enableCamera = true             # Оптическая камера
    enableRadar = true              # 3D Радар
    enableEntityDetector = true     # Направленный детектор сущностей
    enableAcousticSensor = true     # Скалк-акустический датчик вибраций
    enableWeatherStation = true     # Метеостанция
    enableLaserTransceiver = true   # Лазерный приемопередатчик
    enableRFJammer = true           # Генератор помех (RF Jammer)
    enableInductiveCharger = true   # Индукционная зарядная панель
    enableAnalogGauge = true        # Стрелочный аналоговый датчик
    enableIronNoteblock = true      # Железный нотный блок

    # Портативные устройства, карты и апгрейды роботов
    enableWirelessMonitor = true    # Беспроводной широкоформатный монитор
    enableNetworkCardTier3 = true   # Сетевая карта уровня 3 (Jumbo Frames)
    enablePacketSniffer = true      # Карта сниффера сетевых пакетов
    enableCardBoom = true           # Карта самоуничтожения (Boom Card)
    enableCardBeep = true           # Карта звукового генератора
    enableCardFX = true             # Карта эффектов частиц
    enableSpeechUpgrade = true      # Апгрейд речи для роботов
    enableChatUpgrade = true        # Апгрейд чата для роботов
    enableRadarUpgrade = true       # Апгрейд радара для роботов
    enableCameraUpgrade = true      # Апгрейд камеры для роботов
    enableWinchUpgrade = true       # Апгрейд лебедки для роботов

    # Платы для серверной стойки (Rack Boards)
    enableRackLightBoard = true     # Световая плата (Light Board)
    enableRackSwitchBoard = true    # Плата переключателей (Switch Board)
    enableRackCoProcessor = true    # Крипто-сопроцессор
    enableRackCapacitor = true      # Накопитель энергии стойки (Capacitor)
    enableRackBoom = true           # Плата детонатора стойки (Rack Boom)
```

---

### `[control_panel]` — Модульная панель управления
Тонкая настройка лимитов текста, цветовой гаммы и защита от спама:
```toml
[control_panel]
    # Максимальное количество строк текста (от 1 до 8)
    panelMaxLines = 4
    # Максимальная длина строки в символах (от 1 до 128)
    panelMaxCharactersPerLine = 32
    # Разрешить игрокам настраивать произвольные RGB hex цвета
    panelAllowCustomColors = true
    # Лимит программных обновлений в секунду на одну панель (защита от лагов и спама)
    panelMaxUpdatesPerSecond = 20
```

---

### `[chat_box]` & `[speech_box]` — Чат и Синтез речи
```toml
[chat_box]
    # Радиус вещания в блоках (-1 для безлимитного радиуса по всему миру)
    chatBoxMaxDistance = 40
    # Разрешить отправку сообщений между измерениями (Overworld <-> Nether <-> End)
    chatBoxAllowInterdimensional = false
    # Префикс перед сообщениями в чате
    chatBoxDefaultPrefix = "[ChatBox]"
    # Задержка в тиках между отправкой сообщений (20 тиков = 1 секунда)
    chatBoxCooldownTicks = 20

[speech_box]
    # Базовая громкость голоса (от 0.0 до 2.0)
    speechBoxVolume = 1.0
    # Максимальная дистанция слышимости синтезатора речи в блоках
    speechBoxMaxRange = 32
    # Максимальная длина фразы за один вызов .say() (символов)
    speechBoxMaxTextLength = 120
    # Перезарядка между фразами в тиках
    speechBoxCooldownTicks = 40
```

---

### `[radar]` & `[entity_detector]` — Радары и сканирование
```toml
[radar]
    # Максимальный радиус сканирования в блоках (от 4 до 128)
    radarMaxRadius = 32
    # Расход энергии на одно сканирование в OpenComputers EU
    radarEnergyCostPerScan = 15.0
    # Перезарядка между сканированиями в тиках
    radarScanCooldownTicks = 20
    # Обнаруживать игроков
    radarDetectPlayers = true
    # Обнаруживать мобов и животных
    radarDetectMobs = true

[entity_detector]
    # Дальность направленного конусного луча детектора в блоках
    entityDetectorMaxRange = 16
    # Перезарядка сканирования в тиках
    entityDetectorScanCooldownTicks = 10
    # Если true — возвращает точные X,Y,Z координаты; если false — только дистанцию и угол (для баланса PvP серверов)
    entityDetectorShowExactCoordinates = true
```

---

### `[acoustic_sensor]` — Скалк-акустический датчик
```toml
[acoustic_sensor]
    # Радиус улавливания звуковых вибраций
    acousticSensorMaxRadius = 32
    # Если true — реагирует исключительно на действия игроков (игнорирует мобов)
    acousticSensorDetectPlayersOnly = false
```

---

### `[tape_drive]` — Магнитофон и кассеты
```toml
[tape_drive]
    # Радиус распространения звука кассеты в блоках
    tapeDriveAudioRadius = 24
    # Размер сетевого буфера потокового аудио в миллисекундах (от 100 до 5000)
    tapeDriveBufferMs = 750
    # Вместимость железной кассеты в минутах
    tapeIronMinutes = 4
    # Вместимость золотой кассеты в минутах
    tapeGoldMinutes = 8
    # Вместимость алмазной кассеты в минутах
    tapeDiamondMinutes = 16
    # Вместимость кассеты из звезды Незера в минутах
    tapeNetherStarMinutes = 32
```

---

### `[networking]` — Сетевое оборудование
```toml
[networking]
    # Радиус глушения беспроводного сигнала глушилкой (RF Jammer)
    rfJammerMaxRange = 64
    # Энергопотребление глушилки в EU за секунду
    rfJammerEnergyPerSecond = 20.0
    # Максимум перехваченных сетевых пакетов за тик для сниффера (защита от переполнения памяти)
    packetSnifferMaxPacketsPerTick = 100
    # Максимальный размер полезной нагрузки Jumbo-пакетов сетевой карты Т3 в байтах
    jumboFramesMaxPacketSize = 65536
    # Максимальная дистанция лазерной связи прямой видимости в блоках
    laserMaxDistance = 64
```

---

### `[self_destruct]` — Защита от грифа (Анти-гриф)
Критически важно для серверов с приватами и публичных SMP:
```toml
[self_destruct]
    # Если false, взрыв карты компьютера наносит урон мобам/игрокам, но НИКОГДА не ломает блоки мира!
    cardBoomAllowBlockDamage = true
    # Сила взрыва компьютерной карты самоуничтожения
    cardBoomExplosionPower = 3.5
    # Таймер взрывателя в тиках
    cardBoomFuseTicks = 20

    # Если false, взрыв детонатора серверной стойки не разрушает блоки мира
    rackBoomAllowBlockDamage = true
    # Сила взрыва платы детонатора стойки
    rackBoomExplosionPower = 4.5
```

---

### `[wireless_monitor]` & `[server_rack]` — Планшет и стойки
```toml
[wireless_monitor]
    # Емкость встроенного аккумулятора беспроводного монитора в EU
    wirelessMonitorMaxEnergy = 20000.0
    # Расход энергии активного экрана в EU за секунду
    wirelessMonitorEnergyUsagePerSecond = 1.0
    # Сетевой порт по умолчанию
    wirelessMonitorDefaultPort = 404
    # Разрешить прошивку монитора через Shift+ПКМ чипом EEPROM в руке
    wirelessMonitorAllowEEPROMFlashing = true

[server_rack]
    # Емкость платы конденсатора стойки (Rack Capacitor) в EU
    rackCapacitorCapacity = 50000.0
    # Расход энергии за один активный светодиод в тик
    rackLightBoardEnergyPerTick = 0.05
    # Множитель скорости хеширования крипто-сопроцессора (1.0 = норма, 2.0 = в 2 раза быстрее)
    rackCoProcessorSpeedMultiplier = 1.0
    # Защита от дребезга контактов переключателей стойки в тиках
    rackSwitchBoardSignalCooldown = 5
```

---

## 2. Клиентская конфигурация (`computronics-client.toml`)

Оптимизирует FPS, графику и звук на стороне игрока:
```toml
[rendering]
    # Отрисовывать ли 3D-модель широкоформатного планшета в руке игрока
    renderHandheldWirelessMonitor = true
    # Отрисовывать ли 3D-текст и надписи на модульных панелях управления в мире
    renderControlPanelWorldText = true
    # Дистанция отсечения текста панелей в блоках (при превышении текст не рендерится для экономии FPS)
    controlPanelTextRenderDistance = 32
    # Отрисовывать ли светящиеся динамические светодиоды и интерактивные тумблеры на серверных стойках
    renderRackOverlays = true
    # Включать ли размытие заднего плана при открытии GUI отвертки (false = мир остается четким и резким)
    enableScrewdriverGuiBlur = false

[audio]
    # Множитель громкости воспроизведения аудиокассет (от 0.0 до 1.0)
    tapeDriveMasterVolume = 1.0
    # Множитель громкости синтезатора речи (от 0.0 до 2.0)
    speechBoxVolumeMultiplier = 1.0
    # Спавнить ли частицы нот вокруг железного нотного блока при игре
    ironNoteblockParticles = true
```

---

## 3. Рекомендованные пресеты для сборок

### Пресет для публичного SMP / Сервера с приватами:
- `cardBoomAllowBlockDamage = false`
- `rackBoomAllowBlockDamage = false`
- `chatBoxMaxDistance = 50`
- `chatBoxAllowInterdimensional = false`
- `panelMaxUpdatesPerSecond = 10`
- `radarEnergyCostPerScan = 30.0`
- `radarScanCooldownTicks = 40`

### Пресет для слабых ПК / Low-End Modpacks:
- `renderControlPanelWorldText = true`
- `controlPanelTextRenderDistance = 16`
- `enableScrewdriverGuiBlur = false`
- `ironNoteblockParticles = false`
- `speechBoxMaxRange = 24`
- `tapeDriveAudioRadius = 16`
