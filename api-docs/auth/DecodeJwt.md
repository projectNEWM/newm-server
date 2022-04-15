# Decode JWT Access Token

Allows to decode the JWT access token. This API is intended for debugging purposes.


## Request

**URL**: `/v1/auth/jwt`

**Method**: `GET`

**Query Parameters**: None

**Headers**:

`Authorization: Bearer {accessToken}`

`Accept: application/json`


## ✅ Success Response

**Code**: `200 OK`

**Headers**:

`Content-Type: application/json`

**Content**:

`id` - unique token identifier

`issuer` - always "https://projectnewm.io"

`audience` - always "[newm-server-users]"

`subject` - unique user identifier

`expiresAt` - token expiration date and time

**Content example**:

```json
{
	"id": "7bd2862f-8deb-4814-8943-156d9dab80dd",
	"issuer": "https://projectnewm.io",
	"audience": "[newm-server-users]",
	"subject": "e1997031-0dd2-471e-b288-21188a3ac590",
	"expiresAt": "Thu Apr 14 20:24:49 PDT 2022"
}
```

## ❌ Error Response

**Code**: `401 UNAUTHORIZED`

**Condition**: If `{accessToken}` is invalid or expired.


## See Also

[Login using Email/Password](LoginEmailPassword.md)

[Login using OAuth](LoginOAuth.md)

[Refresh JWT Access Token](RefreshJwt.md)

[Request 2FA Code](Request2FACode.md)
