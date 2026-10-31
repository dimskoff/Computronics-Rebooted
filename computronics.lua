-- Computronics: Rebooted - Interactive Workstation GUI
-- Designed for OpenOS on OpenComputers: Rebooted (1.21.1 / NeoForge)
-- Full interactive GPU mouse-driven dashboard for all Computronics blocks and cards

local component = require("component")
local event = require("event")
local term = require("term")
local unicode = require("unicode")
local computer = require("computer")

local gpu = component.gpu
if not gpu then
    print("Error: No GPU component found.")
    return
end

local screenW, screenH = gpu.getResolution()
-- Recommended minimum: 80x25
if screenW < 80 or screenH < 25 then
    gpu.setResolution(math.max(80, screenW), math.max(25, screenH))
    screenW, screenH = gpu.getResolution()
end

-- ============================================================================
-- PALETTE & THEME
-- ============================================================================
local C = {
    BG          = 0x0E1318,
    PANEL_BG    = 0x18202A,
    PANEL_ALT   = 0x1F2A38,
    HEADER_BG   = 0x131A22,
    TAB_INACTIVE= 0x222E3D,
    TAB_ACTIVE  = 0x00A8FF,
    TAB_TEXT    = 0xDFE6E9,
    TEXT        = 0xF5F6FA,
    TEXT_MUTED  = 0x8395A7,
    GREEN       = 0x2ECC71,
    RED         = 0xE74C3C,
    YELLOW      = 0xF1C40F,
    CYAN        = 0x00D2D3,
    ORANGE      = 0xE67E22,
    PURPLE      = 0x9B59B6,
    BTN_BG      = 0x2C3A4D,
    BTN_HOVER   = 0x3E526C,
    BORDER      = 0x2F3B4C
}

-- Safe color depth setup
pcall(function()
    if gpu.getDepth and gpu.maxDepth then
        local cur = gpu.getDepth()
        local maxD = gpu.maxDepth()
        if maxD and maxD ~= cur and (maxD == 1 or maxD == 4 or maxD == 8) then
            pcall(gpu.setDepth, maxD)
        end
    end
end)

-- ============================================================================
-- STATE & COMPONENT WRAPPERS
-- ============================================================================
local running = true
local currentTab = 1
local statusMessage = "Ready. Click on any tab or button to interact."
local statusTime = computer.uptime()

local buttons = {}
local logs = {
    chat = {},
    acoustic = {},
    packets = {},
    tripwire = {},
    panel = {}
}

-- Camera state
local camPitch = 0
local camYaw = 0
local camStep = 15
local camScanResult = nil
local camDistResult = nil

-- Radar state
local radarRadius = 16
local radarEntities = {}
local radarLastScanTime = 0

-- Tape state
local tapeSpinAnim = 0
local tapeLastReadText = ""

-- Chat state
local chatMode = "say" -- "say" or "tell"
local chatTarget = "Steve"
local chatPrefix = "[Intercom]"
local chatDistance = 64
local chatInputBuffer = "Hello from Computronics Station!"

-- Gauge state
local gaugePreset = 0.5
local gaugeLabel = "REACTOR FLUX"
local gaugeColor = 0x00A8FF

-- Network / Sniffer state
local netPort = 4096
local netTarget = ""
local netPayload = "PING_COMPUTRONICS"
local netSpoofMac = "00:DE:AD:BE:EF:01"
local useSpoof = false
local tapActive = true

-- Jammer state
local jammerFreq = 0
local jammerPower = 25.0
local jammerEnabled = false

-- Self-destruct state
local boomArmed = false
local boomCountdown = -1

-- ============================================================================
-- HARDWARE REGISTRY
-- ============================================================================
local H = {
    camera       = component.isAvailable("camera") and component.camera or nil,
    radar        = component.isAvailable("radar") and component.radar or nil,
    tape         = component.isAvailable("tape_drive") and component.tape_drive or nil,
    chat         = component.isAvailable("chat_box") and component.chat_box or nil,
    acoustic     = component.isAvailable("acoustic_sensor") and component.acoustic_sensor or nil,
    weather      = component.isAvailable("weather_station") and component.weather_station or nil,
    charger      = component.isAvailable("inductive_charger") and component.inductive_charger or nil,
    laser        = component.isAvailable("laser_transceiver") and component.laser_transceiver or nil,
    gauge        = component.isAvailable("analog_gauge") and component.analog_gauge or nil,
    modem        = component.isAvailable("modem") and component.modem or nil,
    sniffer      = component.isAvailable("packet_sniffer") and component.packet_sniffer or nil,
    jammer       = component.isAvailable("rf_jammer") and component.rf_jammer or nil,
    boom         = component.isAvailable("self_destruct") and component.self_destruct or nil,
    note         = component.isAvailable("iron_noteblock") and component.iron_noteblock or nil,
    panel        = component.isAvailable("control_panel") and component.control_panel or nil,
    beep         = component.isAvailable("beep") and component.beep or nil,
    fx           = component.isAvailable("particle_fx") and component.particle_fx or nil,
    speech       = component.isAvailable("speech_box") and component.speech_box or nil,
}

local function rescanHardware()
    H.camera   = component.isAvailable("camera") and component.camera or nil
    H.radar    = component.isAvailable("radar") and component.radar or nil
    H.tape     = component.isAvailable("tape_drive") and component.tape_drive or nil
    H.chat     = component.isAvailable("chat_box") and component.chat_box or nil
    H.acoustic = component.isAvailable("acoustic_sensor") and component.acoustic_sensor or nil
    H.weather  = component.isAvailable("weather_station") and component.weather_station or nil
    H.charger  = component.isAvailable("inductive_charger") and component.inductive_charger or nil
    H.laser    = component.isAvailable("laser_transceiver") and component.laser_transceiver or nil
    H.gauge    = component.isAvailable("analog_gauge") and component.analog_gauge or nil
    H.modem    = component.isAvailable("modem") and component.modem or nil
    H.sniffer  = component.isAvailable("packet_sniffer") and component.packet_sniffer or nil
    H.jammer   = component.isAvailable("rf_jammer") and component.rf_jammer or nil
    H.boom     = component.isAvailable("self_destruct") and component.self_destruct or nil
    H.note     = component.isAvailable("iron_noteblock") and component.iron_noteblock or nil
    H.panel    = component.isAvailable("control_panel") and component.control_panel or nil
    H.beep     = component.isAvailable("beep") and component.beep or nil
    H.fx       = component.isAvailable("particle_fx") and component.particle_fx or nil
    H.speech   = component.isAvailable("speech_box") and component.speech_box or nil

    if H.modem and not H.modem.isOpen(netPort) then
        pcall(function() H.modem.open(netPort) end)
    end
end
rescanHardware()

-- ============================================================================
-- GUI PRIMITIVES & DRAWING
-- ============================================================================
local function setColors(fg, bg)
    if fg then gpu.setForeground(fg) end
    if bg then gpu.setBackground(bg) end
end

local function drawBox(x, y, w, h, bg, borderCol)
    setColors(borderCol or C.BORDER, bg or C.PANEL_BG)
    gpu.fill(x, y, w, h, " ")
    -- Top and bottom edges
    gpu.set(x, y, "┌" .. string.rep("─", math.max(0, w - 2)) .. "┐")
    gpu.set(x, y + h - 1, "└" .. string.rep("─", math.max(0, w - 2)) .. "┘")
    -- Left and right vertical edges
    for row = y + 1, y + h - 2 do
        gpu.set(x, row, "│")
        gpu.set(x + w - 1, row, "│")
    end
end

local function addButton(id, x, y, w, h, text, onClick, bgCol, fgCol)
    table.insert(buttons, {
        id = id,
        x = x,
        y = y,
        w = w,
        h = h,
        text = text,
        onClick = onClick,
        bg = bgCol or C.BTN_BG,
        fg = fgCol or C.TEXT
    })
    setColors(fgCol or C.TEXT, bgCol or C.BTN_BG)
    gpu.fill(x, y, w, h, " ")
    local pad = math.max(0, math.floor((w - unicode.len(text)) / 2))
    gpu.set(x + pad, y + math.floor((h - 1) / 2), text)
end

local function setStatus(msg)
    statusMessage = msg
    statusTime = computer.uptime()
end

local function promptInput(promptText, defaultVal)
    setColors(C.YELLOW, C.HEADER_BG)
    gpu.fill(1, screenH, screenW, 1, " ")
    gpu.set(2, screenH, promptText .. ": ")
    local promptLen = unicode.len(promptText) + 4
    term.setCursor(promptLen, screenH)
    term.setCursorBlink(true)
    local inp = term.read({doBreak = false}) or defaultVal or ""
    term.setCursorBlink(false)
    inp = inp:gsub("[\r\n]", "")
    if inp == "" then inp = defaultVal or "" end
    setStatus("Entered: " .. inp)
    return inp
end

-- ============================================================================
-- TAB DEFINITIONS
-- ============================================================================
local TABS = {
    {id = 1,  name = "1:DASH",     title = "SYSTEM DASHBOARD",     comp = nil},
    {id = 2,  name = "2:CAMERA",   title = "OPTICAL CAMERA",       comp = "camera"},
    {id = 3,  name = "3:RADAR",    title = "3D ENTITY RADAR",      comp = "radar"},
    {id = 4,  name = "4:TAPE",     title = "AUDIO TAPE DECK",      comp = "tape_drive"},
    {id = 5,  name = "5:CHAT",     title = "CHAT BOX & INTERCOM",  comp = "chat_box"},
    {id = 6,  name = "6:ACOUSTIC", title = "SCULK VIBRATION SENSOR",comp = "acoustic_sensor"},
    {id = 7,  name = "7:WEATHER",  title = "WEATHER OBSERVATORY",  comp = "weather_station"},
    {id = 8,  name = "8:CHARGER",  title = "INDUCTIVE CHARGE PAD", comp = "inductive_charger"},
    {id = 9,  name = "9:LASER",    title = "LASER TRANSCEIVER",    comp = "laser_transceiver"},
    {id = 10, name = "10:GAUGE",   title = "ANALOG DIAL GAUGE",    comp = "analog_gauge"},
    {id = 11, name = "11:NET/TAP", title = "NETWORK & SNIFFER",    comp = "packet_sniffer"},
    {id = 12, name = "12:JAMMER",  title = "RF SIGNAL JAMMER",     comp = "rf_jammer"},
    {id = 13, name = "13:BOOM",    title = "SELF-DESTRUCT CARD",   comp = "self_destruct"},
    {id = 14, name = "14:NOTE",    title = "IRON NOTE BLOCK",      comp = "iron_noteblock"},
    {id = 15, name = "15:PANEL",   title = "MODULAR CONTROL PANEL",comp = "control_panel"},
    {id = 16, name = "16:FX/BEEP", title = "PARTICLE FX & BEEP",   comp = "particle_fx"},
}

-- Render Header & Tab Navigation
local function renderChrome()
    buttons = {}

    -- Title Bar (Row 1)
    setColors(C.CYAN, C.HEADER_BG)
    gpu.fill(1, 1, screenW, 1, " ")
    gpu.set(2, 1, "COMPUTRONICS REBOOTED // WORKSTATION OS v2.0")

    local clock = os.date("%H:%M:%S")
    local clockX = screenW - 24
    setColors(C.TEXT_MUTED, C.HEADER_BG)
    gpu.set(clockX, 1, clock)

    -- Exit Button (Row 1 top right)
    addButton("btn_exit", screenW - 10, 1, 9, 1, "[ EXIT ]", function()
        running = false
    end, C.RED, C.TEXT)

    -- Tab Rows (Rows 2 & 3)
    setColors(C.TEXT, C.BG)
    gpu.fill(1, 2, screenW, 2, " ")

    -- First 8 tabs on row 2
    local curX = 2
    for i = 1, 8 do
        local tab = TABS[i]
        local isAct = (currentTab == tab.id)
        local bgCol = isAct and C.TAB_ACTIVE or C.TAB_INACTIVE
        local fgCol = isAct and C.BG or C.TAB_TEXT
        local label = " " .. tab.name .. " "
        local tw = unicode.len(label)
        addButton("tab_" .. tab.id, curX, 2, tw, 1, label, function()
            currentTab = tab.id
            setStatus("Switched to " .. tab.title)
        end, bgCol, fgCol)
        curX = curX + tw + 1
    end

    -- Remaining 8 tabs on row 3
    curX = 2
    for i = 9, #TABS do
        local tab = TABS[i]
        local isAct = (currentTab == tab.id)
        local bgCol = isAct and C.TAB_ACTIVE or C.TAB_INACTIVE
        local fgCol = isAct and C.BG or C.TAB_TEXT
        local label = " " .. tab.name .. " "
        local tw = unicode.len(label)
        addButton("tab_" .. tab.id, curX, 3, tw, 1, label, function()
            currentTab = tab.id
            setStatus("Switched to " .. tab.title)
        end, bgCol, fgCol)
        curX = curX + tw + 1
    end
        local bgCol = isAct and C.TAB_ACTIVE or C.TAB_INACTIVE
        local fgCol = isAct and C.BG or C.TAB_TEXT
        local label = " " .. tab.name .. " "
        local tw = unicode.len(label)
        addButton("tab_" .. tab.id, curX, 3, tw, 1, label, function()
            currentTab = tab.id
            setStatus("Switched to " .. tab.title)
        end, bgCol, fgCol)
        curX = curX + tw + 1
    end

    -- Rescan Button on Row 3 right
    addButton("btn_rescan", screenW - 12, 3, 11, 1, "[ RESCAN ]", function()
        rescanHardware()
        setStatus("Hardware components re-scanned successfully.")
    end, C.PANEL_ALT, C.CYAN)

    -- Top separator (Row 4)
    setColors(C.BORDER, C.BG)
    gpu.set(1, 4, string.rep("─", screenW))

    -- Bottom separator (Row screenH - 1)
    gpu.set(1, screenH - 1, string.rep("─", screenW))

    -- Status Bar (Row screenH)
    setColors(C.TEXT_MUTED, C.HEADER_BG)
    gpu.fill(1, screenH, screenW, 1, " ")
    local activeTabDef = TABS[currentTab]
    setColors(C.YELLOW, C.HEADER_BG)
    gpu.set(2, screenH, "▶ " .. (activeTabDef and activeTabDef.title or "SYSTEM"))

    setColors(C.TEXT, C.HEADER_BG)
    local msgTrunc = unicode.sub(statusMessage, 1, screenW - 35)
    gpu.set(26, screenH, "| " .. msgTrunc)

    local memFree = math.floor(computer.freeMemory() / 1024)
    local memTotal = math.floor(computer.totalMemory() / 1024)
    local memStr = string.format("RAM: %dK/%dK", memTotal - memFree, memTotal)
    setColors(C.CYAN, C.HEADER_BG)
    gpu.set(screenW - unicode.len(memStr) - 2, screenH, memStr)
end

-- ============================================================================
-- TAB 1: DASHBOARD
-- ============================================================================
local function renderDashboard()
    drawBox(3, 5, screenW - 6, screenH - 7, C.PANEL_BG)
    setColors(C.CYAN, C.PANEL_BG)
    gpu.set(5, 6, "COMPUTRONICS HARDWARE STATUS OVERVIEW")

    local comps = {
        {"Camera",              H.camera,   "camera",            2},
        {"Entity Radar",        H.radar,    "radar",             3},
        {"Tape Drive",          H.tape,     "tape_drive",        4},
        {"Chat Box",            H.chat,     "chat_box",          5},
        {"Sculk Sensor",        H.acoustic, "acoustic_sensor",   6},
        {"Weather Station",     H.weather,  "weather_station",   7},
        {"Inductive Charger",   H.charger,  "inductive_charger", 8},
        {"Laser Transceiver",   H.laser,    "laser_transceiver", 9},
        {"Analog Dial Gauge",   H.gauge,    "analog_gauge",      10},
        {"Tier 3 NIC / Modem",  H.modem,    "modem",             11},
        {"Packet Sniffer (TAP)",H.sniffer,  "packet_sniffer",    11},
        {"RF Signal Jammer",    H.jammer,   "rf_jammer",         12},
        {"Self-Destruct Card",  H.boom,     "self_destruct",     13},
        {"Iron Note Block",     H.note,     "iron_noteblock",    14},
        {"Modular Control Panel",H.panel,   "control_panel",     15},
        {"Particle FX / Beep",  H.fx or H.beep, "particle_fx",   16},
    }

    local yPos = 8
    local xLeft = 5
    local xRight = math.floor(screenW / 2) + 2

    for i, item in ipairs(comps) do
        local name, obj, compType, tabTarget = item[1], item[2], item[3], item[4]
        local rx = (i <= 8) and xLeft or xRight
        local ry = (i <= 8) and (yPos + (i - 1) * 2) or (yPos + (i - 9) * 2)

        if ry < screenH - 3 then
            if obj then
                setColors(C.GREEN, C.PANEL_BG)
                gpu.set(rx, ry, "[● ONLINE ]")
            else
                setColors(C.RED, C.PANEL_BG)
                gpu.set(rx, ry, "[○ OFFLINE]")
            end

            setColors(C.TEXT, C.PANEL_BG)
            gpu.set(rx + 12, ry, name)

            if obj and obj.address then
                setColors(C.TEXT_MUTED, C.PANEL_BG)
                local shortAddr = unicode.sub(obj.address, 1, 8) .. ".."
                gpu.set(rx + 32, ry, shortAddr)
            end

            addButton("dash_jump_" .. i, rx + 44, ry, 7, 1, "[OPEN]", function()
                currentTab = tabTarget
                setStatus("Opened " .. name .. " control panel.")
            end, C.BTN_BG, C.CYAN)
        end
    end
end

-- ============================================================================
-- TAB 2: CAMERA VIEWPORT
-- ============================================================================
local function renderCamera()
    drawBox(3, 5, screenW - 6, screenH - 7, C.PANEL_BG)
    setColors(C.CYAN, C.PANEL_BG)
    gpu.set(5, 6, "OPTICAL RAYCAST CAMERA SENSOR")

    if not H.camera then
        setColors(C.RED, C.PANEL_BG)
        gpu.set(5, 9, "Camera block is not connected to this network!")
        gpu.set(5, 11, "Connect the camera with OpenComputers cable or attach Camera Upgrade to robot.")
        return
    end

    local facing = "UNKNOWN"
    pcall(function()
        if H.camera.getFacing then
            facing = string.upper(H.camera.getFacing())
        end
    end)

    setColors(C.TEXT, C.PANEL_BG)
    gpu.set(5, 8, string.format("Lens Facing: %-8s | Current Angles: Pitch: %+3d° | Yaw: %+3d°", facing, camPitch, camYaw))

    -- Step Presets
    gpu.set(5, 10, "Angle Step:")
    addButton("c_step5",  18, 10, 6, 1, " 5° ",  function() camStep = 5  setStatus("Step set to 5°") end,  camStep == 5 and C.TAB_ACTIVE or C.BTN_BG, camStep == 5 and C.BG or C.TEXT)
    addButton("c_step15", 26, 10, 6, 1, " 15° ", function() camStep = 15 setStatus("Step set to 15°") end, camStep == 15 and C.TAB_ACTIVE or C.BTN_BG, camStep == 15 and C.BG or C.TEXT)
    addButton("c_step45", 34, 10, 6, 1, " 45° ", function() camStep = 45 setStatus("Step set to 45°") end, camStep == 45 and C.TAB_ACTIVE or C.BTN_BG, camStep == 45 and C.BG or C.TEXT)

    -- Reticle D-Pad
    drawBox(5, 12, 24, 7, C.PANEL_ALT)
    setColors(C.TEXT_MUTED, C.PANEL_ALT)
    gpu.set(7, 13, "ORIENTATION DPAD")

    addButton("c_up",    14, 14, 6, 1, " ▲ UP ", function()
        camPitch = math.min(90, camPitch + camStep)
        setStatus(string.format("Camera Pitch: %+d°", camPitch))
    end)
    addButton("c_down",  14, 16, 6, 1, " ▼ DN ", function()
        camPitch = math.max(-90, camPitch - camStep)
        setStatus(string.format("Camera Pitch: %+d°", camPitch))
    end)
    addButton("c_left",  7,  15, 6, 1, " ◀ L ",  function()
        camYaw = (camYaw - camStep) % 360
        setStatus(string.format("Camera Yaw: %+d°", camYaw))
    end)
    addButton("c_right", 21, 15, 6, 1, " R ▶ ",  function()
        camYaw = (camYaw + camStep) % 360
        setStatus(string.format("Camera Yaw: %+d°", camYaw))
    end)
    addButton("c_reset", 10, 17, 14, 1, "[ RESET (0,0) ]", function()
        camPitch = 0
        camYaw = 0
        setStatus("Camera centered forward (0,0).")
    end, C.PANEL_BG, C.YELLOW)

    -- Actions
    addButton("c_act_dist", 33, 13, 20, 2, "[ 🔍 DISTANCE ]", function()
        local ok, res = pcall(H.camera.distance, camYaw, camPitch)
        if ok and res and res >= 0 then
            camDistResult = string.format("%.2f meters", res)
            setStatus("Obstacle hit at " .. camDistResult)
        else
            camDistResult = "No solid block in range (>64m)"
            setStatus("Laser clear: " .. tostring(camDistResult))
        end
    end, C.BTN_BG, C.GREEN)

    addButton("c_act_scan", 55, 13, 20, 2, "[ 🔬 SPECTRAL SCAN ]", function()
        local ok, res = pcall(H.camera.scan, camYaw, camPitch)
        if ok and type(res) == "table" then
            camScanResult = res
            setStatus(string.format("Scanned block: %s (Dist: %.2fm)", tostring(res.block), tonumber(res.distance) or 0))
        else
            camScanResult = {error = tostring(res or "No block in range")}
            setStatus("Scan completed: " .. tostring(res or "clear"))
        end
    end, C.BTN_BG, C.CYAN)

    -- Results Panel
    drawBox(33, 16, screenW - 39, screenH - 18, C.PANEL_ALT)
    setColors(C.YELLOW, C.PANEL_ALT)
    gpu.set(35, 17, "SPECTRAL ANALYSIS READOUT:")

    if camDistResult then
        setColors(C.TEXT, C.PANEL_ALT)
        gpu.set(35, 18, "Laser Range: " .. camDistResult)
    end

    if camScanResult then
        if camScanResult.error then
            setColors(C.RED, C.PANEL_ALT)
            gpu.set(35, 19, "Result: " .. camScanResult.error)
        else
            setColors(C.GREEN, C.PANEL_ALT)
            gpu.set(35, 19, string.format("Block ID:       %s", tostring(camScanResult.block)))
            setColors(C.TEXT, C.PANEL_ALT)
            gpu.set(35, 20, string.format("Exact Distance: %.3f meters", tonumber(camScanResult.distance) or 0))
            gpu.set(35, 21, string.format("Block Hardness: %.1f", tonumber(camScanResult.hardness) or 0))
            gpu.set(35, 22, string.format("Surface Light:  %d (Raw Luminance)", tonumber(camScanResult.light) or 0))
        end
    else
        setColors(C.TEXT_MUTED, C.PANEL_ALT)
        gpu.set(35, 19, "Click [DISTANCE] or [SPECTRAL SCAN] to pulse raycast.")
    end
end

-- ============================================================================
-- TAB 3: 3D ENTITY RADAR
-- ============================================================================
local function renderRadar()
    drawBox(3, 5, screenW - 6, screenH - 7, C.PANEL_BG)
    setColors(C.CYAN, C.PANEL_BG)
    gpu.set(5, 6, "3D SPHERICAL ENTITY RADAR")

    if not H.radar then
        setColors(C.RED, C.PANEL_BG)
        gpu.set(5, 9, "Radar component is not connected!")
        return
    end

    setColors(C.TEXT, C.PANEL_BG)
    gpu.set(5, 8, "Scanning Radius: ")
    local rads = {8, 16, 32, 64}
    for i, r in ipairs(rads) do
        local isAct = (radarRadius == r)
        addButton("r_rad_" .. r, 22 + (i - 1) * 9, 8, 8, 1, " " .. r .. "m ", function()
            radarRadius = r
            setStatus("Radar radius set to " .. r .. "m")
        end, isAct and C.TAB_ACTIVE or C.BTN_BG, isAct and C.BG or C.TEXT)
    end

    addButton("r_scan", screenW - 22, 8, 16, 1, "[ 🎯 SCAN AREA ]", function()
        local ok, res = pcall(H.radar.getEntities, radarRadius)
        if ok and type(res) == "table" then
            radarEntities = res
            radarLastScanTime = computer.uptime()
            setStatus(string.format("Radar sweep complete: detected %d entities.", #radarEntities))
        else
            radarEntities = {}
            setStatus("Radar error: " .. tostring(res))
        end
    end, C.BTN_BG, C.GREEN)

    -- Left: Radar circular map
    local mapW = 28
    local mapH = screenH - 18
    drawBox(5, 10, mapW, mapH, C.PANEL_ALT)
    setColors(C.TEXT_MUTED, C.PANEL_ALT)
    gpu.set(7, 11, string.format("RADAR DISPLAY (%dm)", radarRadius))

    local cx = 5 + math.floor(mapW / 2)
    local cy = 10 + math.floor(mapH / 2)
    setColors(C.YELLOW, C.PANEL_ALT)
    gpu.set(cx, cy, "+")

    -- Plot entities
    for _, ent in ipairs(radarEntities) do
        local dx = tonumber(ent.x) or 0
        local dz = tonumber(ent.z) or 0
        local px = cx + math.floor((dx / radarRadius) * (mapW / 2 - 2))
        local py = cy + math.floor((dz / radarRadius) * (mapH / 2 - 2))
        if px >= 6 and px <= 5 + mapW - 2 and py >= 11 and py <= 10 + mapH - 2 then
            local isPlayer = ent.name and (ent.name:find("Steve") or ent.name:find("Alex") or ent.name:find("Player"))
            setColors(isPlayer and C.YELLOW or C.RED, C.PANEL_ALT)
            gpu.set(px, py, isPlayer and "P" or "M")
        end
    end

    -- Right: Entity list
    local listX = 5 + mapW + 2
    local listW = screenW - listX - 4
    drawBox(listX, 10, listW, mapH, C.PANEL_ALT)
    setColors(C.CYAN, C.PANEL_ALT)
    gpu.set(listX + 2, 11, string.format("DETECTED TARGETS (%d)", #radarEntities))

    if #radarEntities == 0 then
        setColors(C.TEXT_MUTED, C.PANEL_ALT)
        gpu.set(listX + 2, 13, "No entities detected within radius.")
        gpu.set(listX + 2, 14, "Click [ SCAN AREA ] to pulse radar.")
    else
        local row = 13
        for idx, ent in ipairs(radarEntities) do
            if row < 10 + mapH - 1 then
                local dist = math.sqrt((ent.x or 0)^2 + (ent.y or 0)^2 + (ent.z or 0)^2)
                setColors(C.TEXT, C.PANEL_ALT)
                local line = string.format("%d. %-14s Dist: %5.1fm (dX:%+2d dY:%+2d dZ:%+2d)",
                    idx, unicode.sub(tostring(ent.name or "Unknown"), 1, 14), dist,
                    math.floor(ent.x or 0), math.floor(ent.y or 0), math.floor(ent.z or 0))
                gpu.set(listX + 2, row, unicode.sub(line, 1, listW - 4))
                row = row + 1
            end
        end
    end
end

-- ============================================================================
-- TAB 4: TAPE DRIVE (MAGNETIC AUDIO RECORDER)
-- ============================================================================
local function renderTapeDrive()
    drawBox(3, 5, screenW - 6, screenH - 7, C.PANEL_BG)
    setColors(C.CYAN, C.PANEL_BG)
    gpu.set(5, 6, "INDUSTRIAL MAGNETIC CASSETTE TAPE RECORDER")

    if not H.tape then
        setColors(C.RED, C.PANEL_BG)
        gpu.set(5, 9, "Tape Drive block is not connected!")
        return
    end

    local isReady = false
    local state = "STOPPED"
    local cap = 0
    local pos = 0
    local vol = 1.0
    local spd = 1.0

    pcall(function()
        isReady = H.tape.isReady()
        state = H.tape.getState()
        cap = H.tape.getSize()
        pos = H.tape.getPosition()
        vol = H.tape.getVolume()
        spd = H.tape.getSpeed()
    end)

    -- Animated cassette reels
    tapeSpinAnim = (state == "PLAYING") and ((tapeSpinAnim + 1) % 4) or 0
    local reelFrames = {"|", "/", "─", "\\"}
    local rL = reelFrames[tapeSpinAnim + 1]
    local rR = reelFrames[((tapeSpinAnim + 2) % 4) + 1]

    drawBox(5, 8, screenW - 10, 6, C.PANEL_ALT)
    setColors(C.TEXT_MUTED, C.PANEL_ALT)
    gpu.set(7, 9, "CASSETTE TAPE TRANSPORT MECHANISM:")

    setColors(state == "PLAYING" and C.GREEN or (state == "PAUSED" and C.YELLOW or C.TEXT_MUTED), C.PANEL_ALT)
    local reelDisplay = string.format("  [ REEL L: ( %s ) ]=========[ COMPUTAPE CHASSIS ]=========[ REEL R: ( %s ) ]  STATE: %-8s", rL, rR, state)
    gpu.set(7, 10, reelDisplay)

    -- Position & Progress
    local capKb = math.floor(cap / 1024)
    local posKb = math.floor(pos / 1024)
    local pct = cap > 0 and math.floor((pos / cap) * 100) or 0
    setColors(C.TEXT, C.PANEL_ALT)
    gpu.set(7, 12, string.format("Storage: %s | Position: %d / %d KB (%d%%)", isReady and "TAPE INSERTED" or "NO TAPE", posKb, capKb, pct))

    local barW = screenW - 24
    local filled = math.floor((pct / 100) * barW)
    setColors(C.CYAN, C.PANEL_ALT)
    gpu.set(7, 13, string.rep("█", filled) .. string.rep("░", math.max(0, barW - filled)))

    -- Transport Buttons
    addButton("t_play",   5,  15, 11, 2, "[ ▶ PLAY ]", function()
        pcall(H.tape.play)
        setStatus("Tape playback started.")
    end, C.BTN_BG, C.GREEN)

    addButton("t_pause",  17, 15, 11, 2, "[ ⏸ PAUSE ]", function()
        pcall(H.tape.pause)
        setStatus("Tape playback paused.")
    end, C.BTN_BG, C.YELLOW)

    addButton("t_stop",   29, 15, 11, 2, "[ ⏹ STOP ]", function()
        pcall(H.tape.stop)
        setStatus("Tape playback stopped.")
    end, C.BTN_BG, C.RED)

    addButton("t_rew",    41, 15, 13, 2, "[ ⏮ REWIND ]", function()
        pcall(H.tape.rewind)
        setStatus("Tape rewound to start (pos 0).")
    end, C.BTN_BG, C.CYAN)

    addButton("t_seek_m", 55, 15, 10, 2, "[ ⏪ -5s ]", function()
        pcall(H.tape.seek, -5000)
        setStatus("Tape seeked -5 seconds.")
    end)

    addButton("t_seek_p", 66, 15, 10, 2, "[ +5s ⏩ ]", function()
        pcall(H.tape.seek, 5000)
        setStatus("Tape seeked +5 seconds.")
    end)

    -- Volume & Speed Adjusters
    setColors(C.TEXT, C.PANEL_BG)
    gpu.set(5, 18, string.format("Volume: %3d%%", math.floor(vol * 100)))
    addButton("t_vol_dn", 20, 18, 5, 1, " - ", function()
        pcall(H.tape.setVolume, math.max(0, vol - 0.1))
        setStatus(string.format("Tape volume: %d%%", math.floor((vol - 0.1) * 100)))
    end)
    addButton("t_vol_up", 26, 18, 5, 1, " + ", function()
        pcall(H.tape.setVolume, math.min(1.0, vol + 0.1))
        setStatus(string.format("Tape volume: %d%%", math.floor((vol + 0.1) * 100)))
    end)

    gpu.set(35, 18, string.format("Speed: %.1fx", spd))
    addButton("t_spd_05", 48, 18, 6, 1, "0.5x", function() pcall(H.tape.setSpeed, 0.5) setStatus("Speed: 0.5x") end)
    addButton("t_spd_10", 55, 18, 6, 1, "1.0x", function() pcall(H.tape.setSpeed, 1.0) setStatus("Speed: 1.0x") end)
    addButton("t_spd_15", 62, 18, 6, 1, "1.5x", function() pcall(H.tape.setSpeed, 1.5) setStatus("Speed: 1.5x") end)
    addButton("t_spd_20", 69, 18, 6, 1, "2.0x", function() pcall(H.tape.setSpeed, 2.0) setStatus("Speed: 2.0x") end)

    -- Data Read / Write
    drawBox(5, 20, screenW - 10, 3, C.PANEL_ALT)
    addButton("t_read_txt", 7, 21, 15, 1, "[ 📖 READ TEXT ]", function()
        local ok, data = pcall(H.tape.read, 128)
        if ok and data then
            tapeLastReadText = tostring(data)
            setStatus("Read " .. #tapeLastReadText .. " bytes from tape.")
        else
            tapeLastReadText = "[No data / End of tape]"
        end
    end, C.BTN_BG, C.CYAN)

    addButton("t_write_txt", 24, 21, 16, 1, "[ ✍ WRITE TEXT ]", function()
        local txt = promptInput("Enter text to burn to tape", "Hello from Computronics Tape Drive!")
        if txt and txt ~= "" then
            local ok, written = pcall(H.tape.write, txt)
            setStatus(string.format("Written %s bytes to magnetic tape.", tostring(written or #txt)))
        end
    end, C.BTN_BG, C.ORANGE)

    setColors(C.TEXT_MUTED, C.PANEL_ALT)
    local preview = tapeLastReadText ~= "" and tapeLastReadText or "(click Read to read tape bytes)"
    gpu.set(42, 21, "Data: " .. unicode.sub(preview, 1, screenW - 46))
end

-- ============================================================================
-- TAB 5: CHAT BOX & INTERCOM
-- ============================================================================
local function renderChat()
    drawBox(3, 5, screenW - 6, screenH - 7, C.PANEL_BG)
    setColors(C.CYAN, C.PANEL_BG)
    gpu.set(5, 6, "CHAT BOX & AUTOMATED INTERCOM SYSTEM")

    if not H.chat then
        setColors(C.RED, C.PANEL_BG)
        gpu.set(5, 9, "Chat Box block is not connected!")
        return
    end

    -- Controls
    setColors(C.TEXT, C.PANEL_BG)
    gpu.set(5, 8, "Mode:")
    addButton("c_mode_say", 12, 8, 12, 1, "[ BROADCAST ]", function() chatMode = "say" setStatus("Mode: Broadcast (say)") end,
        chatMode == "say" and C.TAB_ACTIVE or C.BTN_BG, chatMode == "say" and C.BG or C.TEXT)
    addButton("c_mode_tell", 26, 8, 12, 1, "[ WHISPER ]", function() chatMode = "tell" setStatus("Mode: Private Whisper (tell)") end,
        chatMode == "tell" and C.TAB_ACTIVE or C.BTN_BG, chatMode == "tell" and C.BG or C.TEXT)

    if chatMode == "tell" then
        addButton("c_set_target", 40, 8, 18, 1, "[ TARGET: " .. chatTarget .. " ]", function()
            chatTarget = promptInput("Whisper Target Player Name", chatTarget)
        end, C.PANEL_ALT, C.YELLOW)
    end

    addButton("c_set_prefix", 60, 8, 15, 1, "[ TAG: " .. chatPrefix .. " ]", function()
        chatPrefix = promptInput("Chat Custom Prefix Tag", chatPrefix)
        pcall(H.chat.setName, chatPrefix)
    end, C.PANEL_ALT, C.PURPLE)

    -- Message input field
    setColors(C.TEXT, C.PANEL_BG)
    gpu.set(5, 10, "Message:")
    drawBox(14, 10, screenW - 46, 1, C.PANEL_ALT)
    setColors(C.TEXT, C.PANEL_ALT)
    gpu.set(15, 10, unicode.sub(chatInputBuffer, 1, screenW - 48))

    addButton("c_edit_msg", screenW - 30, 10, 8, 1, "[ EDIT ]", function()
        chatInputBuffer = promptInput("Enter chat message", chatInputBuffer)
    end, C.BTN_BG, C.CYAN)

    addButton("c_send_msg", screenW - 21, 10, 8, 1, "[ SEND ]", function()
        if chatMode == "say" then
            pcall(H.chat.say, chatInputBuffer, chatDistance, chatPrefix)
            setStatus("Broadcasted chat: " .. chatInputBuffer)
        else
            pcall(H.chat.tell, chatTarget, chatInputBuffer, chatPrefix)
            setStatus("Whispered to " .. chatTarget .. ": " .. chatInputBuffer)
        end
        table.insert(logs.chat, 1, os.date("%H:%M:%S") .. " [SENT] " .. chatInputBuffer)
    end, C.BTN_BG, C.GREEN)

    if H.speech then
        addButton("c_tts_speak", screenW - 12, 10, 10, 1, "[ 🗣 TTS ]", function()
            pcall(H.speech.say, chatInputBuffer)
            setStatus("Speech Box vocalized: " .. chatInputBuffer)
        end, C.BTN_BG, C.YELLOW)
    end

    -- Intercom Log Window
    drawBox(5, 12, screenW - 10, screenH - 14, C.PANEL_ALT)
    setColors(C.YELLOW, C.PANEL_ALT)
    gpu.set(7, 13, "LIVE CHAT INTERCOM LOG (LAST 8 MESSAGES):")

    if #logs.chat == 0 then
        setColors(C.TEXT_MUTED, C.PANEL_ALT)
        gpu.set(7, 15, "No chat messages captured yet. Send a message or chat in-game!")
    else
        for i = 1, math.min(#logs.chat, screenH - 18) do
            setColors(C.TEXT, C.PANEL_ALT)
            gpu.set(7, 14 + i, unicode.sub(logs.chat[i], 1, screenW - 16))
        end
    end
end

-- ============================================================================
-- TAB 6: ACOUSTIC SCULK SENSOR
-- ============================================================================
local function renderAcoustic()
    drawBox(3, 5, screenW - 6, screenH - 7, C.PANEL_BG)
    setColors(C.CYAN, C.PANEL_BG)
    gpu.set(5, 6, "SCULK ACOUSTIC VIBRATION SEISMOGRAPH")

    if not H.acoustic then
        setColors(C.RED, C.PANEL_BG)
        gpu.set(5, 9, "Acoustic Sensor block is not connected!")
        return
    end

    local sens = 16
    local lastVib = nil
    pcall(function()
        sens = H.acoustic.getSensitivity()
        lastVib = H.acoustic.getLastVibration()
    end)

    setColors(C.TEXT, C.PANEL_BG)
    gpu.set(5, 8, "Vibration Sensitivity:")
    local sensPresets = {8, 16, 24, 32}
    for i, s in ipairs(sensPresets) do
        local isAct = (sens == s)
        addButton("ac_sens_" .. s, 28 + (i - 1) * 8, 8, 7, 1, " " .. s .. " ", function()
            pcall(H.acoustic.setSensitivity, s)
            setStatus("Acoustic sensitivity set to " .. s)
        end, isAct and C.TAB_ACTIVE or C.BTN_BG, isAct and C.BG or C.TEXT)
    end

    -- Seismograph box
    drawBox(5, 10, screenW - 10, 6, C.PANEL_ALT)
    setColors(C.YELLOW, C.PANEL_ALT)
    gpu.set(7, 11, "ACOUSTIC SENSOR READOUT:")

    if lastVib and type(lastVib) == "table" then
        setColors(C.GREEN, C.PANEL_ALT)
        gpu.set(7, 12, string.format("Event:     %-24s | Frequency: %d (GameEvent)", tostring(lastVib.event), tonumber(lastVib.frequency) or 0))
        setColors(C.TEXT, C.PANEL_ALT)
        gpu.set(7, 13, string.format("Distance:  %.2f blocks                  | Source:    X:%d Y:%d Z:%d",
            tonumber(lastVib.distance) or 0, tonumber(lastVib.x) or 0, tonumber(lastVib.y) or 0, tonumber(lastVib.z) or 0))
        gpu.set(7, 14, string.format("Entity:    %-24s | Occlusion: None (Dampening respected)", tostring(lastVib.entity or "World/Block")))
    else
        setColors(C.TEXT_MUTED, C.PANEL_ALT)
        gpu.set(7, 13, "Listening... Sneaking entities and wool blocks are dampened and ignored.")
    end

    -- Vibration History Log
    drawBox(5, 17, screenW - 10, screenH - 19, C.PANEL_ALT)
    setColors(C.CYAN, C.PANEL_ALT)
    gpu.set(7, 18, "VIBRATION SEISMOGRAPH EVENT LOG:")

    if #logs.acoustic == 0 then
        setColors(C.TEXT_MUTED, C.PANEL_ALT)
        gpu.set(7, 20, "No vibrations logged yet. Walk, jump, or place blocks nearby!")
    else
        for i = 1, math.min(#logs.acoustic, screenH - 22) do
            setColors(C.TEXT, C.PANEL_ALT)
            gpu.set(7, 19 + i, unicode.sub(logs.acoustic[i], 1, screenW - 16))
        end
    end
end

-- ============================================================================
-- TAB 7: WEATHER STATION
-- ============================================================================
local function renderWeather()
    drawBox(3, 5, screenW - 6, screenH - 7, C.PANEL_BG)
    setColors(C.CYAN, C.PANEL_BG)
    gpu.set(5, 6, "METEOROLOGICAL OBSERVATORY & ATMOSPHERIC SENSORS")

    if not H.weather then
        setColors(C.RED, C.PANEL_BG)
        gpu.set(5, 9, "Weather Station block is not connected!")
        return
    end

    local isRain = false
    local isThunder = false
    local rainStr = 0.0
    local thunderStr = 0.0
    local temp = 0.8
    local light = 15
    local moon = 0

    pcall(function()
        isRain = H.weather.isRaining()
        isThunder = H.weather.isThundering()
        rainStr = H.weather.getRainStrength()
        thunderStr = H.weather.getThunderStrength()
        temp = H.weather.getTemperature()
        light = H.weather.getLightLevel()
        moon = H.weather.getMoonPhase()
    end)

    -- Weather condition badge
    local condStr = "CLEAR SKY"
    local condCol = C.GREEN
    if isThunder then
        condStr = "THUNDERSTORM WARNING"
        condCol = C.RED
    elseif isRain then
        condStr = "PRECIPITATION / RAIN"
        condCol = C.YELLOW
    end

    drawBox(5, 8, screenW - 10, 4, C.PANEL_ALT)
    setColors(condCol, C.PANEL_ALT)
    gpu.set(7, 9, string.format("[ ATMOSPHERIC STATUS: %s ]", condStr))

    -- Rain Bar
    local rainPct = math.floor(rainStr * 100)
    setColors(C.TEXT, C.PANEL_ALT)
    gpu.set(7, 10, string.format("Rain Intensity: %3d%% [", rainPct))
    local bW = 20
    local filled = math.floor((rainPct / 100) * bW)
    setColors(C.CYAN, C.PANEL_ALT)
    gpu.set(29, 10, string.rep("█", filled) .. string.rep("░", bW - filled))
    setColors(C.TEXT, C.PANEL_ALT)
    gpu.set(50, 10, string.format("] Thunder: %3d%%", math.floor(thunderStr * 100)))

    -- Detailed Metrics Panel
    drawBox(5, 13, screenW - 10, screenH - 15, C.PANEL_ALT)
    setColors(C.CYAN, C.PANEL_ALT)
    gpu.set(7, 14, "ENVIRONMENTAL READOUTS:")

    setColors(C.TEXT, C.PANEL_ALT)
    gpu.set(7, 16, string.format("Biome Base Temperature: %.2f °C (Normalized)", temp))
    gpu.set(7, 17, string.format("Surface Light Exposure:  %d / 15 Raw Luminance", light))
    gpu.set(7, 18, string.format("Lunar Phase:             Phase %d / 7", moon))

    addButton("w_refresh", screenW - 22, 14, 15, 1, "[ 🔄 REFRESH ]", function()
        setStatus("Meteorological parameters refreshed.")
    end, C.BTN_BG, C.CYAN)
end

-- ============================================================================
-- TAB 8: INDUCTIVE CHARGER
-- ============================================================================
local function renderCharger()
    drawBox(3, 5, screenW - 6, screenH - 7, C.PANEL_BG)
    setColors(C.CYAN, C.PANEL_BG)
    gpu.set(5, 6, "INDUCTIVE WIRELESS CHARGE PAD")

    if not H.charger then
        setColors(C.RED, C.PANEL_BG)
        gpu.set(5, 9, "Inductive Charger block is not connected!")
        return
    end

    local charging = false
    local rate = 100.0
    pcall(function()
        charging = H.charger.isCharging()
        rate = H.charger.getChargeRate()
    end)

    drawBox(5, 8, screenW - 10, 4, C.PANEL_ALT)
    setColors(charging and C.GREEN or C.RED, C.PANEL_ALT)
    gpu.set(7, 9, string.format("[ POWER EMITTER: %s ]", charging and "TRANSMITTING WIRELESS FLUX" or "OFFLINE / STANDBY"))

    addButton("ch_toggle", 7, 10, 20, 1, charging and "[ DISABLE CHARGER ]" or "[ ENABLE CHARGER ]", function()
        pcall(H.charger.setCharging, not charging)
        setStatus("Inductive charger set to: " .. (not charging and "ACTIVE" or "DISABLED"))
    end, charging and C.RED or C.GREEN, C.TEXT)

    -- Charge rate controls
    setColors(C.TEXT, C.PANEL_BG)
    gpu.set(5, 13, "Charge Transfer Rate:")
    local rates = {50, 100, 250, 500, 1000}
    for i, r in ipairs(rates) do
        local isAct = (math.floor(rate) == r)
        addButton("ch_rate_" .. r, 28 + (i - 1) * 10, 13, 9, 1, " " .. r .. " EU ", function()
            pcall(H.charger.setChargeRate, r)
            setStatus("Charge rate set to " .. r .. " EU/tick")
        end, isAct and C.TAB_ACTIVE or C.BTN_BG, isAct and C.BG or C.TEXT)
    end

    -- Info Card
    drawBox(5, 15, screenW - 10, screenH - 17, C.PANEL_ALT)
    setColors(C.YELLOW, C.PANEL_ALT)
    gpu.set(7, 16, "MULTIBLOCK PLATFORM INFORMATION:")
    setColors(C.TEXT, C.PANEL_ALT)
    gpu.set(7, 18, "• Supports OpenComputers Robots standing directly on pad.")
    gpu.set(7, 19, "• Wirelessly charges Drones flying up to 4 blocks vertically above pad.")
    gpu.set(7, 20, "• Tiles seamlessly connect into 2x2, 5x5, or 10x10 charging landing fields!")
end

-- ============================================================================
-- TAB 9: LASER TRANSCEIVER
-- ============================================================================
local function renderLaser()
    drawBox(3, 5, screenW - 6, screenH - 7, C.PANEL_BG)
    setColors(C.CYAN, C.PANEL_BG)
    gpu.set(5, 6, "POINT-TO-POINT OPTICAL LASER TRANSCEIVER")

    if not H.laser then
        setColors(C.RED, C.PANEL_BG)
        gpu.set(5, 9, "Laser Transceiver block is not connected!")
        return
    end

    local linked = false
    local dist = -1
    local broken = false
    pcall(function()
        linked = H.laser.isConnected()
        dist = H.laser.getDistance()
        broken = H.laser.isBeamBroken()
    end)

    drawBox(5, 8, screenW - 10, 5, C.PANEL_ALT)
    setColors(linked and C.GREEN or C.YELLOW, C.PANEL_ALT)
    gpu.set(7, 9, string.format("OPTICAL LINK: %s", linked and "CONNECTED (BEAM ALIGNED)" or "SEARCHING FOR MATCHING TARGET"))

    setColors(broken and C.RED or C.GREEN, C.PANEL_ALT)
    gpu.set(7, 10, string.format("TRIPWIRE SECURITY: %s", broken and "BREACH DETECTED (BEAM OCCLUDED)" or "SECURE (UNBROKEN)"))

    setColors(C.TEXT, C.PANEL_ALT)
    gpu.set(7, 11, string.format("Distance to opposing Transceiver: %s", dist > 0 and (dist .. " meters") or "Out of range / Blocked"))

    addButton("ls_pulse", 5, 14, 22, 2, "[ ⚡ SEND TEST PACKET ]", function()
        if H.modem then
            pcall(H.modem.broadcast, 4096, "LASER_OPTICAL_PULSE")
            setStatus("Transmitted test optical pulse across laser bridge.")
        else
            setStatus("No modem card found to inject optical packet.")
        end
    end, C.BTN_BG, C.CYAN)

    -- Tripwire Log
    drawBox(5, 17, screenW - 10, screenH - 19, C.PANEL_ALT)
    setColors(C.YELLOW, C.PANEL_ALT)
    gpu.set(7, 18, "OPTICAL TRIPWIRE SECURITY LOG:")
    if #logs.tripwire == 0 then
        setColors(C.TEXT_MUTED, C.PANEL_ALT)
        gpu.set(7, 20, "No tripwire events recorded. Walk across the laser beam to test!")
    else
        for i = 1, math.min(#logs.tripwire, screenH - 22) do
            setColors(C.RED, C.PANEL_ALT)
            gpu.set(7, 19 + i, logs.tripwire[i])
        end
    end
end

-- ============================================================================
-- TAB 10: ANALOG DIAL GAUGE
-- ============================================================================
local function renderGauge()
    drawBox(3, 5, screenW - 6, screenH - 7, C.PANEL_BG)
    setColors(C.CYAN, C.PANEL_BG)
    gpu.set(5, 6, "PHYSICAL ANALOG DIAL GAUGE METER")

    if not H.gauge then
        setColors(C.RED, C.PANEL_BG)
        gpu.set(5, 9, "Analog Gauge block is not connected!")
        return
    end

    local val = 0.5
    local lbl = ""
    local col = 0xFFFFFF
    pcall(function()
        val = H.gauge.getValue()
        lbl = H.gauge.getLabel()
        col = H.gauge.getColor()
    end)

    -- Dial representation
    drawBox(5, 8, screenW - 10, 7, C.PANEL_ALT)
    setColors(C.YELLOW, C.PANEL_ALT)
    gpu.set(7, 9, string.format("GAUGE DIAL: [ %s ]", lbl ~= "" and lbl or "NO LABEL"))

    local meterW = screenW - 24
    local needlePos = math.floor(val * meterW)
    setColors(C.BORDER, C.PANEL_ALT)
    gpu.set(7, 11, "0.0 " .. string.rep("─", meterW) .. " 1.0")

    setColors(C.CYAN, C.PANEL_ALT)
    local needleLine = string.rep(" ", needlePos) .. "▲"
    gpu.set(11, 12, needleLine)

    setColors(C.TEXT, C.PANEL_ALT)
    gpu.set(7, 13, string.format("Value: %.3f (%.1f%%)", val, val * 100))

    -- Value Preset Buttons
    setColors(C.TEXT, C.PANEL_BG)
    gpu.set(5, 16, "Set Needle Value:")
    local presets = {0.0, 0.25, 0.50, 0.75, 1.0}
    for i, p in ipairs(presets) do
        addButton("g_p_" .. i, 23 + (i - 1) * 9, 16, 8, 1, string.format("%.2f", p), function()
            pcall(H.gauge.setValue, p)
            setStatus(string.format("Gauge set to %.2f", p))
        end)
    end

    addButton("g_dn", 5, 18, 12, 1, "[ - 0.05 ]", function()
        local nv = math.max(0.0, val - 0.05)
        pcall(H.gauge.setValue, nv)
        setStatus(string.format("Gauge set to %.2f", nv))
    end)
    addButton("g_up", 18, 18, 12, 1, "[ + 0.05 ]", function()
        local nv = math.min(1.0, val + 0.05)
        pcall(H.gauge.setValue, nv)
        setStatus(string.format("Gauge set to %.2f", nv))
    end)

    addButton("g_edit_lbl", 32, 18, 18, 1, "[ ✏ EDIT LABEL ]", function()
        local nlbl = promptInput("Enter custom gauge dial label", lbl)
        pcall(H.gauge.setLabel, nlbl)
        setStatus("Gauge label updated to: " .. nlbl)
    end, C.BTN_BG, C.CYAN)
end

-- ============================================================================
-- TAB 11: NETWORK CARD TIER 3 & PACKET SNIFFER (TAP)
-- ============================================================================
local function renderNetwork()
    drawBox(3, 5, screenW - 6, screenH - 7, C.PANEL_BG)
    setColors(C.CYAN, C.PANEL_BG)
    gpu.set(5, 6, "HIGH-THROUGHPUT TIER 3 NIC & PACKET SNIFFER (TAP)")

    -- Transmitter Box
    drawBox(5, 8, screenW - 10, 5, C.PANEL_ALT)
    setColors(C.YELLOW, C.PANEL_ALT)
    gpu.set(7, 9, "ETHERNET FRAME TRANSMITTER (CROSS-RELAY):")

    setColors(C.TEXT, C.PANEL_ALT)
    gpu.set(7, 10, string.format("Target Port: %d | Jumbo Frames: 64 KB Enabled | Hardware Spoof: %s",
        netPort, useSpoof and netSpoofMac or "OFF"))

    addButton("net_send", 7, 11, 24, 1, "[ 🚀 SEND PING PACKET ]", function()
        if H.modem then
            if useSpoof then
                if netTarget and netTarget ~= "" then
                    pcall(H.modem.send, netTarget, netSpoofMac, netPort, netPayload)
                else
                    pcall(H.modem.broadcast, netSpoofMac, netPort, netPayload)
                end
            else
                if netTarget and netTarget ~= "" then
                    pcall(H.modem.send, netTarget, netPort, netPayload)
                else
                    pcall(H.modem.broadcast, netPort, netPayload)
                end
            end
            setStatus("Packet sent across relay on port " .. netPort)
        else
            setStatus("No modem card found to transmit.")
        end
    end, C.BTN_BG, C.GREEN)

    addButton("net_t_spoof", 33, 11, 16, 1, useSpoof and "[ SPOOF: ON ]" or "[ SPOOF: OFF ]", function()
        useSpoof = not useSpoof
        setStatus("Source MAC spoofing: " .. (useSpoof and "ENABLED" or "DISABLED"))
    end, useSpoof and C.PURPLE or C.BTN_BG, C.TEXT)

    addButton("net_edit_msg", 51, 11, 16, 1, "[ EDIT PAYLOAD ]", function()
        netPayload = promptInput("Packet Payload Text", netPayload)
    end, C.BTN_BG, C.CYAN)

    -- Sniffer TAP
    local bps = 0
    local pps = 0
    if H.sniffer then
        pcall(function()
            bps = H.sniffer.getBandwidth()
            pps = H.sniffer.getPacketsPerSecond()
        end)
    end

    drawBox(5, 14, screenW - 10, screenH - 16, C.PANEL_ALT)
    setColors(C.CYAN, C.PANEL_ALT)
    gpu.set(7, 15, string.format("PROMISCUOUS TAP MONITOR (Traffic: %d BPS | Rate: %d PPS):", bps, pps))

    if #logs.packets == 0 then
        setColors(C.TEXT_MUTED, C.PANEL_ALT)
        gpu.set(7, 17, "No packets sniffed. Send a packet above to capture frame traversing the wire!")
    else
        for i = 1, math.min(#logs.packets, screenH - 19) do
            setColors(C.TEXT, C.PANEL_ALT)
            gpu.set(7, 16 + i, unicode.sub(logs.packets[i], 1, screenW - 16))
        end
    end
end

-- ============================================================================
-- TAB 12: RF SIGNAL JAMMER
-- ============================================================================
local function renderJammer()
    drawBox(3, 5, screenW - 6, screenH - 7, C.PANEL_BG)
    setColors(C.CYAN, C.PANEL_BG)
    gpu.set(5, 6, "ELECTRONIC WARFARE // RF SIGNAL JAMMER (BLOCK & CARD)")

    if not H.jammer then
        setColors(C.RED, C.PANEL_BG)
        gpu.set(5, 9, "RF Jammer (block or card) is not installed!")
        return
    end

    local en = false
    local freq = 0
    local pwr = 10.0
    local rad = 7.5
    pcall(function()
        en = H.jammer.isEnabled()
        freq = H.jammer.getFrequency()
        pwr = H.jammer.getPower()
        rad = H.jammer.getRadius()
    end)

    drawBox(5, 8, screenW - 10, 4, C.PANEL_ALT)
    setColors(en and C.RED or C.GREEN, C.PANEL_ALT)
    gpu.set(7, 9, string.format("[ JAMMER TRANSMITTER: %s ]", en and "TRANSMITTING HIGH-POWER NOISE" or "STANDBY"))

    addButton("jm_toggle", 7, 10, 20, 1, en and "[ DEACTIVATE JAMMER ]" or "[ ACTIVATE JAMMER ]", function()
        pcall(H.jammer.setEnabled, not en)
        setStatus("RF Jammer state: " .. (not en and "ACTIVE TRANSMITTING" or "STANDBY"))
    end, en and C.RED or C.GREEN, C.TEXT)

    -- Frequency Presets
    setColors(C.TEXT, C.PANEL_BG)
    gpu.set(5, 13, "Jamming Frequency:")
    addButton("jm_f_0", 25, 13, 16, 1, "[ 0: BROAD SPECTRUM ]", function()
        pcall(H.jammer.setFrequency, 0)
        setStatus("Jammer set to Broad Spectrum (all channels).")
    end, freq == 0 and C.TAB_ACTIVE or C.BTN_BG, freq == 0 and C.BG or C.TEXT)

    addButton("jm_f_4096", 43, 13, 14, 1, "[ PORT: 4096 ]", function()
        pcall(H.jammer.setFrequency, 4096)
        setStatus("Jammer set to frequency 4096.")
    end, freq == 4096 and C.TAB_ACTIVE or C.BTN_BG, freq == 4096 and C.BG or C.TEXT)

    -- Power / Radius
    gpu.set(5, 15, "Power Output:")
    local pwrs = {10, 25, 50, 100}
    for i, p in ipairs(pwrs) do
        local isAct = (math.floor(pwr) == p)
        addButton("jm_p_" .. p, 20 + (i - 1) * 14, 15, 12, 1, string.format("%dW (%.1fm)", p, p * 0.75), function()
            pcall(H.jammer.setPower, p)
            setStatus(string.format("Jammer power set to %dW (Radius: %.1fm)", p, p * 0.75))
        end, isAct and C.TAB_ACTIVE or C.BTN_BG, isAct and C.BG or C.TEXT)
    end

    -- Airspace check
    drawBox(5, 17, screenW - 10, screenH - 19, C.PANEL_ALT)
    setColors(C.YELLOW, C.PANEL_ALT)
    gpu.set(7, 18, "LOCAL RADIO SPECTRUM PROBE:")

    local isJammed = false
    pcall(function()
        if H.jammer.isAreaJammed then
            isJammed = H.jammer.isAreaJammed(freq)
        end
    end)

    setColors(isJammed and C.RED or C.GREEN, C.PANEL_ALT)
    gpu.set(7, 20, isJammed and "⚠️ ALERT: Local airspace is heavily jammed and corrupted!" or "✔ CLEAR: Local radio frequencies are clear of interference.")
end

-- ============================================================================
-- TAB 13: SELF-DESTRUCT (CARD BOOM)
-- ============================================================================
local function renderBoom()
    drawBox(3, 5, screenW - 6, screenH - 7, C.PANEL_BG)
    setColors(C.RED, C.PANEL_BG)
    gpu.set(5, 6, "TACTICAL THERMAL DETONATION MODULE (CARD BOOM)")

    if not H.boom then
        setColors(C.RED, C.PANEL_BG)
        gpu.set(5, 9, "Self-Destruct Card is not inserted in this computer!")
        return
    end

    drawBox(5, 8, screenW - 10, 4, C.PANEL_ALT)
    setColors(boomArmed and C.RED or C.GREEN, C.PANEL_ALT)
    gpu.set(7, 9, string.format("SAFETY STATUS: %s", boomArmed and "⚠️ ARMED (READY TO DETONATE)" or "🔒 SAFE (DETONATOR LOCKED)"))

    addButton("bm_safety", 7, 10, 24, 1, boomArmed and "[ 🔒 ENGAGE SAFETY ]" or "[ ⚠️ DISARM SAFETY ]", function()
        boomArmed = not boomArmed
        setStatus("Safety lock: " .. (boomArmed and "ARMED - DETONATION UNLOCKED!" or "SAFE"))
    end, boomArmed and C.RED or C.GREEN, C.TEXT)

    if boomArmed then
        setColors(C.YELLOW, C.PANEL_BG)
        gpu.set(5, 13, "TIMED DETONATION TRIGGER:")

        addButton("bm_5s",  5,  15, 14, 2, "[ ⏱ 5 SECONDS ]", function()
            pcall(H.boom.start, 5)
            setStatus("FUSE STARTED: 5 SECONDS TO DETONATION!")
        end, C.RED, C.TEXT)

        addButton("bm_10s", 21, 15, 15, 2, "[ ⏱ 10 SECONDS ]", function()
            pcall(H.boom.start, 10)
            setStatus("FUSE STARTED: 10 SECONDS TO DETONATION!")
        end, C.RED, C.TEXT)

        addButton("bm_30s", 38, 15, 15, 2, "[ ⏱ 30 SECONDS ]", function()
            pcall(H.boom.start, 30)
            setStatus("FUSE STARTED: 30 SECONDS TO DETONATION!")
        end, C.RED, C.TEXT)

        addButton("bm_abort", 55, 15, 16, 2, "[ 🛑 ABORT FUSE ]", function()
            pcall(H.boom.abort)
            setStatus("Self-destruct sequence aborted.")
        end, C.PANEL_ALT, C.YELLOW)

        drawBox(5, 18, screenW - 10, screenH - 20, C.PANEL_ALT)
        setColors(C.RED, C.PANEL_ALT)
        gpu.set(7, 19, "DANGER: HIGH-POWER THERMAL CHARGE WILL DESTROY COMPUTER AND ADJACENT BLOCKS!")
    else
        setColors(C.TEXT_MUTED, C.PANEL_BG)
        gpu.set(5, 14, "Detonation triggers are locked. Click [ DISARM SAFETY ] above to access fuses.")
    end
end

-- ============================================================================
-- TAB 14: IRON NOTE BLOCK
-- ============================================================================
local noteInstrument = "harp"
local noteOctave = 0
local noteVolume = 1.0

local function renderIronNote()
    drawBox(3, 5, screenW - 6, screenH - 7, C.PANEL_BG)
    setColors(C.CYAN, C.PANEL_BG)
    gpu.set(5, 6, "PROGRAMMABLE IRON NOTE SYNTHESIZER (MIDI / INSTRUMENTS)")

    if not H.note then
        setColors(C.RED, C.PANEL_BG)
        gpu.set(5, 9, "Iron Note Block is not connected to this network!")
        gpu.set(5, 11, "Connect the block with an OpenComputers cable.")
        return
    end

    setColors(C.TEXT, C.PANEL_BG)
    gpu.set(5, 8, "Instrument:")
    local insts = {"harp", "basedrum", "snare", "hat", "bass", "bell", "flute", "guitar", "chime", "iron_xylophone", "bit", "pling"}
    for i, inst in ipairs(insts) do
        local isAct = (noteInstrument == inst)
        local bx = 18 + ((i - 1) % 6) * 10
        local by = 8 + math.floor((i - 1) / 6)
        addButton("inst_" .. inst, bx, by, 9, 1, " " .. unicode.sub(inst, 1, 7) .. " ", function()
            noteInstrument = inst
            setStatus("Selected instrument: " .. inst)
        end, isAct and C.TAB_ACTIVE or C.BTN_BG, isAct and C.BG or C.TEXT)
    end

    gpu.set(5, 11, "Octave: ")
    addButton("oct_0", 14, 11, 7, 1, " Low ", function() noteOctave = 0 setStatus("Octave: Low (0-12)") end, noteOctave == 0 and C.TAB_ACTIVE or C.BTN_BG, noteOctave == 0 and C.BG or C.TEXT)
    addButton("oct_1", 22, 11, 7, 1, " High ", function() noteOctave = 12 setStatus("Octave: High (12-24)") end, noteOctave == 12 and C.TAB_ACTIVE or C.BTN_BG, noteOctave == 12 and C.BG or C.TEXT)

    gpu.set(33, 11, "Volume: ")
    local vols = {0.25, 0.5, 0.75, 1.0}
    for i, v in ipairs(vols) do
        local isAct = (noteVolume == v)
        addButton("vol_" .. i, 42 + (i - 1) * 7, 11, 6, 1, string.format("%d%%", v * 100), function()
            noteVolume = v
            setStatus(string.format("Volume set to %.0f%%", v * 100))
        end, isAct and C.TAB_ACTIVE or C.BTN_BG, isAct and C.BG or C.TEXT)
    end

    drawBox(5, 13, screenW - 10, 6, C.PANEL_ALT)
    setColors(C.YELLOW, C.PANEL_ALT)
    gpu.set(7, 14, "INTERACTIVE PIANO KEYBOARD (CLICK KEY TO EMIT NOTE):")

    local keys = {
        {"C", 0}, {"C#", 1}, {"D", 2}, {"D#", 3}, {"E", 4}, {"F", 5},
        {"F#", 6}, {"G", 7}, {"G#", 8}, {"A", 9}, {"A#", 10}, {"B", 11}, {"C+", 12}
    }

    local keyX = 7
    for _, k in ipairs(keys) do
        local noteNum = noteOctave + k[2]
        local isSharp = k[1]:find("#")
        local bgCol = isSharp and 0x222222 or 0xDFE6E9
        local fgCol = isSharp and 0xFFFFFF or 0x111111
        addButton("pkey_" .. k[2], keyX, 16, 5, 2, " " .. k[1] .. " ", function()
            pcall(H.note.playNote, noteInstrument, noteNum, noteVolume)
            setStatus(string.format("Played note %s (%d) on %s", k[1], noteNum, noteInstrument))
        end, bgCol, fgCol)
        keyX = keyX + 5
    end

    addButton("ch_maj", 5, 20, 16, 1, "[ MAJOR CHORD ]", function()
        pcall(H.note.playNote, noteInstrument, noteOctave + 0, noteVolume)
        pcall(H.note.playNote, noteInstrument, noteOctave + 4, noteVolume)
        pcall(H.note.playNote, noteInstrument, noteOctave + 7, noteVolume)
        setStatus("Played Major Chord.")
    end, C.BTN_BG, C.GREEN)

    addButton("ch_min", 23, 20, 16, 1, "[ MINOR CHORD ]", function()
        pcall(H.note.playNote, noteInstrument, noteOctave + 0, noteVolume)
        pcall(H.note.playNote, noteInstrument, noteOctave + 3, noteVolume)
        pcall(H.note.playNote, noteInstrument, noteOctave + 7, noteVolume)
        setStatus("Played Minor Chord.")
    end, C.BTN_BG, C.CYAN)

    addButton("ch_arp", 41, 20, 16, 1, "[ ARPEGGIO ]", function()
        for _, n in ipairs({0, 4, 7, 12}) do
            pcall(H.note.playNote, noteInstrument, noteOctave + n, noteVolume)
        end
        setStatus("Played arpeggio cascade.")
    end, C.BTN_BG, C.YELLOW)
end

-- ============================================================================
-- TAB 15: MODULAR CONTROL PANEL
-- ============================================================================
local panelSelectedColor = 0x00FF00

local function renderControlPanel()
    drawBox(3, 5, screenW - 6, screenH - 7, C.PANEL_BG)
    setColors(C.CYAN, C.PANEL_BG)
    gpu.set(5, 6, "MODULAR CONTROL PANEL (8x8 GRID: BUTTONS & MINI LAMPS)")

    if not H.panel then
        setColors(C.RED, C.PANEL_BG)
        gpu.set(5, 9, "Control Panel block is not connected to this network!")
        gpu.set(5, 11, "Connect the block with an OpenComputers cable.")
        gpu.set(5, 13, "Right-click panel face in-world with Mini Lamps or Mini Buttons to mount them.")
        return
    end

    local elements = {}
    pcall(function() elements = H.panel.getElements() end)
    local elemMap = {}
    if type(elements) == "table" then
        for _, elem in ipairs(elements) do
            local key = (elem.x or 0) .. "_" .. (elem.y or 0)
            elemMap[key] = elem
        end
    end

    drawBox(5, 8, 36, 11, C.PANEL_ALT)
    setColors(C.YELLOW, C.PANEL_ALT)
    gpu.set(7, 9, "FRONT FACE GRID (8x8):")

    for gy = 0, 7 do
        for gx = 0, 7 do
            local key = gx .. "_" .. gy
            local elem = elemMap[key]
            local bx = 7 + gx * 4
            local by = 10 + gy
            if elem then
                if elem.type == "button" then
                    addButton("p_btn_" .. gx .. "_" .. gy, bx, by, 3, 1, " B ", function()
                        pcall(H.panel.pressButton, gx, gy)
                        setStatus(string.format("Triggered button at (%d, %d)", gx, gy))
                    end, C.YELLOW, C.BG)
                elseif elem.type == "lamp" then
                    local col = (elem.color and elem.color ~= 0) and elem.color or 0x00FF00
                    addButton("p_lmp_" .. gx .. "_" .. gy, bx, by, 3, 1, " * ", function()
                        pcall(H.panel.setLampColor, gx, gy, panelSelectedColor)
                        setStatus(string.format("Set lamp at (%d, %d) to color #%06X", gx, gy, panelSelectedColor))
                    end, col, 0x000000)
                end
            else
                setColors(C.TEXT_MUTED, C.PANEL_ALT)
                gpu.set(bx, by, " · ")
            end
        end
    end

    local ctrlX = 43
    drawBox(ctrlX, 8, screenW - ctrlX - 4, 5, C.PANEL_ALT)
    setColors(C.CYAN, C.PANEL_ALT)
    gpu.set(ctrlX + 2, 9, "COLOR PRESETS & BATCH ACTIONS:")

    addButton("pl_col_grn", ctrlX + 2, 10, 9, 1, " GREEN ", function()
        panelSelectedColor = 0x00FF00
        for i = 1, 64 do pcall(H.panel.setLampColor, i, 0x00FF00) end
        setStatus("All lamps set to GREEN.")
    end, 0x00AA00, C.TEXT)

    addButton("pl_col_red", ctrlX + 12, 10, 9, 1, "  RED  ", function()
        panelSelectedColor = 0xFF0000
        for i = 1, 64 do pcall(H.panel.setLampColor, i, 0xFF0000) end
        setStatus("All lamps set to RED.")
    end, 0xCC0000, C.TEXT)

    addButton("pl_col_blu", ctrlX + 22, 10, 9, 1, " BLUE  ", function()
        panelSelectedColor = 0x00A8FF
        for i = 1, 64 do pcall(H.panel.setLampColor, i, 0x00A8FF) end
        setStatus("All lamps set to BLUE.")
    end, 0x0088CC, C.TEXT)

    addButton("pl_clr", ctrlX + 2, 11, 15, 1, "[ CLEAR / OFF ]", function()
        pcall(H.panel.clear)
        setStatus("All lamps turned OFF.")
    end, C.BTN_BG, C.YELLOW)

    drawBox(ctrlX, 14, screenW - ctrlX - 4, screenH - 16, C.PANEL_ALT)
    setColors(C.YELLOW, C.PANEL_ALT)
    gpu.set(ctrlX + 2, 15, "BUTTON PRESS EVENT LOG:")

    if #logs.panel == 0 then
        setColors(C.TEXT_MUTED, C.PANEL_ALT)
        gpu.set(ctrlX + 2, 17, "No button presses detected.")
        gpu.set(ctrlX + 2, 18, "Right-click a mounted button in-world!")
    else
        for i = 1, math.min(#logs.panel, screenH - 18) do
            setColors(C.TEXT, C.PANEL_ALT)
            gpu.set(ctrlX + 2, 16 + i, unicode.sub(logs.panel[i], 1, screenW - ctrlX - 6))
        end
    end
end

-- ============================================================================
-- TAB 16: PARTICLE FX & BEEP SYNTHESIZER
-- ============================================================================
local fxCount = 10
local fxSpeed = 0.2

local function renderFXBeep()
    drawBox(3, 5, screenW - 6, screenH - 7, C.PANEL_BG)
    setColors(C.CYAN, C.PANEL_BG)
    gpu.set(5, 6, "AUDIO BEEP SYNTHESIZER & PARTICLE FX EMITTER")

    local halfW = math.floor((screenW - 12) / 2)
    drawBox(5, 8, halfW, screenH - 10, C.PANEL_ALT)
    setColors(C.YELLOW, C.PANEL_ALT)
    gpu.set(7, 9, "AUDIO BEEP SYNTHESIZER (SQUARE WAVE):")

    if not H.beep then
        setColors(C.RED, C.PANEL_ALT)
        gpu.set(7, 11, "Beep Card is not installed!")
        gpu.set(7, 12, "Insert Tier 1/2 Card into computer.")
    else
        setColors(C.TEXT, C.PANEL_ALT)
        gpu.set(7, 11, "Frequency Presets:")

        addButton("bp_440", 7, 13, 14, 1, " [ 440 Hz (A4) ] ", function()
            pcall(H.beep.beep, 440, 0.25)
            setStatus("Playing 440 Hz A4 tone")
        end)
        addButton("bp_880", 23, 13, 14, 1, " [ 880 Hz (A5) ] ", function()
            pcall(H.beep.beep, 880, 0.25)
            setStatus("Playing 880 Hz A5 tone")
        end)
        addButton("bp_1200", 7, 15, 14, 1, " [ 1200 Hz High ]", function()
            pcall(H.beep.beep, 1200, 0.2)
            setStatus("Playing 1200 Hz high tone")
        end)
        addButton("bp_chord", 23, 15, 14, 1, " [ DUAL CHORD ]  ", function()
            pcall(H.beep.beep, {[440] = 0.4, [660] = 0.4})
            setStatus("Playing dual frequency chord")
        end, C.BTN_BG, C.GREEN)

        addButton("bp_alarm", 7, 17, 30, 2, "[ 🚨 WARBLE ALARM SIREN ]", function()
            pcall(H.beep.beep, {[800] = 0.15, [1200] = 0.15})
            setStatus("Alarm warble emitted")
        end, C.RED, C.TEXT)
    end

    local rX = 7 + halfW
    drawBox(rX, 8, halfW, screenH - 10, C.PANEL_ALT)
    setColors(C.CYAN, C.PANEL_ALT)
    gpu.set(rX + 2, 9, "PARTICLE FX CARD GENERATOR:")

    if not H.fx then
        setColors(C.RED, C.PANEL_ALT)
        gpu.set(rX + 2, 11, "Particle FX Card is not installed!")
        gpu.set(rX + 2, 12, "Insert Card into computer.")
    else
        setColors(C.TEXT, C.PANEL_ALT)
        gpu.set(rX + 2, 11, "Spawn Visual Particles:")

        local parts = {
            {"flame", "🔥 FLAME"},
            {"portal", "🌀 PORTAL"},
            {"sculk_charge", "🖤 SCULK"},
            {"electric_spark", "⚡ ELECTRIC"},
            {"sonic_boom", "💥 SONIC BOOM"},
            {"enchant", "✨ ENCHANT"}
        }

        for i, p in ipairs(parts) do
            local px = rX + 2 + ((i - 1) % 2) * 16
            local py = 13 + math.floor((i - 1) / 2) * 2
            addButton("fx_" .. p[1], px, py, 15, 1, "[ " .. p[2] .. " ]", function()
                pcall(H.fx.spawn, p[1], 0, 0.5, 0, fxCount, fxSpeed)
                setStatus("Spawned particles: " .. p[2])
            end, C.BTN_BG, C.CYAN)
        end

        setColors(C.TEXT, C.PANEL_ALT)
        gpu.set(rX + 2, 20, "Particle Count: ")
        local counts = {5, 15, 50}
        for i, cnt in ipairs(counts) do
            addButton("cnt_" .. cnt, rX + 18 + (i - 1) * 6, 20, 5, 1, " " .. cnt .. " ", function()
                fxCount = cnt
                setStatus("Particle count: " .. cnt)
            end, fxCount == cnt and C.TAB_ACTIVE or C.BTN_BG, fxCount == cnt and C.BG or C.TEXT)
        end
    end
end

-- ============================================================================
-- SCREEN DISPATCHER
-- ============================================================================
local function render()
    renderChrome()

    if currentTab == 1 then
        renderDashboard()
    elseif currentTab == 2 then
        renderCamera()
    elseif currentTab == 3 then
        renderRadar()
    elseif currentTab == 4 then
        renderTapeDrive()
    elseif currentTab == 5 then
        renderChat()
    elseif currentTab == 6 then
        renderAcoustic()
    elseif currentTab == 7 then
        renderWeather()
    elseif currentTab == 8 then
        renderCharger()
    elseif currentTab == 9 then
        renderLaser()
    elseif currentTab == 10 then
        renderGauge()
    elseif currentTab == 11 then
        renderNetwork()
    elseif currentTab == 12 then
        renderJammer()
    elseif currentTab == 13 then
        renderBoom()
    elseif currentTab == 14 then
        renderIronNote()
    elseif currentTab == 15 then
        renderControlPanel()
    elseif currentTab == 16 then
        renderFXBeep()
    end
end

-- ============================================================================
-- MAIN EVENT LOOP
-- ============================================================================
term.clear()
render()

while running do
    local ev, a, b, c, d, e = event.pull(0.5)

    if ev == "touch" then
        local tx, ty = b, c
        -- Check buttons
        local clicked = false
        for _, btn in ipairs(buttons) do
            if tx >= btn.x and tx <= btn.x + btn.w - 1 and ty >= btn.y and ty <= btn.y + btn.h - 1 then
                clicked = true
                if btn.onClick then
                    btn.onClick()
                end
                break
            end
        end
        render()

    elseif ev == "key_down" then
        local char = b
        if char == 113 or char == 81 then -- 'q' or 'Q'
            running = false
        elseif char >= 49 and char <= 57 then -- '1' - '9'
            currentTab = char - 48
            setStatus("Switched to Tab " .. currentTab)
            render()
        elseif char == 48 then -- '0' -> Tab 10
            currentTab = 10
            setStatus("Switched to Tab 10")
            render()
        end

    elseif ev == "chat_message" then
        -- Chat signal from Chat Box: (player, message, distance)
        local player = tostring(a or "Player")
        local msg = tostring(b or "")
        local dist = tonumber(c) or 0
        local line = string.format("[%s] <%s> %s (%.0fm)", os.date("%H:%M:%S"), player, msg, dist)
        table.insert(logs.chat, 1, line)
        if #logs.chat > 20 then table.remove(logs.chat) end
        setStatus("Intercom: " .. line)
        render()

    elseif ev == "vibration" then
        -- Acoustic sensor signal: (event, distance, frequency, x, y, z)
        local vEvent = tostring(a or "vibration")
        local vDist = tonumber(b) or 0
        local vFreq = tonumber(c) or 0
        local line = string.format("[%s] %s | Freq:%d Dist:%.1fm", os.date("%H:%M:%S"), vEvent, vFreq, vDist)
        table.insert(logs.acoustic, 1, line)
        if #logs.acoustic > 20 then table.remove(logs.acoustic) end
        setStatus("Acoustic vibration: " .. vEvent)
        render()

    elseif ev == "packet_sniffed" then
        -- Packet sniffer signal: (source, dest, port, size, summary)
        local src = unicode.sub(tostring(a or "src"), 1, 8)
        local dst = unicode.sub(tostring(b or "dst"), 1, 8)
        local port = tonumber(c) or 0
        local size = tonumber(d) or 0
        local preview = tostring(e or "")
        local line = string.format("[%s] P:%d %s->%s %dB: %s", os.date("%H:%M:%S"), port, src, dst, size, preview)
        table.insert(logs.packets, 1, line)
        if #logs.packets > 20 then table.remove(logs.packets) end
        render()

    elseif ev == "panel_button" then
        local slot = tonumber(a) or 0
        local player = tostring(b or "Player")
        local gx = tonumber(c) or 0
        local gy = tonumber(d) or 0
        local line = string.format("[%s] Button #%d by %s at (%d,%d)", os.date("%H:%M:%S"), slot, player, gx, gy)
        table.insert(logs.panel, 1, line)
        if #logs.panel > 20 then table.remove(logs.panel) end
        setStatus("Control Panel: " .. line)
        render()

    elseif ev == "tripwire_triggered" then
        local collider = tostring(a or "Entity")
        local dist = tonumber(b) or 0
        local line = string.format("[%s] TRIPWIRE BREACH by %s at %.1fm", os.date("%H:%M:%S"), collider, dist)
        table.insert(logs.tripwire, 1, line)
        if #logs.tripwire > 20 then table.remove(logs.tripwire) end
        setStatus("LASER ALARM: " .. line)
        render()

    elseif ev == "modem_message" then
        -- Standard modem message
        local recAddr = a
        local sndAddr = b
        local port = c
        local distance = d
        local payload = tostring(e or "")
        local line = string.format("[%s] RX P:%d from %s: %s", os.date("%H:%M:%S"), port, unicode.sub(tostring(sndAddr), 1, 8), payload)
        table.insert(logs.packets, 1, line)
        if #logs.packets > 20 then table.remove(logs.packets) end
        render()
    else
        -- Clock / animated transport update when on tape tab
        if currentTab == 4 and H.tape then
            local st = "STOPPED"
            pcall(function() st = H.tape.getState() end)
            if st == "PLAYING" then
                render()
            end
        end
    end
end

-- Clean Exit
gpu.setBackground(0x000000)
gpu.setForeground(0xFFFFFF)
term.clear()
print("Computronics Workstation closed cleanly.")
