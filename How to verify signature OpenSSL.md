## How to verify signature with OpenSSL

### given:
 - data.file
 - crt.crt
 - sign.64

### convert:
`openssl x509 -in crt.crt -pubkey -noout > crt.pem`

`base64 -d sign.64 > sign.bin`

### verify:
`openssl dgst -sha256 -verify crt.pem -signature sign.bin data.file`
