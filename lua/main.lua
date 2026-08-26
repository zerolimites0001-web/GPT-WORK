local Browser = require("browser")
local Search = require("search")
local Theme = require("theme")
local Extensions = require("extensions")
local settings = require("settings")

local browser = Browser.new(settings)
local extensions = Extensions.new()

browser:add_tab(settings.homepage)

return {
  browser = browser,
  search = Search,
  theme = Theme,
  extensions = extensions,
  settings = settings,
}
