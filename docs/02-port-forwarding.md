# Connection Forwarding

TCP connection forwarding can be setup manually on a ssh session or initially during the construction. Port forwards, both local and remote, can be setup, deleted and modified during a connection without closing the connection.

## Local Port Forwarding

Local port forwarding will bind to a port on the local machine and forward any incoming connections over the ssh connection and then connect outward to any machine reachable from the remote machine on the specified port.

### Manual Setup

Once a session is connected, use `session/set-port-forwarding-local` to configure a local TCP port to be forwarded to the remote host and then connect out to the remote network on a specified port:

```clojure
(session/set-port-forwarding-local
  session
  {:bind-address "0.0.0.0"        ;; address to bind to on the local machine
   :local-port 8080               ;; the port to lisen on locally
   :remote-host "remote-machine"  ;; host to connect to on the remote network
   :remote-port 80                ;; port to connect to on remote-machine
   :connect-timeout 30000         ;; timeout for the remote connection (ms)
   })
```

`:bind-address` is optional. If not specified this defaults to 127.0.0.1. `:connect-timeout` specifies the time in milliseconds to attempt to open the connection on the remote end.

Now whenever you open a connection to your local machine on port `8080`, the remote machine will attempt to open a TCP connection to `remote-machine` on port `80`. It will try for 30 seconds to establish this connection. When the connection is established all traffic between this new local connection and the remote-machine will be proxied back and forth.

Any number of simultaneous connections can be proxied through this port forward.

If you no longer want the local port forward you can close it at any time with `session/delete-port-forwarding-local`.

```clojure
(session/delete-port-forwarding-local
  session
  {:bind-address "0.0.0.0"        ;; optional.
   :local-port 8080               ;; the local port number
   })
```

You can gather a summary of all the local port forwards setup for this session with `session/get-port-forwarding-local`. It will return a vector of all the present local port forwards.

```clojure
(session/get-port-forwarding-local session)
;; => [{:local-port 8080, :remote-host "remote-machine", :remote-port 80}]
```

### Connection Setup

You can setup the local port forwarding when initiating the connection

```clojure
(bbssh/ssh "remotehost"
           {:username "remoteusername"
            :port 22
            :port-forward-local [{:bind-address "0.0.0.0"
                                  :local-port 8080
                                  :remote-host "remote-machine"
                                  :remote-port 80
                                  :connect-timeout 30000}]})
```

## Remote Port Forwarding

Remote port forwarding will bind to a port on the remote machine and forward any incoming connections back over the ssh connection and then connect outward to any machine reachable from the local machine on the specified port.

### Manual Setup

Once a session is connected, use `session/set-port-forwarding-remote` to configure a remote TCP port to be forwarded to the local host and then connect out to the local network on a specified port:

```clojure
(session/set-port-forwarding-remote
  session
  {:bind-address "0.0.0.0"        ;; address to bind to on the remote machine
   :remote-port 8080              ;; the port to listen on remotely
   :local-host "local-machine"   ;; host to connect to on the local network
   :local-port 80                 ;; port to connect to on local-machine
   })
```

`:bind-address` is optional. If not specified this defaults to 127.0.0.1.

Now whenever you open a connection to your remote machine on port `8080`, the local machine will attempt to open a TCP connection to `local-machine` on port `80`. When the connection is established all traffic between this new remote connection and the local-machine will be proxied back and forth.

Any number of simultaneous connections can be proxied through this port forward.

If you no longer want the remote port forward you can close it at any time with `session/delete-port-forwarding-remote`.

```clojure
(session/delete-port-forwarding-remote
  session
  {:bind-address "0.0.0.0"        ;; optional.
   :remote-port 8080              ;; the remote port number
   })
```

You can gather a summary of all the remote port forwards setup for this session with `session/get-port-forwarding-remote`. It will return a vector of all the present remote port forwards.

```clojure
(session/get-port-forwarding-remote session)
;; => [{:remote-port 8080, :local-host "local-machine", :local-port 80}]
```

### Connection Setup

You can setup the remote port forwarding when initiating the connection

```clojure
(bbssh/ssh "remotehost"
           {:username "remoteusername"
            :port 22
            :port-forward-remote [{:bind-address "0.0.0.0"
                                   :remote-port 8080
                                   :local-host "local-machine"
                                   :local-port 80}]})
```
