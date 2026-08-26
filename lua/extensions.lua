local Extensions = {}
Extensions.__index = Extensions

function Extensions.new()
  return setmetatable({ list = {}, hooks = {} }, Extensions)
end

function Extensions:register(extension)
  assert(extension and extension.id, "extension.id is required")
  self.list[extension.id] = extension
  return extension.id
end

function Extensions:on(hook, fn)
  self.hooks[hook] = self.hooks[hook] or {}
  self.hooks[hook][#self.hooks[hook] + 1] = fn
end

function Extensions:emit(hook, payload)
  for _, fn in ipairs(self.hooks[hook] or {}) do fn(payload) end
end

return Extensions
