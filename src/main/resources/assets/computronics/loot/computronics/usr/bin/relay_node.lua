-- Computronics: Rebooted - Remote Relay Node Daemon
-- Designed for OpenOS on OpenComputers: Rebooted (1.21.1 / NeoForge)
-- Runs on Computer 2 to communicate with Computer 1 across an OpenComputers Relay

local component = require("component")
local event = require("event")
local term = require("term")
local computer = require("computer")
local unicode = require("unicode")

local gpu = component.gpu
local modem = component.isAvailable("modem") and component.modem or nil
local sniffer = component.isAvailable("packet_sniffer") and component.packet_sniffer or nil

if not modem then
    print("Error: No network card / modem found on this computer.")
    print("Please install a Network Card or Tier 3 Network Card.")
    return
end

local LISTEN_PORT = 4096
modem.open(LISTEN_PORT)

local w, h = 80, 25
if gpu then
    w, h = gpu.getResolution()
    gpu.setBackground(0x0E1318)
    gpu.setForeground(0x00A8FF)
    term.clear()
end

local function drawHeader()
    if not gpu then return end
    gpu.setBackground(0x131A22)
    gpu.setForeground(0x00D2D3)
    gpu.fill(1, 1, w, 1, " ")
    gpu.set(2, 1, "COMPUTRONICS // REMOTE RELAY NODE (PORT " .. LISTEN_PORT .. ")")

    local hostAddr = unicode.sub(modem.address, 1, 8)
    gpu.setForeground(0x8395A7)
    gpu.set(w - 28, 1, "ADDR: " .. hostAddr .. " | [Q]UIT")

    gpu.setBackground(0x0E1318)
    gpu.setForeground(0x2F3B4C)
    gpu.set(1, 2, string.rep("─", w))
    gpu.set(1, h - 2, string.rep("─", w))

    gpu.setBackground(0x131A22)
    gpu.setForeground(0xF1C40F)
    gpu.fill(1, h - 1, w, 2, " ")
    gpu.set(2, h - 1, "▶ Status: Listening for cross-relay packets...")
    gpu.setForeground(0xDFE6E9)
    gpu.set(2, h, "Commands: [P] Send test ping to Computer 1  |  [C] Clear logs  |  [Q] Exit")
end

drawHeader()

local logs = {}
local packetCount = 0

local function addLog(col, text)
    local line = string.format("[%s] %s", os.date("%H:%M:%S"), text)
    table.insert(logs, {color = col, text = line})
    if #logs > (h - 6) then
        table.remove(logs, 1)
    end

    -- Redraw logs
    if gpu then
        for i, entry in ipairs(logs) do
            local y = 2 + i
            gpu.setBackground(0x0E1318)
            gpu.setForeground(entry.color)
            gpu.fill(1, y, w, 1, " ")
            gpu.set(2, y, unicode.sub(entry.text, 1, w - 2))
        end
    else
        print(line)
    end
end

addLog(0x2ECC71, "Remote Node initialized on port " .. LISTEN_PORT .. " (Address: " .. modem.address .. ")")
addLog(0xDFE6E9, "Ready to receive and acknowledge signals from Computer 1 across the Relay.")

local running = true
while running do
    local ev, a, b, c, d, e, f = event.pull(0.5)

    if ev == "modem_message" then
        packetCount = packetCount + 1
        local localAddr = a
        local remoteAddr = b
        local port = c
        local distance = d
        local payload = tostring(e or "")

        local shortSrc = unicode.sub(tostring(remoteAddr), 1, 8)
        addLog(0x00D2D3, string.format("RX Packet #%d from %s (P:%d): %s", packetCount, shortSrc, port, payload))

        -- Automatic Echo / Response across the Relay
        local ackPayload = string.format("PONG_ACK [%s] RAM:%dK Up:%.0fs",
            os.date("%T"), math.floor(computer.freeMemory()/1024), computer.uptime())

        pcall(function()
            modem.send(remoteAddr, port, ackPayload)
        end)
        addLog(0x2ECC71, string.format("TX Reply sent -> %s: %s", shortSrc, ackPayload))

    elseif ev == "packet_sniffed" then
        local src = unicode.sub(tostring(a or "?"), 1, 8)
        local dst = unicode.sub(tostring(b or "?"), 1, 8)
        local port = tonumber(c) or 0
        local sz = tonumber(d) or 0
        local preview = tostring(e or "")
        addLog(0xF1C40F, string.format("[SNIFF] %s -> %s (P:%d, %dB): %s", src, dst, port, sz, preview))

    elseif ev == "key_down" then
        local char = b
        if char == 113 or char == 81 then -- 'q' or 'Q'
            running = false
        elseif char == 99 or char == 67 then -- 'c' or 'C'
            logs = {}
            if gpu then
                gpu.setBackground(0x0E1318)
                gpu.fill(1, 3, w, h - 5, " ")
            end
            addLog(0xDFE6E9, "Log buffer cleared.")
        elseif char == 112 or char == 80 then -- 'p' or 'P'
            local pingMsg = "REMOTE_NODE_HEARTBEAT_" .. math.floor(computer.uptime())
            pcall(function()
                modem.broadcast(LISTEN_PORT, pingMsg)
            end)
            addLog(0xE67E22, "TX Broadcast sent across relay: " .. pingMsg)
        end
    end
end

if gpu then
    gpu.setBackground(0x000000)
    gpu.setForeground(0xFFFFFF)
    term.clear()
end
print("Remote Relay Node terminated cleanly.")
