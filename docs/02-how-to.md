# How To

## Connect to a machine and execute a command

```
(
```

You can hard code a password in the options hash.

> **Note:** This is not recommended. You may accidentally commit your code to a repository with the password or inadvertantly expose it.

```
(let [session (bbssh/ssh "remotehost"
                         {:username "remoteusername"
                          :port 22
                          :password "thepassword"})]
  ;; use session
  )
```

If the key is encypted and no passphrase is available, the user will be prompted for one on the terminal.

You may hard code
