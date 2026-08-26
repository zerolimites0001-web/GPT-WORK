local Search = {}

Search.providers = {
  google = {
    name = "Google",
    build = function(q) return "https://www.google.com/search?q=" .. q end,
  },
  duckduckgo = {
    name = "DuckDuckGo",
    build = function(q) return "https://duckduckgo.com/?q=" .. q end,
  },
}

function Search.build(provider, query)
  local p = Search.providers[provider] or Search.providers.google
  return p.build(query)
end

function Search.list()
  local out = {}
  for id, p in pairs(Search.providers) do out[#out + 1] = { id = id, name = p.name } end
  table.sort(out, function(a,b) return a.id < b.id end)
  return out
end

return Search
