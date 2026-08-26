local Theme = {}

Theme.presets = {
  dark = { background = "#101114", surface = "#181a20", text = "#f5f7fa", accent = "#7c9cff" },
  light = { background = "#f7f8fa", surface = "#ffffff", text = "#16181d", accent = "#315efb" },
  midnight = { background = "#070b14", surface = "#0e1524", text = "#e8f0ff", accent = "#58a6ff" },
}

function Theme.get(name)
  return Theme.presets[name] or Theme.presets.dark
end

function Theme.custom(base, overrides)
  local t = {}
  for k,v in pairs(Theme.get(base)) do t[k] = v end
  for k,v in pairs(overrides or {}) do t[k] = v end
  return t
end

return Theme
