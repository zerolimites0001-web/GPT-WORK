local Browser = {}
Browser.__index = Browser

function Browser.new(storage)
  return setmetatable({
    tabs = {},
    active_tab = 1,
    history = {},
    bookmarks = {},
    settings = storage or {},
  }, Browser)
end

function Browser:add_tab(url)
  local tab = { url = url or self.settings.homepage or "https://www.google.com", title = "New Tab" }
  self.tabs[#self.tabs + 1] = tab
  self.active_tab = #self.tabs
  return tab
end

function Browser:current_tab()
  return self.tabs[self.active_tab]
end

function Browser:navigate(url)
  local tab = self:current_tab() or self:add_tab()
  tab.url = url
  self.history[#self.history + 1] = url
  return url
end

function Browser:close_tab(index)
  if #self.tabs <= 1 then return false end
  table.remove(self.tabs, index or self.active_tab)
  self.active_tab = math.max(1, math.min(self.active_tab, #self.tabs))
  return true
end

function Browser:add_bookmark(url, title)
  self.bookmarks[#self.bookmarks + 1] = { url = url, title = title or url }
end

return Browser
