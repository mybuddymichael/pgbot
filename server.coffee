net = require "net"

server = net.createServer (c) ->
  console.log "server connected"
  c.on "end", ->
    console.log "server disconnected"
  c.write "hello\r\n"
  c.pipe c

server.listen 8124, ->
  console.log "server bound"
