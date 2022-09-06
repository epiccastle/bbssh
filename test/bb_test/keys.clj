(ns bb-test.keys
  (:require [pod.epiccastle.bbssh.key-pair :as key-pair]))

(def keys
  {:rsa-nopassphrase
   {:public "ssh-rsa AAAAB3NzaC1yc2EAAAADAQABAAAAgQDMWyvRMhFoVWsV6aZs8EfKk0zD8yXr029KsE5evTglpkDyxNcWfyiwqgKd5Lbe6U3DwYiYQDf1eWZtJDfFLPWP8RSvS9cjpWz0GEzNvbGXGK07KscrVlRx1KtVNaJXnh6fHaWnp26WBEVBuDhHDpa3x3a9yaBu6y7UYf4/x3If7w== crispin@vash"
    :private "-----BEGIN RSA PRIVATE KEY-----
MIICWwIBAAKBgQDMWyvRMhFoVWsV6aZs8EfKk0zD8yXr029KsE5evTglpkDyxNcW
fyiwqgKd5Lbe6U3DwYiYQDf1eWZtJDfFLPWP8RSvS9cjpWz0GEzNvbGXGK07Kscr
VlRx1KtVNaJXnh6fHaWnp26WBEVBuDhHDpa3x3a9yaBu6y7UYf4/x3If7wIDAQAB
AoGARjfho303vAj1xc7GL9KUaIgarY4D7rd1G03fb/BGtbEdyg1W9tT0r1eLlKN9
LrUt0mDSxbXzRHbVehUi0K61JYYypER9SLUWZksd2FWntdaoyI1ONwGkWwrQV7Y3
IJCLE3L8COyE+ev3qPLqQAyVl+b7QfvYbr1fgh4BbhZv7jkCQQDy0P7FB7Yw9qqv
GEzwUPQKGl9Th9aVzGn24oJatMQAzQ1XyQpFiZKUGHxzNE7N9K+tWJ0a+LssLaH1
l1Ki+nhjAkEA13OZ5B0vBESu3bdJ9NvRIMF0WvNMEYOfm1sXjuBy6XpnbP3BCb8y
pp8xNxwmd0uSNR2DoTOpFcCewZ+BM/8CBQJAK2pfA0+7rcmM/z2zFA0FdYD9pmvV
XHduQuyBLkLAAPyo9BdINOLCSKSQK/EgXgbwGmiLvrTWkrGeEdF6vxVVzQJAPN74
V663HemZziJ+zqNcTnjZuuiKUVhyu53c5g0b6kMe/XgkFfDjCphnez6Ez6eWQ1N3
YRALcY3eTK4X/uzJUQJAHynZLUihHydoXx/wN7xLo2bY5/Pky+jEpwPIJenSpxYN
AXNFDKvrZPavJbaza89ZAcjXYUCIQaxQDFM2RzISjw==
-----END RSA PRIVATE KEY-----
"
    :passphrase nil
    :fingerprint "e6:a0:ce:eb:00:0c:6d:a3:8f:b6:cb:c0:ea:47:74:74"
    :public-blob '(0 0 0 7 115 115 104 45 114 115 97 0 0 0 3 1 0 1 0 0 0 -127 0 -52 91 43 -47 50 17 104 85 107 21 -23 -90 108 -16 71 -54 -109 76 -61 -13 37 -21 -45 111 74 -80 78 94 -67 56 37 -90 64 -14 -60 -41 22 127 40 -80 -86 2 -99 -28 -74 -34 -23 77 -61 -63 -120 -104 64 55 -11 121 102 109 36 55 -59 44 -11 -113 -15 20 -81 75 -41 35 -91 108 -12 24 76 -51 -67 -79 -105 24 -83 59 42 -57 43 86 84 113 -44 -85 85 53 -94 87 -98 30 -97 29 -91 -89 -89 110 -106 4 69 65 -72 56 71 14 -106 -73 -57 118 -67 -55 -96 110 -21 46 -44 97 -2 63 -57 114 31 -17)}

   :rsa-passphrase
   {:public "ssh-rsa AAAAB3NzaC1yc2EAAAADAQABAAAAgQDKAsmXQk09tgYjA2XpYso12nb4KQmdNW8IXCvDJdcR5MBelL/ofjwp3wLGCIzCWLyFm9u83NhVsYuT0mX+vkNuv8aMH/MfCscCp2thAA+J0k7ED0DGvPvpjfCCJEnZC3RXvwY8fFFVY2VLQOGQAKzelE3RNad5ainmMt67bsAhnQ== crispin@vash"
    :private "-----BEGIN RSA PRIVATE KEY-----
Proc-Type: 4,ENCRYPTED
DEK-Info: AES-128-CBC,C5DB67120CFF07A2D84AD3B7AFED82E7

4UZWgkawc/vATHfYBxH/zjda+gs0m5q8dB0F1eedgAGZ7+TlAMASjoeAwNiYBm+o
MRWxS5UwL5z1Xq4Tc4zD6izJ3A2nEkwgdmiEpbqP55wsMsx+/3Js8SI21WaTsBSO
wVMgEKM7owJhlFnJ4qbKrdFv4b/6wh3E6z8az6LWz08F0Ohu1sZuvk2fZtrDE/ZN
xGlJJgiswio2i80ewiaKQxcrnTZMYhh31PZPHBqz93KQzDSmKrbUA8RyO5BGHfsX
fMTt9LXgbhCKovC2PUKIOgCOwdiMtyLV0+uGMbIBZE860li5gQsj+6jfX+PsK+vM
w73obmVNQ9wONqhVxZRJ+MotRrtu9k7eeVpBsRO8Xm0DGAPVoeLnPItfBFLV3h+U
nBiS/cUNJH9BOrhWIz5bWsCMiuSF52EJ2zv0UIMpVuIxb93o33yQF1FUCV4uDTCp
77CT9oagqdRBztdnZahn0nisxAZcQCD0rkBrwoIJSJxRBkeGcNgqcB4Df95z9RWw
8ELgvKGrWTZQM/BcPvclKf0gTBybY9vCQ/0b+3XXatq2TGLpWVepOhZGSBLFO8MW
3vQvAd3UYRdNwbGHBbMCOuY635ZlqlrJPN5xDCCxme6cFLFzH6/Ck6XAKHQT6kc4
RfFjl4EIiVJeQa/miCIo6U8myIcnn9B8ScCb7AGTG9w3Sa5L78JuOmtSSExaC/DD
vLSWEnadI7YhaJsz6n3xZiiWmdsMAbzFV8nTaBpUvIyQtHjKUsAhFESjuVTDudxP
v/EIaz+MRNrAP9q22qOK+PnN1vzRZgdVAh7+j9XXSHg=
-----END RSA PRIVATE KEY-----
"
    :passphrase "passphrase"
    :fingerprint "58:a5:65:1c:8c:12:c9:6e:67:aa:70:e6:11:e3:c3:10"
    :public-blob '(0 0 0 7 115 115 104 45 114 115 97 0 0 0 3 1 0 1 0 0 0 -127 0 -54 2 -55 -105 66 77 61 -74 6 35 3 101 -23 98 -54 53 -38 118 -8 41 9 -99 53 111 8 92 43 -61 37 -41 17 -28 -64 94 -108 -65 -24 126 60 41 -33 2 -58 8 -116 -62 88 -68 -123 -101 -37 -68 -36 -40 85 -79 -117 -109 -46 101 -2 -66 67 110 -65 -58 -116 31 -13 31 10 -57 2 -89 107 97 0 15 -119 -46 78 -60 15 64 -58 -68 -5 -23 -115 -16 -126 36 73 -39 11 116 87 -65 6 60 124 81 85 99 101 75 64 -31 -112 0 -84 -34 -108 77 -47 53 -89 121 106 41 -26 50 -34 -69 110 -64 33 -99)
    }
   })

(defn create-key-pair [agent key-id]
  (spit "/tmp/bbssh-test-key" (get-in keys [key-id :private]))
  (spit "/tmp/bbssh-test-key.pub" (get-in keys [key-id :public]))
  (key-pair/load agent "/tmp/bbssh-test-key" "/tmp/bbssh-test-key.pub"))
