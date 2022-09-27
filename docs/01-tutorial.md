# Tutorial

## Creating a session

To create a session use `bbssh.core/ssh`:

```clojure
(let [session (bbssh.core/ssh "hostname" {:username "username"})]
  ...
)
```
