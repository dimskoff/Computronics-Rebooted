-- Computronics Rebooted: Pocket Terminal Client Library
local component = require("component")
local m = component.isAvailable("modem") and component.modem or nil

local pocket = {
    PORT = 404,
    WIDTH = 30,
    HEIGHT = 10
}

function pocket.send(termId, cmd, ...)
    if not m then return false, "no modem component available" end
    if not m.isWireless() then return false, "modem must be wireless" end
    m.broadcast(pocket.PORT, "pt", termId or "all", cmd, ...)
    return true
end

function pocket.clear(termId)
    return pocket.send(termId, "clear")
end

function pocket.print(termId, text)
    return pocket.send(termId, "print", tostring(text or ""))
end

function pocket.set(termId, x, y, text)
    return pocket.send(termId, "set", math.floor(x or 1), math.floor(y or 1), tostring(text or ""))
end

function pocket.fill(termId, x, y, w, h, ch)
    return pocket.send(termId, "fill", math.floor(x or 1), math.floor(y or 1), math.floor(w or 1), math.floor(h or 1), tostring(ch or " "))
end

function pocket.color(termId, fg, bg)
    return pocket.send(termId, "color", fg or 0x00FF66, bg or 0x08150A)
end

function pocket.title(termId, title)
    return pocket.send(termId, "title", tostring(title or "POCKET TERMINAL"))
end

function pocket.beep(termId, pitch)
    return pocket.send(termId, "beep", tonumber(pitch) or 1.0)
end

-- Virtual GPU-like device wrapper
function pocket.wrap(termId)
    local dev = {
        id = termId or "all"
    }
    function dev.getResolution() return pocket.WIDTH, pocket.HEIGHT end
    function dev.clear() return pocket.clear(dev.id) end
    function dev.print(text) return pocket.print(dev.id, text) end
    function dev.set(x, y, text) return pocket.set(dev.id, x, y, text) end
    function dev.fill(x, y, w, h, ch) return pocket.fill(dev.id, x, y, w, h, ch) end
    function dev.setColor(fg, bg) return pocket.color(dev.id, fg, bg) end
    function dev.setTitle(t) return pocket.title(dev.id, t) end
    function dev.beep(p) return pocket.beep(dev.id, p) end
    return dev
end

return pocket
