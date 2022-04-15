# Request Two-Factor Authentication (2FA) Code

Allows to request a 2FA code (aka verification code) to be send via email.


## Request

**URL**: `/v1/auth/code`

**Method**: `GET`

**Query Parameters**:

`email` - valid email to send the 2FA code to (required).

**Headers**: None


## ✅ Success Response

**Code**: `204 NO CONTENT`

**Headers**: None

## ❌ Error Response

**Code**: `400 BAD REQUEST`

**Condition**: If missing `email`.

**Headers**:

`Content-Type: application/json`

**Content example**:

```json
{
    "code": 400,
    "description": "Bad Request",
    "cause": "Missing query param: email"
}
```

## See Also

[Login using Email/Password](LoginEmailPassword.md)

[Login using OAuth](LoginOAuth.md)

[Refresh JWT Access Token](RefreshJwt.md)

[Decode JWT Access Token](DecodeJwt.md)
